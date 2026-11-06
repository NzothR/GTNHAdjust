package com.gtnhadjust.machine.collector;

import java.util.Arrays;

/** Interlocked gate structure, stored as depth slices of top-to-bottom rows. */
final class CollectorStructureShape {

    static final int WIDTH = 28;
    static final int DEPTH = 28;
    static final int CONTROLLER_X = 13;
    static final int CONTROLLER_Y = 1;
    static final int BLACK_HOLE_X = 14;
    static final int BLACK_HOLE_Y = 11;
    static final int BLACK_HOLE_Z = 16;

    private CollectorStructureShape() {}

    static final int HEIGHT = 25;

    static String[][] create() {
        char[][][] blocks = new char[DEPTH][HEIGHT][WIDTH];
        for (char[][] plane : blocks) for (char[] row : plane) Arrays.fill(row, ' ');
        interlockedGates(blocks);
        supportFeet(blocks);
        entrance(blocks);
        put(blocks, CONTROLLER_X, CONTROLLER_Y, 0, '~');
        String[][] shape = new String[DEPTH][HEIGHT];
        for (int z = 0; z < DEPTH; z++)
            for (int y = 0; y < HEIGHT; y++) shape[z][HEIGHT - y - 1] = new String(blocks[z][y]);
        return shape;
    }

    /** Small recessed front console and an open, tapered access bridge. */
    private static void entrance(char[][][] b) {
        for (int x = 10; x <= 16; x++) {
            put(b, x, 0, 0, 'S');
            put(b, x, 3, 0, 'S');
        }
        for (int x : new int[] { 10, 11, 15, 16 })
            for (int y = 1; y <= 2; y++) put(b, x, y, 0, x == 10 || x == 16 ? 'S' : 'C');
        put(b, 12, 1, 0, 'C');
        put(b, 14, 1, 0, 'C');
        for (int z = 1; z <= 10; z++) {
            int halfWidth = z < 4 ? 2 : 1;
            for (int x = 13 - halfWidth; x <= 14 + halfWidth; x++) put(b, x, 0, z, 'R');
            put(b, 12, 1, z, 'S');
            put(b, 15, 1, z, 'S');
            if (z % 3 == 0) {
                put(b, 11, 0, z, 'B');
                put(b, 16, 0, z, 'B');
            }
        }
        for (int x = 12; x <= 14; x++) put(b, x, 2, 0, 'C');
    }

    /** Two differently tilted intersecting frames with unequal support wings. */
    private static void interlockedGates(char[][][] b) {
        // Open front and rear leave a clear view of the central black hole.
        for (int x : new int[] { 9, 19 })
            for (int y = 7; y <= 15; y++) for (int z = 14; z <= 18; z++) put(b, x, y, z, 'G');
        tiltedRing(b, 14, 11, 16, 10.8, 10.0, 3.0, 0, 360, 'S');
        // The second frame turns ninety degrees and rises toward the right.
        for (int a = 0; a < 360; a += 2) {
            double t = Math.toRadians(a);
            int z = (int) Math.round(16 + 10 * Math.cos(t));
            int y = (int) Math.round(11 + 9.5 * Math.sin(t));
            int x = (int) Math.round(14 + 3.0 * Math.sin(t));
            pad(b, x, y, z, 1, a < 100 ? 'B' : 'C');
        }
        beam(b, 1, 1, 8, 6, 15, 12, 1, 'S');
        beam(b, 6, 15, 12, 10, 19, 15, 1, 'C');
        beam(b, 10, 19, 15, 12, 24, 16, 1, 'B');
        beam(b, 26, 1, 23, 21, 8, 20, 1, 'S');
        beam(b, 21, 8, 20, 18, 10, 18, 1, 'R');
        beam(b, 15, 7, 13, 18, 9, 15, 1, 'C');
        put(b, 13, 7, 12, 'C');
        beam(b, 11, 1, 10, 10, 5, 13, 1, 'S');
        beam(b, 10, 5, 13, 12, 8, 13, 1, 'C');
        beam(b, 16, 1, 10, 18, 5, 13, 1, 'S');
        beam(b, 13, 1, 22, 12, 0, 27, 1, 'R');
        beam(b, 6, 14, 12, 3, 18, 17, 1, 'B');
        // Reserve an air cavity for the renderer without replacing world blocks.
        for (int x = BLACK_HOLE_X - 2; x <= BLACK_HOLE_X + 2; x++)
            for (int y = BLACK_HOLE_Y - 2; y <= BLACK_HOLE_Y + 2; y++)
                for (int z = BLACK_HOLE_Z - 2; z <= BLACK_HOLE_Z + 2; z++) put(b, x, y, z, '-');
    }

    /** Ground the two outer wings with compact feet below their existing lower ends. */
    private static void supportFeet(char[][][] b) {
        for (int[] foot : new int[][] { { 1, 8 }, { 26, 23 } }) {
            pad(b, foot[0], 0, foot[1], 1, 'S');
            put(b, foot[0], 0, foot[1], 'B');
        }
    }

    private static void tiltedRing(char[][][] b, double cx, double cy, double cz, double rx, double ry,
        double depthSlope, int from, int to, char block) {
        for (int a = from; a <= to; a += 2) {
            double t = Math.toRadians(a);
            int x = (int) Math.round(cx + rx * Math.cos(t));
            int y = (int) Math.round(cy + ry * Math.sin(t));
            int z = (int) Math.round(cz + depthSlope * Math.sin(t));
            pad(b, x, y, z, 1, block);
        }
    }

    private static void beam(char[][][] b, int x1, int y1, int z1, int x2, int y2, int z2, int radius, char block) {
        int steps = 2 * Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        for (int i = 0; i <= steps; i++) {
            double f = steps == 0 ? 0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * f);
            int y = (int) Math.round(y1 + (y2 - y1) * f);
            int z = (int) Math.round(z1 + (z2 - z1) * f);
            pad(b, x, y, z, radius, block);
        }
    }

    private static void pad(char[][][] b, int x, int y, int z, int radius, char block) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) put(b, x + dx, y, z + dz, block);
    }

    private static void put(char[][][] b, int x, int y, int z, char block) {
        if (x >= 0 && x < WIDTH && z >= 0 && z < DEPTH && y >= 0 && y < b[0].length) b[z][y][x] = block;
    }
}
