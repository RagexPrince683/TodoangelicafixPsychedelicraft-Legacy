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

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Created by lukas on 12.03.14.
 */
public abstract class IvClassTransformerClass extends IvClassTransformer {

    public ArrayList<String[]> registeredMethods;

    public IvClassTransformerClass(Logger logger) {
        super(logger);
        registeredMethods = new ArrayList<String[]>();
    }

    public void registerExpectedMethod(String methodID, String obfName, String signature) {
        registeredMethods.add(new String[] { obfName, signature, methodID });
    }

    @Override
    public boolean transform(String className, ClassNode classNode, boolean obf) {
        boolean[] matchedMethods = new boolean[registeredMethods.size()];
        boolean[] transformedMethods = new boolean[registeredMethods.size()];
        MethodNode[] matchedMethodNodes = new MethodNode[registeredMethods.size()];

        for (MethodNode m : classNode.methods) {
            for (int methodIndex = 0; methodIndex < registeredMethods.size(); methodIndex++) {
                String[] methodInfo = registeredMethods.get(methodIndex);
                String srgName = getSrgName(className, m);
                String srgSignature = getSRGDescriptor(m.desc);

                if ((srgName.equals(methodInfo[0]) && srgSignature.equals(methodInfo[1]))) {
                    matchedMethods[methodIndex] = true;
                    matchedMethodNodes[methodIndex] = m;

                    if (transformMethod(className, methodInfo[2], m, obf)) {
                        transformedMethods[methodIndex] = true;
                    }
                }
            }
        }

        boolean didChange = false;

        for (int methodIndex = 0; methodIndex < registeredMethods.size(); methodIndex++) {
            if (!transformedMethods[methodIndex]) {
                String[] methodInfo = registeredMethods.get(methodIndex);

                if (!matchedMethods[methodIndex]) {
                    throw new IllegalStateException(
                        "Required transformation failed: target method was not found: class=" + className
                            + ", expectedMethod=" + methodInfo[0]
                            + methodInfo[1]
                            + ", hook=" + methodInfo[2]
                            + ", obfuscated=" + obf
                            + ", candidates=" + describeCandidateMethods(className, classNode, methodInfo));
                }

                MethodNode matchedMethod = matchedMethodNodes[methodIndex];
                throw new IllegalStateException(
                    "Required transformation failed: hook failed after matching target: class=" + className
                        + ", actualMethod=" + describeMethod(className, matchedMethod)
                        + ", expectedMethod=" + methodInfo[0] + methodInfo[1]
                        + ", hook=" + methodInfo[2]
                        + ", obfuscated=" + obf);
            } else {
                didChange = true;
            }
        }

        return didChange;
    }

    private static String describeCandidateMethods(String className, ClassNode classNode, String[] methodInfo) {
        StringBuilder candidates = new StringBuilder();

        for (MethodNode method : classNode.methods) {
            String normalizedName = getSrgName(className, method);
            String normalizedDescriptor = getSRGDescriptor(method.desc);
            if (methodInfo[0].equals(method.name)
                || methodInfo[0].equals(normalizedName)
                || methodInfo[1].equals(method.desc)
                || methodInfo[1].equals(normalizedDescriptor)) {
                if (candidates.length() > 0) {
                    candidates.append("; ");
                }
                candidates.append(describeMethod(className, method));
            }
        }

        return candidates.length() == 0 ? "<none with the expected name or descriptor>" : candidates.toString();
    }

    private static String describeMethod(String className, MethodNode method) {
        return method.name + method.desc
            + " (normalizedSrgName=" + getSrgName(className, method)
            + ", normalizedDescriptor=" + getSRGDescriptor(method.desc) + ")";
    }

    public abstract boolean transformMethod(String className, String methodID, MethodNode methodNode, boolean obf);
}
