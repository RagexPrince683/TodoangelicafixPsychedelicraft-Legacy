package ivorius.psychedelicraft.client.rendering;

import ivorius.psychedelicraft.entities.drugs.DrugProperties;

/**
 * Immutable, per-view rendering input copied from the drug simulation.
 * Rendering code must not advance or otherwise modify the simulation.
 */
public final class DrugEffectState
{
    public final DrugProperties drugProperties;
    public final float bigWaves;
    public final float smallWaves;
    public final float wiggleWaves;
    public final float surfaceFractal;
    public final float distantWorldDeformation;
    public final float[] pulseColor;
    public final float[] contrastColor;
    public final boolean hasDrugScreenEffect;

    private DrugEffectState(DrugProperties properties, float partialTicks)
    {
        drugProperties = properties;

        if (properties == null)
        {
            bigWaves = 0.0f;
            smallWaves = 0.0f;
            wiggleWaves = 0.0f;
            surfaceFractal = 0.0f;
            distantWorldDeformation = 0.0f;
            pulseColor = new float[]{1.0f, 1.0f, 1.0f, 0.0f};
            contrastColor = new float[]{1.0f, 1.0f, 1.0f, 0.0f};
            hasDrugScreenEffect = false;
            return;
        }

        bigWaves = finite(properties.hallucinationManager.getBigWaveStrength(properties, partialTicks));
        smallWaves = finite(properties.hallucinationManager.getSmallWaveStrength(properties, partialTicks));
        wiggleWaves = finite(properties.hallucinationManager.getWiggleWaveStrength(properties, partialTicks));
        surfaceFractal = finite(properties.hallucinationManager.getSurfaceFractalStrength(properties, partialTicks));
        distantWorldDeformation = finite(properties.hallucinationManager.getDistantWorldDeformationStrength(properties, partialTicks));

        pulseColor = new float[]{1.0f, 1.0f, 1.0f, 0.0f};
        properties.hallucinationManager.applyPulseColor(properties, pulseColor, partialTicks);
        sanitizeColor(pulseColor);

        contrastColor = new float[]{1.0f, 1.0f, 1.0f, 0.0f};
        properties.hallucinationManager.applyContrastColorization(properties, contrastColor, partialTicks);
        sanitizeColor(contrastColor);

        hasDrugScreenEffect = bigWaves > 0.0f || smallWaves > 0.0f || wiggleWaves > 0.0f
            || surfaceFractal > 0.0f || distantWorldDeformation > 0.0f
            || pulseColor[3] > 0.0f || contrastColor[3] > 0.0f;
    }

    public static DrugEffectState capture(DrugProperties properties, float partialTicks)
    {
        return new DrugEffectState(properties, partialTicks);
    }

    private static float finite(float value)
    {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0.0f : value;
    }

    private static void sanitizeColor(float[] color)
    {
        for (int index = 0; index < color.length; index++)
        {
            color[index] = finite(color[index]);
        }
        color[3] = Math.max(0.0f, Math.min(1.0f, color[3]));
    }
}
