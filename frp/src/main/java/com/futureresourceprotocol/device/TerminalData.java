package com.futureresourceprotocol.device;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;

public final class TerminalData {

    public static final int MAX = 900;

    private TerminalData() {}

    public static boolean safeTag(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return true;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            CompressedStreamTools.write(stack.getTagCompound(), new DataOutputStream(bytes));
            return bytes.size() < 1000000 && CompressedStreamTools.compress(stack.getTagCompound()).length < 30000;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }
}
