package com.plokie.management.maps;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class UrchinUnderpass extends GamemodeMap {
    public UrchinUnderpass()
    {
        teamSpawns.put(0, new Vec3(1434, 123, 1343));
        teamSpawns.put(1, new Vec3(1257, 123, 1405));

        this.introStartPosition = new Vec3(1428.612, 129.61529, 1332.714);
        this.introStartRotation = new Vec2(60.54f, 0.0f);

        this.resultsPosition = new Vec3(1291.425, 160.0, 1349.573);
        this.resultsRotation = new Vec2(-42.6f, 41.4f);

        this.mapCorner = new Vec3(1250, 103, 1420);
        this.mapSize = new Vec3(200, 45, -123);

        this.spectatorZone = new Vec3(1346, 146, 1374);
    }
}
