package com.gtnhadjust.machine.collector;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class CollectorStructureShapeTest {

    @Test
    public void structureHasExpectedBoundsAndConnectedController() {
        String[][] shape = CollectorStructureShape.create();
        int height = CollectorStructureShape.HEIGHT;
        assertEquals(28, shape.length);
        int controllers = 0, glass = 0, hatches = 0, bottom = 0;
        boolean left = false, right = false, front = false, back = false, top = false;
        Set<Integer> remaining = new HashSet<>();
        for (int z = 0; z < 28; z++) {
            assertEquals(height, shape[z].length);
            for (int row = 0; row < height; row++) {
                String line = shape[z][row];
                assertEquals(28, line.length());
                for (int x = 0; x < 28; x++) {
                    char c = line.charAt(x);
                    assertTrue("Unknown symbol " + c, " SCRBG~-".indexOf(c) >= 0);
                    if (c == '~') controllers++;
                    if (c == 'G') glass++;
                    if (c == 'C') hatches++;
                    if (c == ' ' || c == '-') continue;
                    remaining.add((z * height + row) * 28 + x);
                    if (row == height - 1) bottom++;
                    if (x == 0) left = true;
                    if (x == 27) right = true;
                    if (z == 0) front = true;
                    if (z == 27) back = true;
                    if (row == 0) top = true;
                }
            }
        }
        assertEquals(1, controllers);
        assertTrue(glass > 0 && hatches >= 4);
        assertTrue(bottom < 150);
        assertTrue(left && right && front && back && top);
        assertEquals('~', shape[0][height - 2].charAt(13));
        for (int[] foot : new int[][] { { 1, 8 }, { 26, 23 } }) {
            assertEquals('B', shape[foot[1]][height - 1].charAt(foot[0]));
            assertEquals('S', shape[foot[1]][height - 2].charAt(foot[0]));
        }
        for (int x = CollectorStructureShape.BLACK_HOLE_X - 2; x <= CollectorStructureShape.BLACK_HOLE_X + 2; x++)
            for (int y = CollectorStructureShape.BLACK_HOLE_Y - 2; y <= CollectorStructureShape.BLACK_HOLE_Y + 2; y++)
                for (int z = CollectorStructureShape.BLACK_HOLE_Z - 2; z
                    <= CollectorStructureShape.BLACK_HOLE_Z + 2; z++)
                    assertEquals('-', shape[z][height - 1 - y].charAt(x));
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int start = (height - 2) * 28 + 13;
        assertTrue(remaining.remove(start));
        queue.add(start);
        int[][] directions = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        while (!queue.isEmpty()) {
            int value = queue.remove();
            int x = value % 28, row = value / 28 % height, z = value / (28 * height);
            for (int[] direction : directions) {
                int nx = x + direction[0], nr = row + direction[1], nz = z + direction[2];
                if (nx < 0 || nx >= 28 || nr < 0 || nr >= height || nz < 0 || nz >= 28) continue;
                int next = (nz * height + nr) * 28 + nx;
                if (remaining.remove(next)) queue.add(next);
            }
        }
        assertTrue("Disconnected structure blocks: " + remaining.size(), remaining.isEmpty());
    }
}
