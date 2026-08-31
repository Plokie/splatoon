package com.plokie.management.maps;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Cyberscape extends GamemodeMap {
    public Cyberscape()
    {
        teamSpawns.put(0, new Vec3(4129, 115, 1324));
        teamSpawns.put(1, new Vec3(3983, 115, 1324));

        //this.introStartPosition = new Vec3(4015.663, 124.11913, 1347.858);
        //this.introStartRotation = new Vec2(-114.0f, 1.8f);

        this.introStartPosition = new Vec3(4120.7, 128.74314, 1322.3);
        this.introStartRotation = new Vec2(66.0f, 4.5f);

        this.resultsPosition = new Vec3(4073.110, 141.71834, 1275.304);
        this.resultsRotation = new Vec2(21.0f, 38.1f);

        this.mapCorner = new Vec3(3991, 92, 1269);
        this.mapSize = new Vec3(131, 85, 111);

        this.spectatorZone = new Vec3(4057, 145, 1324);

        this.groundBlock = Blocks.WHITE_CONCRETE_POWDER;
        this.wallBlock = Blocks.WHITE_WOOL;
    }
}
