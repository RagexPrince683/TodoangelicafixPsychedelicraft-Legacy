/*
 * Copyright 2014 Lukas Tenbrink
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ivorius.psychedelicraft.internal.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.OpenGlHelper;
import ivorius.psychedelicraft.client.rendering.CompletedScene;
import ivorius.psychedelicraft.client.rendering.RenderStateGuard;
import ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;

/** Owns every intermediate image; the parent scene is acquired once per completed main view. */
public class IvOpenGLTexturePingPong {
    public final Logger logger;
    public final int[] cacheTextures = new int[2];
    private final int[] framebuffers = new int[2];
    public int activeBuffer;
    public boolean setup;
    public boolean setupRealtimeFB;
    public boolean setupCacheTextureForTick;
    private int screenWidth;
    private int screenHeight;
    private boolean useFramebuffer;
    private CompletedScene scene;

    public IvOpenGLTexturePingPong(Logger logger) {
        this.logger = logger;
    }

    /** Caller owns a RenderStateGuard, including allocation/reallocation. */
    public void initialize(boolean useFramebuffer) {
        destroy();
        this.useFramebuffer = useFramebuffer;
        if (screenWidth <= 0 || screenHeight <= 0) return;
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        for (int i = 0; i < 2; i++) {
            cacheTextures[i] = IvOpenGLHelper.genStandardTexture();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, screenWidth, screenHeight, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            if (OpenGlHelper.framebufferSupported && useFramebuffer) {
                framebuffers[i] = OpenGlHelper.func_153165_e();
                OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffers[i]);
                OpenGlHelper.func_153188_a(OpenGlHelper.field_153198_e,
                    OpenGlHelper.field_153200_g, GL_TEXTURE_2D, cacheTextures[i], 0);
                glDrawBuffer(OpenGlHelper.field_153200_g);
                glReadBuffer(OpenGlHelper.field_153200_g);
                int status = OpenGlHelper.func_153167_i(OpenGlHelper.field_153198_e);
                if (status != OpenGlHelper.field_153202_i) {
                    logger.error("Ping-pong framebuffer incomplete: " + IvDepthBuffer.getFramebufferStatusString(status));
                    destroy();
                    return;
                }
            }
        }
        setup = true;
        setupRealtimeFB = OpenGlHelper.framebufferSupported && useFramebuffer;
    }

    public void setScreenSize(int width, int height) {
        if (width != screenWidth || height != screenHeight) {
            screenWidth = width;
            screenHeight = height;
            if (setup) initialize(useFramebuffer);
        }
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    public boolean beginFrame(CompletedScene completedScene) {
        setScreenSize(completedScene.width, completedScene.height);
        if (!setup) return false;
        scene = completedScene;
        activeBuffer = 0;
        setupCacheTextureForTick = false;
        scene.bindForReading();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        glBindTexture(GL_TEXTURE_2D, cacheTextures[0]);
        // Read exactly the completed image, at origin zero, independent of the live viewport.
        // Keep CopyTexSubImage: it supports the scene's format without blit-format assumptions.
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, screenWidth, screenHeight);
        PSRenderStates.recordPostProcessStage("scene-copy");
        return true;
    }

    public void pingPong() {
        PSRenderStates.recordPostProcessStage("pass/uniforms-ready");
        if (scene == null) throw new IllegalStateException("No completed scene acquired");
        if (setupCacheTextureForTick) activeBuffer = 1 - activeBuffer;
        if (setupRealtimeFB) {
            // A sampled texture is never attached to the drawing FBO.
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffers[1 - activeBuffer]);
            glDrawBuffer(OpenGlHelper.field_153200_g);
            glReadBuffer(OpenGlHelper.field_153200_g);
        } else {
            // Legacy explicit bypass/no-FBO mode only. Every pass reads our known destination.
            if (setupCacheTextureForTick) {
                scene.bindForReading();
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                glBindTexture(GL_TEXTURE_2D, cacheTextures[activeBuffer]);
                glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, screenWidth, screenHeight);
            }
            scene.bindForDrawing();
        }
        PSRenderStates.recordPostProcessStage("pass/destination-ready");
        prepareFullScreenOutput(screenWidth, screenHeight);
        PSRenderStates.recordPostProcessStage("pass/raster-ready");
        bindCurrentTexture();
        if (setupRealtimeFB) {
            // No destination may retain pixels from an earlier frame, even if a pass fails.
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
        setupCacheTextureForTick = true;
        PSRenderStates.recordPostProcessStage("owned-pass");
    }

    public void bindCurrentTexture() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        glBindTexture(GL_TEXTURE_2D, cacheTextures[activeBuffer]);
    }

    public void copyCurrentOutputToTexture(int texture) {
        if (setupRealtimeFB) {
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, framebuffers[1 - activeBuffer]);
            glReadBuffer(OpenGlHelper.field_153200_g);
        } else scene.bindForReading();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, screenWidth, screenHeight);
    }

    public void postTick(boolean composeResult) {
        try {
            if (setupRealtimeFB && setupCacheTextureForTick && composeResult) {
                scene.bindForDrawing();
                // Never pop inherited color/viewport state before this draw. Restoration
                // belongs to the outer guard, after the complete image is composited.
                OpenGlHelper.func_153161_d(0);
                prepareFullScreenOutput(screenWidth, screenHeight);
                activeBuffer = 1 - activeBuffer;
                bindCurrentTexture();
                IvRenderHelper.drawRectFullScreen(screenWidth, screenHeight);
                PSRenderStates.recordPostProcessStage("main-composite");
            }
        } finally {
            setupCacheTextureForTick = false;
            scene = null;
        }
    }

    public static void prepareFullScreenOutput(int width, int height) {
        glViewport(0, 0, width, height);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_FOG);
        glDisable(GL_LIGHTING);
        glDisable(GL_COLOR_LOGIC_OP);
        glDisable(GL_POLYGON_STIPPLE);
        glDisable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
        glDisable(GL13.GL_SAMPLE_COVERAGE);
        for (int plane = 0; plane < glGetInteger(GL_MAX_CLIP_PLANES); plane++)
            glDisable(GL_CLIP_PLANE0 + plane);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDepthMask(false);
        glColorMask(true, true, true, true);
        // Fixed-function copy/overlay draws must not combine the lightmap or noise units.
        for (int unit = 1; unit < 4; unit++) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
            glDisable(GL_TEXTURE_2D);
        }
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        glEnable(GL_TEXTURE_2D);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        IvOpenGLHelper.setUpOpenGLStandard2D(width, height);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void destroy() {
        for (int i = 0; i < 2; i++) {
            if (cacheTextures[i] > 0) glDeleteTextures(cacheTextures[i]);
            if (framebuffers[i] > 0) OpenGlHelper.func_153174_h(framebuffers[i]);
            cacheTextures[i] = 0;
            framebuffers[i] = 0;
        }
        setup = false;
        setupRealtimeFB = false;
        setupCacheTextureForTick = false;
        activeBuffer = 0;
        scene = null;
    }
}
