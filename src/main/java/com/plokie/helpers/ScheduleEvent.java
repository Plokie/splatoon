package com.plokie.helpers;

import com.plokie.Splatoon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ScheduleEvent {
    static Map<Integer, List<Consumer<MinecraftServer>>> eventQueue = new HashMap<>();


    public ScheduleEvent()
    {
        ServerTickEvents.START_SERVER_TICK.register(server->{
            int currentTick = server.getTickCount();

            List<Consumer<MinecraftServer>> events = eventQueue.get(currentTick);

            if(events != null)
            {
                for(Consumer<MinecraftServer> callback : events)
                {
                    callback.accept(server);
                }
                eventQueue.remove(currentTick);
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

        List<Consumer<MinecraftServer>> cbList = eventQueue.get(currentTick + inTicks);
        if(cbList == null) {
            cbList = eventQueue.put(currentTick + inTicks, new ArrayList<>());
            cbList = eventQueue.get(currentTick + inTicks);
        }

        cbList.add(callback);
    }
}
