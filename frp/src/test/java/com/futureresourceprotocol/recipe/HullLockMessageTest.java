package com.futureresourceprotocol.recipe;

import static org.junit.Assert.*;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HullLockMessageTest {

    @Test
    public void roundTripsSnapshotFlags() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new HullLockMessage(true, 3).toBytes(buffer);
            HullLockMessage result = new HullLockMessage();
            result.fromBytes(buffer);
            assertTrue(result.enabled);
            assertEquals(3, result.mask);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidMask() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeBoolean(true);
            buffer.writeByte(8);
            new HullLockMessage().fromBytes(buffer);
        } finally {
            buffer.release();
        }
    }
}
