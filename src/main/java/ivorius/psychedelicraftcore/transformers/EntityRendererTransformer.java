/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraftcore.transformers;

import ivorius.psychedelicraft.internal.asm.*;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by lukas on 21.02.14.
 */
public class EntityRendererTransformer extends IvClassTransformerClass
{
    public EntityRendererTransformer(Logger logger)
    {
        super(logger);

        registerExpectedMethod("updateCameraAndRender", "func_78480_b", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE));
        registerExpectedMethod("orientCamera", "func_78467_g", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE));
        registerExpectedMethod("renderHand", "func_78476_b", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.INT_TYPE));
        registerExpectedMethod("enableLightmap", "func_78463_b", getMethodDescriptor(Type.VOID_TYPE, Double.TYPE));
        registerExpectedMethod("disableLightmap", "func_78483_a", getMethodDescriptor(Type.VOID_TYPE, Double.TYPE));
        registerExpectedMethod("renderWorld", "func_78471_a", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE));
        registerExpectedMethod("renderWorldAdditions", "func_78471_a", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE));
        registerExpectedMethod("setupFog", "func_78468_a", getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.FLOAT_TYPE));
        registerExpectedMethod("setupCameraTransform", "func_78479_a", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.INT_TYPE));
        registerExpectedMethod("preRenderSky", "func_78471_a", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE));
    }

    @Override
    public boolean transformMethod(String className, String methodID, MethodNode methodNode, boolean obf)
    {
        switch (methodID)
        {
            case "updateCameraAndRender":
                AbstractInsnNode preNode = IvNodeFinder.findNode(new IvNodeMatcherLDC("level"), methodNode);
                AbstractInsnNode postNode = IvNodeFinder.findNode(new IvNodeMatcherFieldSRG(GETSTATIC, "field_148824_g" /* shadersSupported */, "net/minecraft/client/renderer/OpenGlHelper", Type.BOOLEAN_TYPE), methodNode);

                if (preNode == null)
                {
                    printSubMethodError(className, methodID, "pre");
                }
                else
                {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(FLOAD, 1));
                    list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "preWorldRender", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE), false));
                    methodNode.instructions.insert(preNode, list);
                }

                if (postNode == null)
                {
                    printSubMethodError(className, methodID, "post");
                }
                else
                {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(FLOAD, 1));
                    list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "postWorldRender", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE), false));
                    methodNode.instructions.insertBefore(postNode, list);
                }

                return preNode != null && postNode != null;
            case "orientCamera":
            {
                InsnList list = new InsnList();
                list.add(new VarInsnNode(FLOAD, 1));
                list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "orientCamera", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE), false));
                methodNode.instructions.insert(methodNode.instructions.get(0), list);

                return true;
            }
            case "enableLightmap":
            {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "enableLightmap", getMethodDescriptor(Type.VOID_TYPE), false));
                methodNode.instructions.insert(methodNode.instructions.get(0), list);

                return true;
            }
            case "disableLightmap":
            {
                InsnList list = new InsnList();
                list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "enableLightmap", getMethodDescriptor(Type.VOID_TYPE), false));
                methodNode.instructions.insert(methodNode.instructions.get(0), list);

                return true;
            }
            case "renderHand":
                AbstractInsnNode transformMatrixNode = IvNodeFinder.findNode(new IvNodeMatcherMethod(INVOKESTATIC, "glPushMatrix", "org/lwjgl/opengl/GL11", null), methodNode);
                AbstractInsnNode skipOverlayNode = IvNodeFinder.findNode(new IvNodeMatcherMethodSRG(INVOKEVIRTUAL, "func_78447_b" /* renderOverlays */, "net/minecraft/client/renderer/ItemRenderer", Type.VOID_TYPE, Type.FLOAT_TYPE), methodNode);

                if (transformMatrixNode == null)
                {
                    printSubMethodError(className, methodID, "renderHeldItem");
                }
                else
                {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(FLOAD, 1));
                    list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "renderHeldItem", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE), false));
                    methodNode.instructions.insert(transformMatrixNode, list);
                }

                if (skipOverlayNode == null)
                {
                    printSubMethodError(className, methodID, "renderBlockOverlay");
                }
                else
                {
                    LabelNode skipRenderOverlayNode = new LabelNode();
                    methodNode.instructions.insert(skipOverlayNode, skipRenderOverlayNode);

                    AbstractInsnNode overlayArgument = previousExecutable(skipOverlayNode);
                    AbstractInsnNode overlayRenderer = previousExecutable(overlayArgument);
                    AbstractInsnNode overlayThis = previousExecutable(overlayRenderer);
                    if (overlayArgument == null || overlayArgument.getOpcode() != FLOAD
                        || ((VarInsnNode) overlayArgument).var != 1
                        || overlayRenderer == null || overlayRenderer.getOpcode() != GETFIELD
                        || !"net/minecraft/client/renderer/ItemRenderer".equals(
                            getSrgClassName(((FieldInsnNode) overlayRenderer).desc.substring(1, ((FieldInsnNode) overlayRenderer).desc.length() - 1)))
                        || overlayThis == null || overlayThis.getOpcode() != ALOAD
                        || ((VarInsnNode) overlayThis).var != 0)
                    {
                        printSubMethodError(className, methodID, "renderBlockOverlay operand stack");
                        return false;
                    }

                    InsnList preList = new InsnList();
                    preList.add(new VarInsnNode(FLOAD, 1));
                    preList.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "renderBlockOverlay", getMethodDescriptor(Type.BOOLEAN_TYPE, Type.FLOAT_TYPE), false));
                    preList.add(new JumpInsnNode(IFNE, skipRenderOverlayNode));
                    methodNode.instructions.insertBefore(overlayThis, preList);
                }

                return transformMatrixNode != null && skipOverlayNode != null;
            case "renderWorld":
                return transformRenderWorldHand(className, methodID, methodNode);
            case "setupFog":
                List<AbstractInsnNode> glFogiNodes = IvNodeFinder.findNodes(new IvNodeMatcherMethodSRG(INVOKESTATIC, "glFogi", "org/lwjgl/opengl/GL11", null), methodNode);

                for (AbstractInsnNode callListNode : glFogiNodes)
                {
                    InsnList listBefore = new InsnList();
                    listBefore.add(new InsnNode(DUP2));
                    listBefore.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "psycheGLFogi", getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE), false));
                    methodNode.instructions.insertBefore(callListNode, listBefore);
                }

                return true;
            case "setupCameraTransform":
            {
                LabelNode realMethodStartNode = new LabelNode();

                InsnList list = new InsnList();
                list.add(new VarInsnNode(FLOAD, 1));
                list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "setupCameraTransform", getMethodDescriptor(Type.BOOLEAN_TYPE, Type.FLOAT_TYPE), false));
                list.add(new JumpInsnNode(IFEQ, realMethodStartNode));
                list.add(new InsnNode(RETURN));
                list.add(realMethodStartNode);
                methodNode.instructions.insert(methodNode.instructions.get(0), list);

                return true;
            }
            case "renderWorldAdditions":
                List<AbstractInsnNode> valuepatchNodes = new ArrayList<>();
                valuepatchNodes.addAll(IvNodeFinder.findNodes(new IvNodeMatcherLDC("prepareterrain"), methodNode));
                valuepatchNodes.addAll(IvNodeFinder.findNodes(new IvNodeMatcherLDC("water"), methodNode));
                valuepatchNodes.addAll(IvNodeFinder.findNodes(new IvNodeMatcherLDC("entities"), methodNode));

                for (AbstractInsnNode node : valuepatchNodes)
                {
                    InsnList listBefore = new InsnList();
                    listBefore.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "fixGLState", getMethodDescriptor(Type.VOID_TYPE), false));
                    methodNode.instructions.insert(node, listBefore);
                }

                return valuepatchNodes.size() > 0;
            case "preRenderSky":
                AbstractInsnNode preRenderSkyNode = IvNodeFinder.findNode(new IvNodeMatcherMethodSRG(INVOKESTATIC, "func_78558_a", "net/minecraft/client/renderer/culling/ClippingHelperImpl", getMethodDescriptor("net/minecraft/client/renderer/culling/ClippingHelper")), methodNode);

                if (preRenderSkyNode != null)
                {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(FLOAD, 1));
                    list.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "preRenderSky", getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE), false));
                    methodNode.instructions.insert(preRenderSkyNode.getNext(), list);

                    return true;
                }
                break;
        }

        return false;
    }

    private static AbstractInsnNode previousExecutable(AbstractInsnNode node)
    {
        AbstractInsnNode previous = node == null ? null : node.getPrevious();
        while (previous != null && previous.getOpcode() < 0)
        {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private boolean transformRenderWorldHand(String className, String methodID, MethodNode methodNode)
    {
        String preDescriptor = getMethodDescriptor(Type.BOOLEAN_TYPE, Type.FLOAT_TYPE);
        String postDescriptor = getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE);
        boolean hasPreHook = hasHookCall(methodNode, "preRenderHand", preDescriptor);
        boolean hasPostHook = hasHookCall(methodNode, "postRenderHand", postDescriptor);

        if (hasPreHook || hasPostHook)
        {
            if (hasPreHook && hasPostHook)
            {
                return true;
            }

            logDisabledRenderHandFeature(className, methodID, methodNode, "duplicate-hook pairing");
            return true;
        }

        MethodInsnNode renderHandCall = findRenderHandCall(methodNode);
        if (renderHandCall == null)
        {
            logDisabledRenderHandFeature(className, methodID, methodNode, "renderHand call");
            return true;
        }

        AbstractInsnNode renderPass = previousExecutable(renderHandCall);
        AbstractInsnNode partialTicks = previousExecutable(renderPass);
        AbstractInsnNode renderer = previousExecutable(partialTicks);
        AbstractInsnNode depthClearCall = previousExecutable(renderer);
        AbstractInsnNode clearMask = previousExecutable(depthClearCall);

        if (!isRenderHandOperands(renderer, partialTicks, renderPass))
        {
            logDisabledRenderHandFeature(className, methodID, methodNode, "renderHand operand stack");
            return true;
        }
        if (!isSupportedDepthClear(depthClearCall))
        {
            logDisabledRenderHandFeature(className, methodID, methodNode, "hand depth-clear call");
            return true;
        }
        if (!isIntegerConstant(clearMask) || integerConstant(clearMask) != 0x100)
        {
            logDisabledRenderHandFeature(className, methodID, methodNode, "hand depth-clear mask");
            return true;
        }

        LabelNode skipDepthClear = new LabelNode();
        InsnList preList = new InsnList();
        preList.add(new VarInsnNode(FLOAD, 1));
        preList.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "preRenderHand", preDescriptor, false));
        preList.add(new JumpInsnNode(IFNE, skipDepthClear));

        InsnList postList = new InsnList();
        postList.add(new VarInsnNode(FLOAD, 1));
        postList.add(new MethodInsnNode(INVOKESTATIC, "ivorius/psychedelicraftcore/PsycheCoreBusClient", "postRenderHand", postDescriptor, false));

        methodNode.instructions.insertBefore(clearMask, preList);
        methodNode.instructions.insert(depthClearCall, skipDepthClear);
        methodNode.instructions.insert(renderHandCall, postList);
        return true;
    }

    private static MethodInsnNode findRenderHandCall(MethodNode methodNode)
    {
        for (AbstractInsnNode node = methodNode.instructions.getFirst(); node != null; node = node.getNext())
        {
            if (!(node instanceof MethodInsnNode)
                || (node.getOpcode() != INVOKESPECIAL && node.getOpcode() != INVOKEVIRTUAL))
            {
                continue;
            }

            MethodInsnNode method = (MethodInsnNode) node;
            if ("func_78476_b".equals(getSrgName(method))
                && "net/minecraft/client/renderer/EntityRenderer".equals(getSrgClassName(method.owner))
                && getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE, Type.INT_TYPE).equals(getSRGDescriptor(method.desc)))
            {
                return method;
            }
        }
        return null;
    }

    private static boolean isRenderHandOperands(AbstractInsnNode renderer, AbstractInsnNode partialTicks, AbstractInsnNode renderPass)
    {
        return renderer instanceof VarInsnNode
            && renderer.getOpcode() == ALOAD
            && ((VarInsnNode) renderer).var == 0
            && partialTicks instanceof VarInsnNode
            && partialTicks.getOpcode() == FLOAD
            && ((VarInsnNode) partialTicks).var == 1
            && renderPass instanceof VarInsnNode
            && renderPass.getOpcode() == ILOAD;
    }

    private static boolean isSupportedDepthClear(AbstractInsnNode node)
    {
        if (!(node instanceof MethodInsnNode) || node.getOpcode() != INVOKESTATIC)
        {
            return false;
        }

        MethodInsnNode method = (MethodInsnNode) node;
        if (!getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE).equals(method.desc) || !"glClear".equals(method.name))
        {
            return false;
        }

        return "org/lwjgl/opengl/GL11".equals(method.owner)
            || "com/gtnewhorizons/angelica/glsm/GLStateManager".equals(method.owner);
    }

    private static boolean hasHookCall(MethodNode methodNode, String name, String descriptor)
    {
        for (AbstractInsnNode node = methodNode.instructions.getFirst(); node != null; node = node.getNext())
        {
            if (node instanceof MethodInsnNode)
            {
                MethodInsnNode method = (MethodInsnNode) node;
                if (method.getOpcode() == INVOKESTATIC
                    && "ivorius/psychedelicraftcore/PsycheCoreBusClient".equals(method.owner)
                    && name.equals(method.name)
                    && descriptor.equals(method.desc))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void logDisabledRenderHandFeature(String className, String methodID, MethodNode methodNode, String stage)
    {
        logger.warn("Disabled hand depth effect: class=" + className
            + ", method=" + methodNode.name + methodNode.desc
            + ", hook=" + methodID
            + ", failedMatchStage=" + stage
            + ", unmatchedInstructionPattern=GL_DEPTH_BUFFER_BIT; INVOKESTATIC {GL11|Angelica GLStateManager}.glClear(I)V; ALOAD 0; FLOAD 1; ILOAD n; {INVOKESPECIAL|INVOKEVIRTUAL} EntityRenderer.renderHand(FI)V"
            + ". The renderer was left unchanged for this hook.");
    }

    private static boolean isIntegerConstant(AbstractInsnNode node)
    {
        if (node == null)
        {
            return false;
        }
        int opcode = node.getOpcode();
        if (opcode >= ICONST_M1 && opcode <= ICONST_5)
        {
            return true;
        }
        if (opcode == BIPUSH || opcode == SIPUSH)
        {
            return true;
        }
        return opcode == LDC && ((LdcInsnNode) node).cst instanceof Integer;
    }

    private static int integerConstant(AbstractInsnNode node)
    {
        if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5)
        {
            return node.getOpcode() - ICONST_0;
        }
        if (node instanceof IntInsnNode)
        {
            return ((IntInsnNode) node).operand;
        }
        return (Integer) ((LdcInsnNode) node).cst;
    }

}
