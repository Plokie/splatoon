package com.plokie.helpers;

import com.plokie.Splatoon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ScheduleEvent {
    static Map<Integer, Consumer<MinecraftServer>> eventQueue = new HashMap<>();


    public ScheduleEvent()
    {
        ServerTickEvents.START_SERVER_TICK.register(server->{
            int currentTick = server.getTickCount();

            Consumer<MinecraftServer> event = eventQueue.get(currentTick);

            if(event != null)
            {
                event.accept(server);
            }
        });
    }

    public static void schedule(int inTicks, Consumer<MinecraftServer> callback)
    {
        if(inTicks < 1) {
            Splatoon.LOGGER.error("Event must be scheduled for the future, never in the past or present");
            return;
        }

        int currentTick = Splatoon.SERVER.getTickCount();

        eventQueue.put(currentTick + inTicks, callback);
    }
}
