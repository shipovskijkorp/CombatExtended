package com.shipovskijkorp.combatextended.mixin.item;

import com.shipovskijkorp.combatextended.combat.cooldown.BowCooldowns;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void combatextended$applyBowCooldown(
            ItemStack stack,
            Level level,
            LivingEntity user,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level.isClientSide() || !(user instanceof Player player)) {
            return;
        }

        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        int useTicks = stack.getUseDuration(user) - remainingUseTicks;
        if (BowItem.getPowerForTime(useTicks) <= 0.0F) {
            return;
        }

        BowCooldowns.applyShotCooldown(player, stack);
    }
}
