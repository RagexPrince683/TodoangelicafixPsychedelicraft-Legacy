/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraftcore;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Created by lukas on 21.02.14.
 */
@IFMLLoadingPlugin.Name(PsychedelicraftLoadingPlugin.NAME)
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"ivorius.psychedelicraftcore.", "ivorius.psychedelicraft.internal.asm."})
@IFMLLoadingPlugin.SortingIndex(2050)
public class PsychedelicraftLoadingPlugin implements IFMLLoadingPlugin
{
    public static final String NAME = "Psychedelicraft Core";
    public static final String MODID = "psychedelicraftcore";

    public static boolean debugGlErrorTraceDumps = false;

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{PsychedelicraftClassTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass()
    {
        return "ivorius.psychedelicraftcore.PsychedelicraftCoreContainer";
    }

    @Override
    public String getSetupClass()
    {
        return "ivorius.psychedelicraftcore.PsychedelicraftCoreSetup";
    }

    @Override
    public void injectData(Map<String, Object> data)
    {

    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
