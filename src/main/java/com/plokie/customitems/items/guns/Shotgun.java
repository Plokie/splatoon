package com.plokie.customitems.items.guns;

public class Shotgun extends Gun {
    public Shotgun()
    {
        this.usageRate = 15;
        this.damage = 3.5f;
        this.accuracy = 35.0f;
        this.projectilesPerShot = 10;

        this.inkUsage = 1.0f / 20.0f;
    }
}
