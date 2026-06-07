package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

public final class CombatWeaponTooltip {
    private static final Identifier SHARPNESS_ID = Identifier.withDefaultNamespace("sharpness");
    private static final Identifier POWER_ID = Identifier.withDefaultNamespace("power");

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
            Player player,
            Consumer<Component> textConsumer
    ) {
        appendVanillaMainHandHeader(textConsumer);

        if (isRangedWeapon(stack)) {
            appendDamage(textConsumer, rangedWeaponDamageRange(stack));
            return;
        }

        appendDamage(textConsumer, format(calculateMeleeAttackDamage(stack, player)));
        appendAttackSpeed(textConsumer, format(getMainHandAttributeValue(
                stack,
                Attributes.ATTACK_SPEED,
                CombatBalance.VANILLA_PLAYER_BASE_ATTACK_SPEED
        )));
        appendAttackRange(textConsumer, format(getMainHandAttributeValue(
                stack,
                Attributes.ENTITY_INTERACTION_RANGE,
                CombatBalance.VANILLA_PLAYER_ENTITY_INTERACTION_RANGE
        )));
    }

    private static double calculateMeleeAttackDamage(ItemStack stack, Player player) {
        double damage = getMainHandAttributeValue(
                stack,
                Attributes.ATTACK_DAMAGE,
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
            Holder<Attribute> attribute,
            double baseValue
    ) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY
        );

        return modifiers.compute(attribute, baseValue, EquipmentSlot.MAINHAND);
    }

    private static double getSharpnessDamageBonus(ItemStack stack) {
        int level = getEnchantmentLevel(stack, SHARPNESS_ID);
        return level > 0 ? 0.5D * level + 0.5D : 0.0D;
    }

    private static int getEnchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(enchantmentId)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }

    private static double getStatusEffectAttackDamageBonus(Player player) {
        double bonus = 0.0D;

        MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
        if (strength != null) {
            bonus += CombatBalance.STRENGTH_ATTACK_DAMAGE_PER_LEVEL * (strength.getAmplifier() + 1);
        }

        MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
        if (weakness != null) {
            bonus -= CombatBalance.WEAKNESS_ATTACK_DAMAGE_PENALTY_PER_LEVEL * (weakness.getAmplifier() + 1);
        }

        return bonus;
    }

    private static void appendVanillaMainHandHeader(Consumer<Component> textConsumer) {
        textConsumer.accept(Component.empty());
        textConsumer.accept(Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY));
    }

    private static void appendDamage(Consumer<Component> textConsumer, String damage) {
        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.weapon.attack_damage",
                damage
        ).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void appendAttackSpeed(Consumer<Component> textConsumer, String attackSpeed) {
        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.weapon.attack_speed",
                attackSpeed
        ).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static void appendAttackRange(Consumer<Component> textConsumer, String attackRange) {
        textConsumer.accept(Component.translatable(
                "tooltip.combatextended.weapon.attack_range",
                attackRange
        ).withStyle(ChatFormatting.DARK_GREEN));
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

        return CombatBalance.BOW_MINIMUM_ARROW_DAMAGE_TOOLTIP + " - " + format(calculateBowMaximumCriticalDamage(stack));
    }

    private static double calculateBowMaximumCriticalDamage(ItemStack stack) {
        int powerLevel = getEnchantmentLevel(stack, POWER_ID);
        double arrowBaseDamage = CombatBalance.BOW_ARROW_BASE_DAMAGE;

        if (powerLevel > 0) {
            arrowBaseDamage += 0.5D * powerLevel + 0.5D;
        }

        int fullDrawDamage = (int) Math.ceil(CombatBalance.BOW_FULL_DRAW_ARROW_SPEED * arrowBaseDamage);
        int maximumCriticalBonus = fullDrawDamage / 2 + 1;

        return fullDrawDamage + maximumCriticalBonus;
    }

    private static boolean hasMainHandCombatAttributes(ItemStack stack) {
        return attributeDiffersFromBase(stack, Attributes.ATTACK_DAMAGE, CombatBalance.VANILLA_PLAYER_BASE_ATTACK_DAMAGE)
                || attributeDiffersFromBase(stack, Attributes.ATTACK_SPEED, CombatBalance.VANILLA_PLAYER_BASE_ATTACK_SPEED)
                || attributeDiffersFromBase(stack, Attributes.ENTITY_INTERACTION_RANGE, CombatBalance.VANILLA_PLAYER_ENTITY_INTERACTION_RANGE);
    }

    private static boolean attributeDiffersFromBase(ItemStack stack, Holder<Attribute> attribute, double baseValue) {
        return Double.compare(getMainHandAttributeValue(stack, attribute, baseValue), baseValue) != 0;
    }

    private static String format(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        return rounded.stripTrailingZeros().toPlainString();
    }
}
