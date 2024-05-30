package tfcweather.config;

import net.dries007.tfc.config.ConfigBuilder;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.minecraftforge.common.ForgeConfigSpec;

public class WeatherCommonConfig
{
    // General
    public final ForgeConfigSpec.DoubleValue sandstormMinRainfall;
    public final ForgeConfigSpec.DoubleValue sandstormMaxRainfall;
    public final ForgeConfigSpec.DoubleValue snowstormMaxTemp;
    public final ForgeConfigSpec.DoubleValue heatwaveTemp;
    public final ForgeConfigSpec.BooleanValue snowstormFreezing;
    public final ForgeConfigSpec.DoubleValue minCurrentTempBuildup;
    public final ForgeConfigSpec.DoubleValue maxCurrentTempBuildup;
    public final ForgeConfigSpec.DoubleValue minRainfallBuildup;
    public final ForgeConfigSpec.DoubleValue maxRainfallBuildup;
    public final ForgeConfigSpec.BooleanValue windmillWeather2Wind;
    public final ForgeConfigSpec.IntValue barrelFillRate;
    public final ForgeConfigSpec.DoubleValue sandLayerErosionFactor;
    public final ForgeConfigSpec.IntValue sandLayerErosionChance;

    // Monthly chance
    public final ForgeConfigSpec.DoubleValue valueJanuary;
    public final ForgeConfigSpec.DoubleValue valueFebruary;
    public final ForgeConfigSpec.DoubleValue valueMarch;
    public final ForgeConfigSpec.DoubleValue valueApril;
    public final ForgeConfigSpec.DoubleValue valueMay;
    public final ForgeConfigSpec.DoubleValue valueJune;
    public final ForgeConfigSpec.DoubleValue valueJuly;
    public final ForgeConfigSpec.DoubleValue valueAugust;
    public final ForgeConfigSpec.DoubleValue valueSeptember;
    public final ForgeConfigSpec.DoubleValue valueOctober;
    public final ForgeConfigSpec.DoubleValue valueNovember;
    public final ForgeConfigSpec.DoubleValue valueDecember;

    public static final float MINIMUM_TEMPERATURE_SCALE = -20.0F;
    public static final float MAXIMUM_TEMPERATURE_SCALE = 30.0F;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_AMPLITUDE = -3.0F;
    public static final float LATITUDE_TEMPERATURE_VARIANCE_MEAN = 15.0F;
    public static final float REGIONAL_TEMPERATURE_SCALE = 2.0F;
    public static final float REGIONAL_RAINFALL_SCALE = 50.0F;

    WeatherCommonConfig(ConfigBuilder builder)
    {
        builder.push("general");

        windmillWeather2Wind = builder.comment("Should the wind system from Weather 2 be used to determine windmill behavior?").define("windmillWeather2Wind", true);

        snowstormFreezing = builder.comment("Should entities freeze during snowstorms?").define("snowstormFreezing", true);
        sandstormMinRainfall = builder.comment("Minimum limit rainfall value for sandstorms to spawn.").define("sandstormMinRainfall", 0F, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL);
        sandstormMaxRainfall = builder.comment("Maximum limit rainfall value for sandstorms to spawn.").define("sandstormMaxRainfall", 100F, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL);
        snowstormMaxTemp = builder.comment("Minimum required temperature in celsius for snowstorms to spawn.").define("snowstormMaxTemp", OverworldClimateModel.SNOW_FREEZE_TEMPERATURE, MINIMUM_TEMPERATURE_SCALE, MAXIMUM_TEMPERATURE_SCALE);
        heatwaveTemp = builder.comment("Minimum required temperature in celsius for heatwaves to trigger.").define("heatwaveTemp", LATITUDE_TEMPERATURE_VARIANCE_MEAN, MINIMUM_TEMPERATURE_SCALE, MAXIMUM_TEMPERATURE_SCALE);

        minCurrentTempBuildup = builder.comment("Minimum temperature for a storm to increase in strength.").define("minCurrentTempBuildup", 5F, MINIMUM_TEMPERATURE_SCALE, MAXIMUM_TEMPERATURE_SCALE);
        maxCurrentTempBuildup = builder.comment("Maximum temperature for a storm to increase in strength.").define("maxCurrentTempBuildup", MAXIMUM_TEMPERATURE_SCALE, MINIMUM_TEMPERATURE_SCALE, MAXIMUM_TEMPERATURE_SCALE);
        minRainfallBuildup = builder.comment("Minimum rainfall for a storm to increase in strength.").define("minRainfallBuildup", 175F, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL);
        maxRainfallBuildup = builder.comment("Maximum rainfall for a storm to increase in strength.").define("maxRainfallBuildup", ClimateModel.MAXIMUM_RAINFALL, ClimateModel.MINIMUM_RAINFALL, ClimateModel.MAXIMUM_RAINFALL);
        sandLayerErosionFactor = builder.comment("The sand layer displacement distance factor during erosion events.").define("sandLayerErosionFactor", 5D, Double.MIN_VALUE, Double.MAX_VALUE);
        sandLayerErosionChance = builder.comment("The chance for sand layers to erode to an adjacent position. Higher value --> lower chance.").define("sandLayerErosionChance", 25, 1, Integer.MAX_VALUE);

        valueJanuary = builder.comment("Value to change the storm spawn frequency of during January.").define("valueJanuary", 0.01D, 0.001D, 10000D);
        valueFebruary = builder.comment("Value to change the storm spawn frequency of during February.").define("valueFebruary", 0.005D, 0.001D, 10000D);
        valueMarch = builder.comment("Value to change the storm spawn frequency of during March.").define("valueMarch", 0.025D, 0.001D, 10000D);
        valueApril = builder.comment("Value to change the storm spawn frequency of during April.").define("valueApril", 0.045D, 0.001D, 10000D);
        valueMay = builder.comment("Value to change the storm spawn frequency of during May.").define("valueMay", 0.15D, 0.001D, 10000D);
        valueJune = builder.comment("Value to change the storm spawn frequency of during June.").define("valueJune",  0.45D, 0.001D, 10000D);
        valueJuly = builder.comment("Value to change the storm spawn frequency of during July.").define("valueJuly", 0.85D, 0.001D, 10000D);
        valueAugust = builder.comment("Value to change the storm spawn frequency of during August.").define("valueAugust", 1.15D, 0.001D, 10000D);
        valueSeptember = builder.comment("Value to change the storm spawn frequency of during September.").define("valueSeptember", 1.75D, 0.001D, 10000D);
        valueOctober = builder.comment("Value to change the storm spawn frequency of during October.").define("valueOctober", 1.0D, 0.001D, 10000D);
        valueNovember = builder.comment("Value to change the storm spawn frequency of during November.").define("valueNovember", 0.25D, 0.001D, 10000D);
        valueDecember = builder.comment("Value to change the storm spawn frequency of during December.").define("valueDecember", 0.05D, 0.001D, 10000D);

        barrelFillRate = builder.comment("The rate at which barrels will fill up during rain events. Higher value --> slower updates.").define("barrelFillRate", 25, 0, Integer.MAX_VALUE);

        builder.pop();
    }
}
