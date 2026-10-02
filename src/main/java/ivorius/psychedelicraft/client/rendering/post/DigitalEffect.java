package ivorius.psychedelicraft.client.rendering.post;

import ivorius.psychedelicraft.internal.math.IvMathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL13;

/** Pixelation, palette reduction and glyphs need color and an atlas, never inferred depth. */
final class DigitalEffect extends FullscreenPostEffect {
    private static final ResourceLocation GLYPHS = new ResourceLocation("psychedelicraft", "textures/mod/digitalText.png");
    DigitalEffect() { super("Digital", "digital.frag"); }
    public boolean isActive(PostContext c) { return c.digital > 0; }
    protected void bindAuxiliary(PostContext c) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        Minecraft.getMinecraft().renderEngine.bindTexture(GLYPHS);
    }
    protected void upload(PostProgram p, PostContext c, int pass) {
        float value = c.digital;
        float downscale = (float) IvMathHelper.mixEaseInOut(0.0, 0.95, Math.min(value * 3, 1)) + value * 0.05f;
        p.integer("glyphs", 1);
        p.vector("resolution", Math.max(1, c.width * (1 + (c.digitalScaleX - 1) * downscale)),
            Math.max(1, c.height * (1 + (c.digitalScaleY - 1) * downscale)));
        p.scalar("textProgress", (float) IvMathHelper.easeZeroToOne((value - 0.2f) * 5));
        p.scalar("binaryProgress", (float) IvMathHelper.easeZeroToOne((value - 0.8f) * 10));
        p.scalar("palette", value > 0.4f ? Math.max(256 / ((value - 0.4f) * 640 + 1), 2) : 0);
        p.scalar("saturation", 1 - (float) IvMathHelper.easeZeroToOne((value - 0.6f) * 5));
    }
}
