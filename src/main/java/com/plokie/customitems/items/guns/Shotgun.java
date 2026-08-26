package com.plokie.customitems.items.guns;

public class Shotgun extends Gun {
    public Shotgun()
    {
        this.usageRate = 20;
        this.damage = 3;
        this.accuracy = 80.0f;
        this.projectilesPerShot = 10;

        this.inkUsage = 1.0f / 10.0f;
    }
}
