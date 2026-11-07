package com.gtnhadjust.machine.collector;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Render the actual GTNH dimension block in the standard inventory perspective. */
@SideOnly(Side.CLIENT)
final class TierDimensionRenderer implements IItemRenderer {

    private final RenderBlocks blocks = new RenderBlocks();

    private static Block representative(ItemStack stack) {
        int tier = stack.getItemDamage();
        if (tier < 0 || tier >= TierDimensionItem.REPRESENTATIVES.length) return null;
        Item item = GameRegistry
            .findItem("gtneioreplugin", "blockDimensionDisplay_" + TierDimensionItem.REPRESENTATIVES[tier]);
        return item == null ? null : Block.getBlockFromItem(item);
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY && representative(stack) != null;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        Block block = representative(stack);
        if (block == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            minecraft.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glTranslatef(-2.0F, 3.0F, -3.0F);
            GL11.glScalef(10.0F, 10.0F, 10.0F);
            GL11.glTranslatef(1.0F, 0.5F, 1.0F);
            GL11.glScalef(1.0F, 1.0F, -1.0F);
            GL11.glRotatef(210.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glColor4f(1.0F, 216.0F / 255.0F, 117.0F / 255.0F, 1.0F);
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            blocks.useInventoryTint = false;
            blocks.renderBlockAsItem(block, 0, 1.0F);
        } finally {
            blocks.useInventoryTint = true;
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        FontRenderer font = minecraft.fontRenderer;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0.0F, 0.0F, 300.0F);
            GL11.glScalef(0.75F, 0.75F, 1.0F);
            font.drawString("T" + stack.getItemDamage(), 0, (int) (16 / 0.75F) - font.FONT_HEIGHT + 1, 0xFFFFFF, true);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
