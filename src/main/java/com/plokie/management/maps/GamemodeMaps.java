package com.plokie.management.maps;

public enum GamemodeMaps {
    UrchinUnderpass("Urchin Underpass", new UrchinUnderpass());

    final String name;
    final GamemodeMap map;

    public String getName() { return name;}
    public GamemodeMap getMap() { return map; }

    GamemodeMaps(String name, GamemodeMap map)
    {
        this.name = name;
        this.map = map;
    }
}
