package ivorius.psychedelicraft.client.rendering.post;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL13;

/** Current-scene refraction from sliding droplets, fading with the simulation's wet-screen timer. */
final class WetLensEffect extends FullscreenPostEffect {
    private static final ResourceLocation DROPLETS = new ResourceLocation("psychedelicraft", "textures/mod/waterDistortion.png");
    WetLensEffect() { super("WaterDroplets", "wet.frag"); }
    public boolean isActive(PostContext context) { return context.wetLens > 0; }
    protected void bindAuxiliary(PostContext context) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        Minecraft.getMinecraft().renderEngine.bindTexture(DROPLETS);
    }
    protected void upload(PostProgram program, PostContext context, int pass) {
        program.integer("droplets", 1);
        program.scalar("wetness", context.wetLens);
        program.scalar("ticks", context.worldTicks);
    }
}
