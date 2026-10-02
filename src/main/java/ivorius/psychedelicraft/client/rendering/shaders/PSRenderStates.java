/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.client.rendering.shaders;

import ivorius.psychedelicraft.internal.rendering.*;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.rendering.EntityFakeSun;
import ivorius.psychedelicraft.client.rendering.GLStateProxy;
import ivorius.psychedelicraft.client.rendering.PsycheShadowHelper;
import ivorius.psychedelicraft.client.rendering.PsycheMatrixHelper;
import ivorius.psychedelicraft.client.rendering.RenderStateGuard;
import ivorius.psychedelicraft.client.rendering.DrugEffectState;
import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import ivorius.psychedelicraft.client.rendering.effectWrappers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class PSRenderStates
{
    public static ShaderWorld currentShader;
    public static ShaderMain shaderInstance;
    public static ShaderMainDepth shaderInstanceDepth;
    public static ShaderShadows shaderInstanceShadows;

    public static List<EffectWrapper> effectWrappers = new ArrayList<>();

    public static IvOpenGLTexturePingPong realtimePingPong;

    public static IvDepthBuffer depthBuffer;
    public static boolean didDepthPass = false;

    public static boolean disableDepthBuffer = false;
    public static boolean bypassPingPongBuffer = false;
    public static boolean shader3DEnabled = true;
    public static boolean shader2DEnabled = true;
    public static boolean doShadows = false;
    public static boolean doHeatDistortion = false;
    public static boolean doWaterDistortion = false;
    public static boolean doMotionBlur = false;

    public static float sunFlareIntensity;
    public static int shadowPixelsPerChunk = 256;

    public static String currentRenderPass;
    public static float currentRenderPassTicks;

    public static boolean renderFakeSkybox = true;

    private static boolean activeDrugShader;
    private static boolean shaderFramePrepared;
    private static DrugEffectState effectState;
    private static final Deque<PreparedViewState> outerViewStates = new ArrayDeque<PreparedViewState>();
    private static EntityLivingBase viewEntity;
    private static boolean mainView;
    private static int worldViewDepth;
    private static float viewPartialTicks;

    private static boolean glLightEnabled;
    private static boolean glLight0Valid;
    private static float glLight0X;
    private static float glLight0Y;
    private static float glLight0Z;
    private static float glLight0Strength;
    private static float glLight0Specular;
    private static boolean glLight1Valid;
    private static float glLight1X;
    private static float glLight1Y;
    private static float glLight1Z;
    private static float glLight1Strength;
    private static float glLight1Specular;
    private static boolean glLightAmbientValid;
    private static float glLightAmbient;

    public static void preRender(float ticks)
    {
        outerViewStates.push(new PreparedViewState(activeDrugShader, shaderFramePrepared, effectState,
            viewEntity, mainView, didDepthPass, viewPartialTicks));
        activeDrugShader = false;
        shaderFramePrepared = false;
        effectState = null;
        didDepthPass = false;
        Minecraft mc = Minecraft.getMinecraft();
        viewEntity = mc.renderViewEntity;
        mainView = ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.isMainView();
        PsycheMatrixHelper.beginView();
    }

    public static boolean isMainView()
    {
        return mainView && outerViewStates.size() == 1
            && viewEntity == Minecraft.getMinecraft().renderViewEntity;
    }

    public static void beginWorldView()
    {
        worldViewDepth++;
    }

    public static void endWorldView()
    {
        worldViewDepth--;
    }

    public static void recordPostProcessStage(String stage)
    {
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.passDiagnostic(stage);
    }

    /** Retired: the canonical post backend does not infer or capture world depth. */
    @Deprecated
    public static void captureWorldDepth() {}

    public static void preRender3D(float ticks)
    {

    }

    public static List<String> getRenderPasses(float partialTicks)
    {
        List<String> passes = new ArrayList<>();
        passes.add("Default");
        return passes;
    }

    public static boolean startRenderPass(String pass, float partialTicks, float ticks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (currentRenderPass != null)
            endRenderPass();

        currentRenderPass = pass;
        currentRenderPassTicks = ticks;

        switch (pass)
        {
            case "Default":
                shaderInstance.shouldDoShadows = doShadows;
                shaderInstance.shadowDepthTextureIndex = shaderInstanceShadows.depthBuffer.getDepthTextureIndex();

                return useShader(partialTicks, ticks, shaderInstance);
            case "Depth":
                depthBuffer.setParentFB(getMCFBO());
                depthBuffer.setSize(mc.displayWidth, mc.displayHeight);
                depthBuffer.bind();

                return useShader(partialTicks, ticks, shaderInstanceDepth);
            case "Shadows":
                Minecraft.getMinecraft().renderViewEntity = new EntityFakeSun(mc.renderViewEntity);
                return useShader(partialTicks, ticks, shaderInstanceShadows);
        }

        return true;
    }

    public static void endRenderPass()
    {
        switch (currentRenderPass)
        {
            case "Default":

                break;
            case "Depth":
                didDepthPass = shaderInstanceDepth.isShaderActive();
                depthBuffer.unbind();
                break;
            case "Shadows":
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.renderViewEntity instanceof EntityFakeSun)
                    mc.renderViewEntity = ((EntityFakeSun) mc.renderViewEntity).prevViewEntity;
                break;
        }

        if (currentShader != null)
        {
            currentShader.deactivate();
            currentShader = null;
        }

        //IvOpenGLHelper.checkGLError(Psychedelicraft.logger, "Post render pass '" + currentRenderPass + "'");
        currentRenderPass = null;
    }

    public static boolean setupCameraTransform()
    {
        if ("Shadows".equals(currentRenderPass)/* || (Minecraft.getMinecraft().ingameGUI.getUpdateCounter() % 100 > 2)*/)
        {
            PsycheShadowHelper.setupSunGLTransform();

            return true;
        }

        return false;
    }

    public static void setShader3DEnabled(boolean enabled)
    {
        shader3DEnabled = enabled;
    }

    public static void setShader2DEnabled(boolean enabled)
    {
        shader2DEnabled = enabled;
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.enabled = enabled;
    }

    public static void allocate()
    {
        // Resource reload retires the old pipeline; targets are allocated only in a main-view scope.
        deallocate();
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.reset();
    }

    public static void setUpShader(IvShaderInstance shader, String vertexFile, String fragmentFile, String utils)
    {
        IvShaderInstanceMC.trySettingUpShader(shader, new ResourceLocation(Psychedelicraft.MODID, Psychedelicraft.filePathShaders + vertexFile), new ResourceLocation(Psychedelicraft.MODID, Psychedelicraft.filePathShaders + fragmentFile), utils);
    }

    /** Legacy lifecycle entry point terminates at the new owned backend. */
    @Deprecated
    public static void setUpRealtimeCacheTexture() {
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.reset();
    }

    public static void update()
    {
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.clientTick();
    }

    public static boolean useShader(float partialTicks, float ticks, ShaderWorld shader)
    {
        currentShader = null;

        if (shader != null && shader3DEnabled)
        {
            if (shader.isShaderActive())
                return true;

            if (shader.activate(partialTicks, ticks))
            {
                currentShader = shader;
                applyLogicalLightingState(shader);
                return true;
            }
        }

        return false;
    }

    public static void preRenderSky(float partialTicks)
    {
        // This hook runs after Minecraft has installed the world camera matrices.
        // It no longer depends on the retired three-dimensional shader pass.
        if (!isMainView() || worldViewDepth != 1) return;
        PsycheMatrixHelper.captureCurrentWorldView(Minecraft.getMinecraft().renderViewEntity);

        // Screen-space processing needs no synthetic sky geometry or world-depth clear.
    }

    public static void setEnabled(int cap, boolean enabled)
    {
        if (cap == GL11.GL_TEXTURE_2D)
            setTexture2DEnabled(GLStateProxy.getActiveTextureUnit(), enabled);
        else
        {
            GLStateProxy.setEnabled(cap, enabled);

            if (currentShader != null)
            {
                switch (cap)
                {
                    case GL11.GL_FOG:
                        currentShader.setFogEnabled(enabled);
                        break;
                    case GL11.GL_BLEND:
                        currentShader.setBlendModeEnabled(enabled);
                        break;
                }
            }
        }
    }

    public static void setTexture2DEnabled(int textureUnit, boolean enabled)
    {
        GLStateProxy.setTextureEnabled(textureUnit, enabled);

        if (textureUnit == OpenGlHelper.defaultTexUnit && currentShader != null)
            currentShader.setTexture2DEnabled(enabled);

        if (textureUnit == OpenGlHelper.lightmapTexUnit && currentShader != null)
            currentShader.setLightmapEnabled(enabled);
    }

    public static void setBlendFunc(int sFactor, int dFactor, int sFactorAlpha, int dFactorAlpha)
    {
        GLStateProxy.glBlendFunc(sFactor, dFactor, sFactorAlpha, dFactorAlpha);
        if (currentShader != null)
            currentShader.setBlendFunc(sFactor, dFactor, sFactorAlpha, dFactorAlpha);
    }

    public static void setOverrideColor(float... color)
    {
        if (color != null && color.length != 4)
            throw new IllegalArgumentException("Color must be a length-4 float array");

        if (currentShader != null)
            currentShader.setOverrideColor(color);
    }

    public static void setGLLightEnabled(boolean enabled)
    {
        glLightEnabled = enabled;
        if (currentShader != null)
            currentShader.setGLLightEnabled(enabled);
    }

    public static void setGLLight(int number, float x, float y, float z, float strength, float specular)
    {
        if (number == 0)
        {
            glLight0Valid = true;
            glLight0X = x;
            glLight0Y = y;
            glLight0Z = z;
            glLight0Strength = strength;
            glLight0Specular = specular;
        }
        else if (number == 1)
        {
            glLight1Valid = true;
            glLight1X = x;
            glLight1Y = y;
            glLight1Z = z;
            glLight1Strength = strength;
            glLight1Specular = specular;
        }

        if (currentShader != null)
            currentShader.setGLLight(number, x, y, z, strength, specular);
    }

    public static void setGLLightAmbient(float strength)
    {
        glLightAmbientValid = true;
        glLightAmbient = strength;
        if (currentShader != null)
            currentShader.setGLLightAmbient(strength);
    }

    private static void applyLogicalLightingState(ShaderWorld shader)
    {
        shader.setGLLightEnabled(glLightEnabled);

        if (glLight0Valid)
            shader.setGLLight(0, glLight0X, glLight0Y, glLight0Z, glLight0Strength, glLight0Specular);

        if (glLight1Valid)
            shader.setGLLight(1, glLight1X, glLight1Y, glLight1Z, glLight1Strength, glLight1Specular);

        if (glLightAmbientValid)
            shader.setGLLightAmbient(glLightAmbient);
    }

    public static void setFogMode(int mode)
    {
        if (currentShader != null)
            currentShader.setFogMode(mode);
    }

    public static void setDepthMultiplier(float depthMultiplier)
    {
        if (currentShader != null)
            currentShader.setDepthMultiplier(depthMultiplier);
    }

    public static void setUseScreenTexCoords(boolean enabled)
    {
        if (currentShader != null)
            currentShader.setUseScreenTexCoords(enabled);
    }

    public static void setScreenSizeDefault()
    {
        Minecraft mc = Minecraft.getMinecraft();
        setScreenSize(mc.displayWidth, mc.displayHeight);
    }

    public static void setScreenSize(float screenWidth, float screenHeight)
    {
        setPixelSize(1.0f / screenWidth, 1.0f / screenHeight);
    }

    public static void setPixelSize(float pixelWidth, float pixelHeight)
    {
        if (currentShader != null)
            currentShader.setPixelSize(pixelWidth, pixelHeight);
    }

    public static void setForceColorSafeMode(boolean enable)
    {
        if (currentShader != null)
            currentShader.setForceColorSafeMode(enable);
    }

    public static void setProjectShadows(boolean projectShadows)
    {
        if (currentShader != null)
            currentShader.setProjectShadows(projectShadows);
    }

    public static int getCurrentAllowedGLDataMask()
    {
        if ("Depth".equals(currentRenderPass))
            return GL11.GL_DEPTH_BUFFER_BIT;
        else if ("Shadows".equals(currentRenderPass))
            return GL11.GL_DEPTH_BUFFER_BIT;

        return ~0;
    }

    public static int getMCFBO()
    {
        return RenderStateGuard.getBoundFramebuffer();
    }

    public static void postRender(float ticks, float partialTicks)
    {
        apply2DShaders(ticks, partialTicks);
    }

    public static void prepareView(float partialTicks)
    {
        viewPartialTicks = partialTicks;
        activeDrugShader = false;
        shaderFramePrepared = false;
        effectState = DrugEffectState.capture(
            DrugProperties.getDrugProperties(Minecraft.getMinecraft().renderViewEntity), partialTicks);
    }

    @Deprecated
    public static void prepare2DShaders(float partialTicks) { prepareView(partialTicks); }

    public static boolean hasActiveDrugShader()
    {
        return shaderFramePrepared && activeDrugShader;
    }

    public static void finishView()
    {
        PsycheMatrixHelper.finishView();
        activeDrugShader = false;
        shaderFramePrepared = false;
        effectState = null;

        if (!outerViewStates.isEmpty())
        {
            PreparedViewState outer = outerViewStates.pop();
            activeDrugShader = outer.activeDrugShader;
            shaderFramePrepared = outer.shaderFramePrepared;
            effectState = outer.effectState;
            viewEntity = outer.viewEntity;
            mainView = outer.mainView;
            didDepthPass = outer.didDepthPass;
            viewPartialTicks = outer.partialTicks;
        }
    }

    public static DrugEffectState getEffectState()
    {
        return effectState;
    }

    public static DrugProperties getViewDrugProperties()
    {
        return effectState == null ? null : effectState.drugProperties;
    }

    /** Legacy external entry point; all rendering terminates at the canonical processor. */
    @Deprecated
    public static void apply2DShaders(float ticks, float partialTicks) {
        ivorius.psychedelicraft.client.rendering.post.PsychePostProcessor.render(partialTicks);
    }

    public static int getTextureIndex(ResourceLocation loc)
    {
        TextureManager tm = Minecraft.getMinecraft().renderEngine;
        tm.bindTexture(loc); // Allocate texture. MOJANG!
        ITextureObject texture = tm.getTexture(loc);
        return texture.getGlTextureId();
    }

    public static void delete3DShaders()
    {
        if (shaderInstance != null)
            shaderInstance.deleteShader();
        shaderInstance = null;

        if (shaderInstanceDepth != null)
            shaderInstanceDepth.deleteShader();
        shaderInstanceDepth = null;

        if (shaderInstanceShadows != null)
            shaderInstanceShadows.deleteShader();
        shaderInstanceShadows = null;
    }

    public static void deleteRealtimeCacheTexture()
    {
        if (realtimePingPong != null)
            realtimePingPong.destroy();
        realtimePingPong = null;
    }

    public static void deallocate()
    {
        delete3DShaders();
        deleteRealtimeCacheTexture();

        for (EffectWrapper effectWrapper : effectWrappers)
            effectWrapper.dealloc();
        effectWrappers.clear();
        activeDrugShader = false;
        shaderFramePrepared = false;
        effectState = null;
        outerViewStates.clear();

        if (depthBuffer != null)
            depthBuffer.deallocate();
        depthBuffer = null;
    }

    private static final class PreparedViewState
    {
        private final boolean activeDrugShader;
        private final boolean shaderFramePrepared;
        private final DrugEffectState effectState;
        private final EntityLivingBase viewEntity;
        private final boolean mainView;
        private final boolean didDepthPass;
        private final float partialTicks;

        private PreparedViewState(boolean activeDrugShader, boolean shaderFramePrepared,
                                  DrugEffectState effectState, EntityLivingBase viewEntity,
                                  boolean mainView, boolean didDepthPass, float partialTicks)
        {
            this.activeDrugShader = activeDrugShader;
            this.shaderFramePrepared = shaderFramePrepared;
            this.effectState = effectState;
            this.viewEntity = viewEntity;
            this.mainView = mainView;
            this.didDepthPass = didDepthPass;
            this.partialTicks = partialTicks;
        }
    }

    public static void outputShaderInfo()
    {
        Psychedelicraft.logger.info("Graphics card info: ");
        IvShaderInstance.outputShaderInfo(Psychedelicraft.logger);
    }
}
