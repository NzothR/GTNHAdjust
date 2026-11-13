"""Snapshot ingot and molten-fluid forms for the T10 ore-processing dusts."""
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
instance = Path(r"D:\Game\PrismLauncher-Windows-MinGW-w64-Portable-11.1.1\instances\GT_New_Horizons_2.9.0_Java_17-26\.minecraft")
registry = json.loads((instance / "config/gtnhadjust/singularity-registries.json").read_text(encoding="utf8"))
source = (root / "ref/GT5-Unofficial/src/main/java/gregtech/loaders/materials/MaterialsInit.java").read_text(encoding="utf8")
assignments = dict(re.findall(r"Materials\.(\w+)\s*=\s*(load\w+)\(\)", source))
data = json.loads((root / "src/main/resources/assets/gtnhadjust/recipes/t10_ore_processing.json").read_text(encoding="utf8"))
entries = {(item["kind"], item["material"]): item for record in data["ores"] for item in record["dust"]}
excluded = {"Phosphorus", "Olivine", "Forcicium", "Forcillium", "PTMetallicPowder", "Fluorspar", "RareEarth", "RARE_EARTH_LOW", "RARE_EARTH_MID", "RARE_EARTH_HIGH"}
other_names = {"orundum": "Orundum", "KOBOLDITE": "Koboldite"}
fluids = set(registry["fluids"])
rows = []
for (kind, material), _ in entries.items():
    if material in excluded:
        continue
    if material in assignments:
        method = assignments[material]
        match = re.search(r"private static Materials\s+" + method + r"\(\)\s*\{(.*?)\n    \}", source, re.S)
        if not match:
            raise ValueError(f"Missing material initializer {material}")
        key = re.search(r'\.setName\("([^"]+)"\)', match.group(1)).group(1)
    else:
        key = other_names[material]
    ingots = registry["solidMaterials"].get("ingot" + key, [])
    # The 2.9.0 wiremill recipe shown in NEI uses the GT Manyullyn ingot.
    if material == "Manyullyn":
        selected = "gregtech:gt.metaitem.01:11386"
        if selected not in ingots:
            raise ValueError("The verified GT Manyullyn ingot is not registered")
        ingots = [selected]
    item = None
    if ingots:
        item_id, meta = ingots[0].rsplit(":", 1)
        item = {"id": item_id, "meta": int(meta)}
    fluid = "molten." + key.lower()
    rows.append({"material": material, "ingot": item, "fluid": fluid if fluid in fluids else None})

output = {"source": "GTNH 2.9.0 instance ore-dictionary and fluid registry snapshot", "materials": rows}
target = root / "src/main/resources/assets/gtnhadjust/recipes/t10_material_forms.json"
target.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf8")
print(f"{len(rows)} checked; {sum(x['ingot'] is not None for x in rows)} ingots; {sum(x['fluid'] is not None for x in rows)} fluids")
print("Missing:", [(x["material"], x["ingot"] is None, x["fluid"] is None) for x in rows if x["ingot"] is None or x["fluid"] is None])
