package com.gtnhadjust.machine.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Uses the same Shift inventory preview convention as GregTech's recorded data stick. */
@SideOnly(Side.CLIENT)
final class MachineLocatorRenderer implements IItemRenderer {

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY && GuiScreen.isShiftKeyDown() && MachineLocatorItem.read(item) != null;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        RenderHelper.enableGUIStandardItemLighting();
        RenderItem.getInstance()
            .renderItemAndEffectIntoGUI(
                mc.fontRenderer,
                mc.getTextureManager(),
                MachineLocatorItem.read(item)
                    .controller(),
                0,
                0);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopAttrib();
    }
}
