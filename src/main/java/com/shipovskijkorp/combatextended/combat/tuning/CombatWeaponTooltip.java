package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.combat.compatibility.puffishskills.PuffishSkillsCompatibility;
import com.shipovskijkorp.combatextended.combat.config.CombatBalance;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Consumer;

public final class CombatWeaponTooltip {
    private static final Identifier SHARPNESS_ID = Identifier.ofVanilla("sharpness");
    private static final Identifier POWER_ID = Identifier.ofVanilla("power");

    private static final String CHECK_MARK = "✓";
    private static final String CROSS_MARK = "✕";

    private CombatWeaponTooltip() {
    }

    public static boolean shouldReplaceAttributeTooltip(ItemStack stack) {
        if (isRangedWeapon(stack)) {
            return true;
        }

        return hasMainHandCombatAttributes(stack);
    }

    public static boolean shouldShowOriginalValues() {
        CombatTooltipInputState.refresh();
        return CombatTooltipInputState.isOriginalValuesTooltipMode();
    }

    public static void appendOriginalValuesHeader(Consumer<Text> textConsumer) {
        textConsumer.accept(Text.empty());
        textConsumer.accept(Text.translatable("tooltip.combatextended.original_values.header").formatted(Formatting.GRAY));
    }

    public static void appendVanillaStyleAttributes(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        CombatTooltipInputState.refresh();

        if (CombatTooltipInputState.isCeDescriptionDown()) {
            appendCeDescription(stack, player, textConsumer);
            return;
        }

        appendNormalTooltip(stack, player, textConsumer);
    }

    public static double calculateServerBaseAttackDamage(ItemStack stack, PlayerEntity player) {
        double baseAttackDamage = CombatBalance.VANILLA_PLAYER_BASE_ATTACK_DAMAGE;
        double simulatedDamage = getMainHandAttributeValue(
                stack,
                EntityAttributes.ATTACK_DAMAGE,
                baseAttackDamage
        );

        if (player != null) {
            ItemStack realMainHandStack = player.getMainHandStack();
            double realMainHandItemDamage = getMainHandAttributeValue(
                    realMainHandStack,
                    EntityAttributes.ATTACK_DAMAGE,
                    baseAttackDamage
            );
            double realLiveAttackDamage = sanitizeNumber(
                    player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE),
                    realMainHandItemDamage
            );

            simulatedDamage += realLiveAttackDamage - realMainHandItemDamage;
        }

        simulatedDamage += getSharpnessDamageBonus(stack);

        if (player != null) {
            simulatedDamage = PuffishSkillsCompatibility.applyMeleeDamageModifiers(player, stack, simulatedDamage);
        }

        return sanitizeNonNegative(simulatedDamage);
    }

    public static boolean hasCeCompatibility(ItemStack stack) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String namespace = itemId.getNamespace();
        return namespace.equals("minecraft") || namespace.equals(CombatExtended.MOD_ID);
    }

    private static void appendNormalTooltip(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        appendVanillaMainHandHeader(textConsumer);

        if (CombatTooltipInputState.isServerCalculatorAvailable()) {
            appendDamage(textConsumer, format(getBestServerBaseAttackDamage(stack, player)));
        } else if (isRangedWeapon(stack)) {
            appendDamage(textConsumer, rangedWeaponDamageRange(stack, player));
        } else {
            appendDamage(textConsumer, format(calculateClientMeleeAttackDamage(stack, player)));
        }

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
        appendTabHint(textConsumer);
    }

    private static void appendCeDescription(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        boolean itemCompatible = getServerPreview(stack)
                .map(ServerDamagePreviewBridge.Result::itemCompatible)
                .orElseGet(() -> hasCeCompatibility(stack));
        boolean serverCalculatorAvailable = CombatTooltipInputState.isServerCalculatorAvailable();

        appendCompatibilityStatus(textConsumer, "tooltip.combatextended.ce_status.mod", itemCompatible);
        if (!CombatTooltipInputState.isSingleplayer()) {
            appendCompatibilityStatus(textConsumer, "tooltip.combatextended.ce_status.server", serverCalculatorAvailable);
        }

        textConsumer.accept(Text.empty());
        textConsumer.accept(Text.translatable(
                "tooltip.combatextended.damage.client_calculator",
                getClientCalculatorDamageDisplay(stack, player)
        ).formatted(Formatting.GRAY));

        if (serverCalculatorAvailable) {
            textConsumer.accept(Text.translatable(
                    "tooltip.combatextended.damage.server_calculator",
                    format(getBestServerBaseAttackDamage(stack, player))
            ).formatted(Formatting.GRAY));
        }

        if (!itemCompatible || !serverCalculatorAvailable) {
            textConsumer.accept(Text.empty());
        }

        if (!itemCompatible) {
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.no_item_compatibility").formatted(Formatting.RED));
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.values_may_differ").formatted(Formatting.RED));
        }

        if (!serverCalculatorAvailable) {
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.no_server_mod").formatted(Formatting.RED));
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.values_may_differ").formatted(Formatting.RED));
        }

        textConsumer.accept(Text.empty());
        textConsumer.accept(createCtrlOriginalValuesHint());
    }

    private static Optional<ServerDamagePreviewBridge.Result> getServerPreview(ItemStack stack) {
        return ServerDamagePreviewBridge.getPreview(stack);
    }

    private static double getBestServerBaseAttackDamage(ItemStack stack, PlayerEntity player) {
        return getServerPreview(stack)
                .map(ServerDamagePreviewBridge.Result::baseAttackDamage)
                .orElseGet(() -> calculateServerBaseAttackDamage(stack, player));
    }

    private static String getClientCalculatorDamageDisplay(ItemStack stack, PlayerEntity player) {
        if (isRangedWeapon(stack)) {
            return rangedWeaponDamageRange(stack, player);
        }

        return format(calculateClientMeleeAttackDamage(stack, player));
    }

    private static double calculateClientMeleeAttackDamage(ItemStack stack, PlayerEntity player) {
        double damage = getMainHandAttributeValue(
                stack,
                EntityAttributes.ATTACK_DAMAGE,
                CombatBalance.VANILLA_PLAYER_BASE_ATTACK_DAMAGE
        );

        damage += getSharpnessDamageBonus(stack);

        if (player != null) {
            damage += getStatusEffectAttackDamageBonus(player);
            damage = PuffishSkillsCompatibility.applyMeleeDamageModifiers(player, stack, damage);
        }

        return sanitizeNonNegative(damage);
    }

    private static double getMainHandAttributeValue(
            ItemStack stack,
            RegistryEntry<EntityAttribute> attribute,
            double baseValue
    ) {
        AttributeModifiersComponent modifiers = stack.getOrDefault(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                AttributeModifiersComponent.DEFAULT
        );

        return sanitizeNumber(modifiers.applyOperations(attribute, baseValue, EquipmentSlot.MAINHAND), baseValue);
    }

    private static double getSharpnessDamageBonus(ItemStack stack) {
        int level = getEnchantmentLevel(stack, SHARPNESS_ID);
        return level > 0 ? 0.5D * level + 0.5D : 0.0D;
    }

    private static int getEnchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantmentId)) {
                return entry.getIntValue();
            }
        }

        return 0;
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

    private static void appendCompatibilityStatus(Consumer<Text> textConsumer, String translationKey, boolean enabled) {
        textConsumer.accept(Text.translatable(
                translationKey,
                enabled ? CHECK_MARK : CROSS_MARK
        ).formatted(enabled ? Formatting.GREEN : Formatting.RED));
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

    private static void appendTabHint(Consumer<Text> textConsumer) {
        textConsumer.accept(createKeyHint(
                "tooltip.combatextended.hold_tab_for_ce_description.prefix",
                CombatTooltipInputState.getCeDescriptionKeyName(),
                "tooltip.combatextended.hold_tab_for_ce_description.suffix"
        ));
    }

    private static Text createKeyHint(String prefixTranslationKey, String keyName, String suffixTranslationKey) {
        return Text.translatable(prefixTranslationKey)
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(keyName).formatted(Formatting.YELLOW))
                .append(Text.translatable(suffixTranslationKey).formatted(Formatting.DARK_GRAY));
    }

    private static Text createCtrlOriginalValuesHint() {
        return Text.translatable("tooltip.combatextended.hold_ctrl_for_original_values.prefix")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(CombatTooltipInputState.getOriginalValuesKeyName()).formatted(Formatting.YELLOW))
                .append(Text.translatable("tooltip.combatextended.hold_ctrl_for_original_values.middle")
                        .formatted(Formatting.DARK_GRAY))
                .append(Text.literal(CombatTooltipInputState.getCeDescriptionKeyName()).formatted(Formatting.YELLOW))
                .append(Text.translatable("tooltip.combatextended.hold_ctrl_for_original_values.suffix")
                        .formatted(Formatting.DARK_GRAY));
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    private static String rangedWeaponDamageRange(ItemStack stack, PlayerEntity player) {
        Item item = stack.getItem();

        if (item instanceof CrossbowItem) {
            return formatRangedDamage(
                    applyRangedDamageModifiers(player, CombatBalance.CROSSBOW_MINIMUM_ARROW_DAMAGE),
                    applyRangedDamageModifiers(player, CombatBalance.CROSSBOW_MAXIMUM_ARROW_DAMAGE)
            );
        }

        return formatRangedDamage(
                applyRangedDamageModifiers(player, CombatBalance.BOW_MINIMUM_ARROW_DAMAGE),
                applyRangedDamageModifiers(player, calculateBowMaximumCriticalDamage(stack))
        );
    }

    private static double applyRangedDamageModifiers(PlayerEntity player, double damage) {
        return player != null ? PuffishSkillsCompatibility.applyRangedDamageModifiers(player, damage) : damage;
    }

    private static String formatRangedDamage(double minimumDamage, double maximumDamage) {
        return format(minimumDamage) + " - " + format(maximumDamage);
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
        double safeValue = sanitizeNumber(value, 0.0D);
        BigDecimal rounded = BigDecimal.valueOf(safeValue).setScale(2, RoundingMode.HALF_UP);
        return rounded.stripTrailingZeros().toPlainString();
    }

    private static double sanitizeNumber(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double sanitizeNonNegative(double value) {
        return Math.max(0.0D, sanitizeNumber(value, 0.0D));
    }
}
