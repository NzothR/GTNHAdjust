package com.gtnhadjust.structurelib;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.ConfigurationHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** One camera-relative box, independent of block count, chunk contents and particle limits. */
@SideOnly(Side.CLIENT)
public final class PlanetaryEngineOutline {

    private static final PlanetaryEngineOutline INSTANCE = new PlanetaryEngineOutline();

    static {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    private World world;
    private AxisAlignedBB bounds;
    private int expires;

    private PlanetaryEngineOutline() {}

    public static void show(World world, AxisAlignedBB bounds) {
        INSTANCE.world = bounds == null ? null : world;
        INSTANCE.bounds = bounds;
        if (bounds != null) INSTANCE.expires = Minecraft.getMinecraft().thePlayer.ticksExisted
            + ConfigurationHandler.INSTANCE.getHintLifespan();
    }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        if (bounds == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world || mc.thePlayer == null || mc.thePlayer.ticksExisted >= expires) {
            show(null, null);
            return;
        }
        Entity camera = mc.renderViewEntity;
        double x = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        double y = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        double z = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor3f(0.2f, 1.0f, 1.0f);
            GL11.glLineWidth(2);
            RenderGlobal.drawOutlinedBoundingBox(bounds.getOffsetBoundingBox(-x, -y, -z), -1);
        } finally {
            GL11.glPopAttrib();
        }
    }
}
