package com.shipovskijkorp.combatextended.mixin.item;

import com.shipovskijkorp.combatextended.combat.cooldown.BowCooldowns;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("RETURN"))
    private void combatextended$applyBowCooldown(
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (world.isClient() || !(user instanceof PlayerEntity player)) {
            return;
        }

        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        int useTicks = stack.getMaxUseTime(user) - remainingUseTicks;
        if (BowItem.getPullProgress(useTicks) <= 0.0F) {
            return;
        }

        BowCooldowns.applyShotCooldown(player, stack);
    }
}
