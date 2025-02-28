package me.codexadrian.spirit.mixin;

import me.codexadrian.spirit.EngulfableItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements EngulfableItem {
    int engulfTime = 0;
    int maxEngulfTime = 0;

    private static final EntityDataAccessor<Integer> ENGULF_TIME = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_ENGULF_TIME = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void defineCorrupted(CallbackInfo ci) {
        var entityData = ((ItemEntity) (Object) this).getEntityData();
        entityData.define(ENGULF_TIME, 0);
        entityData.define(MAX_ENGULF_TIME, 0);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCorrupted(CompoundTag compoundTag, CallbackInfo ci) {
        this.engulfTime = compoundTag.getInt("engulfTime");
        this.engulfTime = compoundTag.getInt("maxEngulfTime");
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveCorrupted(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putInt("engulfTime", engulfTime);
        compoundTag.putInt("maxEngulfTime", maxEngulfTime);
    }

    @Override
    public void resetEngulfing() {
        engulfTime = 0;
        maxEngulfTime = 0;
    }

    @Override
    public void setMaxEngulfTime(int duration) {
        maxEngulfTime = duration;
    }

    @Override
    public boolean isEngulfed() {
        return engulfTime >= 0 && maxEngulfTime != 0;
    }

    @Override
    public boolean isFullyEngulfed() {
        return engulfTime >= maxEngulfTime;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (isEngulfed() && !itemEntity.level.isClientSide()) {
            if (engulfTime % 5 == 0) {
                ServerLevel sLevel = (ServerLevel) itemEntity.level;
                sLevel.sendParticles(ParticleTypes.SOUL, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 10, 0.5, 0.5, 0.5, 0);
                sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 10, 0.5, 0.5, 0.5, 0);
            }
            engulfTime++;
        }
    }
}
