package com.gtnhadjust.machine.collector;

import static org.junit.Assert.*;

import org.junit.Test;

public class CollectorStructureShapeTest {

    @Test
    public void structurePreservesNativeContainmentFieldLayout() {
        String[][] shape = CollectorStructureShape.create();
        int height = CollectorStructureShape.HEIGHT;
        assertEquals(35, shape.length);
        assertEquals(33, height);
        int controllers = 0, glass = 0, hatches = 0, absorbent = 0, frames = 0;
        for (int z = 0; z < shape.length; z++) {
            assertEquals(height, shape[z].length);
            for (int row = 0; row < height; row++) {
                String line = shape[z][row];
                assertEquals(35, line.length());
                for (int x = 0; x < line.length(); x++) {
                    char c = line.charAt(x);
                    assertTrue("Unknown symbol " + c, " ABCD~".indexOf(c) >= 0);
                    if (c == '~') controllers++;
                    if (c == 'A') glass++;
                    if (c == 'C') hatches++;
                    if (c == 'B') absorbent++;
                    if (c == 'D') frames++;
                }
            }
        }
        assertEquals(1, controllers);
        assertEquals(64, glass);
        assertEquals(3671, hatches);
        assertEquals(988, absorbent);
        assertEquals(144, frames);
        assertEquals('~', shape[10][27].charAt(17));
        assertEquals(
            '~',
            shape[CollectorStructureShape.CONTROLLER_Z][height - 1 - CollectorStructureShape.CONTROLLER_Y]
                .charAt(CollectorStructureShape.CONTROLLER_X));
        assertEquals(' ', shape[17][16].charAt(17));
        assertEquals(7, CollectorStructureShape.BLACK_HOLE_Z - CollectorStructureShape.CONTROLLER_Z);
        assertEquals(11, CollectorStructureShape.BLACK_HOLE_Y - CollectorStructureShape.CONTROLLER_Y);
    }
}
