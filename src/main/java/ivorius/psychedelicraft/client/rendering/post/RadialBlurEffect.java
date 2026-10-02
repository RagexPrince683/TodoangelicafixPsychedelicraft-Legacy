package ivorius.psychedelicraft.client.rendering.post;

final class RadialBlurEffect extends FullscreenPostEffect {
    RadialBlurEffect() { super("RadialBlur", "radial-blur.frag"); }
    public boolean isActive(PostContext c) { return c.radialBlur > 0; }
    protected void bindAuxiliary(PostContext c) {}
    protected void upload(PostProgram p, PostContext c, int pass) { p.scalar("strength", c.radialBlur); }
}
