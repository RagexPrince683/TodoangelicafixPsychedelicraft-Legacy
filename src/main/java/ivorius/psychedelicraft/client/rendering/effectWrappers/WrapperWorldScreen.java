package ivorius.psychedelicraft.client.rendering.effectWrappers;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates;
import ivorius.psychedelicraft.client.rendering.shaders.ShaderWorldScreen;
import ivorius.psychedelicraft.internal.rendering.IvDepthBuffer;

public class WrapperWorldScreen extends ShaderWrapper<ShaderWorldScreen>
{
    public WrapperWorldScreen(String utils)
    {
        super(new ShaderWorldScreen(Psychedelicraft.logger), getRL("shaderBasic.vert"), getRL("shaderWorldScreen.frag"), utils);
    }

    @Override
    public void setShaderValues(float partialTicks, int ticks, IvDepthBuffer depthBuffer)
    {
        shaderInstance.state = PSRenderStates.getEffectState();
    }

    @Override
    public void update()
    {
    }

    @Override
    public boolean wantsDepthBuffer(float partialTicks)
    {
        return false;
    }
}
