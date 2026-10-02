package ivorius.psychedelicraft.client.rendering.post;

final class DrugNoiseEffect extends FullscreenPostEffect {
    DrugNoiseEffect() { super("DrugNoise", "drug-noise.frag"); }
    public boolean isActive(PostContext c) { return c.noiseStrength > 0; }
    protected void bindAuxiliary(PostContext c) {}
    protected void upload(PostProgram p, PostContext c, int pass) {
        p.scalar("strength", c.noiseStrength);
        p.scalar("ticks", c.worldTicks);
    }
}
