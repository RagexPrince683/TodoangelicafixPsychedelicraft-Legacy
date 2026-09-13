package ivorius.psychedelicraft.client.rendering.shaders;

import ivorius.psychedelicraft.client.rendering.DrugEffectState;
import ivorius.psychedelicraft.internal.rendering.IvOpenGLTexturePingPong;
import ivorius.psychedelicraft.internal.rendering.IvShaderInstance2D;
import org.apache.logging.log4j.Logger;

/** Screen-space replacement for the legacy shader that modified submitted world vertices. */
public class ShaderWorldScreen extends IvShaderInstance2D
{
    public DrugEffectState state;

    public ShaderWorldScreen(Logger logger)
    {
        super(logger);
    }

    @Override
    public boolean shouldApply(float ticks)
    {
        return state != null && state.hasDrugScreenEffect && (
            state.bigWaves > 0.0f || state.smallWaves > 0.0f || state.wiggleWaves > 0.0f
                || state.surfaceFractal > 0.0f || state.distantWorldDeformation > 0.0f
                || state.pulseColor[3] > 0.0f || state.contrastColor[3] > 0.0f) && super.shouldApply(ticks);
    }

    @Override
    public void apply(int width, int height, float ticks, IvOpenGLTexturePingPong pingPong)
    {
        if (!useShader())
        {
            return;
        }

        setUniformInts("tex0", 0);
        setUniformFloats("ticks", ticks);
        setUniformFloats("pixelSize", 1.0f / width, 1.0f / height);
        setUniformFloats("bigWaves", state.bigWaves);
        setUniformFloats("smallWaves", state.smallWaves);
        setUniformFloats("wiggleWaves", state.wiggleWaves);
        setUniformFloats("surfaceFractal", state.surfaceFractal);
        setUniformFloats("distantWorldDeformation", state.distantWorldDeformation);
        setUniformFloats("pulseColor", state.pulseColor);
        setUniformFloats("contrastColor", state.contrastColor);
        drawFullScreen(width, height, pingPong);
        stopUsingShader();
    }
}
