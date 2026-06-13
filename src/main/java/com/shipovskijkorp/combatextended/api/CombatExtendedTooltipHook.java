package com.shipovskijkorp.combatextended.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * Optional integration point for mods that want to add lines to Combat Extended's
 * generated tooltip without replacing the vanilla tooltip themselves.
 */
public interface CombatExtendedTooltipHook {
    void appendTooltip(ItemStack stack, PlayerEntity player, Consumer<Text> textConsumer, CombatExtendedTooltipPhase phase);
}
