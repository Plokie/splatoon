package com.plokie.customitems.items.guns;

public class Burstshot extends Gun {
    public Burstshot()
    {
        this.usageRate = 10;
        this.damage = 7;
        this.accuracy = 10.0f;
        this.burst = 2;

        this.inkUsage = 1.0f / 50.0f;
    }
}