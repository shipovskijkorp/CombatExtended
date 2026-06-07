package com.shipovskijkorp.combatextended.combat.cooldown;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BowCooldowns {
    private BowCooldowns() {
    }

    public static void applyShotCooldown(Player player, ItemStack stack) {
        if (CombatBalance.BOW_COOLDOWN_TICKS > 0) {
            player.getCooldowns().addCooldown(stack, CombatBalance.BOW_COOLDOWN_TICKS);
        }
    }
}
