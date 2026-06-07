package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.damage.MultishotProjectileDamage;
import com.shipovskijkorp.combatextended.mixin.accessor.EntityDamageCooldownAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMultishotDamageMixin {
    @Shadow
    protected float lastHurt;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void combatExtended$letMultishotArrowsStackDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!MultishotProjectileDamage.shouldBypassDamageCooldown(source)) {
            return;
        }

        EntityDamageCooldownAccessor cooldownAccessor = (EntityDamageCooldownAccessor) this;
        if (cooldownAccessor.combatExtended$getInvulnerableTime() <= 0 && this.lastHurt <= 0.0F) {
            return;
        }

        cooldownAccessor.combatExtended$setInvulnerableTime(0);
        this.lastHurt = 0.0F;
    }
}
