package ivorius.psychedelicraft.client.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL20;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;

/** The main image after EntityRenderer.renderWorld returns, never an inherited intermediate FBO. */
public final class CompletedScene
{
    public final int framebuffer;
    public final int colorBuffer;
    public final int width;
    public final int height;
    public final int textureWidth;
    public final int textureHeight;
    private final int originalReadBuffer;
    private final IntBuffer originalDrawBuffers;

    private CompletedScene(int framebuffer, int colorBuffer, int width, int height,
                           int textureWidth, int textureHeight)
    {
        this.framebuffer = framebuffer;
        this.colorBuffer = colorBuffer;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        originalReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
        int count = framebuffer == 0 ? 1 : GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        originalDrawBuffers = BufferUtils.createIntBuffer(count);
        for (int index = 0; index < count; index++)
            originalDrawBuffers.put(GL11.glGetInteger(framebuffer == 0
                ? GL11.GL_DRAW_BUFFER : GL20.GL_DRAW_BUFFER0 + index));
        originalDrawBuffers.flip();
    }

    /** Called inside a RenderStateGuard; querying the main attachment changes bindings. */
    public static CompletedScene acquire()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!OpenGlHelper.isFramebufferEnabled())
        {
            if (OpenGlHelper.framebufferSupported)
                OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, 0);
            return new CompletedScene(0, GL11.GL_BACK, mc.displayWidth, mc.displayHeight,
                mc.displayWidth, mc.displayHeight);
        }

        Framebuffer main = mc.getFramebuffer();
        if (main == null || main.framebufferObject <= 0 || main.framebufferTexture <= 0)
            return null;

        OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, main.framebufferObject);
        // Angelica 2.1.29 FinalPassRenderer writes main.framebufferTexture. This is
        // attachment zero even when the incoming FBO has an unrelated MRT layout.
        if (attachmentParameter(OpenGlHelper.field_153200_g, 0x8CD0) != GL11.GL_TEXTURE
            || attachmentParameter(OpenGlHelper.field_153200_g, 0x8CD1) != main.framebufferTexture)
            return null;

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, main.framebufferTexture);
        int textureWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int textureHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (main.framebufferWidth <= 0 || main.framebufferHeight <= 0
            || main.framebufferWidth > textureWidth || main.framebufferHeight > textureHeight)
            return null;

        return new CompletedScene(main.framebufferObject, OpenGlHelper.field_153200_g,
            main.framebufferWidth, main.framebufferHeight, textureWidth, textureHeight);
    }

    public void bindForReading()
    {
        OpenGlHelper.func_153171_g(RenderStateGuard.supportsSeparateFramebufferBindings()
            ? GL30.GL_READ_FRAMEBUFFER : OpenGlHelper.field_153198_e, framebuffer);
        GL11.glReadBuffer(colorBuffer);
    }

    public void bindForDrawing()
    {
        OpenGlHelper.func_153171_g(RenderStateGuard.supportsSeparateFramebufferBindings()
            ? GL30.GL_DRAW_FRAMEBUFFER : OpenGlHelper.field_153198_e, framebuffer);
        GL11.glDrawBuffer(colorBuffer);
    }

    public boolean hasDepth()
    {
        // acquire() leaves this framebuffer bound to both targets.
        return framebuffer != 0 && Minecraft.getMinecraft().getFramebuffer().useDepth
            && attachmentParameter(OpenGlHelper.field_153201_h, 0x8CD0) != GL11.GL_NONE;
    }

    /** Buffer selections belong to each FBO, not just the incoming GL bindings. */
    public void restoreBufferSelections()
    {
        if (OpenGlHelper.framebufferSupported)
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffer);
        if (framebuffer == 0) GL11.glDrawBuffer(originalDrawBuffers.get(0));
        else GL20.glDrawBuffers(originalDrawBuffers);
        GL11.glReadBuffer(originalReadBuffer);
    }

    private static int attachmentParameter(int attachment, int parameter)
    {
        return RenderStateGuard.supportsSeparateFramebufferBindings()
            ? GL30.glGetFramebufferAttachmentParameteri(OpenGlHelper.field_153198_e, attachment, parameter)
            : EXTFramebufferObject.glGetFramebufferAttachmentParameteriEXT(
                OpenGlHelper.field_153198_e, attachment, parameter);
    }
}
