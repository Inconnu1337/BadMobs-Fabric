package net.Inconnu.badmobs.mixin;

import net.Inconnu.badmobs.BadMobsBlocked;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class MixinServerLevel {

    @Inject(
            method = "addFreshEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void badmobs$addFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof BadMobsBlocked blocked && blocked.badmobs$isBlocked()) {
            cir.setReturnValue(false);
        }
    }
}