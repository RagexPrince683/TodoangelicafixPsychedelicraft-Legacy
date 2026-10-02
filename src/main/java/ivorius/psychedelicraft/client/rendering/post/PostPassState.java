package ivorius.psychedelicraft.client.rendering.post;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.ARBSamplerObjects;

/** Narrow shader-draw scope inside the outer framebuffer/copy scope. No fixed-function matrices. */
final class PostPassState implements AutoCloseable {
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int[] textures = new int[2];
    private final boolean hasSamplers = GLContext.getCapabilities().OpenGL33
        || GLContext.getCapabilities().GL_ARB_sampler_objects;
    private final int[] samplers = new int[2];
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final boolean logic = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
    private final boolean discard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
    private final boolean alphaCoverage = GL11.glIsEnabled(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
    private final boolean coverage = GL11.glIsEnabled(GL13.GL_SAMPLE_COVERAGE);
    private final boolean hasSampleMask = GLContext.getCapabilities().OpenGL32;
    private final boolean sampleMask = hasSampleMask && GL11.glIsEnabled(GL32.GL_SAMPLE_MASK);
    private final ByteBuffer colorMask = BufferUtils.createByteBuffer(16);
    private final IntBuffer polygonMode = BufferUtils.createIntBuffer(16);
    private final boolean coreProfile = GLContext.getCapabilities().OpenGL32
        && (GL11.glGetInteger(GL32.GL_CONTEXT_PROFILE_MASK) & GL32.GL_CONTEXT_CORE_PROFILE_BIT) != 0;
    private final boolean[] clip;

    PostPassState() {
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
        GL11.glGetInteger(GL11.GL_POLYGON_MODE, polygonMode);
        clip = new boolean[GL11.glGetInteger(GL30.GL_MAX_CLIP_DISTANCES)];
        for (int i = 0; i < clip.length; i++) clip[i] = GL11.glIsEnabled(GL30.GL_CLIP_DISTANCE0 + i);
        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            if (hasSamplers) samplers[i] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
    }

    void prepare(int width, int height) {
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_SCISSOR_TEST); // outer scope owns restoration
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(GL11.GL_BLEND); // replacement, not accumulation
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
        GL11.glDisable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
        GL11.glDisable(GL13.GL_SAMPLE_COVERAGE);
        if (hasSampleMask) GL11.glDisable(GL32.GL_SAMPLE_MASK);
        // Shaderpack sampler objects must not replace the filtering/comparison contract of our color/noise textures.
        if (hasSamplers) for (int i = 0; i < samplers.length; i++) bindSampler(i, 0);
        for (int i = 0; i < clip.length; i++) GL11.glDisable(GL30.GL_CLIP_DISTANCE0 + i);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    @Override
    public void close() {
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        GL20.glUseProgram(program);
        GL11.glDepthMask(depthMask);
        PostState.enable(GL11.GL_DEPTH_TEST, depth);
        PostState.enable(GL11.GL_STENCIL_TEST, stencil);
        PostState.enable(GL11.GL_BLEND, blend);
        PostState.enable(GL11.GL_ALPHA_TEST, alpha);
        PostState.enable(GL11.GL_CULL_FACE, cull);
        PostState.enable(GL11.GL_COLOR_LOGIC_OP, logic);
        PostState.enable(GL30.GL_RASTERIZER_DISCARD, discard);
        PostState.enable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE, alphaCoverage);
        PostState.enable(GL13.GL_SAMPLE_COVERAGE, coverage);
        if (hasSampleMask) PostState.enable(GL32.GL_SAMPLE_MASK, sampleMask);
        GL11.glColorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
        for (int i = 0; i < clip.length; i++) PostState.enable(GL30.GL_CLIP_DISTANCE0 + i, clip[i]);
        // Core profiles (including Angelica's context) have one mode for both faces.
        // A second result slot is not a separate mode and can remain zero on this driver.
        if (coreProfile || polygonMode.get(0) == polygonMode.get(1))
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, polygonMode.get(0));
        else {
            GL11.glPolygonMode(GL11.GL_FRONT, polygonMode.get(0));
            GL11.glPolygonMode(GL11.GL_BACK, polygonMode.get(1));
        }
        for (int i = 0; i < textures.length; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]);
            if (hasSamplers) bindSampler(i, samplers[i]);
        }
        GL13.glActiveTexture(activeTexture);
    }

    private static void bindSampler(int unit, int sampler) {
        if (GLContext.getCapabilities().OpenGL33) GL33.glBindSampler(unit, sampler);
        else ARBSamplerObjects.glBindSampler(unit, sampler);
    }
}
