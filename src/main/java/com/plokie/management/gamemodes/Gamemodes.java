package com.plokie.management.gamemodes;

import java.util.function.Supplier;

public enum Gamemodes {
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

}
