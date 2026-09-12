/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entities.drugs.DrugProperties;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Created by lukas on 24.05.14.
 */
public interface PSProxy
{
    void preInit();

    void registerRenderers();

    void spawnColoredParticle(Entity entity, float[] color, Vec3 direction, float speed, float size);

    void createDrugRenderer(DrugProperties drugProperties);

    void loadConfig(String configID);

    void handleExtendedEntityPropertiesData(int entityID, String eepKey, String context, byte[] payload);

    void handleTileEntityData(int x, int y, int z, String context, byte[] payload);

    Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z);
}
