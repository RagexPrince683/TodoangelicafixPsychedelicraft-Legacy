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

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.OpenGlHelper;
import ivorius.psychedelicraft.client.rendering.RenderStateGuard;

import org.apache.logging.log4j.Logger;

public class IvOpenGLTexturePingPong {

    public Logger logger;

    public int[] cacheTextures = new int[2];
    public int activeBuffer;

    public int pingPongFB;
    public boolean setup = false;
    public boolean setupRealtimeFB = false;
    public boolean setupCacheTextureForTick = false;

    private int screenWidth;
    private int screenHeight;

    private int parentDrawFrameBuffer;
    private int parentReadFrameBuffer;
    private int parentDrawBuffer;
    private int parentReadBuffer;
    private int sourceViewportX;
    private int sourceViewportY;

    private boolean useFramebuffer;

    public IvOpenGLTexturePingPong(Logger logger) {
        this.logger = logger;
    }

    public void initialize(boolean useFramebuffer) {
        destroy();
        this.useFramebuffer = useFramebuffer;

        boolean fboFailed = false;
        for (int i = 0; i < 2; i++) {
            cacheTextures[i] = IvOpenGLHelper.genStandardTexture();
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                screenWidth,
                screenHeight,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        }

        if (cacheTextures[0] <= 0 || cacheTextures[1] <= 0) {
            fboFailed = true;
            setup = false;
        } else if (OpenGlHelper.framebufferSupported && useFramebuffer) {
            pingPongFB = OpenGlHelper.func_153165_e();

            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, pingPongFB);
            OpenGlHelper.func_153188_a(
                OpenGlHelper.field_153198_e,
                OpenGlHelper.field_153200_g,
                GL_TEXTURE_2D,
                cacheTextures[0],
                0);
            OpenGlHelper.func_153188_a(
                OpenGlHelper.field_153198_e,
                OpenGlHelper.field_153200_g + 1,
                GL_TEXTURE_2D,
                cacheTextures[1],
                0);

            int status = OpenGlHelper.func_153167_i(OpenGlHelper.field_153198_e);
            if (status != OpenGlHelper.field_153202_i) {
                logger.error(
                    "PingPong FBO failed setting up! (" + IvDepthBuffer.getFramebufferStatusString(status) + ")");

                fboFailed = true;
            }

            bindParentFrameBuffers();

            setup = true;
        } else {
            fboFailed = true;
            setup = true;
        }

        if (!fboFailed) {
            setupRealtimeFB = true;
        } else {
            logger.error("Can not PingPong! Using screen pong workaround");
        }
    }

    public void setScreenSize(int screenWidth, int screenHeight) {
        boolean gen = screenWidth != this.screenWidth || screenHeight != this.screenHeight;

        if (gen) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;

            if (setup) {
                initialize(useFramebuffer);
            }
        }
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setParentFrameBuffer(int parentFrameBuffer) {
        setParentFrameBuffers(parentFrameBuffer, parentFrameBuffer);
    }

    public void setParentFrameBuffers(int drawFrameBuffer, int readFrameBuffer) {
        this.parentDrawFrameBuffer = Math.max(drawFrameBuffer, 0);
        this.parentReadFrameBuffer = Math.max(readFrameBuffer, 0);
    }

    public void setParentBuffers(int drawBuffer, int readBuffer) {
        this.parentDrawBuffer = drawBuffer;
        this.parentReadBuffer = readBuffer;
    }

    public void setSourceViewportOrigin(int x, int y) {
        this.sourceViewportX = x;
        this.sourceViewportY = y;
    }

    public int getParentFrameBuffer() {
        return this.parentDrawFrameBuffer;
    }

    public void preTick(int screenWidth, int screenHeight) {
        setupCacheTextureForTick = false;

        setScreenSize(screenWidth, screenHeight);
    }

    public void pingPong() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        if (setupRealtimeFB) {
            if (!setupCacheTextureForTick) {
                activeBuffer = 0;
                bindCurrentTexture();

                bindSceneForReading();
                glCopyTexSubImage2D(
                    GL_TEXTURE_2D, 0, 0, 0,
                    sourceViewportX, sourceViewportY, screenWidth, screenHeight);

                OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, pingPongFB);
                glPushAttrib(GL_VIEWPORT_BIT | GL_COLOR_BUFFER_BIT);

                setupCacheTextureForTick = true;
            } else {
                activeBuffer = 1 - activeBuffer;
                bindCurrentTexture();
            }

            glDrawBuffer(activeBuffer == 1 ? OpenGlHelper.field_153200_g : OpenGlHelper.field_153200_g + 1);
            // glReadBuffer(activeBuffer == 0 ? GL_COLOR_ATTACHMENT0_EXT : GL_COLOR_ATTACHMENT1_EXT);

            glViewport(0, 0, screenWidth, screenHeight);
            prepareFullScreenOutput();
            // glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            // IvOpenGLHelper.setUpOpenGLStandard2D(screenWidth, screenHeight);
        } else // Use direct draw workaround
        {
            glBindTexture(GL_TEXTURE_2D, cacheTextures[0]);
            bindSceneForReading();
            glCopyTexSubImage2D(
                GL_TEXTURE_2D, 0, 0, 0,
                sourceViewportX, sourceViewportY, screenWidth, screenHeight);
            bindParentFrameBuffers();
        }
    }

    public void bindCurrentTexture() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        glBindTexture(GL_TEXTURE_2D, cacheTextures[activeBuffer]);
    }

    public void postTick(boolean composeResult) {
        if (setupRealtimeFB && setupCacheTextureForTick) {
            // glDrawBuffer(GL_BACK);
            // glReadBuffer(GL_BACK);
            glPopAttrib();
            bindParentFrameBuffers();
            glDrawBuffer(parentDrawBuffer);
            glReadBuffer(parentReadBuffer);

            if (!composeResult) {
                setupCacheTextureForTick = false;
                return;
            }

            activeBuffer = 1 - activeBuffer;
            glColor3f(1.0f, 1.0f, 1.0f);
            bindCurrentTexture();
            IvRenderHelper.drawRectFullScreen(screenWidth, screenHeight);
            setupCacheTextureForTick = false;
        }
    }

    private void bindParentFrameBuffers() {
        if (RenderStateGuard.supportsSeparateFramebufferBindings()) {
            OpenGlHelper.func_153171_g(0x8CA9, parentDrawFrameBuffer);
            OpenGlHelper.func_153171_g(0x8CA8, parentReadFrameBuffer);
        } else {
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, parentDrawFrameBuffer);
        }
    }

    private void bindSceneForReading() {
        if (RenderStateGuard.supportsSeparateFramebufferBindings()) {
            // The completed scene belongs to the draw framebuffer.  The independently
            // bound read framebuffer may still refer to an earlier Angelica pass.
            OpenGlHelper.func_153171_g(0x8CA8, parentDrawFrameBuffer);
        } else {
            OpenGlHelper.func_153171_g(OpenGlHelper.field_153198_e, parentDrawFrameBuffer);
        }

        glReadBuffer(parentDrawBuffer);
    }

    private void prepareFullScreenOutput() {
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDepthMask(false);
        glColorMask(true, true, true, true);
    }

    public void destroy() {
        for (int i = 0; i < 2; i++) {
            if (cacheTextures[i] > 0) {
                glDeleteTextures(cacheTextures[i]);
                cacheTextures[i] = 0;
            }
        }

        if (pingPongFB > 0) {
            OpenGlHelper.func_153174_h(pingPongFB);
            pingPongFB = 0;
        }

        setupRealtimeFB = false;
        setup = false;
        setupCacheTextureForTick = false;
        activeBuffer = 0;
    }
}
