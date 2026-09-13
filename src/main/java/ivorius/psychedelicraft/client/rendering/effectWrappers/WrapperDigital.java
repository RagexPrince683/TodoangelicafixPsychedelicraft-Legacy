/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.client.rendering.effectWrappers;

import ivorius.psychedelicraft.internal.rendering.IvDepthBuffer;
import ivorius.psychedelicraft.internal.rendering.IvOpenGLTexturePingPong;

/**
 * Created by lukas on 26.04.14.
 */
public class WrapperDigital implements EffectWrapper
{
    public WrapperDigitalMD digitalMD;
    public WrapperDigitalPD digitalPD;
    private ShaderWrapper<?> preparedWrapper;

    public WrapperDigital(String utils)
    {
        digitalMD = new WrapperDigitalMD(utils);
        digitalPD = new WrapperDigitalPD(utils);
    }

    @Override
    public void alloc()
    {
        digitalMD.alloc();
        digitalPD.alloc();
    }

    @Override
    public void dealloc()
    {
        digitalMD.dealloc();
        digitalPD.dealloc();
    }

    @Override
    public void update()
    {
        digitalMD.update();
        digitalPD.update();
    }

    @Override
    public void prepare(float partialTicks, IvDepthBuffer depthBuffer)
    {
        if (depthBuffer != null)
            preparedWrapper = digitalPD;
        else
            preparedWrapper = digitalMD;

        preparedWrapper.prepare(partialTicks, depthBuffer);
    }

    @Override
    public void apply(float partialTicks, IvOpenGLTexturePingPong pingPong, IvDepthBuffer depthBuffer)
    {
        preparedWrapper.apply(partialTicks, pingPong, depthBuffer);
    }

    @Override
    public boolean isActiveDrugShader()
    {
        return preparedWrapper != null && preparedWrapper.isActiveDrugShader();
    }

    @Override
    public boolean wantsDepthBuffer(float partialTicks)
    {
        return digitalPD.wantsDepthBuffer(partialTicks) || digitalMD.wantsDepthBuffer(partialTicks);
    }
}
