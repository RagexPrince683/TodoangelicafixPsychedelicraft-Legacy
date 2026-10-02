package ivorius.psychedelicraft.client.rendering.post;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL13;

/** Base desert shimmer uses current scene color only, with no cross-pipeline depth dependency. */
final class HeatDistortionEffect extends FullscreenPostEffect {
    private static final ResourceLocation NOISE = new ResourceLocation("psychedelicraft", "textures/mod/heatDistortionNoise.png");
    HeatDistortionEffect() { super("HeatDistortion", "heat.frag"); }

    public boolean isActive(PostContext context) { return context.heatStrength > 0.0f; }

    protected void bindAuxiliary(PostContext context) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        Minecraft.getMinecraft().renderEngine.bindTexture(NOISE);
    }

    protected void upload(PostProgram program, PostContext context, int pass) {
        program.integer("noise", 1);
        program.scalar("strength", context.heatStrength);
        program.scalar("ticks", context.worldTicks * 0.15f);
    }
}
