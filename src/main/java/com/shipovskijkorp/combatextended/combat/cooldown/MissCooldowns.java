package com.shipovskijkorp.combatextended.combat.cooldown;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import com.shipovskijkorp.combatextended.mixin.accessor.LivingEntityAttackCooldownAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class MissCooldowns {
    private MissCooldowns() {
    }

    public static boolean shouldReduceMissCooldown(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return hasMainHandCombatAttribute(stack, Attributes.ATTACK_DAMAGE)
                || hasMainHandCombatAttribute(stack, Attributes.ATTACK_SPEED);
    }

    public static int reduceClientMissCooldown(int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return cooldownTicks;
        }

        return Math.max(0, (int) Math.ceil(cooldownTicks * CombatBalance.MISSED_ATTACK_COOLDOWN_MULTIPLIER));
    }

    public static void advanceAttackCooldownProgressAfterMiss(Player player) {
        int fullCooldownTicks = Math.max(1, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()));
        int restoredTicks = Math.max(1, (int) Math.ceil(fullCooldownTicks * CombatBalance.MISSED_ATTACK_COOLDOWN_REDUCTION));

        LivingEntityAttackCooldownAccessor accessor = (LivingEntityAttackCooldownAccessor) player;
        accessor.combatExtended$setAttackStrengthTicker(Math.max(
                accessor.combatExtended$getAttackStrengthTicker(),
                restoredTicks
        ));
    }

    private static boolean hasMainHandCombatAttribute(ItemStack stack, Holder<Attribute> attribute) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY
        );

        double baseValue = attribute == Attributes.ATTACK_SPEED
                ? CombatBalance.VANILLA_PLAYER_BASE_ATTACK_SPEED
                : CombatBalance.VANILLA_PLAYER_BASE_ATTACK_DAMAGE;

        return Double.compare(modifiers.compute(attribute, baseValue, EquipmentSlot.MAINHAND), baseValue) != 0;
    }
}
