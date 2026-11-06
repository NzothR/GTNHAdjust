"""Verify the static QFT snapshot and the exact original structure geometry."""
import contextlib
import io
import re
import unittest
from pathlib import Path
from unittest.mock import patch

import generate_small_quantum_snapshot as generator


class SnapshotTest(unittest.TestCase):
    def test_static_snapshot_and_scaling(self):
        written = []
        with patch.object(Path, 'write_text', lambda self, text, **kwargs: written.append(text)):
            with contextlib.redirect_stdout(io.StringIO()):
                generator.main()
        source = written[0]
        self.assertEqual(source.count('// QFT source recipe '), 27)
        self.assertIn('PTMetallicPowder.get(OrePrefixes.dust, 1)', source)
        self.assertIn('.duration(40960).eut(480)', source)
        self.assertIn('.outputChances(chances(6, 1666))', source)
        self.assertIn('copyAmountUnsafe(512, ItemList.Circuit_Chip_Biocell.get(1))', source)
        self.assertIn('copyAmountUnsafe(128, getModItem(GalaxySpace.ID', source)
        self.assertIn('.circuit(1)', source)
        self.assertIn('.circuit(2)', source)
        self.assertIn('Materials.Water.getFluid(157)', source)
        self.assertIn('Materials.Water.getFluid(40)', source)
        self.assertIn('getModItem("gregtech", "gt.blockmachines", 0, 31027)', source)
        self.assertNotIn('quantumForceTransformerRecipes', source)
        committed = Path('src/main/java/com/gtnhadjust/quantum/SmallQuantumSnapshot.java').read_text(encoding='utf8')
        normalize = lambda text: re.sub(r'\s+', '', text[text.index('static void register()'):])
        self.assertEqual(normalize(source), normalize(committed))

    def test_geometry_matches_original(self):
        original = Path('ref/GT5-Unofficial/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/production/MTEQuantumForceTransformer.java')
        machine = Path('src/main/java/com/gtnhadjust/quantum/SmallQuantumMachine.java')
        def shape(path):
            source = path.read_text(encoding='utf8')
            start = source.index('new String[][]')
            end = source.index(' })', start) if path == original else source.index('private IStructureDefinition', start)
            return re.findall(r'"([ A-Z~]+)"', source[start:end])
        self.assertEqual(shape(original), shape(machine))
        self.assertEqual(len(shape(machine)), 15 * 21)


if __name__ == '__main__': unittest.main()
