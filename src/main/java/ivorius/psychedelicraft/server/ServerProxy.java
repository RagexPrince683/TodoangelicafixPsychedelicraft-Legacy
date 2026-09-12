/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft.server;

import ivorius.psychedelicraft.PSProxy;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import ivorius.psychedelicraft.events.PSCoreHandlerServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ServerProxy implements PSProxy
{
    @Override
    public void preInit()
    {
        new PSCoreHandlerServer().register();
    }

    @Override
    public void registerRenderers()
    {

    }

    @Override
    public void spawnColoredParticle(Entity entity, float[] color, Vec3 direction, float speed, float size)
    {

    }

    @Override
    public void createDrugRenderer(DrugProperties drugProperties)
    {

    }

    @Override
    public void loadConfig(String configID)
    {

    }

    @Override
    public void handleExtendedEntityPropertiesData(int entityID, String eepKey, String context, byte[] payload)
    {
    }

    @Override
    public void handleTileEntityData(int x, int y, int z, String context, byte[] payload)
    {
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z)
    {
        return null;
    }
}
