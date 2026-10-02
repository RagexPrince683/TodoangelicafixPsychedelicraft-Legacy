package ivorius.psychedelicraft.client.rendering;

import java.nio.IntBuffer;
import java.util.List;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.rendering.effectWrappers.EffectWrapper;
import ivorius.psychedelicraft.client.rendering.effectWrappers.ShaderWrapper;
import ivorius.psychedelicraft.client.rendering.effectWrappers.WrapperMotionBlur;
import ivorius.psychedelicraft.internal.rendering.IvOpenGLTexturePingPong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/** Opt-in frame records, silent (and allocation-free) during normal play. */
public final class PostProcessDiagnostics
{
    public static boolean enabled;
    private static long frame;
    private static StringBuilder stages;
    private static int stageCount;
    private static final IntBuffer RECT = BufferUtils.createIntBuffer(16);

    public static void beginFrame()
    {
        if (!enabled) { stages = null; return; }
        frame++;
        stageCount = 0;
        stages = new StringBuilder();
    }

    public static void record(String stage, boolean main, int nesting,
                              IvOpenGLTexturePingPong pingPong, CompletedScene scene)
    {
        if (!enabled || stages == null || stageCount++ >= 64) return;
        Minecraft mc = Minecraft.getMinecraft();
        stages.append("\n  ").append(stage)
            .append(" main=").append(main).append(" worldDepth=").append(nesting)
            .append(" entity=").append(mc.renderViewEntity == null ? "null"
                : mc.renderViewEntity.getClass().getName() + "#" + mc.renderViewEntity.getEntityId())
            .append(" program=").append(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM))
            .append(" drawFBO=").append(RenderStateGuard.getBoundDrawFramebuffer())
            .append(" readFBO=").append(RenderStateGuard.getBoundReadFramebuffer())
            .append(" drawBuffers=[");
        int count = OpenGlHelper.framebufferSupported && RenderStateGuard.getBoundDrawFramebuffer() != 0
            ? GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS) : 1;
        for (int index = 0; index < count; index++)
        {
            if (index > 0) stages.append(',');
            stages.append(GL11.glGetInteger(count == 1 ? GL11.GL_DRAW_BUFFER : GL20.GL_DRAW_BUFFER0 + index));
        }
        stages.append("] readBuffer=").append(GL11.glGetInteger(GL11.GL_READ_BUFFER))
            .append(" viewport=");
        appendRectangle(GL11.GL_VIEWPORT);
        stages.append(" display=").append(mc.displayWidth).append('x').append(mc.displayHeight)
            .append(" mainFramebuffer=").append(mc.getFramebuffer().framebufferWidth)
            .append('x').append(mc.getFramebuffer().framebufferHeight)
            .append(" source=");
        if (scene == null) stages.append("unacquired");
        else stages.append(scene.framebuffer).append('/').append(scene.colorBuffer)
            .append(" rectangle=0,0,").append(scene.width).append(',').append(scene.height)
            .append(" actualTexture=").append(scene.textureWidth).append('x').append(scene.textureHeight);
        stages.append(" pingPong=").append(pingPong == null ? "none"
            : pingPong.getScreenWidth() + "x" + pingPong.getScreenHeight())
            .append(" activeTexture=").append(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE))
            .append(" scissor=").append(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)).append('/');
        appendRectangle(GL11.GL_SCISSOR_BOX);
        // Consume/report errors only in explicitly enabled diagnostics, including the incoming baseline.
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR)
            stages.append(" GLerror=").append(error);
    }

    private static void appendRectangle(int parameter)
    {
        RECT.clear();
        GL11.glGetInteger(parameter, RECT);
        for (int index = 0; index < 4; index++)
        {
            if (index > 0) stages.append(',');
            stages.append(RECT.get(index));
        }
    }

    public static void finishFrame(List<EffectWrapper> wrappers, float partialTicks, boolean depth)
    {
        if (!enabled || stages == null) return;
        StringBuilder effects = new StringBuilder();
        Minecraft mc = Minecraft.getMinecraft();
        for (EffectWrapper wrapper : wrappers)
        {
            boolean active = mc.renderViewEntity != null
                && ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates.shader2DEnabled
                && (wrapper instanceof ShaderWrapper
                ? ((ShaderWrapper<?>) wrapper).shaderInstance.shouldApply(mc.renderViewEntity.ticksExisted + partialTicks)
                : wrapper instanceof WrapperMotionBlur && ((WrapperMotionBlur) wrapper).screenEffect.motionBlur > 0);
            if (active) effects.append(wrapper.getClass().getSimpleName()).append(' ');
        }
        effects.append("lensFlareIntensity=").append(
            ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates.sunFlareIntensity);
        Psychedelicraft.logger.info("[PS post-process frame " + frame + "] effects=" + effects
            + " matchingWorldDepth=" + depth + stages);
        stages = null;
    }
}
