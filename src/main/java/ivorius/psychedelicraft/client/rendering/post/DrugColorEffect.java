package ivorius.psychedelicraft.client.rendering.post;

import ivorius.psychedelicraft.client.rendering.DrugEffectState;

/** Waves, coloration and fractal modulation share one current-color sample. */
final class DrugColorEffect extends FullscreenPostEffect {
    DrugColorEffect() { super("DrugColor", "drug-color.frag"); }
    public boolean isActive(PostContext c) {
        return c.drugState.hasDrugScreenEffect || c.desaturation > 0 || c.intensification > 0
            || c.slowRotation > 0 || c.quickRotation > 0;
    }
    protected void bindAuxiliary(PostContext c) {}
    protected void upload(PostProgram p, PostContext c, int pass) {
        DrugEffectState d = c.drugState;
        p.vector("waves", d.bigWaves, d.smallWaves, d.wiggleWaves);
        p.vector("deformation", d.surfaceFractal, d.distantWorldDeformation);
        p.color("pulse", d.pulseColor);
        p.color("contrast", d.contrastColor);
        p.vector("rotation", c.slowRotation, c.quickRotation);
        p.vector("saturation", c.desaturation, c.intensification);
        p.scalar("ticks", c.worldTicks);
    }
}
