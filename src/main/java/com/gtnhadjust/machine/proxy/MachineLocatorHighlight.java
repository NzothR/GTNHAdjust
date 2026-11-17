package com.gtnhadjust.machine.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A short-lived through-wall outline; dimension/chunk arrival starts its visible duration after teleportation. */
@SideOnly(Side.CLIENT)
public final class MachineLocatorHighlight {

    private static MachineLocatorNavigation.Highlight target;
    private static Object connection;
    private static long deadline, expires;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new MachineLocatorHighlight());
    }

    public static void show(MachineLocatorNavigation.Highlight message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            target = message;
            connection = mc.getNetHandler();
            deadline = Minecraft.getSystemTime() + 10000;
            expires = 0;
        });
    }

    /** Render one controller block in cyan, while restoring all changed OpenGL attributes. */
    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (target == null) return;
        long now = Minecraft.getSystemTime();
        if (mc.getNetHandler() != connection || now > (expires == 0 ? deadline : expires)) {
            target = null;
            connection = null;
            return;
        }
        if (mc.theWorld == null || mc.theWorld.provider.dimensionId != target.dimension) return;
        if (expires == 0) expires = now + 15000;
        Entity camera = mc.renderViewEntity;
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-x, -y, -z);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(0.2f, 1, 1, 0.9f);
            GL11.glLineWidth(3);
            RenderGlobal.drawOutlinedBoundingBox(
                AxisAlignedBB.getBoundingBox(
                    target.x - 0.005,
                    target.y - 0.005,
                    target.z - 0.005,
                    target.x + 1.005,
                    target.y + 1.005,
                    target.z + 1.005),
                -1);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
