/*
 * Copyright 2014 Lukas Tenbrink
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ivorius.psychedelicraft.internal.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

/** Moves SimpleImpl callbacks onto the Minecraft client thread. */
public final class ClientPacketQueue
{
    private static final Queue<ClientTask> TASKS = new ConcurrentLinkedQueue<>();

    public static void enqueue(World world, Runnable action)
    {
        if (world != null)
        {
            TASKS.add(new ClientTask(world, action));
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null)
        {
            TASKS.clear();
            return;
        }

        ClientTask task;
        while ((task = TASKS.poll()) != null)
        {
            if (task.world == minecraft.theWorld)
            {
                task.action.run();
            }
        }
    }

    private static final class ClientTask
    {
        private final World world;
        private final Runnable action;

        private ClientTask(World world, Runnable action)
        {
            this.world = world;
            this.action = action;
        }
    }
}
