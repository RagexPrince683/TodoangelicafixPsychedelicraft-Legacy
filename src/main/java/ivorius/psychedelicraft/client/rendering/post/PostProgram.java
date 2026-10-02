package ivorius.psychedelicraft.client.rendering.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** A standalone GLSL program and owned clip-space quad; no compatibility shader helpers. */
final class PostProgram {
    private final int program;
    private final int vao;
    private final int vertices;
    private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

    PostProgram(String fragment) {
        this("fullscreen.vert", fragment);
    }

    PostProgram(String vertexResource, String fragment) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, vertexResource);
        int pixel = 0;
        int linked = GL20.glCreateProgram();
        int array = 0;
        int buffer = 0;
        try {
            pixel = compile(GL20.GL_FRAGMENT_SHADER, fragment);
            GL20.glAttachShader(linked, vertex);
            GL20.glAttachShader(linked, pixel);
            GL20.glBindAttribLocation(linked, 0, "postPosition");
            GL30.glBindFragDataLocation(linked, 0, "postColor");
            GL20.glLinkProgram(linked);
            if (GL20.glGetProgrami(linked, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
                throw new IllegalStateException("PsychePost " + fragment + " link: " + GL20.glGetProgramInfoLog(linked, 32768));
            array = GL30.glGenVertexArrays();
            buffer = GL15.glGenBuffers();
            GL30.glBindVertexArray(array);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            FloatBuffer quad = BufferUtils.createFloatBuffer(8);
            quad.put(new float[]{-1, -1, 1, -1, -1, 1, 1, 1}).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0L);
            GL20.glEnableVertexAttribArray(0);
        } catch (RuntimeException failure) {
            if (array != 0) GL30.glDeleteVertexArrays(array);
            if (buffer != 0) GL15.glDeleteBuffers(buffer);
            GL20.glDeleteProgram(linked);
            throw failure;
        } finally {
            GL20.glDeleteShader(vertex);
            if (pixel != 0) GL20.glDeleteShader(pixel);
        }
        program = linked;
        vao = array;
        vertices = buffer;
    }

    private static int compile(int type, String resource) {
        String source;
        ResourceLocation location = new ResourceLocation("psychedelicraft", "shaders/post/" + resource);
        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
            source = IOUtils.toString(stream, "UTF-8");
        } catch (IOException failure) {
            throw new IllegalStateException("PsychePost cannot read " + location, failure);
        }
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String error = GL20.glGetShaderInfoLog(shader, 32768);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("PsychePost " + resource + " compile: " + error);
        }
        return shader;
    }

    void bind() { GL20.glUseProgram(program); }

    private int location(String name) {
        Integer location = uniforms.get(name);
        if (location == null) { location = GL20.glGetUniformLocation(program, name); uniforms.put(name, location); }
        return location;
    }

    void integer(String name, int value) { GL20.glUniform1i(location(name), value); }
    void scalar(String name, float value) { GL20.glUniform1f(location(name), value); }
    void vector(String name, float x, float y) { GL20.glUniform2f(location(name), x, y); }
    void vector(String name, float x, float y, float z) { GL20.glUniform3f(location(name), x, y, z); }
    void vector(String name, float x, float y, float z, float w) { GL20.glUniform4f(location(name), x, y, z, w); }
    void color(String name, float[] value) { GL20.glUniform4f(location(name), value[0], value[1], value[2], value[3]); }

    void draw() {
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
    }

    void destroy() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vertices);
        GL20.glDeleteProgram(program);
    }
}
