package ivorius.psychedelicraft.nei;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.blocks.PSBlocks;
import net.minecraft.item.ItemStack;

/** Loaded only by NEI's client-side configuration discovery. */
public class NEIPsychedelicraftConfig implements IConfigureNEI
{
    @Override
    public void loadConfig()
    {
        DryingRecipeHandler drying = new DryingRecipeHandler();
        AcquisitionRecipeHandler acquisition = new AcquisitionRecipeHandler();
        DrinkPreparationRecipeHandler drinks = new DrinkPreparationRecipeHandler();

        API.registerRecipeHandler(drying);
        API.registerUsageHandler(drying);
        API.registerRecipeHandler(acquisition);
        API.registerUsageHandler(acquisition);
        API.registerRecipeHandler(drinks);
        API.registerUsageHandler(drinks);

        API.addRecipeCatalyst(new ItemStack(PSBlocks.dryingTable), DryingRecipeHandler.ID);
        API.addRecipeCatalyst(new ItemStack(PSBlocks.dryingTableIron), DryingRecipeHandler.ID);
    }

    @Override
    public String getName()
    {
        return "Psychedelicraft NEI";
    }

    @Override
    public String getVersion()
    {
        return Psychedelicraft.VERSION;
    }
}
