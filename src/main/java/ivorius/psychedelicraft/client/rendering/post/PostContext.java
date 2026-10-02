package ivorius.psychedelicraft.client.rendering.post;

import ivorius.psychedelicraft.client.rendering.DrugEffectState;
import ivorius.psychedelicraft.client.ClientProxy;
import ivorius.psychedelicraft.entities.drugs.Drug;
import ivorius.psychedelicraft.entities.drugs.DrugHallucinationManager;
import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import net.minecraft.entity.EntityLivingBase;

/** Simulation inputs for one finished main view; no GL state or inferred source. */
public final class PostContext {
    public final float partialTicks;
    public final float worldTicks;
    public final int width;
    public final int height;
    public final EntityLivingBase viewEntity;
    public final DrugEffectState drugState;
    public final float heatStrength;
    public final float waterStrength;
    public final float wetLens;
    public final float blurHorizontal;
    public final float blurVertical;
    public final float radialBlur;
    public final float doubleVision;
    public final float bloom;
    public final float[] bloomColor;
    public final float desaturation;
    public final float intensification;
    public final float slowRotation;
    public final float quickRotation;
    public final float noiseStrength;
    public final float digital;
    public final float digitalScaleX;
    public final float digitalScaleY;
    public final float motionStrength;

    PostContext(float partialTicks, int width, int height, EntityLivingBase entity, boolean heatEnabled, boolean waterEnabled, boolean motionEnabled, float pauseFade) {
        this.partialTicks = partialTicks;
        this.worldTicks = entity.ticksExisted + partialTicks;
        this.width = width;
        this.height = height;
        this.viewEntity = entity;
        this.drugState = DrugEffectState.capture(DrugProperties.getDrugProperties(entity), partialTicks);
        DrugProperties properties = drugState.drugProperties;
        float heat = heatEnabled && properties != null && properties.renderer != null
            ? properties.renderer.getCurrentHeatDistortion() : 0.0f;
        this.heatStrength = Float.isNaN(heat) || Float.isInfinite(heat) ? 0.0f : Math.max(0.0f, Math.min(0.01f, heat));
        float water = waterEnabled && properties != null && properties.renderer != null
            ? properties.renderer.getCurrentWaterDistortion() : 0.0f;
        waterStrength = finiteClamp(water, 0.025f);
        float wet = DrugProperties.waterOverlayEnabled && properties != null && properties.renderer != null
            ? properties.renderer.getCurrentWaterScreenDistortion() : 0.0f;
        wetLens = finiteClamp(wet, 1.0f);
        float pauseBlur = finiteClamp((float) ClientProxy.pauseMenuBlur * pauseFade * pauseFade * pauseFade, Float.MAX_VALUE);
        blurHorizontal = pauseBlur;
        blurVertical = pauseBlur + (properties == null ? 0 : finiteClamp(properties.getDrugValue("Power"), Float.MAX_VALUE));
        // The old radial wrapper had no simulation producer and always requested zero.
        radialBlur = 0;
        float vision = 0;
        if (properties != null) for (Drug drug : properties.getAllDrugs()) vision += (1 - vision) * drug.doubleVision();
        doubleVision = finiteClamp(vision, 1);
        bloomColor = new float[]{1, 1, 1, 0};
        DrugHallucinationManager manager = properties == null ? null : properties.hallucinationManager;
        bloom = manager == null ? 0 : finiteClamp(manager.getBloom(properties, partialTicks), Float.MAX_VALUE);
        if (manager != null) manager.applyColorBloom(properties, bloomColor, partialTicks);
        for (int i = 0; i < 4; i++) bloomColor[i] = finiteClamp(bloomColor[i], i == 3 ? Float.MAX_VALUE : 1);
        desaturation = manager == null ? 0 : finiteClamp(manager.getDesaturation(properties, partialTicks), 1);
        intensification = manager == null ? 0 : finiteClamp(manager.getColorIntensification(properties, partialTicks), 1);
        slowRotation = manager == null ? 0 : finiteClamp(manager.getSlowColorRotation(properties, partialTicks), 1);
        quickRotation = manager == null ? 0 : finiteClamp(manager.getQuickColorRotation(properties, partialTicks), 1);
        noiseStrength = properties == null ? 0 : finiteClamp(properties.getDrugValue("Power") * 0.6f, Float.MAX_VALUE);
        digital = properties == null ? 0 : finiteClamp(properties.getDrugValue("Zero"), 1);
        float[] scale = properties == null ? null : properties.getDigitalEffectPixelResize();
        digitalScaleX = scale == null ? 0.05f : finiteClamp(scale[0], 1);
        digitalScaleY = scale == null ? 0.05f : finiteClamp(scale[1], 1);
        motionStrength = manager == null || !motionEnabled ? 0 : finiteClamp(manager.getMotionBlur(properties, partialTicks), 0.99f);
    }

    private static float finiteClamp(float value, float maximum) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0 : Math.max(0, Math.min(maximum, value));
    }
}
