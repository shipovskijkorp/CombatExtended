package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.cooldown.TotemCooldowns;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemCooldownMixin {
    @Unique
    private long combatExtended$totemCooldownUntilTick = Long.MIN_VALUE;

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void combatExtended$blockTotemWhileCoolingDown(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.combatExtended$isTotemCoolingDown()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void combatExtended$startTotemCooldownAfterUse(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        this.combatExtended$totemCooldownUntilTick = entity.level().getGameTime() + TotemCooldowns.TOTEM_COOLDOWN_TICKS;

        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(Items.TOTEM_OF_UNDYING.getDefaultInstance(), TotemCooldowns.TOTEM_COOLDOWN_TICKS);
        }
    }

    @Unique
    private boolean combatExtended$isTotemCoolingDown() {
        LivingEntity entity = (LivingEntity) (Object) this;
        Level level = entity.level();
        return level.getGameTime() < this.combatExtended$totemCooldownUntilTick;
    }
}
