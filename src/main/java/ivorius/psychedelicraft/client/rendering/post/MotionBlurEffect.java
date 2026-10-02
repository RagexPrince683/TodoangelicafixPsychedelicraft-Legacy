package ivorius.psychedelicraft.client.rendering.post;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** Only temporal effect: one dedicated history image, never an arbitrary A/B residue. */
final class MotionBlurEffect extends FullscreenPostEffect {
    private PsychePostTarget history;
    private boolean initialized;
    private long completedAt;
    private EntityLivingBase historyView;
    private float historyWeight;

    MotionBlurEffect() { super("MotionBlur", "motion.frag"); }
    public boolean isActive(PostContext c) { return c.motionStrength > 0; }

    void prepareHistory(PostContext c, PsychePostTarget current) {
        long now = System.nanoTime();
        double seconds = completedAt == 0 ? 0 : (now - completedAt) / 1.0e9;
        if (history != null && (history.width != c.width || history.height != c.height)) releaseHistory();
        if (history == null) history = new PsychePostTarget(c.width, c.height);
        if (!initialized || historyView != c.viewEntity || seconds > 0.25 || Minecraft.getMinecraft().isGamePaused()) {
            copy(current, c);
            initialized = true;
            historyView = c.viewEntity;
            historyWeight = 0;
            PsychePostProcessor.passDiagnostic("MotionBlur/history-initialized-current");
        } else {
            // Frame-time-scaled persistence keeps the trail similar at different render rates.
            double persistence = 1.0 - Math.exp(-c.motionStrength * 8.0);
            historyWeight = (float) Math.pow(persistence, Math.max(seconds, 1.0e-6) * 60.0);
        }
    }

    protected void bindAuxiliary(PostContext c) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, history.colorTexture);
    }
    protected void upload(PostProgram p, PostContext c, int pass) {
        p.integer("history", 1);
        p.scalar("weight", historyWeight);
    }

    void completeFrame(PostContext c, PsychePostTarget completed) {
        copy(completed, c);
        completedAt = System.nanoTime();
        PsychePostProcessor.passDiagnostic("MotionBlur/history-committed");
    }

    private void copy(PsychePostTarget source, PostContext c) {
        source.bindRead();
        history.bindDraw();
        PostGL.blit(c.width, c.height);
    }

    void invalidateHistory() { initialized = false; completedAt = 0; historyView = null; }
    void releaseHistory() {
        if (history != null) history.destroy();
        history = null;
        invalidateHistory();
    }
    @Override public void reset() { super.reset(); releaseHistory(); }
}
