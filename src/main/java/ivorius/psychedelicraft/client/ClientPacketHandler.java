package ivorius.psychedelicraft.client;

import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.internal.network.ClientPacketQueue;
import ivorius.psychedelicraft.internal.network.PartialUpdateHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/** Applies server synchronization packets without exposing client classes to common packet handlers. */
final class ClientPacketHandler
{
    private ClientPacketHandler()
    {
    }

    public static void handleExtendedEntityPropertiesData(final int entityID, final String eepKey,
        final String context, final byte[] payload)
    {
        final World world = Minecraft.getMinecraft().theWorld;
        if (world == null)
        {
            return;
        }

        ClientPacketQueue.enqueue(world, new Runnable()
        {
            @Override
            public void run()
            {
                Entity entity = world.getEntityByID(entityID);
                if (entity == null || entity.worldObj != world)
                {
                    return;
                }

                IExtendedEntityProperties properties = entity.getExtendedProperties(eepKey);
                if (properties instanceof PartialUpdateHandler)
                {
                    ((PartialUpdateHandler) properties).readUpdateData(Unpooled.wrappedBuffer(payload), context);
                }
            }
        });
    }

    public static void handleTileEntityData(final int x, final int y, final int z, final String context,
        final byte[] payload)
    {
        final World world = Minecraft.getMinecraft().theWorld;
        if (world == null)
        {
            return;
        }

        ClientPacketQueue.enqueue(world, new Runnable()
        {
            @Override
            public void run()
            {
                if (!world.blockExists(x, y, z))
                {
                    return;
                }

                TileEntity entity = world.getTileEntity(x, y, z);
                if (entity instanceof PartialUpdateHandler)
                {
                    ((PartialUpdateHandler) entity).readUpdateData(Unpooled.wrappedBuffer(payload), context);
                }
            }
        });
    }
}
