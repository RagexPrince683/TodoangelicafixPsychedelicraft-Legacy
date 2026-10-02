package ivorius.psychedelicraft.client.rendering.post;

public interface PsychePostEffect {
    String name();
    boolean isActive(PostContext context);
    default int passCount(PostContext context) { return 1; }
    default boolean isPassActive(PostContext context, int pass) { return true; }
    void render(PostContext context, PsychePostTarget source, PsychePostTarget destination, int pass);
    void reset();
}
