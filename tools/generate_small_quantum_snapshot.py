"""Generate fixed GT5 2.9.0 QFT recipes; run only during development, never in game."""
import re
from pathlib import Path

SOURCE = Path('ref/GT5-Unofficial/src/main/java/gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderChemicalSkips.java')
TARGET = Path('src/main/java/com/gtnhadjust/quantum/SmallQuantumSnapshot.java')
VOLTAGES = {'ZPM': 122880, 'UV': 491520, 'UHV': 1966080, 'UEV': 7864320, 'UIV': 31457280}
# The user's client and server both use GTNR recipe speed mode 2, multiplier 0.1.
# Capture the source recipe's actual duration once, before conversion, to keep this snapshot static.
SOURCE_DURATION_MULTIPLIER = 0.1

# Static entries from the installed GTNH instance; never read these mods at game startup.
# Groups are item inputs, item outputs, fluid inputs, fluid outputs, followed by EU/t, ticks and a fixed circuit.
EXTRA = [
    ('CropsNH', ['CropsNHItemList.platinaLeaf.get(64)', 'CropsNHItemList.osmianthFlower.get(64)', 'CropsNHItemList.iridineFlower.get(64)'], [], [],
     ['Materials.Platinum.getMolten(9216)', 'Materials.Osmium.getMolten(9216)', 'Materials.Iridium.getMolten(9216)'], 491520, 400, None),
    ('GTNL', ['Materials.Lead.getDust(16)', 'Materials.Bauxite.getDust(32)', 'WerkstoffLoader.Ferberite.get(OrePrefixes.dust, 16)'],
     ['Materials.Titanium.getDust(64)', 'Materials.TungstenSteel.getDust(64)', 'Materials.TungstenCarbide.getDust(64)', 'Materials.Indium.getDust(64)'], [], [], 491520, 400, None),
    ('GTRH-water-1', [], [], ['Materials.Water.getFluid(40000)'],
     ['Materials.Grade1PurifiedWater.getFluid(20000)', 'Materials.Grade2PurifiedWater.getFluid(10000)', 'Materials.Grade3PurifiedWater.getFluid(5000)', 'Materials.Grade4PurifiedWater.getFluid(1000)'], 1966080, 400, 1),
    ('GTRH-water-2', [], [], ['Materials.Water.getFluid(40000)'],
     ['Materials.Grade5PurifiedWater.getFluid(20000)', 'Materials.Grade6PurifiedWater.getFluid(10000)', 'Materials.Grade7PurifiedWater.getFluid(5000)', 'Materials.Grade8PurifiedWater.getFluid(1000)'], 31457280, 400, 2),
    ('TST', ['WerkstoffMaterialPool.SamariumOreConcentrate.get(OrePrefixes.dust, 32)'],
     ['Materials.Samarium.getDust(64)', 'Materials.Gadolinium.getDust(64)', 'Materials.Phosphorus.getDust(64)', 'Materials.Thorium.getDust(64)', 'WerkstoffLoader.Thorium232.get(OrePrefixes.dust, 64)'], [], [], 1966080, 400, None),
    ('OTH-machines', ['getModItem("gregtech", "gt.blockmachines", 0, 31027)', 'getModItem("gregtech", "gt.blockmachines", 0, 15560)'],
     ['getModItem("miscutils", "itemDustRhenium", 16)', 'getModItem("miscutils", "itemDustThallium", 16)', 'getModItem("miscutils", "itemDustSelenium", 16)', 'getModItem("miscutils", "itemDustGermanium", 16)', 'getModItem("gregtech", "gt.metaitem.01", 16, 2044)', 'getModItem("gregtech", "gt.metaitem.01", 16, 2073)'],
     ['FluidRegistry.getFluidStack("pineoil", 123)', 'FluidRegistry.getFluidStack("molten.helicopter", 24)'],
     ['FluidRegistry.getFluidStack("water", 123123)', 'FluidRegistry.getFluidStack("mud.red.slurry", 1919810)'], 123123123, 123, None),
    ('OTH-wood', ['Materials.Iron.getDust(64)', 'Materials.Steel.getDust(64)', 'Materials.Wood.getDust(64)', 'new ItemStack(Blocks.leaves, 64)'],
     ['Materials.IronWood.getDust(64)', 'Materials.Steeleaf.getDust(64)'], [], [], 123123123, 114, None),
]


def split_args(text):
    args, start, depth, quoted = [], 0, 0, False
    for i, c in enumerate(text):
        if c == '"': quoted = not quoted
        elif not quoted and c == '(': depth += 1
        elif not quoted and c == ')': depth -= 1
        elif not quoted and c == ',' and depth == 0:
            args.append(text[start:i].strip()); start = i + 1
    if text[start:].strip(): args.append(text[start:].strip())
    return args


def grab(block, method):
    match = re.search(r'\.' + method + r'\s*\(', block)
    if not match: return []
    start, depth, i = match.end(), 1, match.end()
    while depth:
        if block[i] == '(': depth += 1
        if block[i] == ')': depth -= 1
        i += 1
    return split_args(' '.join(block[start:i-1].split()))


def amount(expr):
    aliases = {'stemcells': 'ItemList.Circuit_Chip_Stemcell.get(2048)',
               'biocells': 'ItemList.Circuit_Chip_Biocell.get(2048)',
               'seaweed': 'getModItem(GalaxySpace.ID, "tcetiedandelions", 2048, 4)'}
    expr = aliases.get(expr, expr)
    start = expr.rfind('(')
    head, args = expr[:start+1], split_args(expr[start+1:-1])
    if 'getModItem(' in head: index = 2
    elif 'getStackForType(' in head: index = 1
    elif 'GTOreDictUnificator.get(' in head: index = len(args)-1
    elif 'FluidRegistry.getFluidStack(' in head: index = 1
    else: index = len(args)-1
    value = args[index].replace('_', '').replace('L', '').replace('STACKS', '9216').replace('INGOTS', '144')
    if not re.fullmatch(r'[\d\s*+]+', value): raise ValueError(expr)
    return head, args, index, eval(value, {'__builtins__': {}})


def make_expr(value, count):
    head, args, index, _ = value
    if 'GTBees.combs.getStackForType(' in head:
        comb_ids = {'KEVLAR': 161, 'PLATINUM': 69, 'PALLADIUM': 164, 'OSMIUM': 72, 'IRIDIUM': 70}
        return 'getModItem("gregtech", "gt.comb", ' + str(count) + ', ' + str(comb_ids[args[0].split('.')[-1]]) + ')'
    args = list(args); args[index] = str(count if count <= 64 else 1)
    expr = head + ', '.join(args) + ')'
    return expr if count <= 64 else 'GTUtility.copyAmountUnsafe(' + str(count) + ', ' + expr + ')'


def main():
    source = SOURCE.read_text(encoding='utf8').split('private static void quantumTransformerRecipes()')[1].split('private static void fusionReactorRecipes()')[0]
    blocks = re.findall(r'GTValues\.RA\.stdBuilder\(\)(.*?)\.addTo\(quantumForceTransformerRecipes\);', source, re.S)
    recipes = []
    for idx, block in enumerate(blocks):
        if idx in (8, 12, 15): continue  # A source input or output is already one.
        groups = [[amount(e) for e in grab(block, name)] for name in ('itemInputs', 'itemOutputs', 'fluidInputs', 'fluidOutputs')]
        if any(x[3] <= 1 for group in groups for x in group): continue
        eu = VOLTAGES[grab(block, 'eut')[0].split('_')[-1]]
        ticks = max(1, int(400 * SOURCE_DURATION_MULTIPLIER))
        while eu > 32 and all(x[3] > 1 for group in groups for x in group):
            groups = [[(*x[:3], (x[3] + 1) // 2) for x in group] for group in groups]
            eu = max(32, (eu + 3) // 4); ticks *= 4
        recipes.append((str(idx), groups, eu, ticks, None))
    for name, *parts in EXTRA:
        raw_groups, eu, ticks, circuit = parts[:4], parts[4], parts[5], parts[6]
        ticks = max(1, int(ticks * SOURCE_DURATION_MULTIPLIER))
        groups = [[amount(e) for e in group] for group in raw_groups]
        if any(x[3] == 1 for group in groups for x in group): raise ValueError(name + ' cannot be halved')
        while eu > 32 and all(x[3] == 0 or x[3] > 1 for group in groups for x in group):
            groups = [[(*x[:3], (x[3] + 1) // 2) for x in group] for group in groups]
            eu = max(32, (eu + 3) // 4); ticks *= 4
        recipes.append((name, groups, eu, ticks, circuit))
    # This baseline has no identical/subset input groups. Refuse regeneration if that changes:
    # then circuit assignments must be explicitly fixed in the snapshot.
    keys = []
    for _, groups, _, _, circuit in recipes:
        items = {(x[0], tuple(x[1][:x[2]] + x[1][x[2]+1:])) for x in groups[0]}
        if circuit is not None: items.add(('circuit', circuit))
        keys.append((items,
                     {(x[0], tuple(x[1][:x[2]] + x[1][x[2]+1:])) for x in groups[2]}))
    for i, (a, af) in enumerate(keys):
        for b, bf in keys[i+1:]:
            if (a <= b and af <= bf) or (b <= a and bf <= af):
                raise ValueError('Potential input conflict: assign fixed circuits before generating')
    lines = ['package com.gtnhadjust.quantum;', '',
             'import static gregtech.api.enums.Mods.GalaxySpace;',
             'import static gregtech.api.enums.Mods.NewHorizonsCoreMod;',
             'import static gregtech.api.util.GTModHandler.getModItem;', '',
             'import java.util.Arrays;', 'import java.util.Collection;', '',
             'import net.minecraft.item.ItemStack;', 'import net.minecraft.init.Blocks;', 'import net.minecraftforge.fluids.FluidRegistry;', '',
             'import com.gtnewhorizon.cropsnh.api.CropsNHItemList;',
             'import bartworks.system.material.WerkstoffLoader;',
             'import gregtech.api.enums.ItemList;', 'import gregtech.api.enums.Materials;',
             'import gregtech.api.enums.OrePrefixes;', 'import gregtech.api.util.GTOreDictUnificator;',
             'import gregtech.api.util.GTRecipe;', 'import gregtech.api.util.GTRecipeBuilder;', 'import gregtech.api.util.GTUtility;',
             'import gtPlusPlus.core.material.MaterialMisc;', 'import gtPlusPlus.core.material.MaterialsElements;',
             'import gtnhlanth.common.register.WerkstoffMaterialPool;', '',
             '/** Generated fixed GT5 QFT recipe snapshot. See tools/generate_small_quantum_snapshot.py. */',
             'final class SmallQuantumSnapshot {', '    private SmallQuantumSnapshot() {}',
             '    static void register() {']
    for idx, groups, eu, ticks, circuit in recipes:
        output_count = len(groups[1]) + len(groups[3]); chance = 10000 // output_count
        lines += [f'        // QFT source recipe {idx}; fixed output chance {chance}/10000.', '        add(GTRecipeBuilder.builder()']
        for group_index, (method, group) in enumerate(zip(('itemInputs', 'itemOutputs', 'fluidInputs', 'fluidOutputs'), groups)):
            if group:
                expressions = (make_expr(x, x[3]) if group_index < 2 else x[0] + ', '.join(x[1][:x[2]] + [str(x[3])] + x[1][x[2]+1:]) + ')' for x in group)
                lines.append('            .' + method + '(' + ', '.join(expressions) + ')')
        if circuit is not None: lines.append(f'            .circuit({circuit})')
        lines += [f'            .outputChances(chances({len(groups[1])}, {chance}))',
                  f'            .fluidOutputChances(chances({len(groups[3])}, {chance}))',
                  f'            .duration({ticks}).eut({eu}), {ticks}, "{idx}");']
    lines += ['    }', '',
              '    private static int[] chances(int count, int chance) {',
              '        int[] result = new int[count]; Arrays.fill(result, chance); return result;', '    }',
              '    private static void add(GTRecipeBuilder builder, int duration, String source) {',
              '        Collection<GTRecipe> recipes = builder.addTo(SmallQuantumRecipes.MAP);',
              '        if (recipes.isEmpty()) com.gtnhadjust.GTNHAdjust.LOG.warn("Fixed small quantum recipe {} was rejected", source);',
              '        else {',
              '            for (GTRecipe recipe : recipes) recipe.mDuration = duration;',
              '            if (com.gtnhadjust.GTNHAdjust.debug) com.gtnhadjust.GTNHAdjust.LOG.info("Registered small quantum recipe {} (duration {} ticks)", source, duration);',
              '        }',
              '    }', '}']
    TARGET.write_text('\n'.join(lines) + '\n', encoding='utf8')
    print('generated', len(recipes), 'fixed recipes; water recipes use circuits 1 and 2')


if __name__ == '__main__': main()
