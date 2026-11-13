"""Snapshot TST ore-processing products for the 39 newly identified T10 ores.

The ore dictionary snapshot supplies the item forms registered by the 2.9.0 instance.
TST's OP_Logic and GT material declarations define which forms and byproducts to retain.
"""
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
instance = Path(r"D:\Game\PrismLauncher-Windows-MinGW-w64-Portable-11.1.1\instances\GT_New_Horizons_2.9.0_Java_17-26\.minecraft")
registry = json.loads((instance / "config/gtnhadjust/singularity-registries.json").read_text(encoding="utf8"))["solidMaterials"]
source = (root / "ref/GT5-Unofficial/src/main/java/gregtech/loaders/materials/MaterialsInit.java").read_text(encoding="utf8")
assignments = dict(re.findall(r"Materials\.(\w+)\s*=\s*(load\w+)\(\)", source))
ores = """Indium ElectrumFlux Tellurium Manyullyn Shadow Ytterbium SiliconSG Phosphate
Magnesium Europium Strontium Endium Cheese Barium Rubidium Americium Thulium Tantalum
Lutetium Holmium Ichorium Promethium Praseodymium Scandium Dysprosium Tritanium DarkIron
Duralumin Forcicium Forcillium Electrum Flerovium""".split()
extra = [("werkstoff", "PTMetallicPowder", "PlatinumMetallicPowder"),
         ("werkstoff", "Fluorspar", "Fluorspar"), ("goodgenerator", "orundum", "Orundum"),
         ("MaterialsAlloy", "KOBOLDITE", "Koboldite"),
         ("MaterialMisc", "RARE_EARTH_LOW", "RareEarthI"),
         ("MaterialMisc", "RARE_EARTH_MID", "RareEarthII"),
         ("MaterialMisc", "RARE_EARTH_HIGH", "RareEarthIII")]
assert len(ores) + len(extra) == 39

def material_info(name):
    method = assignments[name]
    m = re.search(r"private static Materials\s+" + method + r"\(\)\s*\{(.*?)\n    \}", source, re.S)
    if not m:
        raise ValueError(f"Missing material initializer: {name}")
    body = m.group(1)
    key = re.search(r'\.setName\("([^"]+)"\)', body)
    if not key:
        raise ValueError(f"Missing registry name: {name}")
    byproducts = re.findall(r"\.addOreByproduct\(\(\) -> Materials\.(\w+)\)", body)
    return key.group(1), byproducts

def stack(kind, material, prefix):
    return {"kind": kind, "material": material, "prefix": prefix}

def exists(prefix, key):
    return bool(registry.get(prefix + key))

records = []
for name in ores:
    key, byproducts = material_info(name)
    if not exists("ore", key):
        raise ValueError(f"Unregistered ore {name}: {key}")
    dusts = []
    for material in [name] + [x for x in byproducts if x not in ("Netherrack", "Endstone", "Stone")]:
        special = {"Platinum": "PTMetallicPowder", "Palladium": "PDMetallicPowder",
                   "Iridium": "IrLeachResidue", "Osmium": "IrOsLeachResidue"}
        if material in special:
            entry = stack("werkstoff", special[material], "dust")
        else:
            material_key, _ = material_info(material)
            if not exists("dust", material_key):
                continue
            entry = stack("gt", material, "dust")
        if entry not in dusts:
            dusts.append(entry)
    gems = []
    if exists("gem", key):
        for prefix in (["gemExquisite", "gemFlawless", "gem"] if exists("gemExquisite", key) else ["gem"]):
            if exists(prefix, key):
                gems.append(stack("gt", name, prefix))
    records.append({"ore": name, "kind": "gt", "dust": dusts, "gem": gems})

for kind, name, key in extra:
    if not exists("ore", key) or not exists("dust", key):
        raise ValueError(f"Missing ore or dust for {name}")
    gems = []
    if kind in ("werkstoff", "goodgenerator") and exists("gem", key):
        for prefix in (["gemExquisite", "gemFlawless", "gem"] if exists("gemExquisite", key) else ["gem"]):
            if exists(prefix, key):
                gems.append(stack(kind, name, prefix))
    records.append({"ore": name, "kind": kind, "dust": [stack(kind, name, "dust")], "gem": gems})

target = root / "src/main/resources/assets/gtnhadjust/recipes/t10_ore_processing.json"
target.write_text(json.dumps({"source": "TST OreProcessingVisualRecipeMap; GTNH 2.9.0 registry snapshot", "ores": records}, ensure_ascii=False, indent=2) + "\n", encoding="utf8")
print(f"{len(records)} ore records; {sum(len(x['dust']) for x in records)} dust entries; {sum(len(x['gem']) for x in records)} gem entries")
