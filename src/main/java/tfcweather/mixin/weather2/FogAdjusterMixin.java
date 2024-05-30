package tfcweather.mixin.weather2;

import java.util.Random;

import org.spongepowered.asm.mixin.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import weather2.ClientWeatherProxy;
import weather2.client.SceneEnhancer;
import weather2.datatypes.WeatherEventType;
import weather2.util.WeatherUtilEntity;
import weather2.weathersystem.fog.FogAdjuster;
import weather2.weathersystem.fog.FogProfile;

@Mixin(FogAdjuster.class)
@OnlyIn(Dist.CLIENT)
public class FogAdjusterMixin
{
    @Shadow private FogProfile activeProfile;
    @Shadow private FogProfile activeProfileLerps;
    @Shadow private int lerpTicksCur = 20 * 15;
    @Shadow private int lerpTicksMax = 20 * 15;
    @Shadow private boolean useFarFog = false;
    @Shadow public int randDelay = 0;

    @Shadow
    public void initProfiles(boolean spectator) {}

    @Overwrite(remap = false)
    public void tickGame(ClientWeatherProxy weather)
    {
        updateWeatherState();

        boolean fogDisco = false;
        if (fogDisco)
        {
            if (randDelay <= 0)
            {
                Random rand = new Random();
                randDelay = 20 + rand.nextInt(5);
                startRandom();
            }
            randDelay--;
        }
        if (SceneEnhancer.getWeatherState() == WeatherEventType.SANDSTORM || SceneEnhancer.getWeatherState() == WeatherEventType.SNOWSTORM || SceneEnhancer.getWeatherState() == WeatherEventType.HEATWAVE)
        {
            Player player = Minecraft.getInstance().player;

            boolean isPlayerOutside = WeatherUtilEntity.isEntityOutside(player);
            boolean playerOutside = isPlayerOutside || (player != null && player.isInWater());
            boolean setFogFar = !playerOutside || (player != null && player.isSpectator());

            if (player != null)
            {
                if (((setFogFar && !useFarFog) || !setFogFar && useFarFog))
                {
                    initProfiles(setFogFar);

                    if (SceneEnhancer.getWeatherState() == WeatherEventType.HEATWAVE)
                    {
                        startHeatwave();
                    }
                    if (SceneEnhancer.getWeatherState() == WeatherEventType.SANDSTORM)
                    {
                        startSandstorm();
                    }
                    if (SceneEnhancer.getWeatherState() == WeatherEventType.SNOWSTORM)
                    {
                        startSnowstorm();
                    }
                }
                useFarFog = setFogFar;
            }
        }
        if (lerpTicksCur < lerpTicksMax)
        {
            float newLerpX = activeProfile.getRgb().x() + activeProfileLerps.getRgb().x();
            float newLerpY = activeProfile.getRgb().y() + activeProfileLerps.getRgb().y();
            float newLerpZ = activeProfile.getRgb().z() + activeProfileLerps.getRgb().z();
            activeProfile.getRgb().set(newLerpX, newLerpY, newLerpZ);

            activeProfile.setFogStart(activeProfile.getFogStart() + activeProfileLerps.getFogStart());
            activeProfile.setFogEnd(activeProfile.getFogEnd() + activeProfileLerps.getFogEnd());

            activeProfile.setFogStartSky(activeProfile.getFogStartSky() + activeProfileLerps.getFogStartSky());
            activeProfile.setFogEndSky(activeProfile.getFogEndSky() + activeProfileLerps.getFogEndSky());

            lerpTicksCur++;
        }
    }

    @Shadow
    public void startRandom() {}

    @Shadow
    public void startHeatwave() {}

    @Shadow
    public void startSandstorm() {}

    @Shadow
    public void startSnowstorm() {}

    @Shadow
    public void restoreVanilla() {}

    @Shadow
    public void updateWeatherState() {}
}
