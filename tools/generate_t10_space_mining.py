"""Snapshot GTNL dev-290's space-mining output declarations without their quantities."""
import json
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
revision = "f93edbe8c425e2db93c089e7bc7a689b014b7755"
path = "src/main/java/com/science/gtnl/common/recipe/gtnl/SpaceMinerRecipes.java"
source = subprocess.check_output(
    ["git", "-C", str(root / "ref/GT-Not-Leisure-filtered"), "show", f"{revision}:{path}"], text=True
)
section = source[source.index("List<OreGroup> oreGroups"):source.index("for (OreGroup group")]
groups = re.split(r"\bnew OreGroup\(", section)[1:]
entries = []
pattern = re.compile(
    r"GTOreDictUnificator\.get\(OrePrefixes\.(\w+),\s*Materials\.(\w+),\s*\d+\)"
    r"|WerkstoffLoader\.(\w+)\.get\(OrePrefixes\.(\w+),\s*\d+\)"
    r"|GGMaterial\.(\w+)\.get\(OrePrefixes\.(\w+),\s*\d+\)"
    r"|(MaterialsOres|MaterialsAlloy|MaterialMisc)\.(\w+)\.getOre\(\d+\)"
    r"|new ItemStack\(Blocks\.(\w+),\s*\d+\)"
    r"|ModsItemlist\.(\w+)\.get\(\d+\)"
    r"|aeBlocks\.skyStone\(\)\s*\.maybeStack\(\d+\)\s*\.orNull\(\)"
)
for group in groups:
    matches = list(pattern.finditer(group))
    if len(matches) != 9:
        raise ValueError(f"Expected 9 outputs per group, found {len(matches)}")
    for match in matches:
        p, material, werkstoff, wp, gg, gp, owner, gtpp, block, moditem = match.groups()
        if material:
            entry = {"kind": "gt", "prefix": p, "material": material}
        elif werkstoff or gg:
            entry = {"kind": "werkstoff" if werkstoff else "goodgenerator", "prefix": wp or gp, "material": werkstoff or gg}
        elif gtpp:
            entry = {"kind": owner, "material": gtpp}
        else:
            ids = {"clay": "minecraft:clay", "HardcoreEnderExpansionEndPowderOre": "HardcoreEnderExpansion:end_powder_ore"}
            entry = {"kind": "item", "id": ids[block or moditem] if block or moditem else "appliedenergistics2:tile.BlockSkyStone", "meta": 0}
        if entry not in entries:
            entries.append(entry)
if len(groups) != 31:
    raise ValueError(f"Expected 31 groups, found {len(groups)}")
target = root / "src/main/resources/assets/gtnhadjust/recipes/t10_space_mining.json"
target.write_text(json.dumps({"sourceCommit": revision, "outputs": entries}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"{len(groups)} groups, {len(entries)} distinct symbolic outputs")
