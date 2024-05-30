package tfcweather.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import weather2.util.WeatherUtilEntity;
import weather2.util.WindReader;

public class TFCWindVaneBlockEntity extends BlockEntity
{
	public float smoothAngle = 0;
	public float smoothAnglePrev = 0;
	public float smoothAngleRotationalVelAccel = 0;
	public boolean isOutsideCached = false;

	public TFCWindVaneBlockEntity(BlockPos pos, BlockState state)
    {
		super(TFCWeatherBlockEntities.WIND_VANE.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TFCWindVaneBlockEntity entity)
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
				float targetAngle = WindReader.getWindAngle(level, new Vec3(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()));
				float windSpeed = WindReader.getWindSpeed(level, pos, 1);
				if (smoothAngle > 180)
				{
					smoothAngle -= 360;
				}
				if (smoothAngle < -180)
				{
					smoothAngle += 360;
				}
				if (smoothAnglePrev > 180)
				{
					smoothAnglePrev -= 360;
				}
				if (smoothAnglePrev < -180)
				{
					smoothAnglePrev += 360;
				}
				smoothAnglePrev = smoothAngle;
				float bestMove = Mth.wrapDegrees(targetAngle - smoothAngle);
				if (Math.abs(bestMove) < 180)
				{
					if (bestMove > 0)
					{
						smoothAngleRotationalVelAccel -= windSpeed * 0.4;
					}
					if (bestMove < 0)
					{
						smoothAngleRotationalVelAccel += windSpeed * 0.4;
					}
					if (smoothAngleRotationalVelAccel > 0.3 || smoothAngleRotationalVelAccel < -0.3)
					{
						smoothAngle += smoothAngleRotationalVelAccel;
					}
					smoothAngleRotationalVelAccel *= 0.96F;
				}
			}
		}
	}
}
