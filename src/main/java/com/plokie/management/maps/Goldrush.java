package com.plokie.management.maps;

import com.plokie.management.gamemodes.Payload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Goldrush extends PayloadMap {
    public Goldrush()
    {
        teamSpawns.put(0, new Vec3(1558, 123, 2483));
        teamSpawns.put(1, new Vec3(1357, 123, 2381));

        altSpawns.put(0, new Vec3(1506, 126, 2444));
        altSpawns.put(1, new Vec3(1409, 126, 2420));

        this.introStartPosition = new Vec3(1420.449, 139.409, 2475.549);
        this.introStartRotation = new Vec2(-138.0f, 0.0f);

        this.resultsPosition = new Vec3(1426.626, 126, 2446.469);
        this.resultsRotation = new Vec2(-115.7f, 14.8f);

        this.mapCorner = new Vec3(1353, 116, 2359);
        this.mapSize = new Vec3(197+20, 51, 149);

        this.spectatorZone = new Vec3(1457, 161, 2431);

        for(int i=0;i<5;i++) {
            this.podiums.add(new BlockPos(1474, 126, 2400 + (i*4)));
        }
        this.podiumViewerPosition = new Vec3(1484.5, 129.0, 2408.5);
        this.podiumViewerRotation = new Vec2(-90.0f, 20.0f);
    }
}
