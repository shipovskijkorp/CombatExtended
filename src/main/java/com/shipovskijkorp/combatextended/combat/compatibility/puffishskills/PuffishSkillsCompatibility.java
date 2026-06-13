package com.shipovskijkorp.combatextended.combat.compatibility.puffishskills;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Soft compatibility for Pufferfish's Skills packs that use Pufferfish's Attributes.
 *
 * <p>Pufferfish's Skills applies skill rewards as live player attribute modifiers.
 * RPG skill tree packs commonly express damage bonuses/debuffs through the
 * {@code puffish_attributes:melee_damage} and {@code puffish_attributes:ranged_damage}
 * multiplier attributes. Combat Extended does not need a hard dependency on either mod:
 * when those attributes are registered on the player, their current values are folded into
 * the displayed weapon damage.</p>
 */
public final class PuffishSkillsCompatibility {
    private static final Identifier MELEE_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "melee_damage");
    private static final Identifier RANGED_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "ranged_damage");

    private PuffishSkillsCompatibility() {
    }

    public static double applyMeleeDamageModifiers(PlayerEntity player, double damage) {
        return applyMultiplier(damage, getPlayerAttributeMultiplier(player, MELEE_DAMAGE_ATTRIBUTE));
    }

    public static double applyRangedDamageModifiers(PlayerEntity player, double damage) {
        return applyMultiplier(damage, getPlayerAttributeMultiplier(player, RANGED_DAMAGE_ATTRIBUTE));
    }

    private static double applyMultiplier(double damage, double multiplier) {
        return Math.max(0.0D, damage * multiplier);
    }

    private static double getPlayerAttributeMultiplier(PlayerEntity player, Identifier attributeId) {
        if (player == null) {
            return 1.0D;
        }

        Optional<EntityAttribute> attribute = Registries.ATTRIBUTE.getOptionalValue(attributeId);
        if (attribute.isEmpty()) {
            return 1.0D;
        }

        RegistryEntry<EntityAttribute> attributeEntry = Registries.ATTRIBUTE.getEntry(attribute.get());
        EntityAttributeInstance instance = player.getAttributeInstance(attributeEntry);
        if (instance == null) {
            return 1.0D;
        }

        return instance.getValue();
    }
}
