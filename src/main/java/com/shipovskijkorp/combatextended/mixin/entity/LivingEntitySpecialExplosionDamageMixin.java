package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.damage.SpecialExplosionDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpecialExplosionDamageMixin {
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void combatextended$reduceSpecialExplosionDamageForPlayers(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof Player)) {
            return;
        }

        cir.setReturnValue(SpecialExplosionDamage.reducePlayerDamageIfNeeded(source, cir.getReturnValue()));
    }
}
