package com.plokie.management.gamemodes;

import java.util.function.Supplier;

public enum Gamemodes {
    TurfWar(com.plokie.management.gamemodes.TurfWar::new);

    final Gamemode gamemode;

    public Gamemode getGamemode() { return gamemode; }

    Gamemodes(Supplier<Gamemode> gamemodeConstructor)
    {
        gamemode = gamemodeConstructor.get();
    }

}
