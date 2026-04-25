package com.shipovskijkorp.combatextended.combat.cooldown;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class BowCooldowns {
    private BowCooldowns() {
    }

    public static void applyShotCooldown(PlayerEntity player, ItemStack stack) {
        player.getItemCooldownManager().set(stack, CombatBalance.BOW_COOLDOWN_TICKS);
    }
}
