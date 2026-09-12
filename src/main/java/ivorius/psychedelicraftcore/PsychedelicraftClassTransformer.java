/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraftcore;

import ivorius.psychedelicraft.internal.asm.IvClassTransformerManager;
import ivorius.psychedelicraft.internal.asm.IvClassTransformerClass;
import ivorius.psychedelicraftcore.transformers.*;
import org.apache.logging.log4j.Logger;

import java.security.CodeSource;

/**
 * Created by lukas on 21.02.14.
 */
public class PsychedelicraftClassTransformer extends IvClassTransformerManager
{
    public PsychedelicraftClassTransformer()
    {
        PsycheDevRemapper.setUp();
        Logger logger = PsychedelicraftCoreContainer.logger;

        logTransformerIdentity(logger);

        registerTransformer("net.minecraft.client.renderer.EntityRenderer", new EntityRendererTransformer(logger));
        registerTransformer("net.minecraft.client.renderer.RenderGlobal", new RenderGlobalTransformer(logger));
        registerTransformer("net.minecraft.client.renderer.OpenGlHelper", new OpenGLHelperTransformer(logger));
        registerTransformer("net.minecraft.client.renderer.RenderHelper", new RenderHelperTransformer(logger));
        registerTransformer("net.minecraft.client.audio.SoundManager", new SoundManagerTransformer(logger));

        registerTransformer(new OpenGLTransfomer(logger));
    }

    private static void logTransformerIdentity(Logger logger)
    {
        Class<?> transformerBase = IvClassTransformerClass.class;
        CodeSource codeSource = transformerBase.getProtectionDomain().getCodeSource();
        Package transformerPackage = transformerBase.getPackage();
        String sourceLocation = codeSource == null ? "<unavailable>" : codeSource.getLocation().toExternalForm();
        String buildVersion = transformerPackage == null ? null : transformerPackage.getImplementationVersion();

        logger.info("Psychedelicraft transformer identity: class=" + transformerBase.getName()
            + ", codeSource=" + sourceLocation
            + ", implementationVersion=" + (buildVersion == null ? "<unavailable>" : buildVersion));
    }
}
