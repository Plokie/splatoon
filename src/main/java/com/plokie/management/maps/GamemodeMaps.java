package com.plokie.management.maps;

public enum GamemodeMaps {
    UrchinUnderpass(new UrchinUnderpass());

    final GamemodeMap map;

    public GamemodeMap getMap() { return map; }

    GamemodeMaps(GamemodeMap map)
    {
        this.map = map;
    }
}
