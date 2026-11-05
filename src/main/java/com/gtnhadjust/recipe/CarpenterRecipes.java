package com.gtnhadjust.recipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.enums.GTValues;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

public final class CarpenterRecipes {

    private static final String DATA = "/assets/gtnhadjust/recipes/carpenter-2.9.0.dat";

    private CarpenterRecipes() {}

    public static void register() {
        int registered = 0;
        int skipped = 0;
        NBTTagCompound root;
        try (InputStream input = CarpenterRecipes.class.getResourceAsStream(DATA)) {
            if (input == null) throw new IOException("Missing " + DATA);
            root = CompressedStreamTools.readCompressed(input);
            if (root == null || root.getInteger("version") != 1) throw new IOException("Invalid static recipe data");
        } catch (IOException | RuntimeException error) {
            GTNHAdjust.LOG.error("Cannot load static carpenter recipes", error);
            return;
        }
        NBTTagList recipes = root.getTagList("recipes", 10);
        for (int i = 0; i < recipes.tagCount(); i++) {
            NBTTagCompound tag = recipes.getCompoundTagAt(i);
            try {
                GTRecipe recipe = makeRecipe(tag);
                RecipeMaps.assemblerRecipes.add(recipe);
                registered++;
                if (GTNHAdjust.debug)
                    GTNHAdjust.LOG.info("Registered static carpenter recipe {}", tag.getString("outputKey"));
            } catch (RuntimeException error) {
                skipped++;
                GTNHAdjust.LOG.warn(
                    "Cannot register static carpenter recipe {}: {}",
                    tag.getString("outputKey"),
                    error.getMessage());
            }
        }
        int farms = FarmCarpenterRecipes.register(recipes);
        GTNHAdjust.LOG.info(
            "Static carpenter recipes: {} registered, {} farm recipes registered, {} skipped",
            registered,
            farms,
            skipped + 44 - farms);
    }

    private static GTRecipe makeRecipe(NBTTagCompound tag) {
        ItemStack output = readStack(tag.getCompoundTag("output"));
        if (output == null) throw new IllegalArgumentException("output item is unavailable");
        FluidStack fluid = tag.hasKey("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid")) : null;
        if (tag.hasKey("fluid") && fluid == null) throw new IllegalArgumentException("input fluid is unavailable");
        NBTTagList storedInputs = tag.getTagList("inputs", 10);
        List<Object> inputs = new ArrayList<>();
        for (int i = 0; i < storedInputs.tagCount(); i++)
            inputs.add(readIngredient(storedInputs.getCompoundTagAt(i), -1));
        if (inputs.isEmpty() || inputs.size() > 9) throw new IllegalArgumentException("unsupported input count");
        int duration = tag.getInteger("duration"), eut = tag.getInteger("eut");
        if (duration <= 0 || eut <= 0) throw new IllegalArgumentException("invalid duration or energy");
        GTRecipeBuilder builder = GTValues.RA.stdBuilder()
            .itemInputs(inputs.toArray(new Object[0]))
            .itemOutputs(output)
            .duration(duration)
            .eut(eut);
        if (fluid != null) builder.fluidInputs(fluid);
        return builder.buildWithAlt()
            .orElseThrow(() -> new IllegalArgumentException("invalid recipe"));
    }

    static Object readIngredient(NBTTagCompound stored, int overrideCount) {
        int count = overrideCount > 0 ? overrideCount : stored.getInteger("count");
        if (count <= 0) throw new IllegalArgumentException("invalid input count");
        NBTTagList alternatives = stored.getTagList("alternatives", 10);
        List<ItemStack> choices = new ArrayList<>();
        for (int i = 0; i < alternatives.tagCount(); i++) {
            ItemStack stack = readStack(alternatives.getCompoundTagAt(i));
            if (stack != null) choices.add(stack);
        }
        if (choices.isEmpty())
            throw new IllegalArgumentException("all input alternatives unavailable for " + stored.getString("key"));
        if (choices.size() == 1 && !stored.hasKey("ore")) {
            ItemStack only = choices.get(0)
                .copy();
            only.stackSize = count;
            return only;
        }
        String name = "gtnhadjustStaticCarpenter" + UUID.nameUUIDFromBytes(
            stored.getString("key")
                .getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
        if (!OreDictionary.doesOreNameExist(name))
            for (ItemStack stack : choices) OreDictionary.registerOre(name, stack.copy());
        return new OreDictItemStack(name, count);
    }

    private static ItemStack readStack(NBTTagCompound tag) {
        Item item = (Item) Item.itemRegistry.getObject(tag.getString("item"));
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, tag.getInteger("count"), tag.getInteger("damage"));
        if (tag.hasKey("data", 10)) stack.setTagCompound(tag.getCompoundTag("data"));
        return stack;
    }
}
