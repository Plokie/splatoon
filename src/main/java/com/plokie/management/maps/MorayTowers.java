package com.plokie.management.maps;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class MorayTowers extends GamemodeMap {
    public MorayTowers()
    {
        teamSpawns.put(0, new Vec3(2830, 170, 1298));
        teamSpawns.put(1, new Vec3(2709, 170, 1284));

        this.introStartPosition = new Vec3(2836, 174.22428, 1342.205);
        this.introStartRotation = new Vec2(123.3f, 17.6f);

        this.resultsPosition = new Vec3(2809.5, 179.0, 1333.144);
        this.resultsRotation = new Vec2(144.1f, 36.4f);

        this.mapCorner = new Vec3(2697, 118, 1244);
        this.mapSize = new Vec3(142, 71, 102);

        this.spectatorZone = new Vec3(2769, 188, 1291);
    }
}
