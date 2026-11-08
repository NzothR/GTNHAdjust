package com.futureresourceprotocol.device;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BookmarkExportMessageTest {

    @Test
    public void roundTripsSnapshotMetadataAndBothDebtClasses() {
        BookmarkExportMessage source = new BookmarkExportMessage();
        source.requestId = 42;
        source.marks.windowId = 7;
        source.marks.session = "test-session";
        source.marks.revision = 9;
        source.marks.part = 0;
        source.marks.parts = 1;
        source.marks.total = 0;
        source.marks.valid = true;
        source.debts = new LinkedHashMap<>();
        source.debts.put("example:item@2", new long[] { 64, 128 });
        ByteBuf buffer = Unpooled.buffer();
        try {
            source.toBytes(buffer);
            BookmarkExportMessage loaded = new BookmarkExportMessage();
            loaded.fromBytes(buffer);
            assertEquals(42, loaded.requestId);
            assertEquals(7, loaded.marks.windowId);
            assertEquals("test-session", loaded.marks.session);
            assertEquals(9, loaded.marks.revision);
            assertTrue(loaded.marks.valid);
            assertArrayEquals(new long[] { 64, 128 }, loaded.debts.get("example:item@2"));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }
}
