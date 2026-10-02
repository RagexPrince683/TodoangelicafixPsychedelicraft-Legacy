package ivorius.psychedelicraft.client.rendering.post;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** One independently owned, complete, color-only post-processing image. */
public final class PsychePostTarget {
    public final int width;
    public final int height;
    public final int framebuffer;
    public final int colorTexture;

    // Allocation is performed inside PostState; no pixel-unpack buffer may back a null upload.
    PsychePostTarget(int width, int height) {
        this.width = width;
        this.height = height;
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        int texture = GL11.glGenTextures();
        int fbo = PostGL.create();
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height,
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            PostGL.bind(GL30.GL_FRAMEBUFFER, fbo);
            PostGL.attach(texture);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            int status = PostGL.status();
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("PsychePost target " + width + "x" + height
                    + " incomplete: 0x" + Integer.toHexString(status));
        } catch (RuntimeException failure) {
            PostGL.delete(fbo);
            GL11.glDeleteTextures(texture);
            throw failure;
        }
        framebuffer = fbo;
        colorTexture = texture;
    }

    void bindRead() {
        PostGL.bind(GL30.GL_READ_FRAMEBUFFER, framebuffer);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
    }

    void bindDraw() {
        PostGL.bind(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
    }

    void destroy() {
        PostGL.delete(framebuffer);
        GL11.glDeleteTextures(colorTexture);
    }
}
