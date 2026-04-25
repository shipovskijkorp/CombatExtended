package com.shipovskijkorp.combatextended.combat.cooldown;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import com.shipovskijkorp.combatextended.mixin.accessor.LivingEntityAttackCooldownAccessor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class MissCooldowns {
    private MissCooldowns() {
    }

    public static boolean shouldReduceMissCooldown(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean[] hasMeleeCombatAttribute = {false};

        stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)
                    || attribute.equals(EntityAttributes.ATTACK_SPEED)) {
                hasMeleeCombatAttribute[0] = true;
            }
        });

        return hasMeleeCombatAttribute[0];
    }

    public static int reduceClientMissCooldown(int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return cooldownTicks;
        }

        return Math.max(0, (int) Math.ceil(cooldownTicks * CombatBalance.MISSED_ATTACK_COOLDOWN_MULTIPLIER));
    }

    public static void advanceAttackCooldownProgressAfterMiss(PlayerEntity player) {
        int fullCooldownTicks = Math.max(1, (int) Math.ceil(1.0F / player.getAttackCooldownProgressPerTick()));
        int restoredTicks = Math.max(1, (int) Math.ceil(fullCooldownTicks * CombatBalance.MISSED_ATTACK_COOLDOWN_REDUCTION));

        LivingEntityAttackCooldownAccessor accessor = (LivingEntityAttackCooldownAccessor) player;
        accessor.combatExtended$setTicksSinceLastAttack(Math.max(
                accessor.combatExtended$getTicksSinceLastAttack(),
                restoredTicks
        ));
    }
}
