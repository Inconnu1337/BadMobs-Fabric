package net.Inconnu.badmobs.mixin;

import net.Inconnu.badmobs.BadMobs;
import net.Inconnu.badmobs.BadMobsBlocked;
import net.Inconnu.badmobs.Constants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MixinFinalizeSpawn implements BadMobsBlocked {

    @Unique
    private boolean badmobs$blocked = false;

    @Override
    public boolean badmobs$isBlocked() {
        return badmobs$blocked;
    }

    @Inject(
            method = "finalizeSpawn",
            at = @At("HEAD"),
            cancellable = true
    )
    private void badmobs$finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.isClientSide()) {
            return;
        }

        Mob self = (Mob) (Object) this;

        if (BadMobs.config == null) {
            Constants.LOG.error("BadMobs config is NOT loaded! Cannot check spawn for: {}", self.getType().getDescriptionId());
            return;
        }

        if (!BadMobs.config.allowSpawn(self, spawnType)) {
            badmobs$blocked = true;
            cir.setReturnValue(null);
        }
    }
}