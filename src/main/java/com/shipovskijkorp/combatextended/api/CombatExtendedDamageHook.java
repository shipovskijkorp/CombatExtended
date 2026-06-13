package com.shipovskijkorp.combatextended.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Optional integration point for mods that want Combat Extended's tooltip preview
 * to include their own damage modifiers without hard dependencies inside CE.
 */
public interface CombatExtendedDamageHook {
    default double modifyMeleeDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
        return currentDamage;
    }

    default double modifyRangedDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
        return currentDamage;
    }
}
