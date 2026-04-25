package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.damage.SpecialExplosionDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpecialExplosionDamageMixin {
    @Inject(method = "modifyAppliedDamage", at = @At("RETURN"), cancellable = true)
    private void combatextended$reduceSpecialExplosionDamageForPlayers(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof PlayerEntity)) {
            return;
        }

        Float modifiedDamage = cir.getReturnValue();
        if (modifiedDamage == null) {
            return;
        }

        cir.setReturnValue(SpecialExplosionDamage.reducePlayerDamageIfNeeded(source, modifiedDamage));
    }
}
