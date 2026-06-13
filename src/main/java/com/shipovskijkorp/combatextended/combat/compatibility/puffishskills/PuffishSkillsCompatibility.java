package com.shipovskijkorp.combatextended.combat.compatibility.puffishskills;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Soft compatibility for Pufferfish's Skills packs that use Pufferfish's Attributes.
 *
 * <p>Pufferfish's Attributes uses dynamic attributes whose base value is {@code NaN}.
 * Reading {@link EntityAttributeInstance#getValue()} therefore either returns {@code NaN}
 * or does not represent the value that the mod applies to damage. The real mod walks the
 * attribute modifiers directly and applies them to the current damage number. Combat Extended
 * mirrors that logic here without taking a hard dependency on Pufferfish classes.</p>
 */
public final class PuffishSkillsCompatibility {
    private static final Identifier MELEE_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "melee_damage");
    private static final Identifier RANGED_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "ranged_damage");
    private static final Identifier SWORD_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "sword_damage");
    private static final Identifier AXE_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "axe_damage");
    private static final Identifier TRIDENT_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "trident_damage");
    private static final Identifier MACE_DAMAGE_ATTRIBUTE = Identifier.of("puffish_attributes", "mace_damage");
    private static final Identifier BOW_PROJECTILE_SPEED_ATTRIBUTE = Identifier.of("puffish_attributes", "bow_projectile_speed");
    private static final Identifier CROSSBOW_PROJECTILE_SPEED_ATTRIBUTE = Identifier.of("puffish_attributes", "crossbow_projectile_speed");

    private PuffishSkillsCompatibility() {
    }

    public static double applyMeleeDamageModifiers(PlayerEntity player, ItemStack stack, double damage) {
        double modifiedDamage = applyDynamicAttribute(player, MELEE_DAMAGE_ATTRIBUTE, damage);

        if (stack != null && !stack.isEmpty()) {
            if (stack.isIn(ItemTags.SWORDS)) {
                modifiedDamage = applyDynamicAttribute(player, SWORD_DAMAGE_ATTRIBUTE, modifiedDamage);
            }

            if (stack.isIn(ItemTags.AXES)) {
                modifiedDamage = applyDynamicAttribute(player, AXE_DAMAGE_ATTRIBUTE, modifiedDamage);
            }

            Item item = stack.getItem();
            if (item instanceof TridentItem) {
                modifiedDamage = applyDynamicAttribute(player, TRIDENT_DAMAGE_ATTRIBUTE, modifiedDamage);
            }

            if (item instanceof MaceItem) {
                modifiedDamage = applyDynamicAttribute(player, MACE_DAMAGE_ATTRIBUTE, modifiedDamage);
            }
        }

        return sanitizeNonNegative(modifiedDamage, damage);
    }

    public static double applyMeleeDamageModifiers(PlayerEntity player, double damage) {
        return applyMeleeDamageModifiers(player, player != null ? player.getMainHandStack() : ItemStack.EMPTY, damage);
    }

    public static double applyRangedDamageModifiers(PlayerEntity player, double damage) {
        return applyDynamicAttribute(player, RANGED_DAMAGE_ATTRIBUTE, damage);
    }

    public static double applyBowProjectileSpeedModifiers(PlayerEntity player, double speed) {
        return sanitizeNonNegative(applyDynamicAttribute(player, BOW_PROJECTILE_SPEED_ATTRIBUTE, speed), speed);
    }

    public static double applyCrossbowProjectileSpeedModifiers(PlayerEntity player, double speed) {
        return sanitizeNonNegative(applyDynamicAttribute(player, CROSSBOW_PROJECTILE_SPEED_ATTRIBUTE, speed), speed);
    }

    private static double applyDynamicAttribute(PlayerEntity player, Identifier attributeId, double initialDamage) {
        if (player == null || !Double.isFinite(initialDamage)) {
            return sanitizeNonNegative(initialDamage, 0.0D);
        }

        EntityAttributeInstance instance = getPlayerAttributeInstance(player, attributeId);
        if (instance == null) {
            return sanitizeNonNegative(initialDamage, initialDamage);
        }

        double base = initialDamage;
        for (EntityAttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE && Double.isFinite(modifier.value())) {
                base += modifier.value();
            }
        }

        double result = base;
        for (EntityAttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE && Double.isFinite(modifier.value())) {
                result += base * modifier.value();
            }
        }

        for (EntityAttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL && Double.isFinite(modifier.value())) {
                result *= 1.0D + modifier.value();
            }
        }

        return sanitizeNonNegative(result, initialDamage);
    }

    private static EntityAttributeInstance getPlayerAttributeInstance(PlayerEntity player, Identifier attributeId) {
        Optional<EntityAttribute> attribute = Registries.ATTRIBUTE.getOptionalValue(attributeId);
        if (attribute.isEmpty()) {
            return null;
        }

        RegistryEntry<EntityAttribute> attributeEntry = Registries.ATTRIBUTE.getEntry(attribute.get());
        return player.getAttributeInstance(attributeEntry);
    }

    private static double sanitizeNonNegative(double value, double fallback) {
        if (!Double.isFinite(value)) {
            return Math.max(0.0D, Double.isFinite(fallback) ? fallback : 0.0D);
        }

        return Math.max(0.0D, value);
    }
}
