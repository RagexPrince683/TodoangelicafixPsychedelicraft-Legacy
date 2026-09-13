package ivorius.psychedelicraft.client.rendering;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

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

    private final int activeTexture;
    private final int framebuffer;
    private final int matrixMode;
    private final int program;
    private boolean restored;

    private RenderStateGuard()
    {
        activeTexture = GL11.glGetInteger(GL13_ACTIVE_TEXTURE);
        framebuffer = OpenGlHelper.framebufferSupported
            ? GL11.glGetInteger(GL_FRAMEBUFFER_BINDING)
            : 0;
        matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

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
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffer);
        }
        OpenGlHelper.setActiveTexture(activeTexture);
        GL11.glMatrixMode(matrixMode);
        restored = true;
    }

    public static int getBoundFramebuffer()
    {
        return OpenGlHelper.framebufferSupported
            ? GL11.glGetInteger(GL_FRAMEBUFFER_BINDING)
            : 0;
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
