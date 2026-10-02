package ivorius.psychedelicraft.client.rendering.post;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Ordinary GL entry points remain visible to Angelica's GLSMRedirector. */
final class PostGL {
    private PostGL() {}

    static boolean supported() {
        return GLContext.getCapabilities().OpenGL30
            || (GLContext.getCapabilities().OpenGL21 && GLContext.getCapabilities().GL_ARB_framebuffer_object);
    }

    static boolean hasSrgb() {
        return GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_ARB_framebuffer_sRGB
            || GLContext.getCapabilities().GL_EXT_framebuffer_sRGB;
    }

    static void bind(int target, int framebuffer) {
        if (GLContext.getCapabilities().OpenGL30) GL30.glBindFramebuffer(target, framebuffer);
        else ARBFramebufferObject.glBindFramebuffer(target, framebuffer);
    }

    static int create() {
        return GLContext.getCapabilities().OpenGL30
            ? GL30.glGenFramebuffers() : ARBFramebufferObject.glGenFramebuffers();
    }

    static void delete(int framebuffer) {
        if (GLContext.getCapabilities().OpenGL30) GL30.glDeleteFramebuffers(framebuffer);
        else ARBFramebufferObject.glDeleteFramebuffers(framebuffer);
    }

    static void attach(int texture) {
        if (GLContext.getCapabilities().OpenGL30)
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texture, 0);
        else ARBFramebufferObject.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
    }

    static int status() {
        return GLContext.getCapabilities().OpenGL30
            ? GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
            : ARBFramebufferObject.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
    }

    static int attachment(int parameter) {
        return GLContext.getCapabilities().OpenGL30
            ? GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, parameter)
            : ARBFramebufferObject.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0, parameter);
    }

    static void blit(int width, int height) {
        if (GLContext.getCapabilities().OpenGL30)
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        else ARBFramebufferObject.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }
}
