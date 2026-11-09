package com.gtnhadjust.animation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;

import org.lwjgl.opengl.GL11;

/** Bridges GTNRLib's shoulder-space animation to YSMU's first-person arm renderer. */
public final class YsmuAnimatedArm {

    private static final Logger LOG = Logger.getLogger("GTNHAdjust");
    private static Method contextMethod;
    private static Method renderMethod;
    private static Field rendererField, playerField, armField, textureField;
    private static boolean initialized, failed, rendering, mirrored, resolving;

    private YsmuAnimatedArm() {}

    public static boolean isRendering() {
        return rendering;
    }

    public static boolean isResolving() {
        return resolving;
    }

    public static boolean isMirrored() {
        return rendering && mirrored;
    }

    private static void initialize() throws ReflectiveOperationException {
        if (initialized) return;
        Class<?> type = Class.forName("com.fox.ysmu.client.renderer.FirstPersonHandRenderer");
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName()
                .equals("getCustomHandRenderContext")) contextMethod = method;
            if (method.getName()
                .equals("renderCustomEmptyMainHand")) renderMethod = method;
        }
        if (contextMethod == null || renderMethod == null) throw new NoSuchMethodException("YSMU 1.9 hand renderer");
        contextMethod.setAccessible(true);
        renderMethod.setAccessible(true);
        Class<?> context = contextMethod.getReturnType();
        rendererField = field(context, "renderer");
        playerField = field(context, "customPlayer");
        armField = field(context, "rightArm");
        textureField = field(context, "texture");
        initialized = true;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static boolean render(boolean left, float shoulderRoll) {
        if (failed || rendering) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.entityRenderer == null) return false;
        ItemRenderer itemRenderer = mc.entityRenderer.itemRenderer;
        try {
            initialize();
            Object context;
            try {
                resolving = true;
                context = contextMethod.invoke(null, mc, mc.thePlayer, itemRenderer);
            } finally {
                resolving = false;
            }
            if (context == null) return false;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            try {
                if (left) GL11.glScalef(-1, 1, 1);
                GL11.glRotatef((float) Math.toDegrees(shoulderRoll) * (left ? -1 : 1), 0, 0, 1);
                // YSMU aligns to the biped origin; GTNRLib positions the arm at its shoulder.
                GL11.glTranslatef(-1f / 16, 2f / 16, 0);
                mirrored = left;
                rendering = true;
                renderMethod.invoke(
                    null,
                    mc,
                    mc.thePlayer,
                    itemRenderer,
                    rendererField.get(context),
                    playerField.get(context),
                    armField.get(context),
                    textureField.get(context),
                    1.0f);
            } finally {
                rendering = false;
                mirrored = false;
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            LOG.warning("YSMU animated arm compatibility disabled; using GTNRLib arms: " + e);
            return false;
        }
    }
}
