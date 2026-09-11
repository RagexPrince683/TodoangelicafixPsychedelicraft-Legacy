package ivorius.psychedelicraft.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.crafting.DryingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DryingRecipeHandler extends TemplateRecipeHandler
{
    public static final String ID = "psychedelicraft.drying";
    private static final int[] SLOT_X = {37, 55, 73, 37, 55, 73, 37, 55, 73};
    private static final int[] SLOT_Y = {12, 12, 12, 30, 30, 30, 48, 48, 48};
    private List<CachedDryingRecipe> recipeIndex;

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal("nei.psychedelicraft.drying");
    }

    @Override
    public String getGuiTexture()
    {
        return "psychedelicraft:textures/mod/guiDryingTable.png";
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
        for (CachedDryingRecipe recipe : index())
            if (recipe.output.contains(result))
                arecipes.add(recipe);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        for (CachedDryingRecipe recipe : index())
            if (recipe.input.contains(ingredient))
                arecipes.add(recipe);
    }

    @Override
    public void drawExtras(int recipe)
    {
        int secondsWood = Math.max(0, PSConfig.dryingTableTickDuration / 20);
        int secondsIron = Math.max(0, PSConfig.ironDryingTableTickDuration / 20);
        Minecraft.getMinecraft().fontRenderer.drawString(
                StatCollector.translateToLocalFormatted("nei.psychedelicraft.drying.time", secondsWood, secondsIron), 4, 70, 0x404040);
        Minecraft.getMinecraft().fontRenderer.drawSplitString(
                StatCollector.translateToLocal("nei.psychedelicraft.drying.conditions"), 4, 82, 162, 0x404040);
    }

    private List<CachedDryingRecipe> index()
    {
        if (recipeIndex == null)
        {
            List<CachedDryingRecipe> recipes = new ArrayList<>();
            for (Map.Entry<Object, ItemStack> entry : DryingRegistry.dryingRecipes().entrySet())
            {
                List<ItemStack> alternatives = alternatives(entry.getKey());
                if (!alternatives.isEmpty())
                    recipes.add(new CachedDryingRecipe(alternatives, entry.getValue()));
            }
            recipeIndex = Collections.unmodifiableList(recipes);
        }
        return recipeIndex;
    }

    private static List<ItemStack> alternatives(Object source)
    {
        List<ItemStack> stacks = new ArrayList<>();
        if (source instanceof String)
        {
            for (ItemStack stack : OreDictionary.getOres((String) source))
                stacks.add(stack.copy());
        }
        else if (source instanceof ItemStack)
            stacks.add(((ItemStack) source).copy());
        else if (source instanceof Item)
            stacks.add(new ItemStack((Item) source));
        else if (source instanceof Block)
            stacks.add(new ItemStack((Block) source));
        return stacks;
    }

    private class CachedDryingRecipe extends CachedRecipe
    {
        private final PositionedStack input;
        private final PositionedStack output;

        private CachedDryingRecipe(List<ItemStack> alternatives, ItemStack result)
        {
            Object[] ingredients = new Object[alternatives.size()];
            for (int i = 0; i < alternatives.size(); i++)
            {
                ingredients[i] = alternatives.get(i).copy();
                ((ItemStack) ingredients[i]).stackSize = 1;
            }
            input = new PositionedStack(ingredients, SLOT_X[0], SLOT_Y[0]);
            output = new PositionedStack(result.copy(), 119, 30);
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            List<PositionedStack> occupiedSlots = new ArrayList<>();
            for (int i = 0; i < SLOT_X.length; i++)
            {
                PositionedStack slot = new PositionedStack(input.items, SLOT_X[i], SLOT_Y[i]);
                occupiedSlots.add(slot);
            }
            return occupiedSlots;
        }

        @Override
        public PositionedStack getResult()
        {
            return output;
        }
    }
}
