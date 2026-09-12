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

package ivorius.psychedelicraft.internal.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Created by lukas on 21.02.14.
 */
public class IvClassTransformerManager implements IClassTransformer {

    public Hashtable<String, IvClassTransformer> transformers;
    public ArrayList<IvClassTransformer> generalTransformers;
    public Hashtable<String, ArrayList<IvClassTransformer>> scopedGeneralTransformers;

    private static final Set<String> APPROVED_CLASSES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "net.minecraft.client.renderer.EntityRenderer",
        "net.minecraft.client.renderer.RenderGlobal",
        "net.minecraft.client.renderer.OpenGlHelper",
        "net.minecraft.client.renderer.RenderHelper",
        "net.minecraft.client.audio.SoundManager"
    )));

    public IvClassTransformerManager() {
        transformers = new Hashtable<String, IvClassTransformer>();
        generalTransformers = new ArrayList<IvClassTransformer>();
        scopedGeneralTransformers = new Hashtable<String, ArrayList<IvClassTransformer>>();

        IvDevRemapper.setUp();
    }

    public void registerTransformer(String clazz, IvClassTransformer transformer) {
        requireApprovedClass(clazz);
        transformers.put(clazz, transformer);
    }

    public void registerTransformer(IvClassTransformer transformer) {
        generalTransformers.add(transformer);
    }

    public void registerGeneralTransformer(String clazz, IvClassTransformer transformer) {
        requireApprovedClass(clazz);

        ArrayList<IvClassTransformer> classTransformers = scopedGeneralTransformers.get(clazz);
        if (classTransformers == null) {
            classTransformers = new ArrayList<IvClassTransformer>();
            scopedGeneralTransformers.put(clazz, classTransformers);
        }
        classTransformers.add(transformer);
    }

    public static boolean isApprovedClass(String transformedName) {
        return transformedName != null && APPROVED_CLASSES.contains(transformedName);
    }

    private static void requireApprovedClass(String clazz) {
        if (!isApprovedClass(clazz)) {
            throw new IllegalArgumentException("Transformer target is not explicitly approved: " + clazz);
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        // This must remain before every parser, matcher, hierarchy query, and writer.
        if (!isApprovedClass(transformedName)) {
            return basicClass;
        }

        byte[] result = basicClass;

        IvClassTransformer transformer = transformers.get(transformedName);
        if (transformer != null) {
            byte[] data = transformer.transform(name, transformedName, result, transformedName.equals(name));

            if (data != null) {
                result = data;
            }
        }

        ArrayList<IvClassTransformer> scopedTransformers = scopedGeneralTransformers.get(transformedName);
        if (scopedTransformers != null) {
            for (IvClassTransformer generalTransformer : scopedTransformers) {
                byte[] data = generalTransformer.transform(name, transformedName, result, transformedName.equals(name));

                if (data != null) {
                    result = data;
                }
            }
        }

        for (IvClassTransformer generalTransformer : generalTransformers) {
            byte[] data = generalTransformer.transform(name, transformedName, result, transformedName.equals(name));

            if (data != null) {
                result = data;
            }
        }

        return result;
    }
}
