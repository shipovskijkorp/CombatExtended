package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;

public final class TridentTuning {
    private TridentTuning() {
    }

    public static AttributeModifiersComponent createAttributeModifiers() {
        AttributeModifierSlot mainHand = AttributeModifierSlot.forEquipmentSlot(EquipmentSlot.MAINHAND);

        return AttributeModifiersComponent.builder()
                .add(
                        EntityAttributes.ATTACK_DAMAGE,
                        new EntityAttributeModifier(
                                CombatExtended.id("trident_attack_damage"),
                                CombatBalance.tridentAttackDamageModifier(),
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        mainHand
                )
                .add(
                        EntityAttributes.ATTACK_SPEED,
                        new EntityAttributeModifier(
                                CombatExtended.id("trident_attack_speed"),
                                CombatBalance.tridentAttackSpeedModifier(),
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        mainHand
                )
                .add(
                        EntityAttributes.ENTITY_INTERACTION_RANGE,
                        new EntityAttributeModifier(
                                CombatExtended.id("trident_attack_range"),
                                CombatBalance.tridentAttackRangeModifier(),
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ),
                        mainHand,
                        AttributeModifiersComponent.Display.getHidden()
                )
                .build();
    }
}
