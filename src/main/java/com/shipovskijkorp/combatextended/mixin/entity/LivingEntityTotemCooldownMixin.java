package com.shipovskijkorp.combatextended.mixin.entity;

import com.shipovskijkorp.combatextended.combat.cooldown.TotemCooldowns;
import com.shipovskijkorp.combatextended.mixin.accessor.EntityWorldAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemCooldownMixin {
    @Unique
    private long combatExtended$totemCooldownUntilTick = Long.MIN_VALUE;

    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    private void combatExtended$blockTotemWhileCoolingDown(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (this.combatExtended$isTotemCoolingDown()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tryUseDeathProtector", at = @At("RETURN"))
    private void combatExtended$startTotemCooldownAfterUse(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        this.combatExtended$totemCooldownUntilTick = this.combatExtended$getWorld().getTime() + TotemCooldowns.TOTEM_COOLDOWN_TICKS;

        if (entity instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(Items.TOTEM_OF_UNDYING.getDefaultStack(), TotemCooldowns.TOTEM_COOLDOWN_TICKS);
        }
    }

    @Unique
    private boolean combatExtended$isTotemCoolingDown() {
        return this.combatExtended$getWorld().getTime() < this.combatExtended$totemCooldownUntilTick;
    }

    @Unique
    private World combatExtended$getWorld() {
        return ((EntityWorldAccessor) this).combatExtended$getWorld();
    }
}
