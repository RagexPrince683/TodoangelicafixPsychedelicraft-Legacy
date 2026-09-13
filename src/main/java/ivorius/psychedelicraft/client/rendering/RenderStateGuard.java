package ivorius.psychedelicraft.client.rendering;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

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
    private static final int TRACKED_TEXTURE_UNITS = 4;

    private final int activeTexture;
    private final int drawFramebuffer;
    private final int readFramebuffer;
    private final int matrixMode;
    private final int program;
    private final int[] textureBindings = new int[TRACKED_TEXTURE_UNITS];
    private boolean restored;

    private RenderStateGuard()
    {
        activeTexture = GL11.glGetInteger(GL13_ACTIVE_TEXTURE);
        drawFramebuffer = getBoundDrawFramebuffer();
        readFramebuffer = getBoundReadFramebuffer();
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

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

    private static final int GL13_ACTIVE_TEXTURE = 0x84E0;
}
