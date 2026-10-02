package ivorius.psychedelicraft.client.rendering.post;

final class DoubleVisionEffect extends FullscreenPostEffect {
    DoubleVisionEffect() { super("DoubleVision", "double-vision.frag"); }
    public boolean isActive(PostContext c) { return c.doubleVision > 0; }
    protected void bindAuxiliary(PostContext c) {}
    protected void upload(PostProgram p, PostContext c, int pass) {
        p.scalar("strength", c.doubleVision);
        p.scalar("distance", (float) Math.sin(c.worldTicks / 20) * 0.05f * c.doubleVision);
    }
}
