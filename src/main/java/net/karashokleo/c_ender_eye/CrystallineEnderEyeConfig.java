package net.karashokleo.c_ender_eye;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "c_ender_eye")
public class CrystallineEnderEyeConfig implements ConfigData
{
    public boolean message_overlay = true;
    public boolean enableRandomWarp = true;
    public boolean enableFixedPointWarp = true;
    public boolean allowDimensionalWarp = true;
    public boolean allowRewritePositionForFixedPointWarp = true;
    public int attemptsForRandomWarp = 16;
    public double minDistanceForRandomWarp = 32;
    public double maxDistanceForRandomWarp = 64;
    public double minDistanceForFixedPointWarp = 0;
    public double maxDistanceForFixedPointWarp = 64;
    public boolean enableCoolDownAfterWritePosition = true;
    public boolean enableCoolDownAfterRandomWarp = true;
    public boolean enableCoolDownAfterFixedPointWarp = true;
    public int coolDownAfterWritePosition = 20;
    public int coolDownAfterRandomWarp = 100;
    public int coolDownAfterFixedPointWarp = 100;
}
