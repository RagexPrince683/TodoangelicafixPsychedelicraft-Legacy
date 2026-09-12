/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.events;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.achievements.PSAchievementList;
import ivorius.psychedelicraft.blocks.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.crafting.RecipeActionRegistry;
import ivorius.psychedelicraft.entities.EntityRealityRift;
import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import ivorius.psychedelicraft.fluids.PSFluids;
import ivorius.psychedelicraft.gui.UpdatableContainer;
import ivorius.psychedelicraft.items.PSItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Created by lukas on 18.02.14.
 */
public class PSEventFMLHandler
{
    public static void spawnRiftAtPlayer(EntityPlayer player)
    {
        if (player.getEntityWorld().isRemote || !PSConfig.enableRealityRifts)
        {
            return;
        }

        EntityRealityRift rift = new EntityRealityRift(player.getEntityWorld());

        double xP = (player.getRNG().nextDouble() - 0.5) * 100.0;
        double yP = (player.getRNG().nextDouble() - 0.5) * 100.0;
        double zP = (player.getRNG().nextDouble() - 0.5) * 100.0;

        rift.setPosition(player.posX + xP, player.posY + yP, player.posZ + zP);
        player.getEntityWorld().spawnEntityInWorld(rift);
    }

    public static boolean isFluidStack(ItemStack stack, ItemStack container, Fluid fluid)
    {
        return stack.getItem()== container.getItem()
                && (container.getItemDamage() == OreDictionary.WILDCARD_VALUE || container.getItemDamage() == stack.getItemDamage())
                && PSFluids.containsFluid(stack, fluid);
    }

    public void register()
    {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent event)
    {
        if ((event.type == TickEvent.Type.CLIENT || event.type == TickEvent.Type.SERVER) && event.phase == TickEvent.Phase.END)
        {
            for (FMLInterModComms.IMCMessage message : FMLInterModComms.fetchRuntimeMessages(Psychedelicraft.instance))
            {
                Psychedelicraft.communicationHandler.onIMCMessage(message, event.type == TickEvent.Type.SERVER, true);
            }
        }

    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            DrugProperties drugProperties = DrugProperties.getDrugProperties(event.player);

            if (drugProperties != null)
            {
                drugProperties.updateDrugEffects(event.player);

                if (!event.player.getEntityWorld().isRemote
                        && PSConfig.enableRealityRifts
                        && PSConfig.randomTicksUntilRiftSpawn > 0)
                {
                    if (event.player.getRNG().nextInt(PSConfig.randomTicksUntilRiftSpawn) == 0)
                    {
                        spawnRiftAtPlayer(event.player);
                    }
                }

                Container container = event.player.openContainer;
                if (container instanceof UpdatableContainer)
                    ((UpdatableContainer) container).updateAsCustomContainer();
            }
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        if (event.craftMatrix instanceof InventoryCrafting)
            RecipeActionRegistry.finalizeCrafting(event.crafting, (InventoryCrafting) event.craftMatrix, event.player);

        if (event.crafting.isItemEqual(new ItemStack(PSBlocks.mashTub)))
        {
            event.player.triggerAchievement(PSAchievementList.madeMashTub);
        }
        if (event.crafting.isItemEqual(new ItemStack(PSBlocks.distillery)))
        {
            event.player.triggerAchievement(PSAchievementList.madeDistillery);
        }
        if (isFluidStack(event.crafting, new ItemStack(PSItems.itemMashTub, 1, OreDictionary.WILDCARD_VALUE), PSFluids.alcWheatHop))
        {
            event.player.triggerAchievement(PSAchievementList.beerWash);
        }
        if (isFluidStack(event.crafting, new ItemStack(PSItems.itemMashTub, 1, OreDictionary.WILDCARD_VALUE), PSFluids.alcRedGrapes))
        {
            event.player.triggerAchievement(PSAchievementList.grapeWash);
        }
        if (isFluidStack(event.crafting, new ItemStack(PSItems.itemMashTub, 1, OreDictionary.WILDCARD_VALUE), PSFluids.alcSugarCane))
        {
            event.player.triggerAchievement(PSAchievementList.sugarcaneWash);
        }

        if (event.crafting.isItemEqual(new ItemStack(PSBlocks.dryingTable)) || event.crafting.isItemEqual(new ItemStack(PSBlocks.dryingTableIron)))
        {
            event.player.triggerAchievement(PSAchievementList.madeDryingTable);
        }
        if (event.crafting.isItemEqual(new ItemStack(PSItems.joint)))
        {
            event.player.triggerAchievement(PSAchievementList.madeJoint);
        }
        if (event.crafting.isItemEqual(new ItemStack(PSItems.hashMuffin)))
        {
            event.player.triggerAchievement(PSAchievementList.madeHashMuffin);
        }
    }

    @SubscribeEvent
    public void onItemPickup(PlayerEvent.ItemPickupEvent event)
    {
        if (event.pickedUp.getEntityItem().isItemEqual(new ItemStack(PSItems.hopCones)))
        {
            event.player.triggerAchievement(PSAchievementList.hopCones);
        }
        else if (event.pickedUp.getEntityItem().isItemEqual(new ItemStack(PSItems.wineGrapes)))
        {
            event.player.triggerAchievement(PSAchievementList.grapes);
        }
        else if (event.pickedUp.getEntityItem().isItemEqual(new ItemStack(PSItems.cannabisBuds)))
        {
            event.player.triggerAchievement(PSAchievementList.cannabisBuds);
        }
    }
}
