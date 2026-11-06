"""Generate a static GTNR 0.1.29 recipe snapshot; never reads a running game's recipe maps."""
import collections
import json
from pathlib import Path

SOURCE = Path('ref/GT-Never-Reset/src/main/resources/assets/gtneverreset/recipes/fragment_world_collection.json')
DEST = Path('src/main/resources/assets/gtnhadjust/recipes/super_fragment_world.json')
DIMENSIONS = ['DimensionOverworld', 'DimensionNether', 'DimensionEnd', 'DimensionTwilight', 'DimensionMoon', 'DimensionMars', 'DimensionVenus', 'DimensionMercury', 'DimensionCeres', 'DimensionIo', 'DimensionGanymede', 'DimensionPluto', 'DimensionEnceladus', 'DimensionTitan', 'DimensionTriton', 'DimensionBarnardC']
ROCKS = ['MoonStone', 'MarsStone', 'VenusRock', 'MercuryRock', 'CeresRock', 'IoStone', 'GanymedeRock', 'PlutoStone', 'EnceladusRock', 'TitanStone', 'TritonStone']
ALIASES = {'Goethite': 'BrownLimonite', 'Hematite': 'BandedIron', 'Plutonium239': 'Plutonium'}


def add(outputs, kind, name, amount, chance=10000, meta=0):
    key = (kind, name, meta)
    if key in outputs:
        outputs[key]['amount'] += amount
        outputs[key]['chance'] = max(outputs[key]['chance'], chance)
    else:
        outputs[key] = dict(kind=kind, name=name, meta=meta, amount=amount, chance=chance)


def add_special(dim, out, fluids):
    if dim == 0:
        for name, qty, chance in [('dirt',16,6000),('gravel',16,4000),('sand',16,3000)]: add(out, 'block', name, qty, chance)
        add(out, 'item', 'clay_ball', 64, 2000)
        for meta in range(4): add(out, 'block', 'sapling', 8, 2000, meta)
        for name in ['CherrySapling','MangrovePropagule']: add(out, 'mod', name, 8, 2000)
        for name,qty,chance in [('reeds',8,2000),('leather',4,500),('string',8,500)]: add(out,'item',name,qty,chance)
        add(out,'ic2','rubberSapling',4,1000)
        for name,qty,chance in [('Honeycomb',1,2000),('TotemOfUndying',1,10)]: add(out,'mod',name,qty,chance)
        add(out,'block','soul_sand',4,5)
        fluids.extend([dict(name='lava',amount=1000,chance=500),dict(name='RawOil',amount=1000,chance=2000)])
        return 2
    if dim == 1:
        for name,qty,chance in [('soul_sand',16,6000)]: add(out,'block',name,qty,chance)
        for name,qty,chance in [('SoulSoil',16,3000),('AncientDebris',4,500),('CrimsonFungus',8,2000),('WarpedFungus',8,2000)]: add(out,'mod',name,qty,chance)
        for name,qty,chance in [('nether_wart',12,200),('blaze_rod',8,500)]: add(out,'item',name,qty,chance)
        fluids.append(dict(name='lava',amount=8000,chance=5000)); return 1
    if dim == 2:
        add(out,'block','dragon_egg',1,5)
        for name,qty,chance in [('DragonBreath',1,500),('ShulkerShell',8,2000),('ChorusFruit',16,4000),('ChorusFlower',1,500)]: add(out,'mod',name,qty,chance)
        return 1
    if dim == 3:
        for name in ['dirt','sand','gravel']: add(out,'block',name,16,6000)
        for name in ['Diorite','Andesite','Granite','RedGranite','Marble']: add(out,'mod',name,16,6000)
        add(out,'mod','SkyStone',16,500); add(out,'dust','DamascusSteel',1)
        return 2
    if dim == 14: add(out,'dust','Ice',1,9500); return 1
    if dim == 15:
        add(out,'mod','BarnardaCLog',1,500); add(out,'mod','BarnardaCLeaves',1,9500)
        fluids.append(dict(name='BarnardaAir',amount=16000,chance=2000)); return 1
    return 0


def main():
    source = json.loads(SOURCE.read_text(encoding='utf8'))
    result = []
    for dimension, name in enumerate(DIMENSIONS):
        ores, count_fluids = source['dimensionCounts'][dimension]
        mapping = source['dimensionMaps'][dimension]
        out = collections.OrderedDict()
        duration = 0
        for vein in mapping[:ores]:
            duration += 24000 // source['oreCounts'][vein][4]
            for ore, amount in zip(source['ores'][vein], source['oreCounts'][vein][:4]):
                add(out,'ore',ALIASES.get(ore,ore),amount)
            if dimension == 0:
                for kind,n in [('block','stone'),('dust','Stone'),('mod','Deepslate'),('stoneDust','Deepslate')]: add(out,kind,n,32)
            elif dimension == 1:
                for kind,n in [('block','netherrack'),('dust','Netherrack'),('mod','Basalt'),('dust','Basalt')]: add(out,kind,n,32)
            elif dimension == 2:
                add(out,'block','end_stone',64); add(out,'dust','Endstone',64)
            elif dimension == 3:
                add(out,'mod','Diorite',64); add(out,'oreDust','Diorite',64)
            elif dimension < 15: add(out,'mod',ROCKS[dimension-4],64)
            else: add(out,'block','stone',64)
        fluids = []
        extra = add_special(dimension,out,fluids)
        recipes = ores + extra
        if recipes:
            # Twilight's Damascus-steel recipe has no dimension-marker output.
            add(out,'mod',name,recipes - (1 if dimension == 3 else 0),50)
            result.append(dict(dimension=name,mode='items',duration=duration+200*extra,eut=8*recipes,
                outputs=list(out.values()),fluids=fluids,extraSteelAndLubricant=dimension==3))
        for drill, multiplier in enumerate([1,16,128,1024,0]):
            if not count_fluids: continue
            fluids = [dict(name=source['fluids'][f],amount=(2147483647 if drill==4 else source['fluidAmounts'][f]*multiplier),chance=10000) for f in mapping[ores:ores+count_fluids]]
            result.append(dict(dimension=name,mode='fluids',drill=['Steel','Titanium','NaquadahAlloy','Neutronium','Infinity'][drill],
                drillChance=100-drill*10,duration=200*count_fluids,eut=8*count_fluids,
                outputs=[dict(kind='mod',name=name,meta=0,amount=count_fluids,chance=50)],fluids=fluids))
    DEST.parent.mkdir(parents=True,exist_ok=True)
    DEST.write_text(json.dumps(dict(source='GT-Never-Reset 0.1.29',recipes=result),ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('recipes',len(result),'max items',max(len(r['outputs']) for r in result),'max fluids',max(len(r['fluids']) for r in result))


if __name__ == '__main__': main()
