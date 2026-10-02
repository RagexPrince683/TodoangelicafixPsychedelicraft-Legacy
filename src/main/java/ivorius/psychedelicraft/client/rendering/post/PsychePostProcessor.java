package ivorius.psychedelicraft.client.rendering.post;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Explicit completed-world color pipeline; every effect reads/writes only the two owned scene targets. */
public final class PsychePostProcessor {
    public static boolean enabled = true;
    public static boolean debug;
    public static boolean heatEnabled = true;
    public static boolean waterEnabled = true;
    public static boolean motionEnabled = true;
    public static float sunFlareIntensity;
    private static final Deque<ViewCall> calls = new ArrayDeque<ViewCall>();
    private static int worldDepth;
    private static PsychePostTarget sceneA;
    private static PsychePostTarget sceneB;
    private static World resourceWorld;
    private static int resourceDimension;
    private static long frame;
    private static boolean unsupportedReported;
    private static boolean validateIdentity;
    private static final HeatDistortionEffect HEAT = new HeatDistortionEffect();
    private static final MotionBlurEffect MOTION = new MotionBlurEffect();
    private static final LensFlareOverlay LENS = new LensFlareOverlay();
    private static float pauseFade;
    private static final PsychePostEffect[] EFFECTS = {
        new DrugColorEffect(), new ConvolutionEffect(ConvolutionEffect.BLUR), new RadialBlurEffect(),
        new DoubleVisionEffect(), new ConvolutionEffect(ConvolutionEffect.COLOR_BLOOM),
        new ConvolutionEffect(ConvolutionEffect.BLOOM), new DrugNoiseEffect(), new DigitalEffect(),
        HEAT, new WaterDistortionEffect(), new WetLensEffect(), MOTION
    };
    private static boolean unsupportedHeatReported;
    private static final PsychePostEffect IDENTITY = new PsychePostEffect() {
        public String name() { return "Identity"; }
        public void reset() {}
        public boolean isActive(PostContext context) { return true; }
        public void render(PostContext context, PsychePostTarget source, PsychePostTarget destination, int pass) {
            if (source == destination) throw new IllegalArgumentException("Post source equals destination");
            source.bindRead();
            destination.bindDraw();
            PostGL.blit(context.width, context.height);
        }
    };

    private PsychePostProcessor() {}

    /** Only the actual updateCameraAndRender -> renderWorld call offers a main view. */
    public static void beginMainCall() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean main = calls.isEmpty() && worldDepth == 0 && mc.theWorld != null
            && mc.thePlayer != null && mc.renderViewEntity == mc.thePlayer;
        calls.push(new ViewCall(mc.renderViewEntity, main));
    }

    public static void endMainCall() {
        if (!calls.isEmpty()) calls.pop();
    }

    public static void enterWorld() { worldDepth++; }
    public static void exitWorld() { worldDepth--; }

    public static boolean isMainView() {
        return calls.size() == 1 && calls.peek().main && worldDepth <= 1
            && calls.peek().entity == Minecraft.getMinecraft().renderViewEntity;
    }

    /** After Forge dispatchRenderLast: Angelica finalized and rebound the main FBO; before vanilla hand/HUD. */
    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isMainView() || worldDepth != 1 || calls.peek().processed
            || mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        // Anaglyph invokes the stage twice inside one renderWorld; process the finished second eye only.
        if (mc.gameSettings.anaglyph && EntityRenderer.anaglyphField == 0) return;
        calls.peek().processed = true;
        if (!PostGL.supported()) {
            if (!unsupportedReported) Psychedelicraft.logger.error(
                "PsychePost requires OpenGL 3.0 or OpenGL 2.1 with ARB_framebuffer_object; completed-world processing unavailable.");
            unsupportedReported = true;
            return;
        }
        frame++;
        try (PostState state = new PostState()) {
            diagnostic("entered", true);
            if (enabled) ensureTargets(mc);
            else MOTION.invalidateHistory();
            int mainFbo = OpenGlHelper.isFramebufferEnabled() ? mc.getFramebuffer().framebufferObject : 0;
            int colorBuffer = mainFbo == 0 ? GL11.GL_BACK : GL30.GL_COLOR_ATTACHMENT0;
            PostGL.bind(GL30.GL_FRAMEBUFFER, mainFbo);
            int mainRead = GL11.glGetInteger(GL11.GL_READ_BUFFER);
            IntBuffer mainDraw = PostState.captureDrawBuffers(mainFbo);
            try {
                if (!validateMain(mc, mainFbo)) return;
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                if (PostGL.hasSrgb()) GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
                GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
                if (!enabled) {
                    GL11.glDrawBuffer(colorBuffer);
                    if (GLContext.getCapabilities().OpenGL30) LENS.render(partialTicks, mc.displayWidth, mc.displayHeight, mc.renderViewEntity);
                    return;
                }
                PostGL.bind(GL30.GL_READ_FRAMEBUFFER, mainFbo);
                GL11.glReadBuffer(colorBuffer);
                sceneA.bindDraw();
                PostGL.blit(mc.displayWidth, mc.displayHeight);
                if (debug) diagnostic("capture main=" + mainFbo + " -> A=" + sceneA.framebuffer
                    + " complete display=" + mc.displayWidth + "x" + mc.displayHeight, false);
                // A single full-image comparison after creation/resize, only in diagnostic mode.
                // This reads GPU pixels in memory; it creates no captures or test artifacts.
                PostContext context = new PostContext(partialTicks, mc.displayWidth, mc.displayHeight,
                    mc.renderViewEntity, heatEnabled, waterEnabled, motionEnabled, pauseFade);
                int active = 0;
                for (PsychePostEffect effect : EFFECTS) if (effect.isActive(context)) active++;
                boolean shaders = GLContext.getCapabilities().OpenGL30;
                if (!shaders || !MOTION.isActive(context)) MOTION.invalidateHistory();
                if (active > 0 && !shaders && !unsupportedHeatReported) {
                    Psychedelicraft.logger.error("PsychePost GLSL effects require OpenGL 3.0; identity copying remains available.");
                    unsupportedHeatReported = true;
                }
                ByteBuffer original = debug && validateIdentity && (active == 0 || !shaders)
                    ? readPixels(mc.displayWidth, mc.displayHeight) : null;

                PsychePostTarget source = sceneA;
                PsychePostTarget destination = sceneB;
                if (shaders && active > 0) {
                    for (PsychePostEffect effect : EFFECTS) {
                        if (!effect.isActive(context)) continue;
                        if (effect == MOTION) MOTION.prepareHistory(context, source);
                        for (int pass = 0; pass < effect.passCount(context); pass++) {
                            if (!effect.isPassActive(context, pass)) continue;
                            effect.render(context, source, destination, pass);
                            if (debug) diagnostic(effect.name() + " pass=" + pass + " " + source.framebuffer + " -> " + destination.framebuffer, false);
                            PsychePostTarget swap = source; source = destination; destination = swap;
                        }
                    }
                } else {
                    IDENTITY.render(context, source, destination, 0);
                    source = destination;
                    if (debug) diagnostic("Identity A=" + sceneA.framebuffer + " -> B=" + sceneB.framebuffer, false);
                }

                source.bindRead();
                PostGL.bind(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
                GL11.glDrawBuffer(colorBuffer);
                PostGL.blit(mc.displayWidth, mc.displayHeight);
                if (debug) diagnostic("composite " + source.framebuffer + " -> main=" + mainFbo, false);
                // Commit history only after all current-frame passes and main composition have finished.
                if (shaders && MOTION.isActive(context)) MOTION.completeFrame(context, source);
                if (original != null) {
                    PostGL.bind(GL30.GL_READ_FRAMEBUFFER, mainFbo);
                    GL11.glReadBuffer(colorBuffer);
                    ByteBuffer result = readPixels(mc.displayWidth, mc.displayHeight);
                    int changed = 0;
                    for (int i = 0; i < original.capacity(); i += 4)
                        if (original.get(i) != result.get(i) || original.get(i + 1) != result.get(i + 1)
                            || original.get(i + 2) != result.get(i + 2) || original.get(i + 3) != result.get(i + 3)) changed++;
                    Psychedelicraft.logger.info("PsychePost identity pixel comparison: changed=" + changed
                        + "/" + (mc.displayWidth * mc.displayHeight));
                    validateIdentity = false;
                    diagnostic("identity verification", false);
                }
                // Glare is an overlay on the composed main image, outside capture and temporal history.
                if (shaders) LENS.render(partialTicks, mc.displayWidth, mc.displayHeight, mc.renderViewEntity);
            } finally {
                // Selectors belong to the main FBO, so restore them before restoring caller bindings.
                PostGL.bind(GL30.GL_FRAMEBUFFER, mainFbo);
                GL11.glReadBuffer(mainRead);
                PostState.restoreDrawBuffers(mainFbo, mainDraw);
            }
        }
        diagnostic("restored", false);
    }

    private static boolean validateMain(Minecraft mc, int mainFbo) {
        if (mainFbo == 0) return true;
        Framebuffer main = mc.getFramebuffer();
        if (main.framebufferWidth != mc.displayWidth || main.framebufferHeight != mc.displayHeight
            || PostGL.attachment(GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL11.GL_TEXTURE
            || PostGL.attachment(GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME) != main.framebufferTexture)
            throw new IllegalStateException("PsychePost completed main attachment does not match Minecraft's display image");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, main.framebufferTexture);
        if (GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) != mc.displayWidth
            || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) != mc.displayHeight
            || PostGL.status() != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("PsychePost completed main texture dimensions/completeness invalid");
        return true;
    }

    private static void ensureTargets(Minecraft mc) {
        int dimension = mc.theWorld.provider.dimensionId;
        if (sceneA != null && (resourceWorld != mc.theWorld || resourceDimension != dimension
            || sceneA.width != mc.displayWidth || sceneA.height != mc.displayHeight)) resetTargets();
        if (sceneA == null) {
            sceneA = new PsychePostTarget(mc.displayWidth, mc.displayHeight);
            try { sceneB = new PsychePostTarget(mc.displayWidth, mc.displayHeight); }
            catch (RuntimeException failure) { resetTargets(); throw failure; }
            resourceWorld = mc.theWorld;
            resourceDimension = dimension;
            validateIdentity = true;
            diagnostic("targets A=" + sceneA.framebuffer + " B=" + sceneB.framebuffer
                + " framebuffer completeness=COMPLETE", false);
        }
    }

    /** Called on the client thread at resource reload/unload; targets are recreated lazily. */
    public static void reset() {
        for (PsychePostEffect effect : EFFECTS) effect.reset();
        LENS.reset();
        resetTargets();
    }

    private static void resetTargets() {
        MOTION.releaseHistory();
        LENS.invalidateVisibility();
        if (sceneA != null) sceneA.destroy();
        if (sceneB != null) sceneB.destroy();
        sceneA = null;
        sceneB = null;
        resourceWorld = null;
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        pauseFade = mc.theWorld == null ? 0 : Math.max(0, Math.min(1, pauseFade + (mc.isGamePaused() ? 0.25f : -0.25f)));
        if (resourceWorld != null && mc.theWorld != resourceWorld) resetTargets();
        if (mc.theWorld == null) LENS.invalidateVisibility();
    }

    private static ByteBuffer readPixels(int width, int height) {
        int pack = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] parameters = {GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_ROW_LENGTH,
            GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SKIP_PIXELS};
        int[] saved = new int[parameters.length];
        for (int i = 0; i < parameters.length; i++) saved[i] = GL11.glGetInteger(parameters[i]);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            for (int i = 0; i < parameters.length; i++) GL11.glPixelStorei(parameters[i], i == 0 ? 1 : 0);
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            return pixels;
        } finally {
            for (int i = 0; i < parameters.length; i++) GL11.glPixelStorei(parameters[i], saved[i]);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pack);
        }
    }

    private static void diagnostic(String stage, boolean incoming) {
        if (!debug) return;
        int error;
        boolean failed = false;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            failed = true;
            Psychedelicraft.logger.error("PsychePost frame=" + frame + " " + stage
                + (incoming ? " incoming GL error=" : " GL error=") + error);
        }
        // Keep deliberate diagnostic runs readable; errors are always reported immediately.
        if (!failed && (frame <= 8 || frame % 120 == 0))
            Psychedelicraft.logger.info("PsychePost frame=" + frame + " " + stage + " GL_NO_ERROR");
    }

    public static void passDiagnostic(String stage) { diagnostic(stage, false); }

    private static final class ViewCall {
        final EntityLivingBase entity;
        final boolean main;
        boolean processed;
        ViewCall(EntityLivingBase entity, boolean main) { this.entity = entity; this.main = main; }
    }
}
