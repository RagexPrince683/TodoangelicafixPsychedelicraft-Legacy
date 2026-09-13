/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.client.rendering;

import cpw.mods.fml.relauncher.ReflectionHelper;
import ivorius.psychedelicraft.internal.math.IvMatrixHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by lukas on 09.03.14.
 */
public class PsycheMatrixHelper
{
    private static Method getFOVMethod;
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT_BUFFER = BufferUtils.createIntBuffer(16);
    private static final Matrix4f capturedModelView = new Matrix4f();
    private static final Matrix4f capturedProjection = new Matrix4f();
    private static final int[] capturedViewport = new int[4];
    private static Entity capturedViewEntity;
    private static boolean hasCapturedWorldView;

    /** Captures the camera-relative matrices while Minecraft is rendering the normal world view. */
    public static void captureCurrentWorldView(Entity viewEntity)
    {
        MATRIX_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
        capturedModelView.load(MATRIX_BUFFER);

        MATRIX_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MATRIX_BUFFER);
        capturedProjection.load(MATRIX_BUFFER);

        VIEWPORT_BUFFER.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUFFER);
        for (int index = 0; index < capturedViewport.length; index++)
        {
            capturedViewport[index] = VIEWPORT_BUFFER.get(index);
        }

        capturedViewEntity = viewEntity;
        hasCapturedWorldView = true;
    }

    public static void invalidateCapturedWorldView()
    {
        hasCapturedWorldView = false;
        capturedViewEntity = null;
    }

    /**
     * Projects a camera-relative direction with the matrices captured for this exact view.
     * The returned x/y values use the top-left overlay coordinate system.
     */
    public static Vector3f projectDirectionCurrentView(Entity viewEntity, Vector3f direction,
                                                       int overlayHeight)
    {
        if (!hasCapturedWorldView || capturedViewEntity != viewEntity
            || capturedViewport[2] <= 0 || capturedViewport[3] <= 0)
        {
            return null;
        }

        Vector4f eyePoint = Matrix4f.transform(
            capturedModelView,
            new Vector4f(direction.x, direction.y, direction.z, 1.0f),
            null);
        Vector4f clipPoint = Matrix4f.transform(capturedProjection, eyePoint, null);

        if (clipPoint.w <= 0.00001f)
        {
            return null;
        }

        float inverseW = 1.0f / clipPoint.w;
        float normalizedX = clipPoint.x * inverseW;
        float normalizedY = clipPoint.y * inverseW;
        float normalizedZ = clipPoint.z * inverseW;
        if (normalizedZ < -1.0f || normalizedZ > 1.0f)
        {
            return null;
        }

        float viewportPixelX = (normalizedX * 0.5f + 0.5f) * capturedViewport[2];
        float viewportPixelY = (normalizedY * 0.5f + 0.5f) * capturedViewport[3];
        return new Vector3f(
            capturedViewport[0] + viewportPixelX,
            overlayHeight - (capturedViewport[1] + viewportPixelY),
            normalizedZ * 0.5f + 0.5f);
    }

    public static Matrix4f getCurrentProjectionMatrix(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();
        float degToRad = (float) Math.PI / 180.0f;

        float farPlaneDistance = (float) (mc.gameSettings.renderDistanceChunks * 16);
        return IvMatrixHelper.getProjectionMatrix(getCurrentFOV(partialTicks, true) * degToRad, (float) mc.displayWidth / (float) mc.displayHeight, 0.05f, farPlaneDistance * 2.0f);
    }

    public static Matrix4f getLookProjectionMatrix(Matrix4f projectionMatrix, Entity entity)
    {
        float degToRad = (float) Math.PI / 180.0f;

        float roll = 0.0f;
//        return IvMatrixHelper.lookFrom((float) entity.posX, (float) entity.posY, (float) entity.posZ, (entity.rotationYaw + 180.0f) * degToRad, entity.rotationPitch * degToRad, roll * degToRad, projectionMatrix, projectionMatrix);
        // Entity is in center implicitly when drawn
        return IvMatrixHelper.lookFrom(0.0f, 0.0f, 0.0f, (entity.rotationYaw + 180.0f) * degToRad, entity.rotationPitch * degToRad, roll * degToRad, projectionMatrix, projectionMatrix);
    }

    public static Vector3f projectPoint(Entity entity, Vector3f point, float partialTicks)
    {
        Matrix4f projectionMatrix = getLookProjectionMatrix(getCurrentProjectionMatrix(partialTicks), entity);

        Vector4f clippedPoint = new Vector4f(point.x, point.y, point.z, 1.0f);
        Matrix4f.transform(projectionMatrix, clippedPoint, clippedPoint);
        return new Vector3f(clippedPoint.x, -clippedPoint.y, clippedPoint.z);
    }

    public static Vector3f projectPointCurrentView(Vector3f point, float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase renderViewEntity = mc.renderViewEntity;

        Matrix4f transformMatrix = getCurrentProjectionMatrix(partialTicks);

        // Copied from EntityRenderer
        if (mc.gameSettings.thirdPersonView > 0)
        {
            float f1 = renderViewEntity.yOffset - 1.62F;
            double d0 = renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * (double) partialTicks;
            double d1 = renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * (double) partialTicks - (double) f1;
            double d2 = renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * (double) partialTicks;

//            double d7 = (double)(entityRenderer.thirdPersonDistanceTemp + (entityRenderer.thirdPersonDistance - entityRenderer.thirdPersonDistanceTemp) * partialTicks);
            double d7 = 4.0F;
            float f2;
            float f6;

            f6 = renderViewEntity.rotationYaw;
            f2 = renderViewEntity.rotationPitch;

            if (mc.gameSettings.thirdPersonView == 2)
            {
                f2 += 180.0F;
            }

            double d3 = (double) (-MathHelper.sin(f6 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d7;
            double d4 = (double) (MathHelper.cos(f6 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d7;
            double d5 = (double) (-MathHelper.sin(f2 / 180.0F * (float) Math.PI)) * d7;

            for (int k = 0; k < 8; ++k)
            {
                float f3 = (float) ((k & 1) * 2 - 1);
                float f4 = (float) ((k >> 1 & 1) * 2 - 1);
                float f5 = (float) ((k >> 2 & 1) * 2 - 1);
                f3 *= 0.1F;
                f4 *= 0.1F;
                f5 *= 0.1F;
                MovingObjectPosition movingobjectposition = mc.theWorld.rayTraceBlocks(Vec3.createVectorHelper(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5), Vec3.createVectorHelper(d0 - d3 + (double) f3 + (double) f5, d1 - d5 + (double) f4, d2 - d4 + (double) f5));

                if (movingobjectposition != null)
                {
                    double d6 = movingobjectposition.hitVec.distanceTo(Vec3.createVectorHelper(d0, d1, d2));

                    if (d6 < d7)
                    {
                        d7 = d6;
                    }
                }
            }

            if (mc.gameSettings.thirdPersonView == 2)
            {
                Matrix4f.rotate(180.0f, new Vector3f(0.0f, 1.0f, 0.0f), transformMatrix, transformMatrix);
            }

            Matrix4f.rotate(renderViewEntity.rotationPitch - f2, new Vector3f(1.0f, 0.0f, 0.0f), transformMatrix, transformMatrix);
            Matrix4f.rotate(renderViewEntity.rotationYaw - f6, new Vector3f(0.0f, 1.0f, 0.0f), transformMatrix, transformMatrix);
            Matrix4f.translate(new Vector3f(0.0f, 0.0f, (float) (-d7)), transformMatrix, transformMatrix);
            Matrix4f.rotate(f6 - renderViewEntity.rotationYaw, new Vector3f(0.0f, 1.0f, 0.0f), transformMatrix, transformMatrix);
            Matrix4f.rotate(f2 - renderViewEntity.rotationPitch, new Vector3f(1.0f, 0.0f, 0.0f), transformMatrix, transformMatrix);
        }

        transformMatrix = getLookProjectionMatrix(transformMatrix, renderViewEntity);

        Vector4f clippedPoint = new Vector4f(point.x, point.y, point.z, 1.0f);
        Matrix4f.transform(transformMatrix, clippedPoint, clippedPoint);
        return new Vector3f(clippedPoint.x, -clippedPoint.y, clippedPoint.z);
    }

    public static float getCurrentFOV(float partialTicks, boolean isWorld)
    {
        EntityRenderer instance = Minecraft.getMinecraft().entityRenderer;
        if (getFOVMethod == null)
            getFOVMethod = ReflectionHelper.findMethod(EntityRenderer.class, instance, new String[]{"func_78481_a", "getFOVModifier"}, Float.TYPE, Boolean.TYPE);

        try
        {
            return (Float) getFOVMethod.invoke(instance, partialTicks, isWorld);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }

        return 90.0f;
    }
}
