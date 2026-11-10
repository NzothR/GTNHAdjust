package com.gtnhadjust.machine.integrated;

import com.science.gtnl.utils.StructureUtils;

final class IntegratedShapes {

    private IntegratedShapes() {}

    static String[][] load(String piece) {
        String[][] original = StructureUtils.readStructureFromFile("gtnhadjust:multiblock/integrated_line/" + piece);
        if (piece.equals("center")) {
            String[][] result = original.clone();
            // The back pillar's glass intersects the module's optional casing positions.
            // Use the same casing as the adjacent pillar so either or both modules can form.
            for (int y : new int[] { 0, 1, 3, 4, 6, 7, 9, 10 }) {
                result[y] = original[y].clone();
                change(result, y, 26, 52, 'M', 'A');
            }
            for (int y : new int[] { 2, 8 }) {
                result[y] = original[y].clone();
                replace(result, y, 0, 27, 'A');
                replace(result, y, 52, 27, 'A');
                replace(result, y, 26, 52, 'M');
            }
            return result;
        }
        if (!piece.equals("up") && !piece.equals("down")) return original;
        String[][] result = original.clone();
        int y = piece.equals("up") ? 5 : 16;
        result[y] = original[y].clone();
        for (int[] pos : new int[][] { { 26, 0 }, { 0, 26 }, { 52, 26 }, { 26, 52 } }) {
            replace(result, y, pos[0], pos[1], 'A');
        }
        return result;
    }

    private static void replace(String[][] shape, int y, int x, int z, char expected) {
        change(shape, y, x, z, expected, 'Y');
    }

    private static void change(String[][] shape, int y, int x, int z, char expected, char replacement) {
        if (shape[y][z].charAt(x) != expected)
            throw new IllegalStateException("Unexpected integrated line structure at " + y + "," + x + "," + z);
        StringBuilder row = new StringBuilder(shape[y][z]);
        row.setCharAt(x, replacement);
        shape[y][z] = row.toString();
    }
}
