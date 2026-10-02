package ivorius.psychedelicraft.client.rendering.post;

/** Separable kernels expose each draw to the processor; they never manage scene swaps. */
final class ConvolutionEffect extends FullscreenPostEffect {
    static final int BLUR = 0;
    static final int BLOOM = 1;
    static final int COLOR_BLOOM = 2;
    private final int mode;

    ConvolutionEffect(int mode) {
        super(mode == BLUR ? "Blur" : mode == BLOOM ? "Bloom" : "ColorBloom", "convolution.frag");
        this.mode = mode;
    }
    private float strength(PostContext c, int axis) {
        return mode == BLUR ? (axis == 0 ? c.blurHorizontal : c.blurVertical)
            : mode == BLOOM ? c.bloom : c.bloomColor[3];
    }
    public boolean isActive(PostContext c) { return Math.max(strength(c, 0), strength(c, 1)) > 0; }
    public int passCount(PostContext c) { return (int) Math.ceil(Math.max(strength(c, 0), strength(c, 1))) * 2; }
    public boolean isPassActive(PostContext c, int pass) { return strength(c, pass % 2) > pass / 2; }
    protected void bindAuxiliary(PostContext c) {}
    protected void upload(PostProgram p, PostContext c, int pass) {
        int axis = pass % 2;
        float spacing = mode == BLOOM ? 6 : 1;
        p.vector("direction", axis == 0 ? spacing / c.width : 0, axis == 1 ? spacing / c.height : 0);
        p.integer("mode", mode);
        p.scalar("strength", Math.min(1, strength(c, axis) - pass / 2));
        p.vector("bloomColor", c.bloomColor[0], c.bloomColor[1], c.bloomColor[2]);
    }
}
