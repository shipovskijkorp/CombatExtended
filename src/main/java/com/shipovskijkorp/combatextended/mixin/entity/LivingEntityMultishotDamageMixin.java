package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.damage.MultishotProjectileDamage;
import com.shipovskijkorp.combatextended.mixin.accessor.EntityDamageCooldownAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMultishotDamageMixin {
    @Shadow
    protected float lastDamageTaken;

    @Inject(method = "damage", at = @At("HEAD"))
    private void combatExtended$letMultishotArrowsStackDamage(
            ServerWorld world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!MultishotProjectileDamage.shouldBypassDamageCooldown(source)) {
            return;
        }

        EntityDamageCooldownAccessor cooldownAccessor = (EntityDamageCooldownAccessor) this;
        if (cooldownAccessor.combatExtended$getTimeUntilRegen() <= 0 && this.lastDamageTaken <= 0.0F) {
            return;
        }

        cooldownAccessor.combatExtended$setTimeUntilRegen(0);
        this.lastDamageTaken = 0.0F;
    }
}
