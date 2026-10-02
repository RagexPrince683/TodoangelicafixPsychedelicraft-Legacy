package ivorius.psychedelicraft.client.rendering.post;

import ivorius.psychedelicraft.client.rendering.PsycheMatrixHelper;
import ivorius.psychedelicraft.internal.math.IvMathHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;

/** Current-camera additive sprites on the known main destination. No scene/history/copy API. */
final class LensFlareOverlay {
    private static final float[] SIZES = {0.15f, 0.24f, 0.12f, 0.036f, 0.06f, 0.048f, 0.006f, 0.012f, 0.5f, 0.09f, 0.036f, 0.09f, 0.06f, 0.05f, 0.6f};
    private static final float[] INFLUENCES = {-1.3f, -2, 0.2f, 0.4f, 0.25f, -0.25f, -0.7f, -1, 1, 1.4f, -1.31f, -1.2f, -1.5f, -1.55f, -3};
    private final ResourceLocation[] textures = new ResourceLocation[SIZES.length];
    private final ResourceLocation blindness = texture("sunBlindness");
    private PostProgram program;
    private World world;
    private EntityLivingBase view;
    private float visibility;

    LensFlareOverlay() { for (int i = 0; i < textures.length; i++) textures[i] = texture("flare" + i); }
    private static ResourceLocation texture(String name) { return new ResourceLocation("psychedelicraft", "textures/mod/" + name + ".png"); }

    void render(float partialTicks, int width, int height, EntityLivingBase entity) {
        Minecraft mc = Minecraft.getMinecraft();
        float intensity = PsychePostProcessor.sunFlareIntensity;
        if (!(intensity > 0) || Float.isInfinite(intensity) || mc.theWorld == null || mc.theWorld.provider.hasNoSky) return;
        if (world != mc.theWorld || view != entity) {
            invalidateVisibility(); world = mc.theWorld; view = entity;
        }
        updateVisibility(partialTicks, entity);
        if (visibility <= 0) return;
        float angle = world.getCelestialAngleRadians(partialTicks);
        Vector3f position = PsycheMatrixHelper.projectDirectionCurrentView(entity,
            new Vector3f(-MathHelper.sin(angle) * 120, MathHelper.cos(angle) * 120, 0), height);
        if (position == null || !finite(position.x) || !finite(position.y)) return;
        Vec3 fog = world.getFogColor(partialTicks);
        float red = Math.max(0, (float) fog.xCoord - 0.1f);
        float green = Math.max(0, (float) fog.yCoord - 0.1f);
        float blue = Math.max(0, (float) fog.zCoord - 0.1f);
        float dx = position.x - width * 0.5f;
        float dy = position.y - height * 0.5f;
        float size = Math.max(width, height);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        try (PostPassState state = new PostPassState()) {
            int main = OpenGlHelper.isFramebufferEnabled() ? mc.getFramebuffer().framebufferObject : 0;
            PostGL.bind(GL30.GL_DRAW_FRAMEBUFFER, main);
            GL11.glDrawBuffer(main == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0);
            state.prepare(width, height);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            if (program == null) program = new PostProgram("lens.vert", "lens.frag");
            program.bind();
            program.integer("flare", 0);
            for (int i = 0; i < textures.length; i++) {
                program.vector("tint", red, green, blue, (i == 8 ? 1 : 0.5f) * visibility * intensity);
                draw(mc, textures[i], width * 0.5f + dx * INFLUENCES[i], height * 0.5f + dy * INFLUENCES[i],
                    size * SIZES[i] * 0.5f, width, height);
            }
            float distance = 1 - dx * dx / (width * width * 0.25f) - dy * dy / (height * height * 0.25f);
            float diameter = (distance - 0.1f) * intensity * 250 * size;
            if (diameter > 0) {
                program.vector("tint", red, green, blue, Math.min(1, diameter / size / 150) * visibility);
                draw(mc, blindness, position.x, position.y, diameter * 0.5f, width, height);
            }
            PsychePostProcessor.passDiagnostic("LensFlare/drawn-after-composition-history");
        } finally {
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL20.glBlendEquationSeparate(eqRgb, eqAlpha);
        }
        PsychePostProcessor.passDiagnostic("LensFlare/state-restored");
    }

    private void draw(Minecraft mc, ResourceLocation texture, float x, float y, float halfSize, int width, int height) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        mc.renderEngine.bindTexture(texture);
        program.vector("rect", x * 2 / width - 1, 1 - y * 2 / height, halfSize * 2 / width, halfSize * 2 / height);
        program.draw();
    }

    private void updateVisibility(float partialTicks, EntityLivingBase entity) {
        float angle = world.getCelestialAngleRadians(partialTicks);
        float spread = 5.0f / 180 * (float) Math.PI;
        Vec3 origin = entity.getPosition(partialTicks);
        float visible = 0;
        for (int corner = 0; corner < 4; corner++) {
            float a = angle + (corner < 2 ? -spread : spread);
            Vec3 start = Vec3.createVectorHelper(origin.xCoord, origin.yCoord, origin.zCoord);
            Vec3 end = start.addVector(-MathHelper.sin(a) * 120, MathHelper.cos(a) * 120, (corner % 2 == 0 ? -20 : 20));
            if (world.func_147447_a(start, end, true, true, true) == null) visible += 0.25f;
        }
        visibility = (float) IvMathHelper.nearValue(visibility, visible * (1 - world.getRainStrength(partialTicks)), 0.1f, 0.01f);
    }

    void invalidateVisibility() { world = null; view = null; visibility = 0; }
    void reset() { if (program != null) program.destroy(); program = null; invalidateVisibility(); }
    private static boolean finite(float value) { return !Float.isNaN(value) && !Float.isInfinite(value); }
}
