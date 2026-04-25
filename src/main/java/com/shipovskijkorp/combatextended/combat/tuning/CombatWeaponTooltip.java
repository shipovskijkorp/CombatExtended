package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

public final class CombatWeaponTooltip {
    private static final Identifier SHARPNESS_ID = Identifier.ofVanilla("sharpness");

    private CombatWeaponTooltip() {
    }

    public static boolean shouldReplaceAttributeTooltip(ItemStack stack) {
        if (isRangedWeapon(stack)) {
            return true;
        }

        return hasMainHandCombatAttributes(stack);
    }

    public static void appendVanillaStyleAttributes(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        appendVanillaMainHandHeader(textConsumer);

        if (isRangedWeapon(stack)) {
            appendDamage(textConsumer, rangedWeaponDamageRange(stack));
            return;
        }

        appendDamage(textConsumer, format(calculateMeleeAttackDamage(stack, player)));
        appendAttackSpeed(textConsumer, format(getMainHandAttributeValue(
                stack,
                EntityAttributes.ATTACK_SPEED,
                CombatBalance.VANILLA_PLAYER_BASE_ATTACK_SPEED
        )));
        appendAttackRange(textConsumer, format(getMainHandAttributeValue(
                stack,
                EntityAttributes.ENTITY_INTERACTION_RANGE,
                CombatBalance.VANILLA_PLAYER_ENTITY_INTERACTION_RANGE
        )));
    }

    private static double calculateMeleeAttackDamage(ItemStack stack, PlayerEntity player) {
        double damage = getMainHandAttributeValue(
                stack,
                EntityAttributes.ATTACK_DAMAGE,
                CombatBalance.VANILLA_PLAYER_BASE_ATTACK_DAMAGE
        );

        damage += getSharpnessDamageBonus(stack);

        if (player != null) {
            damage += getStatusEffectAttackDamageBonus(player);
        }

        return Math.max(0.0D, damage);
    }

    private static double getMainHandAttributeValue(
            ItemStack stack,
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
            double baseValue
    ) {
        AttributeModifiersComponent modifiers = stack.getOrDefault(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.DEFAULT
        );

        return modifiers.applyOperations(attribute, baseValue, EquipmentSlot.MAINHAND);
    }

    private static double getSharpnessDamageBonus(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(SHARPNESS_ID)) {
                int level = entry.getIntValue();
                return level > 0 ? 0.5D * level + 0.5D : 0.0D;
            }
        }

        return 0.0D;
    }

    private static double getStatusEffectAttackDamageBonus(PlayerEntity player) {
        double bonus = 0.0D;

        StatusEffectInstance strength = player.getStatusEffect(StatusEffects.STRENGTH);
        if (strength != null) {
            bonus += CombatBalance.STRENGTH_ATTACK_DAMAGE_PER_LEVEL * (strength.getAmplifier() + 1);
        }

        StatusEffectInstance weakness = player.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness != null) {
            bonus -= CombatBalance.WEAKNESS_ATTACK_DAMAGE_PENALTY_PER_LEVEL * (weakness.getAmplifier() + 1);
        }

        return bonus;
    }

    private static void appendVanillaMainHandHeader(Consumer<Text> textConsumer) {
        textConsumer.accept(Text.empty());
        textConsumer.accept(Text.translatable("item.modifiers.mainhand").formatted(Formatting.GRAY));
    }

    private static void appendDamage(Consumer<Text> textConsumer, String damage) {
        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.weapon.attack_damage",
                damage
        ).formatted(Formatting.DARK_GREEN));
    }

    private static void appendAttackSpeed(Consumer<Text> textConsumer, String attackSpeed) {
        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.weapon.attack_speed",
                attackSpeed
        ).formatted(Formatting.DARK_GREEN));
    }

    private static void appendAttackRange(Consumer<Text> textConsumer, String attackRange) {
        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.weapon.attack_range",
                attackRange
        ).formatted(Formatting.DARK_GREEN));
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    private static String rangedWeaponDamageRange(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof CrossbowItem) {
            return CombatBalance.CROSSBOW_ARROW_DAMAGE_TOOLTIP;
        }

        return CombatBalance.BOW_ARROW_DAMAGE_TOOLTIP;
    }

    private static boolean hasMainHandCombatAttributes(ItemStack stack) {
        boolean[] found = {false};

        stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)
                    || attribute.equals(EntityAttributes.ATTACK_SPEED)
                    || attribute.equals(EntityAttributes.ENTITY_INTERACTION_RANGE)) {
                found[0] = true;
            }
        });

        return found[0];
    }

    private static String format(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        return rounded.stripTrailingZeros().toPlainString();
    }
}
