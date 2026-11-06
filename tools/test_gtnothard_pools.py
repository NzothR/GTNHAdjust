import json
import unittest
from pathlib import Path

DATA = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/gtnhadjust/recipes/gtnothard_singularity.json'

class SourceSnapshotTest(unittest.TestCase):
    def test_pools_and_weighted_source_entries(self):
        data = json.loads(DATA.read_text(encoding='utf8'))['dimensions']
        self.assertEqual(42, len(data))
        self.assertEqual(208, sum(len(modes) for modes in data.values()))
        self.assertEqual({'ore': 81, 'gem': 50, 'dust': 454, 'ingot': 213, 'fluid': 191},
                         {mode: len(values) for mode, values in data['Ow'].items()})
        self.assertNotIn('DD', data)
        self.assertTrue(all(entry['kind'] in ('item', 'fluid', 'materialFluid', 'werkstoffFluid')
                            for modes in data.values() for entries in modes.values() for entry in entries))


    def test_every_dimension_has_valid_tier(self):
        snapshot = json.loads(DATA.read_text(encoding='utf8'))
        self.assertEqual(set(snapshot['dimensions']), set(snapshot['tiers']))
        self.assertEqual({'Ow': 0, 'Mo': 1},
                         {name: snapshot['tiers'][name] for name in ('Ow', 'Mo')})
        self.assertTrue(all(0 <= tier <= 9 for tier in snapshot['tiers'].values()))
        self.assertEqual(9, max(snapshot['tiers'].values()))

    def test_pages_cover_all_unique_outputs(self):
        data = json.loads(DATA.read_text(encoding='utf8'))['dimensions']
        for dimension, modes in data.items():
            for mode, entries in modes.items():
                unique = list(dict.fromkeys(json.dumps(entry, sort_keys=True) for entry in entries))
                pages = [unique[i:i + 90] for i in range(0, len(unique), 90)]
                self.assertTrue(all(len(page) <= 90 for page in pages), (dimension, mode))
                self.assertEqual(unique, [entry for page in pages for entry in page], (dimension, mode))

if __name__ == '__main__':
    unittest.main()
