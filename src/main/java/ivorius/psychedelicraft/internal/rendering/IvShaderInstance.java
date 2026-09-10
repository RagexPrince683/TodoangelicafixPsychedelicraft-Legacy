/*
 * Copyright 2014 Lukas Tenbrink
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ivorius.psychedelicraft.internal.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.client.renderer.OpenGlHelper;

import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix2f;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class IvShaderInstance {

    public Logger logger;

    private int shaderID = 0;

    private boolean shaderActive = false;
    private int previousShaderID = 0;

    private TObjectIntMap<String> uniformLocations = new TObjectIntHashMap<>();

    /* One bounded scratch set per render thread: no per-upload native buffer wrappers and no unsafe sharing. */
    private static final int SCRATCH_CAPACITY = 256;
    private static final ThreadLocal<UniformScratch> UNIFORM_SCRATCH = new ThreadLocal<UniformScratch>() {
        @Override
        protected UniformScratch initialValue() {
            return new UniformScratch();
        }
    };

    private static class UniformScratch {
        final IntBuffer ints = BufferUtils.createIntBuffer(SCRATCH_CAPACITY);
        final FloatBuffer floats = BufferUtils.createFloatBuffer(SCRATCH_CAPACITY);
    }

    public int getShaderID() {
        return shaderID;
    }

    public IvShaderInstance(Logger logger) {
        this.logger = logger;
    }

    public void trySettingUpShader(String vertexShaderFile, String fragmentShaderFile) {
        if (shaderID <= 0) {
            registerShader(vertexShaderFile, fragmentShaderFile);
        }
    }

    public void registerShader(String vertexShaderCode, String fragmentShaderCode) {
        deleteShader();

        int vertShader = -1;
        int fragShader = -1;

        try {
            if (vertexShaderCode != null) vertShader = createShader(vertexShaderCode, OpenGlHelper.field_153209_q);

            if (fragmentShaderCode != null) fragShader = createShader(fragmentShaderCode, OpenGlHelper.field_153210_r);
        } catch (Exception exc) {
            if (vertShader > 0) OpenGlHelper.func_153180_a(vertShader);
            if (fragShader > 0) OpenGlHelper.func_153180_a(fragShader);
            exc.printStackTrace();
            return;
        }

        shaderID = OpenGlHelper.func_153183_d();

        if (vertShader > 0) {
            OpenGlHelper.func_153178_b(shaderID, vertShader);
            OpenGlHelper.func_153180_a(vertShader);
        }

        if (fragShader > 0) {
            OpenGlHelper.func_153178_b(shaderID, fragShader);
            OpenGlHelper.func_153180_a(fragShader);
        }

        OpenGlHelper.func_153179_f(shaderID);
        if (OpenGlHelper.func_153175_a(shaderID, OpenGlHelper.field_153207_o) == GL11.GL_FALSE) {
            logger.error(OpenGlHelper.func_153166_e(shaderID, 0x8000));
            deleteShader();
            return;
        }

        IvOpenGLHelper.glValidateProgram(shaderID);
        if (OpenGlHelper.func_153175_a(shaderID, IvOpenGLHelper.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            logger.error(OpenGlHelper.func_153166_e(shaderID, 0x8000));
            deleteShader();
        }
    }

    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = OpenGlHelper.func_153195_b(shaderType);

            if (shader == 0) return 0;

            byte[] shaderCodeBytes = shaderCode.getBytes();
            ByteBuffer shaderCodeBuf = BufferUtils.createByteBuffer(shaderCodeBytes.length);
            shaderCodeBuf.put(shaderCodeBytes);
            shaderCodeBuf.position(0);

            OpenGlHelper.func_153169_a(shader, shaderCodeBuf);
            OpenGlHelper.func_153170_c(shader);

            if (OpenGlHelper.func_153157_c(shader, OpenGlHelper.field_153208_p) == GL11.GL_FALSE) {
                throw new RuntimeException("Error creating shader: " + OpenGlHelper.func_153166_e(shader, 0x8000));
            }

            return shader;
        } catch (Exception exc) {
            if (shader != 0) OpenGlHelper.func_153180_a(shader);

            throw new RuntimeException(exc);
        }
    }

    public boolean useShader() {
        if (shaderID <= 0 && !shaderActive) {
            return false;
        }

        shaderActive = true;
        previousShaderID = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        OpenGlHelper.func_153161_d(shaderID);

        return true;
    }

    public void stopUsingShader() {
        if (shaderID <= 0 && shaderActive) {
            return;
        }

        OpenGlHelper.func_153161_d(previousShaderID);
        previousShaderID = 0;
        shaderActive = false;
    }

    public boolean isShaderActive() {
        return shaderActive;
    }

    public boolean setUniformInts(String key, int value) {
        IntBuffer buffer = prepareInts(1);
        buffer.put(value).flip();
        return uploadInts(key, 1, buffer);
    }

    public boolean setUniformInts(String key, int first, int second) {
        IntBuffer buffer = prepareInts(2);
        buffer.put(first).put(second).flip();
        return uploadInts(key, 2, buffer);
    }

    public boolean setUniformInts(String key, int first, int second, int third) {
        IntBuffer buffer = prepareInts(3);
        buffer.put(first).put(second).put(third).flip();
        return uploadInts(key, 3, buffer);
    }

    public boolean setUniformInts(String key, int first, int second, int third, int fourth) {
        IntBuffer buffer = prepareInts(4);
        buffer.put(first).put(second).put(third).put(fourth).flip();
        return uploadInts(key, 4, buffer);
    }

    public boolean setUniformInts(String key, int... values) {
        return setUniformIntsOfType(key, values.length, values);
    }

    public boolean setUniformIntsOfType(String key, int typeLength, int... values) {
        return uploadInts(key, typeLength, values, values.length);
    }

    private boolean uploadInts(String key, int typeLength, int[] values, int length) {
        IntBuffer buffer = prepareInts(length);
        buffer.put(values, 0, length).flip();
        return uploadInts(key, typeLength, buffer);
    }

    private boolean uploadInts(String key, int typeLength, IntBuffer buffer) {
        if (shaderID <= 0 || !shaderActive) return false;
        int location = getUniformLocation(key);
        switch (typeLength) {
            case 1: OpenGlHelper.func_153181_a(location, buffer); break;
            case 2: OpenGlHelper.func_153182_b(location, buffer); break;
            case 3: OpenGlHelper.func_153192_c(location, buffer); break;
            case 4: OpenGlHelper.func_153162_d(location, buffer); break;
            default: throw new IllegalArgumentException("Uniform vector width must be 1 through 4");
        }
        return true;
    }

    public boolean setUniformFloats(String key, float value) {
        FloatBuffer buffer = prepareFloats(1);
        buffer.put(value).flip();
        return uploadFloats(key, 1, buffer);
    }

    public boolean setUniformFloats(String key, float first, float second) {
        FloatBuffer buffer = prepareFloats(2);
        buffer.put(first).put(second).flip();
        return uploadFloats(key, 2, buffer);
    }

    public boolean setUniformFloats(String key, float first, float second, float third) {
        FloatBuffer buffer = prepareFloats(3);
        buffer.put(first).put(second).put(third).flip();
        return uploadFloats(key, 3, buffer);
    }

    public boolean setUniformFloats(String key, float first, float second, float third, float fourth) {
        FloatBuffer buffer = prepareFloats(4);
        buffer.put(first).put(second).put(third).put(fourth).flip();
        return uploadFloats(key, 4, buffer);
    }

    public boolean setUniformFloats(String key, float... values) {
        return setUniformFloatsOfType(key, values.length, values);
    }

    public boolean setUniformFloatsOfType(String key, int typeLength, float... values) {
        FloatBuffer buffer = prepareFloats(values.length);
        buffer.put(values).flip();
        return uploadFloats(key, typeLength, buffer);
    }

    private boolean uploadFloats(String key, int typeLength, FloatBuffer buffer) {
        if (shaderID <= 0 || !shaderActive) return false;
        int location = getUniformLocation(key);
        switch (typeLength) {
            case 1: OpenGlHelper.func_153168_a(location, buffer); break;
            case 2: OpenGlHelper.func_153177_b(location, buffer); break;
            case 3: OpenGlHelper.func_153191_c(location, buffer); break;
            case 4: OpenGlHelper.func_153159_d(location, buffer); break;
            default: throw new IllegalArgumentException("Uniform vector width must be 1 through 4");
        }
        return true;
    }

    public boolean setUniformMatrix(String key, Matrix matrix) {
        if (shaderID <= 0 || !shaderActive) return false;
        int width;
        if (matrix instanceof Matrix2f) {
            width = 2;
        } else if (matrix instanceof Matrix3f) {
            width = 3;
        } else if (matrix instanceof Matrix4f) {
            width = 4;
        } else {
            width = 0;
        }
        if (width == 0) throw new IllegalArgumentException("Unsupported matrix type");
        FloatBuffer buffer = prepareFloats(width * width);
        matrix.store(buffer);
        buffer.flip();
        int location = getUniformLocation(key);
        if (width == 2) OpenGlHelper.func_153173_a(location, false, buffer);
        else if (width == 3) OpenGlHelper.func_153189_b(location, false, buffer);
        else OpenGlHelper.func_153160_c(location, false, buffer);
        return true;
    }

    private static IntBuffer prepareInts(int size) {
        if (size > SCRATCH_CAPACITY) return BufferUtils.createIntBuffer(size);
        IntBuffer buffer = UNIFORM_SCRATCH.get().ints;
        buffer.clear();
        buffer.limit(size);
        return buffer;
    }

    private static FloatBuffer prepareFloats(int size) {
        if (size > SCRATCH_CAPACITY) return BufferUtils.createFloatBuffer(size);
        FloatBuffer buffer = UNIFORM_SCRATCH.get().floats;
        buffer.clear();
        buffer.limit(size);
        return buffer;
    }

    public Integer getUniformLocation(String key) {
        if (shaderID <= 0) return 0;

        if (!uniformLocations.containsKey(key)) uniformLocations.put(key, OpenGlHelper.func_153194_a(shaderID, key));

        return uniformLocations.get(key);
    }

    public void deleteShader() {
        if (shaderActive) stopUsingShader();

        if (shaderID > 0) {
            OpenGlHelper.func_153187_e(shaderID);
            shaderID = 0;
        }

        uniformLocations.clear();
    }

    public static void outputShaderInfo(Logger logger) {
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String version = GL11.glGetString(GL11.GL_VERSION);
        boolean fboSupported = OpenGlHelper.framebufferSupported;

        String majorVersion;
        String minorVersion;

        String glslVersion;

        try {
            glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        } catch (Exception ex) {
            glslVersion = "? (No GL20)";
        }

        try {
            minorVersion = "" + GL11.glGetInteger(GL30.GL_MINOR_VERSION);
            majorVersion = "" + GL11.glGetInteger(GL30.GL_MAJOR_VERSION);
        } catch (Exception ex) {
            minorVersion = "?";
            majorVersion = "? (No GL 30)";
        }

        printAlignedInfo("Vendor", vendor, logger);
        printAlignedInfo("Renderer", renderer, logger);
        printAlignedInfo("Version", version, logger);
        printAlignedInfo("Versions", getGLVersions(GLContext.getCapabilities()), logger);
        printAlignedInfo("Version Range", String.format("%s - %s", minorVersion, majorVersion), logger);
        printAlignedInfo("GLSL Version", glslVersion, logger);
        printAlignedInfo("Frame buffer object", fboSupported ? "Supported" : "Unsupported", logger);
    }

    private static void printAlignedInfo(String category, String info, Logger logger) {
        logger.info(String.format("%-20s: %s", category, info));
    }

    private static String getGLVersions(ContextCapabilities cap) {
        String versions = "";

        try {
            if (cap.OpenGL11) versions += ":11";
            if (cap.OpenGL12) versions += ":12";
            if (cap.OpenGL13) versions += ":13";
            if (cap.OpenGL14) versions += ":14";
            if (cap.OpenGL15) versions += ":15";
        } catch (Throwable throwable) {
            versions += ":lwjgl-Error-1";
        }

        try {
            if (cap.OpenGL20) versions += ":20";
            if (cap.OpenGL21) versions += ":21";
        } catch (Throwable throwable) {
            versions += ":lwjgl-Error-2";
        }

        try {
            if (cap.OpenGL30) versions += ":30";
            if (cap.OpenGL31) versions += ":31";
            if (cap.OpenGL32) versions += ":32";
            if (cap.OpenGL33) versions += ":33";
        } catch (Throwable throwable) {
            versions += ":lwjgl-Error-3";
        }

        // try
        // {
        // if (cap.OpenGL40)
        // {
        // versions += ":40";
        // }
        // if (cap.OpenGL41)
        // {
        // versions += ":41";
        // }
        // if (cap.OpenGL42)
        // {
        // versions += ":42";
        // }
        // if (cap.OpenGL43)
        // {
        // versions += ":43";
        // }
        // if (cap.OpenGL44)
        // {
        // versions += ":44";
        // }
        // }
        // catch (Throwable throwable)
        // {
        // versions += ":lwjgl-Error-4";
        // }

        if (versions.length() > 0) versions = versions.substring(1);

        return versions;
    }
}
