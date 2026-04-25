package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.BigDecimal;
import java.util.function.Consumer;

public final class TridentTooltip {
    private TridentTooltip() {
    }

    public static void appendVanillaStyleAttributes(Consumer<Text> textConsumer) {
        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.trident.attack_damage",
                format(CombatBalance.TRIDENT_MELEE_DAMAGE)
        ).formatted(Formatting.DARK_GREEN));

        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.trident.attack_speed",
                format(CombatBalance.TRIDENT_ATTACK_SPEED)
        ).formatted(Formatting.DARK_GREEN));

        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.trident.entity_interaction_range",
                format(CombatBalance.TRIDENT_ATTACK_RANGE)
        ).formatted(Formatting.DARK_GREEN));
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
