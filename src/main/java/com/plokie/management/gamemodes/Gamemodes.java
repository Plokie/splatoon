package com.plokie.management.gamemodes;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum Gamemodes implements StringRepresentable {
    TurfWar("Turf War", com.plokie.management.gamemodes.TurfWar::new),
    Payload("Payload", com.plokie.management.gamemodes.Payload::new);

    final String name;
    final Gamemode gamemode;

    public String getName() { return name; }
    public Gamemode getGamemode() { return gamemode; }

    Gamemodes(String name, Supplier<Gamemode> gamemodeConstructor)
    {
        this.name = name;
        gamemode = gamemodeConstructor.get();
    }

    public static final Codec<Gamemodes> CODEC = StringRepresentable.fromEnum(Gamemodes::values);

    @Override
    public @NotNull String getSerializedName() {
        return this.toString().toLowerCase();
    }
}
