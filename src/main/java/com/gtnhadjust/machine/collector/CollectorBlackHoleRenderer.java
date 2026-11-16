package com.gtnhadjust.machine.collector;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.tileentities.render.RenderingTileEntityBlackhole;

/** Native GT black hole with four outward decompression jets, using a client-only tile and batched light geometry. */
@SideOnly(Side.CLIENT)
final class CollectorBlackHoleRenderer {

    private final RenderingTileEntityBlackhole tile = new RenderingTileEntityBlackhole();
    private static final int[][] AXES = { { 1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 } };
    private static final int[][] JETS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    private static final double INNER = 1.1;
    private static final double OUTER = 8.5;
    private final FloatBuffer orientation = BufferUtils.createFloatBuffer(16);
    private final int[] offset = new int[3];

    CollectorBlackHoleRenderer(World world) {
        NBTTagCompound data = new NBTTagCompound();
        data.setFloat("BLACKHOLESTABILITY", 1.0f);
        data.setBoolean("BLACKHOLESCALING", true);
        data.setLong("BLACKHOLESTART_TIME", world.getTotalWorldTime());
        // The native inward-facing lasers are replaced by outward jets; the black hole itself stays native.
        data.setBoolean("BLACKHOLELASER_RENDER", false);
        tile.readFromNBT(data);
        tile.setWorldObj(world);
    }

    /** Align the extraction field to the multiblock, including rotation and mirror alignment. */
    void render(double x, double y, double z, float partialTick, ExtendedFacing facing) {
        TileEntitySpecialRenderer renderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(tile);
        if (renderer != null) renderer.renderTileEntityAt(tile, x, y, z, partialTick);
        double time = tile.getWorldObj()
            .getTotalWorldTime() + (double) partialTick;
        double growth = Math.min(1, (time - tile.getStartTime()) / 40.0);
        orientation.clear();
        for (int[] axis : AXES) {
            facing.getWorldOffset(axis, offset);
            orientation.put(offset[0])
                .put(offset[1])
                .put(offset[2])
                .put(0);
        }
        orientation.put(0)
            .put(0)
            .put(0)
            .put(1)
            .flip();
        int shader = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL20.glUseProgram(0);
            GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
            GL11.glMultMatrix(orientation);
            GL11.glScaled(growth, growth, growth);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            renderField(time);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL20.glUseProgram(shader);
        }
    }

    /** One draw call for the steady cores, outward moving packets, double helices and expanding wavefronts. */
    private static void renderField(double time) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setBrightness(0xF000F0);
        for (int[] jet : JETS) {
            tess.setColorRGBA_F(0.12f, 0.68f, 1.0f, 0.16f);
            beam(tess, jet, INNER, OUTER, 0.09, 0.24);
            tess.setColorRGBA_F(0.55f, 0.95f, 1.0f, 0.6f);
            beam(tess, jet, INNER, OUTER, 0.018, 0.045);
            for (int packet = 0; packet < 6; packet++) {
                double phase = (time / 32.0 + packet / 6.0) % 1;
                double end = INNER + (OUTER - INNER) * phase;
                float alpha = (float) (Math.sin(phase * Math.PI) * 0.85);
                tess.setColorRGBA_F(1.0f, 0.78f, 0.32f, alpha);
                beam(tess, jet, Math.max(INNER, end - 0.7), end, 0.025, 0.12);
            }
            for (int strand = 0; strand < 2; strand++) {
                for (int segment = 0; segment < 32; segment++) {
                    double start = INNER + (OUTER - INNER) * segment / 32;
                    double end = INNER + (OUTER - INNER) * (segment + 1) / 32;
                    // Constant-phase crests travel outwards as time increases.
                    double angle = start * 2.6 - time * 0.16 + strand * Math.PI;
                    double next = end * 2.6 - time * 0.16 + strand * Math.PI;
                    double radius = 0.10 + segment * 0.006;
                    tess.setColorRGBA_F(0.18f, 0.8f, 1.0f, 0.42f * (1 - segment / 40.0f));
                    ribbon(tess, jet, start, end, angle, next, radius);
                }
            }
        }
        for (int wave = 0; wave < 3; wave++) {
            double phase = (time / 60.0 + wave / 3.0) % 1;
            double radius = 1.25 + phase * 3.1;
            tess.setColorRGBA_F(0.15f, 0.75f, 1.0f, (float) ((1 - phase) * 0.32));
            ring(tess, radius, 0.025 + phase * 0.025, time * 0.012 + wave * Math.PI / 3);
        }
        tess.draw();
    }

    /** Crossed tapered ribbons retain the original four axial beam silhouette from every viewing direction. */
    private static void beam(Tessellator tess, int[] jet, double start, double end, double inner, double outer) {
        vertex(tess, jet, start, -inner, 0);
        vertex(tess, jet, end, -outer, 0);
        vertex(tess, jet, end, outer, 0);
        vertex(tess, jet, start, inner, 0);
        vertex(tess, jet, start, 0, -inner);
        vertex(tess, jet, end, 0, -outer);
        vertex(tess, jet, end, 0, outer);
        vertex(tess, jet, start, 0, inner);
    }

    /** Twin helical filaments wind around the beam without spawning world particles or entities. */
    private static void ribbon(Tessellator tess, int[] jet, double start, double end, double angle, double next,
        double radius) {
        double side = Math.cos(angle) * radius;
        double up = Math.sin(angle) * radius;
        double nextSide = Math.cos(next) * radius;
        double nextUp = Math.sin(next) * radius;
        vertex(tess, jet, start, side - 0.015, up);
        vertex(tess, jet, end, nextSide - 0.015, nextUp);
        vertex(tess, jet, end, nextSide + 0.015, nextUp);
        vertex(tess, jet, start, side + 0.015, up);
        vertex(tess, jet, start, side, up - 0.015);
        vertex(tess, jet, end, nextSide, nextUp - 0.015);
        vertex(tess, jet, end, nextSide, nextUp + 0.015);
        vertex(tess, jet, start, side, up + 0.015);
    }

    /** Radial wavefronts expand away from the event horizon on slowly precessing planes. */
    private static void ring(Tessellator tess, double radius, double width, double tilt) {
        double sin = Math.sin(tilt), cos = Math.cos(tilt);
        for (int segment = 0; segment < 96; segment++) {
            double angle = segment * Math.PI * 2 / 96;
            double next = (segment + 1) * Math.PI * 2 / 96;
            ringVertex(tess, radius - width, angle, sin, cos);
            ringVertex(tess, radius - width, next, sin, cos);
            ringVertex(tess, radius + width, next, sin, cos);
            ringVertex(tess, radius + width, angle, sin, cos);
        }
    }

    private static void ringVertex(Tessellator tess, double radius, double angle, double sin, double cos) {
        double z = Math.sin(angle) * radius;
        tess.addVertex(Math.cos(angle) * radius, z * sin, z * cos);
    }

    private static void vertex(Tessellator tess, int[] jet, double radial, double side, double up) {
        tess.addVertex(jet[0] * radial - jet[1] * side, up, jet[1] * radial + jet[0] * side);
    }
}
