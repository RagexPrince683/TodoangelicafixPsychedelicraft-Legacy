package ivorius.psychedelicraft.client.rendering.post;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL13;

/** Slow underwater waves retain the existing configured strength, independent of depth. */
final class WaterDistortionEffect extends FullscreenPostEffect {
    private static final ResourceLocation NOISE = new ResourceLocation("psychedelicraft", "textures/mod/heatDistortionNoise.png");
    WaterDistortionEffect() { super("WaterDistortion", "heat.frag"); }
    public boolean isActive(PostContext context) { return context.waterStrength > 0; }
    protected void bindAuxiliary(PostContext context) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        Minecraft.getMinecraft().renderEngine.bindTexture(NOISE);
    }
    protected void upload(PostProgram program, PostContext context, int pass) {
        program.integer("noise", 1);
        program.scalar("strength", context.waterStrength);
        program.scalar("ticks", context.worldTicks * 0.03f);
    }
}
