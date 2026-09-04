package com.plokie.management.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class PayloadMap extends GamemodeMap {
    Map<Integer, Vec3> altSpawns = new HashMap<>();
}
