/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.client.rendering.effectWrappers;

import ivorius.psychedelicraft.internal.rendering.Iv2DScreenEffect;
import ivorius.psychedelicraft.internal.rendering.IvDepthBuffer;
import ivorius.psychedelicraft.internal.rendering.IvOpenGLTexturePingPong;
import net.minecraft.client.Minecraft;

/**
 * Created by lukas on 26.04.14.
 */
public abstract class ScreenEffectWrapper<ScreenEffect extends Iv2DScreenEffect> implements EffectWrapper
{
    public ScreenEffect screenEffect;
    private boolean preparedToApply;

    protected ScreenEffectWrapper(ScreenEffect screenEffect)
    {
        this.screenEffect = screenEffect;
    }

    @Override
    public void alloc()
    {

    }

    @Override
    public void dealloc()
    {
        screenEffect.destruct();
    }

    @Override
    public void prepare(float partialTicks, IvDepthBuffer depthBuffer)
    {
        Minecraft mc = Minecraft.getMinecraft();
        int ticks = mc.ingameGUI.getUpdateCounter();

        setScreenEffectValues(partialTicks, ticks);
        preparedToApply = screenEffect.shouldApply(ticks + partialTicks);
    }

    @Override
    public void apply(float partialTicks, IvOpenGLTexturePingPong pingPong, IvDepthBuffer depthBuffer)
    {
        if (preparedToApply)
        {
            int ticks = Minecraft.getMinecraft().ingameGUI.getUpdateCounter();
            screenEffect.apply(
                pingPong.getScreenWidth(),
                pingPong.getScreenHeight(),
                ticks + partialTicks,
                pingPong);
        }
    }

    @Override
    public boolean isActiveDrugShader()
    {
        return false;
    }

    public abstract void setScreenEffectValues(float partialTicks, int ticks);

    @Override
    public boolean wantsDepthBuffer(float partialTicks)
    {
        return false;
    }
}
