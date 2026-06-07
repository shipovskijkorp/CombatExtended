package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.function.Consumer;

public final class TridentTooltip {
    private TridentTooltip() {
    }

    public static void appendVanillaStyleAttributes(Consumer<Component> textConsumer) {
        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.trident.attack_damage",
                format(CombatBalance.TRIDENT_MELEE_DAMAGE)
        ).withStyle(ChatFormatting.DARK_GREEN));

        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.trident.attack_speed",
                format(CombatBalance.TRIDENT_ATTACK_SPEED)
        ).withStyle(ChatFormatting.DARK_GREEN));

        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.trident.entity_interaction_range",
                format(CombatBalance.TRIDENT_ATTACK_RANGE)
        ).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
