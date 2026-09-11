package ivorius.psychedelicraft.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import ivorius.psychedelicraft.crafting.RecipeFillDrink;
import ivorius.psychedelicraft.items.PSItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrinkPreparationRecipeHandler extends TemplateRecipeHandler
{
    public static final String ID = "psychedelicraft.drinkPreparation";
    private List<CachedDrink> index;

    @Override
    public String getRecipeName()
    {
        return StatCollector.translateToLocal("nei.psychedelicraft.drinkPreparation");
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
        for (CachedDrink recipe : index())
            if (recipe.output.contains(result))
                arecipes.add(recipe);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        for (CachedDrink recipe : index())
        {
            for (PositionedStack input : recipe.inputs)
            {
                if (input.contains(ingredient))
                {
                    arecipes.add(recipe);
                    break;
                }
            }
        }
    }

    private List<CachedDrink> index()
    {
        if (index == null)
        {
            List<CachedDrink> recipes = new ArrayList<>();
            for (Object candidate : CraftingManager.getInstance().getRecipeList())
            {
                if (!(candidate instanceof RecipeFillDrink))
                    continue;

                RecipeFillDrink recipe = (RecipeFillDrink) candidate;
                FluidStack fluid = recipe.getFluidOutput();
                for (ItemStack holder : emptyHolders())
                {
                    IFluidContainerItem container = (IFluidContainerItem) holder.getItem();
                    if (container.fill(holder, fluid.copy(), false) < fluid.amount)
                        continue;

                    ItemStack result = holder.copy();
                    container.fill(result, fluid.copy(), true);
                    recipes.add(new CachedDrink(recipe.recipeItems, holder, result));
                }
            }
            index = Collections.unmodifiableList(recipes);
        }
        return index;
    }

    private static List<ItemStack> emptyHolders()
    {
        List<ItemStack> holders = new ArrayList<>();
        holders.add(new ItemStack(PSItems.woodenMug));
        holders.add(new ItemStack(PSItems.stoneCup));
        holders.add(new ItemStack(PSItems.glassChalice));
        holders.add(new ItemStack(PSItems.bottle));
        holders.add(new ItemStack(PSItems.syringe));
        return holders;
    }

    private class CachedDrink extends CachedRecipe
    {
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final PositionedStack output;

        private CachedDrink(List<Object> ingredients, ItemStack holder, ItemStack result)
        {
            inputs.add(new PositionedStack(holder.copy(), 25, 24));
            for (int i = 0; i < ingredients.size(); i++)
            {
                Object ingredient = ingredients.get(i);
                Object display = ingredient;
                if (ingredient instanceof ItemStack)
                    display = ((ItemStack) ingredient).copy();
                else if (ingredient instanceof Item)
                    display = new ItemStack((Item) ingredient);
                else if (ingredient instanceof List)
                {
                    List<?> alternatives = (List<?>) ingredient;
                    display = alternatives.toArray(new Object[alternatives.size()]);
                }
                inputs.add(new PositionedStack(display, 49 + (i % 4) * 18, 15 + (i / 4) * 18));
            }
            output = new PositionedStack(result.copy(), 132, 24);
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
