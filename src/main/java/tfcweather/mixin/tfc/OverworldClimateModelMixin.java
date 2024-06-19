package tfcweather.mixin.tfc;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.OverworldClimateModel;

import weather2.util.WindReader;

import tfcweather.config.TFCWeatherConfig;

import static net.dries007.tfc.util.climate.OverworldClimateModel.*;

@Mixin(OverworldClimateModel.class)
public class OverworldClimateModelMixin
{
    @Overwrite(remap = false)
    public Vec2 getWindVector(Level level, BlockPos pos, long calendarTime)
    {
        if (TFCWeatherConfig.COMMON.windmillWeather2Wind.get())
        {
            final Vec3 vec3Pos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            final float intensity = WindReader.getWindSpeed(level, pos);
            final float angle = WindReader.getWindAngle(level, vec3Pos); // In degrees
            return new Vec2(Mth.cos(angle), Mth.sin(angle)).scale(intensity);
        }
        else
        {
            final int y = pos.getY();
            if (y < SEA_LEVEL - 6)
                return Vec2.ZERO;
            final Random random = seededRandom(ICalendar.getTotalDays(calendarTime), 129341623413L);
            final float preventFrequentWindyDays = random.nextFloat() < 0.1f ? 1f : random.nextFloat();
            final float intensity = Math.min(0.5f * random.nextFloat() * preventFrequentWindyDays
                + 0.3f * Mth.clampedMap(y, SEA_LEVEL, SEA_LEVEL + 65, 0f, 1f)
                + 0.4f * level.getRainLevel(0f)
                + 0.3f * level.getThunderLevel(0f), 1f);
            final float angle = random.nextFloat() * Mth.TWO_PI;
            return new Vec2(Mth.cos(angle), Mth.sin(angle)).scale(intensity);
        }
    }

    @Shadow
    protected Random seededRandom(long day, long salt)
    {
        return new Random(1);
    }
}
