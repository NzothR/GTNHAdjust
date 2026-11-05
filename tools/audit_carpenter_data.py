"""Audit the frozen GTNH 2.9.0 carpenter inputs; requires the nbtlib package."""

from collections import defaultdict
from pathlib import Path

import nbtlib

DATA = Path(__file__).resolve().parents[1] / "src/main/resources/assets/gtnhadjust/recipes/carpenter-2.9.0.dat"
root = nbtlib.load(DATA, gzipped=True)
assert int(root["version"]) == 1
records = root["recipes"]
assert len(records) == 373, len(records)

groups = defaultdict(list)
recipes = []
for record in records:
    units = []
    for ingredient in record["inputs"]:
        choices = frozenset((str(item["item"]), int(item["damage"])) for item in ingredient["alternatives"])
        assert choices and all(name != "Forestry:ffarm" for name, _ in choices)
        units.extend([choices] * int(ingredient["count"]))
    assert 0 < len(record["inputs"]) <= 9
    fluid = record.get("fluid")
    fluid_key = (str(fluid.get("FluidName", "")), int(fluid.get("Amount", 0)), str(fluid.get("Tag", ""))) if fluid else None
    output = str(record["outputKey"])
    assert int(record["duration"]) > 0 and int(record["eut"]) > 0
    canonical = tuple(sorted((int(i["count"]), tuple(sorted((str(a["item"]), int(a["damage"])) for a in i["alternatives"]))) for i in record["inputs"]))
    groups[(canonical, fluid_key)].append(output)
    recipes.append((units, fluid_key, output))

def overlaps(first, second):
    return any(a == b and (x == y or x == 32767 or y == 32767) for a, x in first for b, y in second)

def same_materials(first, second):
    if len(first) != len(second):
        return False
    edges = [[j for j, right in enumerate(second) if overlaps(left, right)] for left in first]
    order = sorted(range(len(first)), key=lambda index: len(edges[index]))
    def match(index, used):
        if index == len(order):
            return True
        return any(not used & (1 << j) and match(index + 1, used | (1 << j)) for j in edges[order[index]])
    return match(0, 0)

conflicts = []
for index, (first, first_fluid, first_output) in enumerate(recipes):
    for second, second_fluid, second_output in recipes[index + 1:]:
        if first_fluid == second_fluid and first_output != second_output and same_materials(first, second):
            conflicts.append((first_output, second_output))
assert not conflicts, conflicts
assert all(len(set(outputs)) == 1 for outputs in groups.values())
print(f"{len(records)} static recipes audited; no internal unordered input conflicts.")
print("The separate 44 farm recipes require distinct FarmBlock NBT or distinct tube types.")
