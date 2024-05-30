package tfcweather.mixin.weather2;

import java.util.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import weather2.util.WeatherUtilEntity;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.TornadoHelper;

@Mixin(TornadoHelper.class)
public abstract class TornadoHelperMixin
{
	@Shadow public StormObject storm;
    @Shadow public int grabDist = 100;

    @Overwrite(remap = false)
    public boolean forceRotate(Level parWorld, boolean featherFallInstead)
    {
        double dist = grabDist * 2;
		if (storm.isPet())
        {
			dist = 3F;
		}
        AABB aabb = new AABB(storm.pos.x, storm.currentTopYBlock, storm.pos.z, storm.pos.x, storm.currentTopYBlock, storm.pos.z);
		if (storm.isPet())
        {
			aabb = aabb.inflate(dist, 3, dist);
		}
        else
        {
			aabb = aabb.inflate(dist, this.storm.maxHeight * 3.8, dist);
		}

        List<Entity> list = parWorld.getEntitiesOfClass(Entity.class, aabb);
        boolean foundEnt = false;

        if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
            {
                Entity entity1 = (Entity)list.get(i);
                if (canGrabEntity(entity1))
                {
					if (getDistanceXZ(storm.posBaseFormationPos, entity1.getX(), entity1.getY(), entity1.getZ()) < dist)
					{
						if (!storm.isPet())
                        {
							if (entity1 instanceof Player player)
                            {
								if (WeatherUtilEntity.isEntityOutside(player) || (storm.isPlayerControlled() && WeatherUtilEntity.canPosSeePos(parWorld, player.position(), storm.posGround)))
                                {
									if (featherFallInstead)
                                    {
										player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, false, true, true));
									}
                                    else
                                    {
										storm.spinEntityv2(player);
									}
									foundEnt = true;
								}
							}
                            else if (entity1 instanceof LivingEntity entityLiving && (WeatherUtilEntity.isEntityOutside(entityLiving, false) || (storm.isPlayerControlled() && WeatherUtilEntity.canPosSeePos(parWorld, entityLiving.position(), storm.posGround))))
                            {
								if (featherFallInstead)
                                {
									entityLiving.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, false, true, true));
								}
                                else
                                {
									storm.spinEntityv2(entityLiving);
								}
								foundEnt = true;
							}
						}
                        else if (entity1 instanceof ItemEntity item && storm.isPetGrabsItems())
                        {
							storm.spinEntityv2(item);
							foundEnt = true;
						}
                        else if (WeatherUtilEntity.isEntityOutside(entity1, false))
                        {
							storm.spinEntityv2(entity1);
							foundEnt = true;
						}
					}
				}
            }
        }
        return foundEnt;
    }

    @Shadow
    public boolean canGrabEntity(Entity ent) // Dummy method
    {
		return true;
	}

    @Shadow
	@OnlyIn(Dist.CLIENT)
	public boolean canGrabEntityClient(Entity ent) // Dummy method
    {
		return true;
	}

    @Shadow
    public double getDistanceXZ(Vec3 parVec, double var1, double var3, double var5) // Dummy method
    {
        return 0;
    }

    @Shadow
    public double getDistanceXZ(Entity ent, double var1, double var3, double var5) // Dummy method
    {
        return 0;
    }
}
