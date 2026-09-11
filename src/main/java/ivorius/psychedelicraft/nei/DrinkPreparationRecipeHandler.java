package ivorius.psychedelicraft.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.crafting.RecipeFillDrink;
import ivorius.psychedelicraft.items.PSItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrinkPreparationRecipeHandler extends TemplateRecipeHandler
{
    public static final String ID = "psychedelicraft.drinkPreparation";
    private static volatile List<DrinkDescriptor> sharedSnapshot;

    private List<CachedDrink> recipeIndex;

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
    public void drawBackground(int recipe)
    {
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 65);
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
        if (!validStack(result))
            return;

        for (CachedDrink recipe : index())
            if (sameFilledDrink(recipe.resultStack, result))
                arecipes.add(recipe);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        if (!validStack(ingredient))
            return;

        for (CachedDrink recipe : index())
        {
            for (PositionedStack input : recipe.inputs)
            {
                if (input.containsWithNBT(ingredient))
                {
                    arecipes.add(recipe);
                    break;
                }
            }
        }
    }

    private List<CachedDrink> index()
    {
        if (recipeIndex == null)
        {
            List<CachedDrink> recipes = new ArrayList<>();
            for (DrinkDescriptor descriptor : snapshot())
                recipes.add(new CachedDrink(descriptor));
            recipeIndex = Collections.unmodifiableList(recipes);
        }
        return recipeIndex;
    }

    private static List<DrinkDescriptor> snapshot()
    {
        List<DrinkDescriptor> snapshot = sharedSnapshot;
        if (snapshot == null)
        {
            synchronized (DrinkPreparationRecipeHandler.class)
            {
                snapshot = sharedSnapshot;
                if (snapshot == null)
                {
                    snapshot = buildSnapshot();
                    sharedSnapshot = snapshot;
                }
            }
        }
        return snapshot;
    }

    private static List<DrinkDescriptor> buildSnapshot()
    {
        List<DrinkDescriptor> recipes = new ArrayList<>();
        int invalidRecipes = 0;
        for (Object candidate : CraftingManager.getInstance().getRecipeList())
        {
            if (!(candidate instanceof RecipeFillDrink))
                continue;

            RecipeFillDrink recipe = (RecipeFillDrink) candidate;
            FluidStack fluid = recipe.getFluidOutput();
            List<List<ItemStack>> ingredients = normalizeIngredients(recipe.recipeItems);
            if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0 || ingredients == null)
            {
                invalidRecipes++;
                continue;
            }

            for (ItemStack holder : emptyHolders())
            {
                IFluidContainerItem container = (IFluidContainerItem) holder.getItem();
                if (container.fill(holder, fluid.copy(), false) < fluid.amount)
                    continue;

                ItemStack result = holder.copy();
                container.fill(result, fluid.copy(), true);
                if (validStack(result))
                    recipes.add(new DrinkDescriptor(ingredients, holder, result));
            }
        }

        if (invalidRecipes > 0 && Psychedelicraft.logger != null)
            Psychedelicraft.logger.warn("NEI omitted {} invalid drink preparation recipe entries", invalidRecipes);
        return Collections.unmodifiableList(recipes);
    }

    private static List<List<ItemStack>> normalizeIngredients(List<Object> source)
    {
        if (source == null || source.size() > 8)
            return null;

        List<List<ItemStack>> ingredients = new ArrayList<>();
        for (Object ingredient : source)
        {
            List<ItemStack> alternatives = normalizeIngredient(ingredient);
            if (alternatives.isEmpty())
                return null;
            ingredients.add(alternatives);
        }
        return Collections.unmodifiableList(ingredients);
    }

    private static List<ItemStack> normalizeIngredient(Object ingredient)
    {
        List<ItemStack> alternatives = new ArrayList<>();
        if (ingredient instanceof ItemStack)
            addValidCopy(alternatives, (ItemStack) ingredient);
        else if (ingredient instanceof Item)
            addValidCopy(alternatives, new ItemStack((Item) ingredient));
        else if (ingredient instanceof Block && Item.getItemFromBlock((Block) ingredient) != null)
            addValidCopy(alternatives, new ItemStack((Block) ingredient));
        else if (ingredient instanceof List)
        {
            for (Object alternative : (List<?>) ingredient)
                if (alternative instanceof ItemStack)
                    addValidCopy(alternatives, (ItemStack) alternative);
        }
        return Collections.unmodifiableList(alternatives);
    }

    private static void addValidCopy(List<ItemStack> alternatives, ItemStack stack)
    {
        if (validStack(stack))
        {
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            alternatives.add(copy);
        }
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

    private static boolean sameFilledDrink(ItemStack expected, ItemStack requested)
    {
        if (!validStack(expected) || !validStack(requested) || expected.getItem() != requested.getItem())
            return false;
        if (!(expected.getItem() instanceof IFluidContainerItem))
            return false;

        IFluidContainerItem container = (IFluidContainerItem) expected.getItem();
        FluidStack expectedFluid = container.drain(expected.copy(), Integer.MAX_VALUE, false);
        FluidStack requestedFluid = container.drain(requested.copy(), Integer.MAX_VALUE, false);
        return expectedFluid != null && requestedFluid != null && expectedFluid.isFluidEqual(requestedFluid);
    }

    private static boolean validStack(ItemStack stack)
    {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    private static final class DrinkDescriptor
    {
        private final List<List<ItemStack>> ingredients;
        private final ItemStack holder;
        private final ItemStack result;

        private DrinkDescriptor(List<List<ItemStack>> ingredients, ItemStack holder, ItemStack result)
        {
            this.ingredients = ingredients;
            this.holder = holder.copy();
            this.result = result.copy();
        }
    }

    private class CachedDrink extends CachedRecipe
    {
        private final List<PositionedStack> inputs;
        private final ItemStack resultStack;
        private final PositionedStack output;

        private CachedDrink(DrinkDescriptor descriptor)
        {
            List<PositionedStack> preparedInputs = new ArrayList<>();
            preparedInputs.add(new PositionedStack(descriptor.holder.copy(), 25, 6));
            for (int i = 0; i < descriptor.ingredients.size(); i++)
            {
                List<ItemStack> alternatives = descriptor.ingredients.get(i);
                List<ItemStack> displayAlternatives = new ArrayList<>(alternatives.size());
                for (ItemStack alternative : alternatives)
                    displayAlternatives.add(alternative.copy());

                int slot = i + 1;
                preparedInputs.add(new PositionedStack(displayAlternatives,
                        25 + (slot % 3) * 18, 6 + (slot / 3) * 18));
            }
            inputs = Collections.unmodifiableList(preparedInputs);
            resultStack = descriptor.result.copy();
            output = new PositionedStack(resultStack.copy(), 119, 24);
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
