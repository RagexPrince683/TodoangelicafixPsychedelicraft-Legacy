package ivorius.psychedelicraft.client.rendering.post;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/** The single draw contract for all owned full-screen shader effects. */
abstract class FullscreenPostEffect implements PsychePostEffect {
    private PostProgram program;
    private final String name;
    private final String fragment;
    private final String[] stages;

    FullscreenPostEffect(String name, String fragment) {
        this.name = name;
        this.fragment = fragment;
        stages = new String[]{name + "/state-captured", name + "/raster-prepared", name + "/program-textures-bound",
            name + "/uniforms-uploaded", name + "/drawn", name + "/state-restored"};
    }

    public final String name() { return name; }

    @Override
    public final void render(PostContext context, PsychePostTarget source, PsychePostTarget destination, int pass) {
        if (source == destination) throw new IllegalArgumentException("Post source equals destination");
        try (PostPassState state = new PostPassState()) {
            PsychePostProcessor.passDiagnostic(stages[0]);
            destination.bindDraw();
            state.prepare(context.width, context.height);
            PsychePostProcessor.passDiagnostic(stages[1]);
            if (program == null) program = new PostProgram(fragment);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, source.colorTexture);
            bindAuxiliary(context);
            program.bind();
            PsychePostProcessor.passDiagnostic(stages[2]);
            program.integer("scene", 0);
            program.vector("texel", 1.0f / context.width, 1.0f / context.height);
            upload(program, context, pass);
            PsychePostProcessor.passDiagnostic(stages[3]);
            program.draw();
            PsychePostProcessor.passDiagnostic(stages[4]);
            GL20.glUseProgram(0);
        }
        PsychePostProcessor.passDiagnostic(stages[5]);
    }

    protected abstract void bindAuxiliary(PostContext context);
    protected abstract void upload(PostProgram program, PostContext context, int pass);

    public void reset() { if (program != null) program.destroy(); program = null; }
}
