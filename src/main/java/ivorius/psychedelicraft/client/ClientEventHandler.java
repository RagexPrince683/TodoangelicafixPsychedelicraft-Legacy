package ivorius.psychedelicraft.client;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.blocks.PSBlocks;
import ivorius.psychedelicraft.client.audio.MovingSoundDrug;
import ivorius.psychedelicraft.client.rendering.DrugEffectInterpreter;
import ivorius.psychedelicraft.client.rendering.SmoothCameraHelper;
import ivorius.psychedelicraft.client.rendering.shaders.PSRenderStates;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import ivorius.psychedelicraft.fluids.FluidWithIconSymbolRegistering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/** Owns event callbacks that reference client-only Minecraft and Forge classes. */
public class ClientEventHandler
{
    public void register()
    {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!event.world.isRemote)
        {
            return;
        }

        DrugProperties properties = DrugProperties.getDrugProperties(event.entity);
        if (properties == null)
        {
            return;
        }

        SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
        for (String drugName : properties.getAllDrugNames())
        {
            if (PSConfig.hasBGM(drugName))
            {
                ResourceLocation sound = new ResourceLocation(Psychedelicraft.MODID,
                    "drug." + drugName.toLowerCase());
                soundHandler.playSound(new MovingSoundDrug(sound, event.entity, properties, drugName));
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.PORTAL)
        {
            return;
        }

        EntityLivingBase renderEntity = Minecraft.getMinecraft().renderViewEntity;
        DrugProperties properties = DrugProperties.getDrugProperties(renderEntity);
        if (properties != null && properties.renderer != null)
        {
            properties.renderer.renderOverlaysAfterShaders(event.partialTicks, renderEntity,
                renderEntity.ticksExisted, event.resolution.getScaledWidth(), event.resolution.getScaledHeight(),
                properties);
        }
    }

    @SubscribeEvent
    public void onTextureStitchPre(TextureStitchEvent.Pre event)
    {
        IIconRegister iconRegister = event.map;
        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values())
        {
            if (fluid instanceof FluidWithIconSymbolRegistering)
            {
                ((FluidWithIconSymbolRegistering) fluid).registerIcons(iconRegister, event.map.getTextureType());
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event)
    {
        if (event.type == TickEvent.Type.CLIENT && event.phase == TickEvent.Phase.START)
        {
            PSRenderStates.update();
        }
        else if (event.type == TickEvent.Type.RENDER && event.phase == TickEvent.Phase.START)
        {
            PSBlocks.psycheLeaves.setGraphicsLevel(Minecraft.getMinecraft().gameSettings.fancyGraphics);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START && !minecraft.isGamePaused())
        {
            DrugProperties properties = DrugProperties.getDrugProperties(minecraft.renderViewEntity);
            if (properties != null)
            {
                SmoothCameraHelper.instance.update(minecraft.gameSettings.mouseSensitivity,
                    DrugEffectInterpreter.getSmoothVision(properties));
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.modID.equals(Psychedelicraft.MODID))
        {
            PSConfig.loadConfig(event.configID);
            if (Psychedelicraft.config.hasChanged())
            {
                Psychedelicraft.config.save();
            }
        }
    }
}
