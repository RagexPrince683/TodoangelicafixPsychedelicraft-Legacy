package ivorius.psychedelicraft.nei;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.crafting.DryingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DryingRecipeHandler extends TemplateRecipeHandler
{
    public static final String ID = "psychedelicraft.drying";
    private static final int[] SLOT_X = {25, 43, 61, 25, 43, 61, 25, 43, 61};
    private static final int[] SLOT_Y = {6, 6, 6, 24, 24, 24, 42, 42, 42};
    private static volatile List<DryingDescriptor> sharedSnapshot;

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
    public int recipiesPerPage()
    {
        return 1;
    }

    @Override
    public int getRecipeHeight()
    {
        return 118;
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(new RecipeTransferRect(new Rectangle(83, 23, 25, 16), ID));
    }

    @Override
    public void drawBackground(int recipe)
    {
        GuiDraw.drawRect(0, 0, 166, 118, 0xffeeeeee);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 54);
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

        for (CachedDryingRecipe recipe : index())
            if (recipe.output.containsWithNBT(result))
                arecipes.add(recipe);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        if (!validStack(ingredient))
            return;

        for (CachedDryingRecipe recipe : index())
            if (recipe.input.containsWithNBT(ingredient))
                arecipes.add(recipe);
    }

    @Override
    public void drawExtras(int recipe)
    {
        int secondsWood = Math.max(0, PSConfig.dryingTableTickDuration / 20);
        int secondsIron = Math.max(0, PSConfig.ironDryingTableTickDuration / 20);
        Minecraft.getMinecraft().fontRenderer.drawString(
                StatCollector.translateToLocalFormatted("nei.psychedelicraft.drying.time", secondsWood, secondsIron),
                4, 66, 0x404040);
        Minecraft.getMinecraft().fontRenderer.drawSplitString(
                StatCollector.translateToLocal("nei.psychedelicraft.drying.conditions"), 4, 78, 162, 0x404040);
    }

    private List<CachedDryingRecipe> index()
    {
        if (recipeIndex == null)
        {
            List<CachedDryingRecipe> recipes = new ArrayList<>();
            for (DryingDescriptor descriptor : snapshot())
                recipes.add(new CachedDryingRecipe(descriptor));
            recipeIndex = Collections.unmodifiableList(recipes);
        }
        return recipeIndex;
    }

    private static List<DryingDescriptor> snapshot()
    {
        List<DryingDescriptor> snapshot = sharedSnapshot;
        if (snapshot == null)
        {
            synchronized (DryingRecipeHandler.class)
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

    private static List<DryingDescriptor> buildSnapshot()
    {
        List<DryingDescriptor> recipes = new ArrayList<>();
        int invalidRecipes = 0;
        for (Map.Entry<Object, ItemStack> entry : DryingRegistry.dryingRecipes().entrySet())
        {
            List<ItemStack> alternatives = alternatives(entry.getKey());
            ItemStack result = copyValid(entry.getValue());
            if (alternatives.isEmpty() || result == null)
            {
                invalidRecipes++;
                continue;
            }
            recipes.add(new DryingDescriptor(alternatives, result));
        }

        if (invalidRecipes > 0 && Psychedelicraft.logger != null)
            Psychedelicraft.logger.warn("NEI omitted {} invalid drying recipe entries", invalidRecipes);
        return Collections.unmodifiableList(recipes);
    }

    private static List<ItemStack> alternatives(Object source)
    {
        List<ItemStack> stacks = new ArrayList<>();
        if (source instanceof String)
        {
            for (ItemStack stack : OreDictionary.getOres((String) source))
                addValidCopy(stacks, stack);
        }
        else if (source instanceof ItemStack)
            addValidCopy(stacks, (ItemStack) source);
        else if (source instanceof Item && source != null)
            addValidCopy(stacks, new ItemStack((Item) source));
        else if (source instanceof Block && Item.getItemFromBlock((Block) source) != null)
            addValidCopy(stacks, new ItemStack((Block) source));
        return Collections.unmodifiableList(stacks);
    }

    private static void addValidCopy(List<ItemStack> stacks, ItemStack stack)
    {
        ItemStack copy = copyValid(stack);
        if (copy != null)
        {
            copy.stackSize = 1;
            stacks.add(copy);
        }
    }

    private static ItemStack copyValid(ItemStack stack)
    {
        return validStack(stack) ? stack.copy() : null;
    }

    private static boolean validStack(ItemStack stack)
    {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    private static final class DryingDescriptor
    {
        private final List<ItemStack> alternatives;
        private final ItemStack output;

        private DryingDescriptor(List<ItemStack> alternatives, ItemStack output)
        {
            this.alternatives = alternatives;
            this.output = output.copy();
        }
    }

    private class CachedDryingRecipe extends CachedRecipe
    {
        private final PositionedStack input;
        private final List<PositionedStack> occupiedSlots;
        private final PositionedStack output;

        private CachedDryingRecipe(DryingDescriptor descriptor)
        {
            ItemStack[] ingredients = new ItemStack[descriptor.alternatives.size()];
            for (int i = 0; i < descriptor.alternatives.size(); i++)
            {
                ingredients[i] = descriptor.alternatives.get(i).copy();
                ingredients[i].stackSize = 1;
            }

            input = new PositionedStack(ingredients, SLOT_X[0], SLOT_Y[0]);
            List<PositionedStack> slots = new ArrayList<>(SLOT_X.length);
            for (int i = 0; i < SLOT_X.length; i++)
                slots.add(new PositionedStack(ingredients, SLOT_X[i], SLOT_Y[i]));
            occupiedSlots = Collections.unmodifiableList(slots);
            output = new PositionedStack(descriptor.output.copy(), 119, 24);
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return occupiedSlots;
        }

        @Override
        public PositionedStack getResult()
        {
            return output;
        }
    }
}
