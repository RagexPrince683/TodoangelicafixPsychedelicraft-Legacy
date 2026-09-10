/*
 * Copyright 2014 Lukas Tenbrink
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ivorius.psychedelicraft.internal.network;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Created by lukas on 02.07.14.
 */
public class PacketTileEntityDataHandler implements IMessageHandler<PacketTileEntityData, IMessage> {

    @Override
    public IMessage onMessage(PacketTileEntityData message, MessageContext ctx) {
        final World world = Minecraft.getMinecraft().theWorld;
        final int x = message.getX();
        final int y = message.getY();
        final int z = message.getZ();
        final String context = message.getContext();
        final ByteBuf payload = message.getPayload();
        final byte[] payloadCopy = new byte[payload.readableBytes()];
        payload.getBytes(payload.readerIndex(), payloadCopy);

        ClientPacketQueue.enqueue(world, new Runnable() {
            @Override
            public void run() {
                if (!world.blockExists(x, y, z)) {
                    return;
                }

                TileEntity entity = world.getTileEntity(x, y, z);
                if (entity instanceof PartialUpdateHandler) {
                    ((PartialUpdateHandler) entity).readUpdateData(Unpooled.wrappedBuffer(payloadCopy), context);
                }
            }
        });

        return null;
    }
}
