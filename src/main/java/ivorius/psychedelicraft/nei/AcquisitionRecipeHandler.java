package ivorius.psychedelicraft.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import ivorius.psychedelicraft.blocks.PSBlocks;
import ivorius.psychedelicraft.items.PSItems;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small, bounded index of non-recipe acquisition rules. Ordinary crafting and
 * furnace recipes deliberately remain with NEI's built-in handlers.
 */
public class AcquisitionRecipeHandler extends TemplateRecipeHandler
{
    public static final String ID = "psychedelicraft.acquisition";
    private List<Acquisition> index;

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal("nei.psychedelicraft.acquisition");
    }

    @Override
    public String getGuiTexture()
    {
        return "textures/gui/container/crafting_table.png";
    }

    @Override
    public String getOverlayIdentifier()
    {
        return ID;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (ID.equals(outputId))
            arecipes.addAll(index());
        else
            super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result)
    {
        for (Acquisition acquisition : index())
            if (acquisition.output.contains(result))
                arecipes.add(acquisition);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        for (Acquisition acquisition : index())
        {
            for (PositionedStack input : acquisition.inputs)
            {
                if (input.contains(ingredient))
                {
                    arecipes.add(acquisition);
                    break;
                }
            }
        }
    }

    @Override
    public void drawExtras(int recipe)
    {
        Acquisition acquisition = (Acquisition) arecipes.get(recipe);
        Minecraft.getMinecraft().fontRenderer.drawSplitString(
                StatCollector.translateToLocal(acquisition.textKey), 4, 65, 162, 0x404040);
    }

    private List<Acquisition> index()
    {
        if (index == null)
        {
            List<Acquisition> entries = new ArrayList<>();
            addPlant(entries, "cannabis", new ItemStack(PSItems.cannabisSeeds), new ItemStack(PSBlocks.cannabisPlant, 1, 11),
                    new ItemStack(PSItems.cannabisBuds), new ItemStack(PSItems.cannabisLeaf, 3));
            addPlant(entries, "tobacco", new ItemStack(PSItems.tobaccoSeeds), new ItemStack(PSBlocks.tobaccoPlant, 1, 15),
                    new ItemStack(PSItems.tobaccoLeaf, 8));
            addPlant(entries, "coca", new ItemStack(PSItems.cocaSeeds), new ItemStack(PSBlocks.cocaPlant, 1, 11),
                    new ItemStack(PSItems.cocaLeaf, 6));
            addPlant(entries, "hops", new ItemStack(PSItems.hopSeeds), new ItemStack(PSBlocks.hopPlant, 1, 11),
                    new ItemStack(PSItems.hopCones, 2));
            addPlant(entries, "coffee", new ItemStack(PSItems.coffeaCherries), new ItemStack(PSBlocks.coffea, 1, 14),
                    new ItemStack(PSItems.coffeaCherries, 6));

            entries.add(new Acquisition("nei.psychedelicraft.acquire.grapes", new ItemStack(PSItems.wineGrapes, 3),
                    new ItemStack(PSBlocks.wineGrapeLattice, 1, 8)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.peyote", new ItemStack(PSBlocks.peyote),
                    new ItemStack(Blocks.sand), new ItemStack(Items.emerald)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.juniper", new ItemStack(PSItems.juniperBerries),
                    new ItemStack(PSBlocks.psycheLeaves, 1, 1), new ItemStack(PSBlocks.psycheSapling)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.mushrooms", new ItemStack(PSItems.magicMushroomsBrown, 3),
                    new ItemStack(Blocks.brown_mushroom)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.mushrooms", new ItemStack(PSItems.magicMushroomsRed, 3),
                    new ItemStack(Blocks.red_mushroom)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.trades", new ItemStack(PSItems.driedCannabisBuds),
                    new ItemStack(Items.emerald)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.loot", new ItemStack(PSItems.driedTobacco),
                    new ItemStack(Blocks.chest)));
            entries.add(new Acquisition("nei.psychedelicraft.acquire.unavailable", new ItemStack(PSItems.harmonium),
                    new ItemStack(Items.dye)));
            index = Collections.unmodifiableList(entries);
        }
        return index;
    }

    private void addPlant(List<Acquisition> entries, String name, ItemStack seed, ItemStack maturePlant,
                          ItemStack... harvest)
    {
        entries.add(new Acquisition("nei.psychedelicraft.acquire." + name + ".seed", seed,
                maturePlant, new ItemStack(Items.emerald)));
        for (ItemStack output : harvest)
            entries.add(new Acquisition("nei.psychedelicraft.acquire." + name + ".harvest", output, seed, maturePlant));
    }

    private class Acquisition extends CachedRecipe
    {
        private final String textKey;
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final PositionedStack output;

        private Acquisition(String textKey, ItemStack output, ItemStack... inputs)
        {
            this.textKey = textKey;
            this.output = new PositionedStack(output.copy(), 119, 28);
            for (int i = 0; i < inputs.length; i++)
                this.inputs.add(new PositionedStack(inputs[i].copy(), 20 + i * 24, 28));
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return inputs;
        }

        @Override
        public PositionedStack getResult()
        {
            return output;
        }
    }
}
