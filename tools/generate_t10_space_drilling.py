"""Extract the fixed GTNL space-drilling fluid output declarations."""
import json
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
revision = "f93edbe8c425e2db93c089e7bc7a689b014b7755"
path = "src/main/java/com/science/gtnl/common/recipe/gtnl/SpaceDrillRecipes.java"
source = subprocess.check_output(
    ["git", "-C", str(root / "ref/GT-Not-Leisure-filtered"), "show", f"{revision}:{path}"], text=True
)
section = source[source.index("List<List<FluidStack>> tierOutputFluids"):source.index("List<MinerTierData> minerTiers")]
section += source[source.index("        addSpaceDrillOutputRecipe(\n            23,"):]
pattern = re.compile(
    r"Materials\.(\w+)\.(getGas|getFluid|getMolten)\(\d+\)"
    r"|WerkstoffLoader\.(\w+)\.getFluidOrGas\(\d+\)"
    r"|FluidRegistry\.getFluidStack\(\"([^\"]+)\",\s*\d+\)"
    r"|new FluidStack\(GTPPFluids\.(\w+),\s*\d+\)"
    r"|new FluidStack\(MaterialsElements\.getInstance\(\)\.(\w+)\.getFluid\(\),\s*\d+\)"
    r"|GTModHandler\.(getDistilledWater|getLiquidDNA)\(\d+\)"
)
entries = []
for match in pattern.finditer(section):
    material, method, werkstoff, fluid, gtpp, element, handler = match.groups()
    if material:
        entry = {"kind": "gt", "material": material, "method": method}
    elif werkstoff:
        entry = {"kind": "werkstoff", "material": werkstoff}
    elif fluid:
        entry = {"kind": "fluid", "id": fluid}
    elif gtpp:
        entry = {"kind": "gtpp", "material": gtpp}
    elif element:
        entry = {"kind": "element", "material": element}
    else:
        entry = {"kind": "handler", "method": handler}
    if entry not in entries:
        entries.append(entry)
if len(entries) != 43:
    raise ValueError(f"Expected 43 distinct output declarations, found {len(entries)}")
target = root / "src/main/resources/assets/gtnhadjust/recipes/t10_space_drilling.json"
target.write_text(json.dumps({"sourceCommit": revision, "outputs": entries}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"{len(entries)} distinct symbolic fluid outputs")
