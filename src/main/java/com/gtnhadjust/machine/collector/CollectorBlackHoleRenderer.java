package com.gtnhadjust.machine.collector;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Self-contained black hole shadow, rotating spherical confinement field and four outward extraction jets. */
@SideOnly(Side.CLIENT)
final class CollectorBlackHoleRenderer {

    private static final int[][] AXES = { { 1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 } };
    private static final int[][] JETS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    private static final int LATITUDES = 32;
    private static final int LONGITUDES = 96;
    private static final double SHADOW_RADIUS = 1.24;
    private static final double FIELD_RADIUS = 1.95;
    private static final double INNER = 1.29;
    private static final double OUTER = 8.5;
    // Shared unit sphere vertices avoid rebuilding the two spherical surfaces every frame.
    private static final double[] SPHERE = createSphere();
    private final World world;
    private final long startTime;
    private final FloatBuffer orientation = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final int[] offset = new int[3];
    private final double[] right = new double[3];
    private final double[] up = new double[3];
    private final double[] eyeDirection = new double[3];
    // The silhouette lies on the sphere's perspective tangent circle, not its equatorial view plane.
    private double eyeDistance;
    private double rimOffset;
    private double rimRadius;
    private final float[] shellAlpha = new float[(LATITUDES + 1) * (LONGITUDES + 1)];

    CollectorBlackHoleRenderer(World world) {
        this.world = world;
        startTime = world.getTotalWorldTime();
    }

    /** Preserve the caller's GL state and align all effects with the multiblock, including mirrored alignment. */
    void render(double x, double y, double z, float partialTick, ExtendedFacing facing) {
        double time = world.getTotalWorldTime() + (double) partialTick;
        double growth = Math.max(0.001, Math.min(1, (time - startTime) / 40.0));
        growth = growth * growth * (3 - 2 * growth);
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
        int textureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT
                | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL20.glUseProgram(0);
            // Both texture units must be disabled for untextured, self-lit geometry.
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
            GL11.glMultMatrix(orientation);
            GL11.glScaled(growth, growth, growth);
            readBillboardBasis();

            // The opaque shadow writes depth, so rear jets and field lines cannot shine through it.
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            renderShadow();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glDepthMask(false);
            renderPhotonRim(time);
            renderField(time);
            GL11.glPushMatrix();
            try {
                GL11.glRotated(time * 0.13, 0, 1, 0);
                GL11.glRotated(19, 0, 0, 1);
                renderConfinement(time);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(textureUnit);
            GL20.glUseProgram(shader);
        }
    }

    /** Cache smooth unit sphere positions, including the duplicate longitude seam. */
    private static double[] createSphere() {
        double[] vertices = new double[(LATITUDES + 1) * (LONGITUDES + 1) * 3];
        for (int lat = 0; lat <= LATITUDES; lat++) {
            double angle = Math.PI * lat / LATITUDES;
            for (int lon = 0; lon <= LONGITUDES; lon++) {
                double azimuth = Math.PI * 2 * lon / LONGITUDES;
                int index = (lat * (LONGITUDES + 1) + lon) * 3;
                vertices[index] = Math.sin(angle) * Math.cos(azimuth);
                vertices[index + 1] = Math.cos(angle);
                vertices[index + 2] = Math.sin(angle) * Math.sin(azimuth);
            }
        }
        return vertices;
    }

    /** Recover the actual eye position and tangent circle so the thin rim stays attached in close and oblique views. */
    private void readBillboardBasis() {
        modelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        double scale = Math.sqrt(
            modelView.get(0) * modelView.get(0) + modelView.get(4) * modelView.get(4)
                + modelView.get(8) * modelView.get(8));
        double distanceSquared = 0;
        for (int axis = 0; axis < 3; axis++) {
            eyeDirection[axis] = -(modelView.get(axis * 4) * modelView.get(12)
                + modelView.get(axis * 4 + 1) * modelView.get(13)
                + modelView.get(axis * 4 + 2) * modelView.get(14)) / (scale * scale);
            distanceSquared += eyeDirection[axis] * eyeDirection[axis];
        }
        eyeDistance = Math.sqrt(distanceSquared);
        if (eyeDistance <= SHADOW_RADIUS) return;
        for (int axis = 0; axis < 3; axis++) eyeDirection[axis] /= eyeDistance;
        double ux = modelView.get(1) / scale, uy = modelView.get(5) / scale, uz = modelView.get(9) / scale;
        right[0] = uy * eyeDirection[2] - uz * eyeDirection[1];
        right[1] = uz * eyeDirection[0] - ux * eyeDirection[2];
        right[2] = ux * eyeDirection[1] - uy * eyeDirection[0];
        double length = Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
        for (int axis = 0; axis < 3; axis++) right[axis] /= length;
        up[0] = eyeDirection[1] * right[2] - eyeDirection[2] * right[1];
        up[1] = eyeDirection[2] * right[0] - eyeDirection[0] * right[2];
        up[2] = eyeDirection[0] * right[1] - eyeDirection[1] * right[0];
        rimOffset = SHADOW_RADIUS * SHADOW_RADIUS / eyeDistance;
        rimRadius = SHADOW_RADIUS * Math.sqrt(1 - SHADOW_RADIUS * SHADOW_RADIUS / distanceSquared);
    }

    /** The opaque black sphere supplies an unlit shadow without an accretion disc or a GT render tile. */
    private static void renderShadow() {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_F(0, 0, 0, 1);
        for (int lat = 0; lat < LATITUDES; lat++) {
            for (int lon = 0; lon < LONGITUDES; lon++) {
                int a = lat * (LONGITUDES + 1) + lon;
                int b = a + LONGITUDES + 1;
                sphereVertex(tess, a, SHADOW_RADIUS);
                sphereVertex(tess, b, SHADOW_RADIUS);
                sphereVertex(tess, b + 1, SHADOW_RADIUS);
                sphereVertex(tess, a + 1, SHADOW_RADIUS);
            }
        }
        tess.draw();
    }

    /** A thin cool-white photon-ring impression follows the silhouette; its uneven brightness rotates. */
    private void renderPhotonRim(double time) {
        if (eyeDistance <= SHADOW_RADIUS) return;
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int layer = 0; layer < 5; layer++) {
            double inner = rimRadius + 0.008 + layer * 0.014;
            double outer = inner + 0.014;
            for (int segment = 0; segment < 192; segment++) {
                double angle = segment * Math.PI * 2 / 192;
                double next = (segment + 1) * Math.PI * 2 / 192;
                float alpha = (float) ((0.74 + 0.16 * Math.sin(angle * 3 - time * 0.025)) * Math.exp(-layer * 0.85));
                tess.setColorRGBA_F(0.82f, 0.93f, 1, alpha);
                billboardVertex(tess, inner, angle);
                billboardVertex(tess, outer, angle);
                billboardVertex(tess, outer, next);
                billboardVertex(tess, inner, next);
            }
        }
        tess.draw();
    }

    /** Emit a rim point in the view plane without creating a flat disc over the black shadow. */
    private void billboardVertex(Tessellator tess, double radius, double angle) {
        double a = Math.cos(angle) * radius, b = Math.sin(angle) * radius;
        tess.addVertex(
            right[0] * a + up[0] * b + eyeDirection[0] * rimOffset,
            right[1] * a + up[1] * b + eyeDirection[1] * rimOffset,
            right[2] * a + up[2] * b + eyeDirection[2] * rimOffset);
    }

    /** A translucent full sphere has limb-weighted emission and materializing patches on its rotating surface. */
    private void renderConfinement(double time) {
        modelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        double vx = modelView.get(2), vy = modelView.get(6), vz = modelView.get(10);
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        vx /= length;
        vy /= length;
        vz /= length;
        for (int index = 0; index < shellAlpha.length; index++) {
            int vertex = index * 3;
            double x = SPHERE[vertex], y = SPHERE[vertex + 1], z = SPHERE[vertex + 2];
            double limb = 1 - Math.abs(x * vx + y * vy + z * vz);
            double cloud = 0.5 + 0.5 * Math.sin(x * 11 + y * 7 + z * 13 - time * 0.016)
                * Math.sin(x * 5 - y * 13 + z * 3 + time * 0.011);
            shellAlpha[index] = (float) (0.005 + 0.07 * limb * limb * limb
                + 0.025 * cloud * cloud * (0.25 + 0.75 * limb));
        }
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int lat = 0; lat < LATITUDES; lat++) {
            for (int lon = 0; lon < LONGITUDES; lon++) {
                int a = lat * (LONGITUDES + 1) + lon;
                int b = a + LONGITUDES + 1;
                shellVertex(tess, a);
                shellVertex(tess, b);
                shellVertex(tess, b + 1);
                shellVertex(tess, a + 1);
            }
        }
        // Sparse broken contours reveal the full sphere without turning it into an opaque cage.
        for (int latitude = 1; latitude < 8; latitude++) {
            double polar = Math.PI * latitude / 8;
            for (int segment = 0; segment < 128; segment++) {
                if ((segment + latitude * 7) % 16 >= 11) continue;
                double angle = segment * Math.PI * 2 / 128;
                double next = (segment + 1) * Math.PI * 2 / 128;
                float scan = (float) Math.pow(0.5 + 0.5 * Math.cos(angle - time * 0.035 + latitude), 6);
                tess.setColorRGBA_F(0.38f, 0.72f, 0.95f, 0.055f + scan * 0.20f);
                contourVertex(tess, polar - 0.0025, angle);
                contourVertex(tess, polar + 0.0025, angle);
                contourVertex(tess, polar + 0.0025, next);
                contourVertex(tess, polar - 0.0025, next);
            }
        }
        tess.draw();
    }

    /** Draw a shell vertex using the precomputed per-frame surface density. */
    private void shellVertex(Tessellator tess, int index) {
        tess.setColorRGBA_F(0.34f, 0.61f, 0.83f, shellAlpha[index]);
        sphereVertex(tess, index, FIELD_RADIUS);
    }

    /** Map a cached sphere point to its world-block radius. */
    private static void sphereVertex(Tessellator tess, int index, double radius) {
        int vertex = index * 3;
        tess.addVertex(SPHERE[vertex] * radius, SPHERE[vertex + 1] * radius, SPHERE[vertex + 2] * radius);
    }

    /** Keep thin materializing contours just above the confinement surface. */
    private static void contourVertex(Tessellator tess, double polar, double angle) {
        double radius = FIELD_RADIUS + 0.005;
        tess.addVertex(
            Math.sin(polar) * Math.cos(angle) * radius,
            Math.cos(polar) * radius,
            Math.sin(polar) * Math.sin(angle) * radius);
    }

    /** Batch continuous blue-white jets, outward helical flow and expanding pressure-wave impressions. */
    private static void renderField(double time) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        for (int[] jet : JETS) {
            for (int segment = 0; segment < 64; segment++) {
                double start = INNER + (OUTER - INNER) * segment / 64;
                double end = INNER + (OUTER - INNER) * (segment + 1) / 64;
                float fade = (float) Math.min(1, (OUTER - end) / 0.65);
                float flow = (float) (0.65 + 0.35 * Math.cos(start * 3.2 - time * 0.24));
                tess.setColorRGBA_F(0.23f, 0.55f, 1, 0.055f * fade);
                beam(tess, jet, start, end, 0.14 + start * 0.027, 0.14 + end * 0.027);
                tess.setColorRGBA_F(0.35f, 0.76f, 1, 0.19f * fade * flow);
                beam(tess, jet, start, end, 0.055 + start * 0.01, 0.055 + end * 0.01);
                tess.setColorRGBA_F(0.86f, 0.95f, 1, 0.78f * fade * flow);
                beam(tess, jet, start, end, 0.017 + start * 0.002, 0.017 + end * 0.002);
                for (int strand = 0; strand < 2; strand++) {
                    double angle = start * 2.8 - time * 0.19 + strand * Math.PI;
                    double next = end * 2.8 - time * 0.19 + strand * Math.PI;
                    tess.setColorRGBA_F(0.22f, 0.63f, 1, 0.15f * fade);
                    ribbon(tess, jet, start, end, angle, next, 0.065);
                    tess.setColorRGBA_F(0.60f, 0.88f, 1, 0.85f * fade * flow);
                    ribbon(tess, jet, start, end, angle, next, 0.022);
                }
            }
        }
        for (int wave = 0; wave < 4; wave++) {
            double phase = (time / 80.0 + wave / 4.0) % 1;
            double radius = FIELD_RADIUS + 0.08 + phase * 2.65;
            float alpha = (float) (Math.sin(phase * Math.PI) * (1 - phase));
            tess.setColorRGBA_F(0.2f, 0.58f, 1, alpha * 0.14f);
            ring(tess, radius, 0.075, time * 0.006 + wave * Math.PI / 4);
            tess.setColorRGBA_F(0.66f, 0.88f, 1, alpha * 0.65f);
            ring(tess, radius, 0.016, time * 0.006 + wave * Math.PI / 4);
        }
        tess.draw();
    }

    /** Layered crossed ribbons form a narrow bright core and a broader faint emission envelope. */
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

    /** Double helices widen away from the core; constant-phase crests travel outwards over time. */
    private static void ribbon(Tessellator tess, int[] jet, double start, double end, double angle, double next,
        double width) {
        double radius = 0.14 + start * 0.025, nextRadius = 0.14 + end * 0.025;
        double side = Math.cos(angle) * radius, up = Math.sin(angle) * radius;
        double nextSide = Math.cos(next) * nextRadius, nextUp = Math.sin(next) * nextRadius;
        vertex(tess, jet, start, side - width, up);
        vertex(tess, jet, end, nextSide - width, nextUp);
        vertex(tess, jet, end, nextSide + width, nextUp);
        vertex(tess, jet, start, side + width, up);
        vertex(tess, jet, start, side, up - width);
        vertex(tess, jet, end, nextSide, nextUp - width);
        vertex(tess, jet, end, nextSide, nextUp + width);
        vertex(tess, jet, start, side, up + width);
    }

    /** Thin wavefronts expand beyond the spherical field, with slowly changing plane orientations. */
    private static void ring(Tessellator tess, double radius, double width, double tilt) {
        double sin = Math.sin(tilt), cos = Math.cos(tilt);
        for (int segment = 0; segment < 128; segment++) {
            double angle = segment * Math.PI * 2 / 128;
            double next = (segment + 1) * Math.PI * 2 / 128;
            ringVertex(tess, radius - width, angle, sin, cos);
            ringVertex(tess, radius - width, next, sin, cos);
            ringVertex(tess, radius + width, next, sin, cos);
            ringVertex(tess, radius + width, angle, sin, cos);
        }
    }

    /** Place a wavefront vertex on its tilted plane. */
    private static void ringVertex(Tessellator tess, double radius, double angle, double sin, double cos) {
        double z = Math.sin(angle) * radius;
        tess.addVertex(Math.cos(angle) * radius, z * sin, z * cos);
    }

    /** Convert beam-local radial, transverse and vertical coordinates into the multiblock's coordinates. */
    private static void vertex(Tessellator tess, int[] jet, double radial, double side, double up) {
        tess.addVertex(jet[0] * radial - jet[1] * side, up, jet[1] * radial + jet[0] * side);
    }
}
