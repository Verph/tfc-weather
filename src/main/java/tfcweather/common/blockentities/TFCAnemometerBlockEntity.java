package tfcweather.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import weather2.util.WeatherUtilEntity;
import weather2.util.WindReader;

public class TFCAnemometerBlockEntity extends BlockEntity
{
	public float smoothAngle = 0;
	public float smoothAnglePrev = 0;
	public float smoothAngleRotationalVel = 0;
	public boolean isOutsideCached = false;

	public TFCAnemometerBlockEntity(BlockPos pos, BlockState state)
    {
		super(TFCWeatherBlockEntities.ANEMOMETER.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TFCAnemometerBlockEntity entity)
    {
		entity.tick(level, pos, state);
	}

	public void tick(Level level, BlockPos pos, BlockState state)
    {
		if (level.isClientSide)
        {
			if (level.getGameTime() % 40 == 0)
            {
				isOutsideCached = WeatherUtilEntity.isPosOutside(level, new Vec3(getBlockPos().getX()+0.5F, getBlockPos().getY()+0.5F, getBlockPos().getZ()+0.5F), false, true);
			}
			if (isOutsideCached)
            {
				float windSpeed = WindReader.getWindSpeed(level, pos, 1);
				float rotMax = 50F;
				float maxSpeed = (windSpeed / 1.2F) * rotMax;
				if (smoothAngleRotationalVel < maxSpeed)
                {
					smoothAngleRotationalVel += windSpeed * 0.3F;
				}
				if (smoothAngleRotationalVel > rotMax)
                {
                    smoothAngleRotationalVel = rotMax;
                }
				if (smoothAngle >= 180)
                {
                    smoothAngle -= 360;
                }
			}
			smoothAnglePrev = smoothAngle;
			smoothAngle += smoothAngleRotationalVel;
			smoothAngleRotationalVel -= 0.01F;
			smoothAngleRotationalVel *= 0.99F;
			if (smoothAngleRotationalVel <= 0)
            {
                smoothAngleRotationalVel = 0;
            }
		}
	}
}
