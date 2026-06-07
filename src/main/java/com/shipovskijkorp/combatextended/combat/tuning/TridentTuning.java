package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class TridentTuning {
    private TridentTuning() {
    }

    public static ItemAttributeModifiers createAttributeModifiers() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                CombatExtended.id("trident_attack_damage"),
                                CombatBalance.tridentAttackDamageModifier(),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                CombatExtended.id("trident_attack_speed"),
                                CombatBalance.tridentAttackSpeedModifier(),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(
                                CombatExtended.id("trident_attack_range"),
                                CombatBalance.tridentAttackRangeModifier(),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }
}
