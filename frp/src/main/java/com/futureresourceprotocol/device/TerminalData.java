package com.futureresourceprotocol.device;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class TerminalData {

    public static final int MAX = 900;

    private TerminalData() {}

    public static List<ItemStack> read(ItemStack terminal) {
        List<ItemStack> result = new ArrayList<>();
        if (terminal == null || !terminal.hasTagCompound()) return result;
        NBTTagList list = terminal.getTagCompound()
            .getTagList("frpMarks", 10);
        for (int i = 0; i < Math.min(MAX, list.tagCount()); i++) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(i));
            if (stack != null) result.add(stack);
        }
        return result;
    }

    public static void write(ItemStack terminal, List<ItemStack> marks) {
        if (!terminal.hasTagCompound()) terminal.setTagCompound(new NBTTagCompound());
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : marks) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        terminal.getTagCompound()
            .setTag("frpMarks", list);
    }

    public static boolean fitsNetwork(ItemStack terminal, List<ItemStack> marks) {
        ItemStack copy = terminal.copy();
        write(copy, marks);
        return safeTag(copy);
    }

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
