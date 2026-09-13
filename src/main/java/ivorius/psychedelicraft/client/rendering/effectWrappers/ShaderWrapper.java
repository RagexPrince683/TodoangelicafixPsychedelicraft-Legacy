/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.client.rendering.effectWrappers;

import ivorius.psychedelicraft.internal.rendering.IvDepthBuffer;
import ivorius.psychedelicraft.internal.rendering.IvOpenGLTexturePingPong;
import ivorius.psychedelicraft.internal.rendering.IvShaderInstance2D;
import ivorius.psychedelicraft.internal.rendering.IvShaderInstanceMC;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * Created by lukas on 26.04.14.
 */
public abstract class ShaderWrapper<ShaderInstance extends IvShaderInstance2D> implements EffectWrapper
{
    public ShaderInstance shaderInstance;

    public ResourceLocation vertexShaderFile;
    public ResourceLocation fragmentShaderFile;

    public String utils;
    private boolean preparedToApply;
    private boolean activeDrugShader;

    public ShaderWrapper(ShaderInstance shaderInstance, ResourceLocation vertexShaderFile, ResourceLocation fragmentShaderFile, String utils)
    {
        this.shaderInstance = shaderInstance;
        this.vertexShaderFile = vertexShaderFile;
        this.fragmentShaderFile = fragmentShaderFile;
        this.utils = utils;
    }

    public static ResourceLocation getRL(String shaderFile)
    {
        return new ResourceLocation(Psychedelicraft.MODID, Psychedelicraft.filePathShaders + shaderFile);
    }

    @Override
    public void alloc()
    {
        if (PSRenderStates.shader2DEnabled)
        {
            IvShaderInstanceMC.trySettingUpShader(shaderInstance, vertexShaderFile, fragmentShaderFile, utils);
        }
    }

    @Override
    public void dealloc()
    {
        shaderInstance.deleteShader();
    }

    @Override
    public void prepare(float partialTicks, IvDepthBuffer depthBuffer)
    {
        preparedToApply = false;
        activeDrugShader = false;

        if (PSRenderStates.shader2DEnabled && Minecraft.getMinecraft().renderViewEntity != null)
        {
            Minecraft mc = Minecraft.getMinecraft();
            int ticks = mc.renderViewEntity.ticksExisted;
            setShaderValues(partialTicks, ticks, depthBuffer);
            preparedToApply = shaderInstance.shouldApply(ticks + partialTicks);
            activeDrugShader = preparedToApply && isDrugShaderActive(partialTicks, ticks);
        }
    }

    @Override
    public void apply(float partialTicks, IvOpenGLTexturePingPong pingPong, IvDepthBuffer depthBuffer)
    {
        if (preparedToApply)
        {
            int ticks = Minecraft.getMinecraft().renderViewEntity.ticksExisted;
            shaderInstance.apply(
                pingPong.getScreenWidth(),
                pingPong.getScreenHeight(),
                ticks + partialTicks,
                pingPong);
        }
    }

    protected boolean isDrugShaderActive(float partialTicks, int ticks)
    {
        return true;
    }

    @Override
    public boolean isActiveDrugShader()
    {
        return activeDrugShader;
    }

    public abstract void setShaderValues(float partialTicks, int ticks, IvDepthBuffer depthBuffer);
}
