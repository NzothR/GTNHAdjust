package com.gtnhadjust.machine.integrated;

import com.science.gtnl.utils.StructureUtils;

final class IntegratedShapes {

    private IntegratedShapes() {}

    static String[][] load(String piece) {
        String[][] original = StructureUtils.readStructureFromFile("gtnhadjust:multiblock/integrated_line/" + piece);
        if (!piece.equals("up") && !piece.equals("down")) return original;
        String[][] result = original.clone();
        int y = piece.equals("up") ? 5 : 16;
        result[y] = original[y].clone();
        for (int[] pos : new int[][] { { 26, 0 }, { 0, 26 }, { 52, 26 }, { 26, 52 } }) {
            int x = pos[0], z = pos[1];
            if (result[y][z].charAt(x) != 'A')
                throw new IllegalStateException("Unexpected integrated line structure at " + piece);
            StringBuilder row = new StringBuilder(result[y][z]);
            row.setCharAt(x, 'Y');
            result[y][z] = row.toString();
        }
        return result;
    }
}
