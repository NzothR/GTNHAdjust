"""Extract GT-Not-Hard's weighted Singularity lists as fixed symbolic data."""
from pathlib import Path
import re, json

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'ref/GT-Not-Hard/src/main/java'
MODS=ROOT/'ref/GT5-Unofficial/src/main/java/gregtech/api/enums/Mods.java'
t=MODS.read_text(encoding='utf8')
constants=dict(re.findall(r'\b(\w+)\s*=\s*"([^"]+)"',t))
mods={n:constants[k] for n,k in re.findall(r'\b(\w+)\(ModIDs\.(\w+)\)',t) if k in constants}
classes={}
for p in list((SRC/'EnumList/EnumItemList/EnumOreProductList').glob('*.java'))+list(SRC.glob('Recipes/SingularityRecipes_Void*/*.java')):
 if p.name.startswith('Fake'): continue
 s=re.sub(r'//[^\n]*','',p.read_text(encoding='utf8'))
 cls=p.stem
 for typ,arr,name,expr in re.findall(r'\b(ItemStack|FluidStack)(\[\])?\s+(\w+)\s*=\s*(.*?);',s,re.S):
  classes[(cls,name)]=(typ,bool(arr),expr.strip())

def split(s):
 result=[];start=0;dep=0;string=False
 for i,c in enumerate(s):
  if c=='"':string=not string
  if not string:
   if c in '({[':dep+=1
   elif c in ')}]':dep-=1
   elif c==',' and dep==0:result.append(s[start:i].strip());start=i+1
 if s[start:].strip():result.append(s[start:].strip())
 return result

def calc(expr,cls):
 expr=expr.strip()
 m=re.fullmatch(r'getModItem\(\s*(\w+)\.ID\s*,\s*"([^"]+)"\s*,\s*(\d+)L\s*(?:,\s*(\d+)\s*)?\)',expr,re.S)
 if m:
  mod,name,amount,meta=m.groups()
  if mod not in mods: raise ValueError('mod '+mod)
  return [{'kind':'item','id':mods[mod]+':'+name,'meta':int(meta or 0)}]
 m=re.fullmatch(r'(?:Materials\.(\w+)\.(getMolten|getFluid|getGas)|WerkstoffLoader\.(\w+)\.(getFluidOrGas|getMolten|getFluid|getGas))\(\s*\d+\s*\)',expr,re.S)
 if m:
  mat,method,bw,bwm=m.groups();return [{'kind':'materialFluid' if mat else 'werkstoffFluid','material':mat or bw,'method':method or bwm}]
 m=re.fullmatch(r'FluidRegistry\.getFluidStack\(\s*"([^"]+)"\s*,\s*\d+\s*\)',expr,re.S)
 if m:return [{'kind':'fluid','id':m.group(1)}]
 m=re.fullmatch(r'new\s+(?:ItemStack|FluidStack)\[\]\s*\{(.*)\}',expr,re.S)
 if m:return sum((calc(a,cls) for a in split(m.group(1))),[])
 m=re.fullmatch(r'(addAggregateArray|addCompressAggregateArray|checkItemStack|checkFluidStack)\((.*)\)',expr,re.S)
 if m:
  result=sum((calc(a,cls) for a in split(m.group(2))),[])
  if m.group(1)=='addCompressAggregateArray':
   seen=set(); result=[x for x in result if not ((k:=json.dumps(x,sort_keys=True)) in seen or seen.add(k))]
  return result
 m=re.fullmatch(r'(?:(\w+)\.)?(\w+)',expr)
 if m:
  target,name=m.groups();key=(target or cls,name)
  if key not in classes:raise ValueError('missing '+str(key))
  return calc(classes[key][2],key[0])
 raise ValueError('unsupported '+cls+': '+expr[:160])

result={};tiers={};errors=[]
for mode in ['Ore','Gem','Dust','Ingot','Fluid']:
 p=SRC/f'Recipes/SingularityRecipes_Void{mode}/Singularity{mode}Recipes.java'
 text=re.sub(r'//[^\n]*','',p.read_text(encoding='utf8'))
 maps=re.findall(r'Void\w+Recipes_T(\d+)\.put\("(\w+)",\s*(\w+)\)',text)
 for tier,dimension,var in maps:
  tier=int(tier)
  if tier == 10: continue
  if dimension in tiers and tiers[dimension] != tier: raise ValueError(f'Inconsistent tier for {dimension}')
  tiers[dimension]=tier
  try:result.setdefault(dimension,{})[mode.lower()]=calc(var,p.stem)
  except Exception as e:errors.append((mode,dimension,str(e)))
if errors:
 for e in errors:print(e)
 raise SystemExit(f'{len(errors)} extraction errors')
out=ROOT/'src/main/resources/assets/gtnhadjust/recipes/gtnothard_singularity.json'
if set(tiers) != set(result): raise ValueError('Dimension tier mappings are incomplete')
out.write_text(json.dumps({'sourceCommit':'e8b2133fe08e7182bfa0316bff43379c132218f7','tiers':tiers,'dimensions':result},ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('dimensions',len(result),'pools',sum(len(v) for v in result.values()),'max',max(len(v) for modes in result.values() for v in modes.values()))
