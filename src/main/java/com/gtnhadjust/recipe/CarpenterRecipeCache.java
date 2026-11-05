package com.gtnhadjust.recipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

final class CarpenterRecipeCache {

    private CarpenterRecipeCache() {}

    static Map<String, CarpenterRecipes.Entry> load(File file) throws IOException {
        Map<String, CarpenterRecipes.Entry> entries = new TreeMap<>();
        if (!file.exists()) return entries;
        NBTTagCompound root;
        try (FileInputStream input = new FileInputStream(file)) {
            root = CompressedStreamTools.readCompressed(input);
        }
        if (root == null || root.getInteger("version") != 1)
            throw new IOException("Unsupported carpenter recipe cache");
        NBTTagList recipes = root.getTagList("recipes", 10);
        for (int i = 0; i < recipes.tagCount(); i++) {
            NBTTagCompound tag = recipes.getCompoundTagAt(i);
            ItemStack output = readStack(tag.getCompoundTag("output"));
            if (output == null || output.getItem() == null)
                throw new IOException("Cached output item is unavailable: " + tag.getString("outputKey"));
            FluidStack fluid = tag.hasKey("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid"))
                : null;
            if (tag.hasKey("fluid") && fluid == null)
                throw new IOException("Cached fluid is unavailable: " + tag.getString("outputKey"));
            List<CarpenterRecipes.Ingredient> ingredients = new ArrayList<>();
            NBTTagList storedInputs = tag.getTagList("inputs", 10);
            for (int j = 0; j < storedInputs.tagCount(); j++) {
                NBTTagCompound stored = storedInputs.getCompoundTagAt(j);
                NBTTagList storedChoices = stored.getTagList("alternatives", 10);
                List<ItemStack> choices = new ArrayList<>();
                for (int k = 0; k < storedChoices.tagCount(); k++) {
                    ItemStack item = readStack(storedChoices.getCompoundTagAt(k));
                    if (item == null || item.getItem() == null)
                        throw new IOException("Cached input item is unavailable: " + tag.getString("outputKey"));
                    choices.add(item);
                }
                if (choices.isEmpty() || stored.getInteger("count") <= 0)
                    throw new IOException("Invalid cached ingredient");
                CarpenterRecipes.Ingredient ingredient = new CarpenterRecipes.Ingredient(
                    stored.getString("key"),
                    stored.hasKey("ore") ? stored.getString("ore") : null,
                    choices);
                ingredient.count = stored.getInteger("count");
                ingredients.add(ingredient);
            }
            if (ingredients.isEmpty() || tag.getInteger("duration") <= 0 || tag.getInteger("eut") <= 0)
                throw new IOException("Invalid cached recipe");
            entries.put(
                tag.getString("id"),
                new CarpenterRecipes.Entry(
                    tag.getString("inputKey"),
                    ingredients,
                    fluid,
                    output,
                    tag.getString("outputKey"),
                    tag.getInteger("duration"),
                    tag.getInteger("eut")));
        }
        return entries;
    }

    static void save(File file, Map<String, CarpenterRecipes.Entry> entries) throws IOException {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("version", 1);
        NBTTagList recipes = new NBTTagList();
        for (Map.Entry<String, CarpenterRecipes.Entry> mapping : entries.entrySet()) {
            CarpenterRecipes.Entry entry = mapping.getValue();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", mapping.getKey());
            tag.setString("inputKey", entry.inputKey);
            tag.setString("outputKey", entry.outputKey);
            tag.setInteger("duration", entry.duration);
            tag.setInteger("eut", entry.eut);
            tag.setTag("output", writeStack(entry.output));
            if (entry.fluid != null) tag.setTag("fluid", entry.fluid.writeToNBT(new NBTTagCompound()));
            NBTTagList inputs = new NBTTagList();
            for (CarpenterRecipes.Ingredient ingredient : entry.inputs) {
                NBTTagCompound stored = new NBTTagCompound();
                stored.setString("key", ingredient.key);
                if (ingredient.oreName != null) stored.setString("ore", ingredient.oreName);
                stored.setInteger("count", ingredient.count);
                NBTTagList choices = new NBTTagList();
                for (ItemStack stack : ingredient.alternatives) choices.appendTag(writeStack(stack));
                stored.setTag("alternatives", choices);
                inputs.appendTag(stored);
            }
            tag.setTag("inputs", inputs);
            recipes.appendTag(tag);
        }
        root.setTag("recipes", recipes);
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static NBTTagCompound writeStack(ItemStack stack) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("item", Item.itemRegistry.getNameForObject(stack.getItem()));
        tag.setInteger("count", stack.stackSize);
        tag.setInteger("damage", stack.getItemDamage());
        if (stack.hasTagCompound()) tag.setTag(
            "data",
            stack.getTagCompound()
                .copy());
        return tag;
    }

    private static ItemStack readStack(NBTTagCompound tag) {
        Item item = (Item) Item.itemRegistry.getObject(tag.getString("item"));
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, tag.getInteger("count"), tag.getInteger("damage"));
        if (tag.hasKey("data", 10)) stack.setTagCompound(tag.getCompoundTag("data"));
        return stack;
    }
}
