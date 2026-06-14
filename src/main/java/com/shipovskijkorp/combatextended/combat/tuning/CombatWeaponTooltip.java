package com.shipovskijkorp.combatextended.combat.tuning;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.api.CombatExtendedApi;
import com.shipovskijkorp.combatextended.api.CombatExtendedTooltipPhase;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CombatCompatibilityConfig;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CompatibilityDecision;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CompatibilityDecisionSource;
import com.shipovskijkorp.combatextended.combat.compatibility.puffishskills.PuffishSkillsCompatibility;
import com.shipovskijkorp.combatextended.combat.compatibility.heritage.HeritageOfGodsCompatibility;
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
        if (!CombatCompatibilityConfig.shouldShowCustomTooltip(stack)) {
            return false;
        }

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
        simulatedDamage = CombatExtendedApi.modifyMeleeDamage(player, stack, simulatedDamage);

        return sanitizeNonNegative(simulatedDamage);
    }

    public static double calculateServerRangedMinimumDamage(ItemStack stack, PlayerEntity player) {
        return calculateRangedDamageRange(stack, player).minimumDamage();
    }

    public static double calculateServerRangedMaximumDamage(ItemStack stack, PlayerEntity player) {
        return calculateRangedDamageRange(stack, player).maximumDamage();
    }

    public static boolean hasCeCompatibility(ItemStack stack) {
        return CombatCompatibilityConfig.hasCeCompatibility(stack);
    }

    private static void appendNormalTooltip(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        appendVanillaMainHandHeader(textConsumer);

        appendDamage(textConsumer, getBestCalculatorDamageDisplay(stack, player));

        if (isRangedWeapon(stack)) {
            CombatExtendedApi.appendTooltip(stack, player, textConsumer, CombatExtendedTooltipPhase.NORMAL);
            appendTabHint(textConsumer);
            return;
        }

        appendAttackSpeed(textConsumer, format(getMainHandAttributeValue(
                stack,
                EntityAttributes.ATTACK_SPEED,
                CombatBalance.VANILLA_PLAYER_BASE_ATTACK_SPEED
        )));
        CombatExtendedApi.appendTooltip(stack, player, textConsumer, CombatExtendedTooltipPhase.NORMAL);
        appendTabHint(textConsumer);
    }

    private static void appendCeDescription(
            ItemStack stack,
            PlayerEntity player,
            Consumer<Text> textConsumer
    ) {
        CompatibilityDecision compatibilityDecision = CombatCompatibilityConfig.resolve(stack);
        boolean itemCompatible = compatibilityDecision.isCompatibleForTooltipStatus();
        boolean serverCalculatorAvailable = CombatTooltipInputState.isServerCalculatorAvailable();

        appendCompatibilityStatus(textConsumer, "tooltip.combatextended.ce_status.mod", compatibilityDecision);
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
                    getServerCalculatorDamageDisplay(stack, player)
            ).formatted(Formatting.GRAY));
        }

        if (!itemCompatible || compatibilityDecision.isUserWhitelisted() || !serverCalculatorAvailable) {
            textConsumer.accept(Text.empty());
        }

        if (compatibilityDecision.isUserWhitelisted()) {
            if (compatibilityDecision.source() == CompatibilityDecisionSource.USER_ITEM_RULE) {
                textConsumer.accept(Text.translatable("tooltip.combatextended.warning.item_marked_compatible_by_user").formatted(Formatting.GOLD));
            } else {
                textConsumer.accept(Text.translatable("tooltip.combatextended.warning.mod_marked_compatible_by_user").formatted(Formatting.GOLD));
            }
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.values_may_differ").formatted(Formatting.GOLD));
        }

        if (!itemCompatible) {
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.no_item_compatibility").formatted(Formatting.RED));
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.values_may_differ").formatted(Formatting.RED));
        }

        if (!serverCalculatorAvailable) {
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.no_server_mod").formatted(Formatting.RED));
            textConsumer.accept(Text.translatable("tooltip.combatextended.warning.values_may_differ").formatted(Formatting.RED));
        }

        CombatExtendedApi.appendTooltip(stack, player, textConsumer, CombatExtendedTooltipPhase.CE_DESCRIPTION);

        textConsumer.accept(Text.empty());
        textConsumer.accept(createCtrlOriginalValuesHint());
    }

    private static Optional<ServerDamagePreviewBridge.Result> getServerPreview(ItemStack stack) {
        return ServerDamagePreviewBridge.getPreview(stack);
    }

    private static String getBestCalculatorDamageDisplay(ItemStack stack, PlayerEntity player) {
        if (CombatTooltipInputState.isServerCalculatorAvailable()) {
            return getServerCalculatorDamageDisplay(stack, player);
        }

        return getClientCalculatorDamageDisplay(stack, player);
    }

    private static String getServerCalculatorDamageDisplay(ItemStack stack, PlayerEntity player) {
        if (isRangedWeapon(stack)) {
            return getServerRangedDamageRange(stack, player);
        }

        return format(getBestServerBaseAttackDamage(stack, player));
    }

    private static double getBestServerBaseAttackDamage(ItemStack stack, PlayerEntity player) {
        return getServerPreview(stack)
                .map(ServerDamagePreviewBridge.Result::baseAttackDamage)
                .orElseGet(() -> calculateServerBaseAttackDamage(stack, player));
    }

    private static String getServerRangedDamageRange(ItemStack stack, PlayerEntity player) {
        Optional<ServerDamagePreviewBridge.Result> preview = getServerPreview(stack);
        if (preview.isPresent() && preview.get().rangedWeapon()) {
            return formatRangedDamage(preview.get().minimumRangedDamage(), preview.get().maximumRangedDamage());
        }

        return formatRangedDamage(
                calculateServerRangedMinimumDamage(stack, player),
                calculateServerRangedMaximumDamage(stack, player)
        );
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
        damage = CombatExtendedApi.modifyMeleeDamage(player, stack, damage);

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

    private static void appendCompatibilityStatus(Consumer<Text> textConsumer, String translationKey, CompatibilityDecision decision) {
        Formatting formatting;
        String mark;

        if (decision.isBuiltInCompatible()) {
            formatting = Formatting.GREEN;
            mark = CHECK_MARK;
        } else if (decision.isUserWhitelisted()) {
            formatting = Formatting.YELLOW;
            mark = CHECK_MARK;
        } else {
            formatting = Formatting.RED;
            mark = CROSS_MARK;
        }

        textConsumer.accept(Text.translatable(
                translationKey,
                mark
        ).formatted(formatting));
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

    public static boolean isRangedWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    private static String rangedWeaponDamageRange(ItemStack stack, PlayerEntity player) {
        RangedDamageRange range = calculateRangedDamageRange(stack, player);
        return formatRangedDamage(range.minimumDamage(), range.maximumDamage());
    }

    private static RangedDamageRange calculateRangedDamageRange(ItemStack stack, PlayerEntity player) {
        double[] heritageRange = HeritageOfGodsCompatibility.getRangedDamageRange(stack, player);
        if (heritageRange != null) {
            return new RangedDamageRange(
                    applyRangedDamageModifiers(player, stack, heritageRange[0]),
                    applyRangedDamageModifiers(player, stack, heritageRange[1])
            ).sanitized();
        }

        Item item = stack.getItem();

        if (item instanceof CrossbowItem) {
            double projectileSpeedMultiplier = getCrossbowProjectileSpeedMultiplier(player);
            return new RangedDamageRange(
                    applyRangedDamageModifiers(player, stack, CombatBalance.CROSSBOW_MINIMUM_ARROW_DAMAGE * projectileSpeedMultiplier),
                    applyRangedDamageModifiers(player, stack, CombatBalance.CROSSBOW_MAXIMUM_ARROW_DAMAGE * projectileSpeedMultiplier)
            ).sanitized();
        }

        return new RangedDamageRange(
                applyRangedDamageModifiers(player, stack, CombatBalance.BOW_MINIMUM_ARROW_DAMAGE),
                applyRangedDamageModifiers(player, stack, calculateBowMaximumCriticalDamage(stack, player))
        ).sanitized();
    }

    private static double applyRangedDamageModifiers(PlayerEntity player, ItemStack stack, double damage) {
        double modifiedDamage = player != null ? PuffishSkillsCompatibility.applyRangedDamageModifiers(player, damage) : damage;
        modifiedDamage = CombatExtendedApi.modifyRangedDamage(player, stack, modifiedDamage);
        return sanitizeNonNegative(modifiedDamage);
    }

    private static String formatRangedDamage(double minimumDamage, double maximumDamage) {
        RangedDamageRange range = new RangedDamageRange(minimumDamage, maximumDamage).sanitized();
        return format(range.minimumDamage()) + " - " + format(range.maximumDamage());
    }

    private static double calculateBowMaximumCriticalDamage(ItemStack stack, PlayerEntity player) {
        int powerLevel = getEnchantmentLevel(stack, POWER_ID);
        double arrowBaseDamage = CombatBalance.BOW_ARROW_BASE_DAMAGE;

        if (powerLevel > 0) {
            arrowBaseDamage += 0.5D * powerLevel + 0.5D;
        }

        int fullDrawDamage = (int) Math.ceil(getBowFullDrawArrowSpeed(player) * arrowBaseDamage);
        int maximumCriticalBonus = fullDrawDamage / 2 + 1;

        return fullDrawDamage + maximumCriticalBonus;
    }

    private static double getBowFullDrawArrowSpeed(PlayerEntity player) {
        double speed = CombatBalance.BOW_FULL_DRAW_ARROW_SPEED;
        return player != null ? PuffishSkillsCompatibility.applyBowProjectileSpeedModifiers(player, speed) : speed;
    }

    private static double getCrossbowProjectileSpeedMultiplier(PlayerEntity player) {
        if (player == null) {
            return 1.0D;
        }

        double modifiedSpeed = PuffishSkillsCompatibility.applyCrossbowProjectileSpeedModifiers(player, 1.0D);
        return sanitizeNumber(modifiedSpeed, 1.0D);
    }

    private static boolean hasMainHandCombatAttributes(ItemStack stack) {
        boolean[] found = {false};

        stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)
                    || attribute.equals(EntityAttributes.ATTACK_SPEED)) {
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

    private record RangedDamageRange(double minimumDamage, double maximumDamage) {
        private RangedDamageRange sanitized() {
            double sanitizedMinimum = sanitizeNonNegative(minimumDamage);
            double sanitizedMaximum = sanitizeNonNegative(maximumDamage);

            if (sanitizedMinimum > sanitizedMaximum) {
                return new RangedDamageRange(sanitizedMaximum, sanitizedMinimum);
            }

            return new RangedDamageRange(sanitizedMinimum, sanitizedMaximum);
        }
    }
}
