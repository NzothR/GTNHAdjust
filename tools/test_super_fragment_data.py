"""Checks for the committed, static collector recipe snapshot (no game required)."""
import json
import unittest
from pathlib import Path


DATA = Path('src/main/resources/assets/gtnhadjust/recipes/super_fragment_world.json')


class CollectorSnapshotTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.recipes = json.loads(DATA.read_text(encoding='utf8'))['recipes']

    def recipe(self, dimension, mode, drill=None):
        return next(r for r in self.recipes if r['dimension'] == dimension
                    and r['mode'] == mode and r.get('drill') == drill)

    def test_display_capacity_and_unique_groups(self):
        groups = {(r['dimension'], r['mode'], r.get('drill')) for r in self.recipes}
        self.assertEqual(len(groups), 81)
        self.assertEqual(len(groups), len(self.recipes))
        self.assertEqual(max(len(r['outputs']) for r in self.recipes), 89)
        self.assertEqual(max(len(r['fluids']) for r in self.recipes), 6)
        self.assertTrue(all(r['duration'] > 0 and r['eut'] > 0 for r in self.recipes))

    def test_overworld_sums_preserve_large_stacks_and_chances(self):
        recipe = self.recipe('DimensionOverworld', 'items')
        outputs = {(o['kind'], o['name'], o['meta']): o for o in recipe['outputs']}
        self.assertEqual((recipe['duration'], recipe['eut']), (4363, 192))
        self.assertEqual(outputs['block', 'stone', 0]['amount'], 704)
        self.assertEqual(outputs['mod', 'DimensionOverworld', 0]['amount'], 24)
        self.assertEqual(outputs['item', 'leather', 0]['chance'], 500)
        self.assertEqual({f['name'] for f in recipe['fluids']}, {'lava', 'RawOil'})

    def test_twilight_special_input_and_marker_output(self):
        recipe = self.recipe('DimensionTwilight', 'items')
        self.assertTrue(recipe['extraSteelAndLubricant'])
        outputs = {o['name']: o for o in recipe['outputs'] if o['kind'] == 'mod'}
        self.assertEqual(outputs['DimensionTwilight']['amount'], 9)
        self.assertEqual((recipe['duration'], recipe['eut']), (2024, 80))
        self.assertTrue(any(o['name'] == 'DamascusSteel' for o in recipe['outputs']))

    def test_five_drill_variants(self):
        base = self.recipe('DimensionOverworld', 'fluids', 'Steel')
        for drill, multiplier, chance in [('Steel', 1, 100), ('Titanium', 16, 90),
                                          ('NaquadahAlloy', 128, 80), ('Neutronium', 1024, 70),
                                          ('Infinity', None, 60)]:
            recipe = self.recipe('DimensionOverworld', 'fluids', drill)
            self.assertEqual(recipe['drillChance'], chance)
            self.assertEqual((recipe['duration'], recipe['eut']), (1200, 48))
            self.assertEqual([f['name'] for f in recipe['fluids']], [f['name'] for f in base['fluids']])
            for actual, original in zip(recipe['fluids'], base['fluids']):
                self.assertEqual(actual['amount'], 2147483647 if multiplier is None
                                 else original['amount'] * multiplier)


if __name__ == '__main__':
    unittest.main()
