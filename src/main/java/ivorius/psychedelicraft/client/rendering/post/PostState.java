package ivorius.psychedelicraft.client.rendering.post;

import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** Saves only bindings, selectors and state touched by color allocation/blitting. */
final class PostState implements AutoCloseable {
    private final int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER);
    private final IntBuffer drawBuffers = captureDrawBuffers(drawFbo);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
    private final int texture0;
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final boolean srgb = PostGL.hasSrgb() && GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB);

    PostState() {
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    static IntBuffer captureDrawBuffers(int framebuffer) {
        int count = framebuffer == 0 ? 1 : GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        IntBuffer buffers = BufferUtils.createIntBuffer(count);
        for (int i = 0; i < count; i++)
            buffers.put(GL11.glGetInteger(framebuffer == 0 ? GL11.GL_DRAW_BUFFER : GL20.GL_DRAW_BUFFER0 + i));
        buffers.flip();
        return buffers;
    }

    static void restoreDrawBuffers(int framebuffer, IntBuffer buffers) {
        if (framebuffer == 0) GL11.glDrawBuffer(buffers.get(0));
        else GL20.glDrawBuffers(buffers);
    }

    static void enable(int capability, boolean enabled) {
        if (enabled) GL11.glEnable(capability);
        else GL11.glDisable(capability);
    }

    @Override
    public void close() {
        PostGL.bind(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL11.glReadBuffer(readBuffer);
        PostGL.bind(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        restoreDrawBuffers(drawFbo, drawBuffers);
        GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        enable(GL11.GL_SCISSOR_TEST, scissor);
        if (PostGL.hasSrgb()) enable(GL30.GL_FRAMEBUFFER_SRGB, srgb);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
        GL13.glActiveTexture(activeTexture);
    }
}
