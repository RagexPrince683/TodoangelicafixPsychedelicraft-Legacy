package ivorius.psychedelicraft.client.rendering;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

/**
 * Saves the compatibility-profile state touched by Psychedelicraft's screen effects.
 *
 * <p>The explicit program, framebuffer, and active-texture restoration is intentional.
 * Angelica tracks those objects outside the legacy attribute stack, while vanilla
 * normally renders into Minecraft's framebuffer. Querying the live bindings makes
 * the same code safe for both render pipelines.</p>
 */
public final class RenderStateGuard
{
    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_MAX_DRAW_BUFFERS = 0x8824;
    private static final int GL_DRAW_BUFFER0 = 0x8825;
    private static final int TRACKED_TEXTURE_UNITS = 4;
    private static final ThreadLocal<IntBuffer> VIEWPORT_BUFFER = new ThreadLocal<IntBuffer>()
    {
        @Override
        protected IntBuffer initialValue()
        {
            return BufferUtils.createIntBuffer(4);
        }
    };

    private final int activeTexture;
    private final int drawFramebuffer;
    private final int readFramebuffer;
    private final int matrixMode;
    private final int program;
    private final int drawBuffer;
    private final int[] drawBuffers;
    private final int readBuffer;
    private final int viewportX;
    private final int viewportY;
    private final int viewportWidth;
    private final int viewportHeight;
    private final int[] textureBindings = new int[TRACKED_TEXTURE_UNITS];
    private boolean restored;

    private RenderStateGuard()
    {
        activeTexture = GL11.glGetInteger(GL13_ACTIVE_TEXTURE);
        drawFramebuffer = getBoundDrawFramebuffer();
        readFramebuffer = getBoundReadFramebuffer();
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        drawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
        drawBuffers = captureDrawBuffers(drawFramebuffer, drawBuffer);
        readBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);

        IntBuffer viewport = VIEWPORT_BUFFER.get();
        viewport.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        viewportX = viewport.get(0);
        viewportY = viewport.get(1);
        viewportWidth = viewport.get(2);
        viewportHeight = viewport.get(3);

        for (int unit = 0; unit < TRACKED_TEXTURE_UNITS; unit++)
        {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            textureBindings[unit] = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
        }
        OpenGlHelper.setActiveTexture(activeTexture);

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // Tessellator changes the enabled client arrays and their pointer bindings.
        // Pixel-store state is not touched by the guarded screen effects.
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        pushMatrix(GL11.GL_TEXTURE);
        pushMatrix(GL11.GL_PROJECTION);
        pushMatrix(GL11.GL_MODELVIEW);
        GL11.glMatrixMode(matrixMode);
    }

    public static RenderStateGuard capture()
    {
        return new RenderStateGuard();
    }

    public void restore()
    {
        if (restored)
        {
            return;
        }

        popMatrix(GL11.GL_TEXTURE);
        popMatrix(GL11.GL_PROJECTION);
        popMatrix(GL11.GL_MODELVIEW);
        GL11.glPopClientAttrib();
        GL11.glPopAttrib();

        OpenGlHelper.func_153161_d(program);
        if (OpenGlHelper.framebufferSupported)
        {
            if (supportsSeparateFramebufferBindings())
            {
                OpenGlHelper.func_153171_g(GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                OpenGlHelper.func_153171_g(GL_READ_FRAMEBUFFER, readFramebuffer);
            }
            else
            {
                OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, drawFramebuffer);
            }
        }
        restoreDrawBuffers();
        GL11.glReadBuffer(readBuffer);
        for (int unit = 0; unit < TRACKED_TEXTURE_UNITS; unit++)
        {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBindings[unit]);
        }
        OpenGlHelper.setActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
        restored = true;
    }

    public static int getBoundFramebuffer()
    {
        return getBoundDrawFramebuffer();
    }

    public static int getBoundDrawFramebuffer()
    {
        if (!OpenGlHelper.framebufferSupported)
        {
            return 0;
        }
        return GL11.glGetInteger(GL_FRAMEBUFFER_BINDING);
    }

    public static int getBoundReadFramebuffer()
    {
        if (!OpenGlHelper.framebufferSupported)
        {
            return 0;
        }
        return supportsSeparateFramebufferBindings()
            ? GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
            : GL11.glGetInteger(GL_FRAMEBUFFER_BINDING);
    }

    public static boolean supportsSeparateFramebufferBindings()
    {
        return GLContext.getCapabilities().OpenGL30;
    }

    public int getDrawBuffer()
    {
        return drawBuffer;
    }

    public int getReadBuffer()
    {
        return readBuffer;
    }

    public int getViewportX()
    {
        return viewportX;
    }

    public int getViewportY()
    {
        return viewportY;
    }

    public int getViewportWidth()
    {
        return viewportWidth;
    }

    public int getViewportHeight()
    {
        return viewportHeight;
    }

    private static void pushMatrix(int mode)
    {
        GL11.glMatrixMode(mode);
        GL11.glPushMatrix();
    }

    private static void popMatrix(int mode)
    {
        GL11.glMatrixMode(mode);
        GL11.glPopMatrix();
    }

    private static int[] captureDrawBuffers(int framebuffer, int firstDrawBuffer)
    {
        if (framebuffer == 0 || !OpenGlHelper.framebufferSupported)
        {
            return new int[]{firstDrawBuffer};
        }

        int count = Math.max(1, GL11.glGetInteger(GL_MAX_DRAW_BUFFERS));
        int[] buffers = new int[count];
        for (int index = 0; index < count; index++)
        {
            buffers[index] = GL11.glGetInteger(GL_DRAW_BUFFER0 + index);
        }
        return buffers;
    }

    private void restoreDrawBuffers()
    {
        if (drawFramebuffer == 0 || drawBuffers.length == 1)
        {
            GL11.glDrawBuffer(drawBuffer);
            return;
        }

        IntBuffer buffers = BufferUtils.createIntBuffer(drawBuffers.length);
        buffers.put(drawBuffers);
        buffers.flip();
        GL20.glDrawBuffers(buffers);
    }

    private static final int GL13_ACTIVE_TEXTURE = 0x84E0;
}
