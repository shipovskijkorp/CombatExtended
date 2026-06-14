package com.shipovskijkorp.combatextended.combat.compatibility.heritage;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.api.CombatExtendedApi;
import com.shipovskijkorp.combatextended.api.CombatExtendedDamageHook;
import com.shipovskijkorp.combatextended.api.CombatExtendedTooltipHook;
import com.shipovskijkorp.combatextended.api.CombatExtendedTooltipPhase;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CombatCompatibilityConfig;
import com.shipovskijkorp.combatextended.combat.tuning.CombatWeaponTooltip;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Soft compatibility layer for Heritage of the Gods RPG.
 *
 * <p>Heritage hides the vanilla attribute section on its weapons and draws its own
 * statistic lines in {@code appendTooltip}. CE therefore cannot safely replace the
 * normal attribute block for these items. Instead this integration post-processes
 * only the Heritage {@code stat_*} lines and leaves every active/passive/Shift
 * description line exactly where Heritage placed it.</p>
 */
public final class HeritageOfGodsCompatibility {
    private static final String MOD_ID = "heritage";

    private static final Identifier MELEE_WEAPON_DAMAGE_ATTRIBUTE = Identifier.of(MOD_ID, "melee_weapon_damage");
    private static final Identifier RANGED_WEAPON_DAMAGE_ATTRIBUTE = Identifier.of(MOD_ID, "ranged_weapon_damage");
    private static final Identifier FREEZE_EFFECT = Identifier.of(MOD_ID, "freeze");

    private static final Identifier ANCIENT_TRIDENT = Identifier.of(MOD_ID, "ancient_trident");
    private static final Identifier DANZLEIF_AND_TYRFING = Identifier.of(MOD_ID, "danzleif_and_tyrfing");
    private static final Identifier MJOLNIR = Identifier.of(MOD_ID, "mjolnir");
    private static final Identifier NECROTIC_SCYTHE = Identifier.of(MOD_ID, "necrotic_scythe");
    private static final Identifier HEAVENLY_GUST_CROSSBOW = Identifier.of(MOD_ID, "heavenly_gust_crossbow");
    private static final Identifier GRIFFIN_SPEAR = Identifier.of(MOD_ID, "griffin_spear");
    private static final Identifier SPIRIT_SPEAR = Identifier.of(MOD_ID, "spirit_spear");
    private static final Identifier PHOENIX_BOW = Identifier.of(MOD_ID, "phoenix_bow");
    private static final Identifier CRYSTAL_STAFF = Identifier.of(MOD_ID, "crystal_staff");

    private static final Set<Identifier> HERITAGE_WEAPONS = Set.of(
            ANCIENT_TRIDENT,
            DANZLEIF_AND_TYRFING,
            MJOLNIR,
            NECROTIC_SCYTHE,
            HEAVENLY_GUST_CROSSBOW,
            GRIFFIN_SPEAR,
            SPIRIT_SPEAR,
            PHOENIX_BOW,
            CRYSTAL_STAFF
    );

    private static final Set<Identifier> HERITAGE_MELEE_WEAPONS = Set.of(
            ANCIENT_TRIDENT,
            DANZLEIF_AND_TYRFING,
            MJOLNIR,
            NECROTIC_SCYTHE,
            GRIFFIN_SPEAR,
            SPIRIT_SPEAR,
            CRYSTAL_STAFF
    );

    private static final Set<Identifier> HERITAGE_RANGED_WEAPONS = Set.of(
            PHOENIX_BOW,
            HEAVENLY_GUST_CROSSBOW
    );

    private static final double PHOENIX_BOW_FULL_DRAW_DAMAGE = 12.5D;
    private static final double PHOENIX_BOW_MINIMUM_PULL = 0.1D;
    private static final int PHOENIX_BOW_FULL_DRAW_TICKS = 20;
    private static final double HEAVENLY_GUST_CROSSBOW_SHOT_DAMAGE = 15.0D;
    private static final int HEAVENLY_GUST_CROSSBOW_RELOAD_TICKS = 25;
    private static final int HEAVENLY_GUST_CROSSBOW_QUICK_CHARGE_TICK_REDUCTION = 5;

    private static final double[] HERITAGE_SHARPNESS_BONUS = {0.0D, 0.10D, 0.15D, 0.20D, 0.25D, 0.30D};
    private static final double[] HERITAGE_POWER_BONUS = {0.0D, 0.11D, 0.165D, 0.21D, 0.265D, 0.32D};
    private static final double[] HERITAGE_FREEZE_SLOW = {0.0D, 0.07D, 0.14D};

    private static boolean initialized;
    private static boolean loaded;

    private HeritageOfGodsCompatibility() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        loaded = FabricLoader.getInstance().isModLoaded(MOD_ID);

        if (!loaded) {
            return;
        }

        CombatExtendedApi.registerCompatibleMod(MOD_ID);
        CombatExtendedApi.registerDamageHook(new HeritageDamageHook());
        CombatExtendedApi.registerTooltipHook(new HeritageTooltipHook());
        CombatExtended.LOGGER.info("Heritage of the Gods compatibility enabled.");
    }

    public static boolean shouldPatchTooltip(ItemStack stack) {
        return isLoaded()
                && isHeritageWeapon(stack)
                && CombatCompatibilityConfig.shouldShowCustomTooltip(stack);
    }

    public static boolean applyTooltipPatch(ItemStack stack, PlayerEntity player, List<Text> tooltip) {
        if (!shouldPatchTooltip(stack) || tooltip == null || tooltip.isEmpty()) {
            return false;
        }

        int firstStatIndex = -1;
        for (int index = 0; index < tooltip.size(); index++) {
            if (isHeritageStatLine(stack, tooltip.get(index))) {
                firstStatIndex = index;
                break;
            }
        }

        if (firstStatIndex < 0) {
            return false;
        }

        int insertIndex = firstStatIndex;
        for (int index = tooltip.size() - 1; index >= firstStatIndex; index--) {
            if (isHeritageStatLine(stack, tooltip.get(index))) {
                tooltip.remove(index);
            }
        }

        if (insertIndex > 0 && isEmptyLine(tooltip.get(insertIndex - 1))) {
            tooltip.remove(insertIndex - 1);
            insertIndex--;
        }

        List<Text> ceLines = new ArrayList<>();
        CombatWeaponTooltip.appendVanillaStyleAttributes(stack, player, ceLines::add);
        if (ceLines.isEmpty()) {
            return true;
        }

        tooltip.addAll(Math.min(insertIndex, tooltip.size()), ceLines);
        return true;
    }

    /**
     * Returns raw Heritage ranged weapon damage before CE/Puffish/API ranged modifiers are applied.
     */
    public static double[] getRangedDamageRange(ItemStack stack, PlayerEntity player) {
        Identifier itemId = getItemId(stack);
        if (!isLoaded() || itemId == null) {
            return null;
        }

        if (PHOENIX_BOW.equals(itemId)) {
            double fullDrawDamage = applyHeritagePower(stack, PHOENIX_BOW_FULL_DRAW_DAMAGE);
            if (player != null) {
                fullDrawDamage += getStrengthWeaknessDamageBonus(player);
            }
            fullDrawDamage = sanitizeNonNegative(fullDrawDamage);
            return new double[] {
                    sanitizeNonNegative(fullDrawDamage * PHOENIX_BOW_MINIMUM_PULL),
                    fullDrawDamage
            };
        }

        if (HEAVENLY_GUST_CROSSBOW.equals(itemId)) {
            double shotDamage = sanitizeNonNegative(applyHeritagePower(stack, HEAVENLY_GUST_CROSSBOW_SHOT_DAMAGE));
            return new double[] {shotDamage, shotDamage};
        }

        return null;
    }

    private static boolean isLoaded() {
        return loaded || (!initialized && FabricLoader.getInstance().isModLoaded(MOD_ID));
    }

    private static boolean isHeritageWeapon(ItemStack stack) {
        Identifier itemId = getItemId(stack);
        return itemId != null && HERITAGE_WEAPONS.contains(itemId);
    }

    private static boolean isHeritageMeleeWeapon(ItemStack stack) {
        Identifier itemId = getItemId(stack);
        return itemId != null && HERITAGE_MELEE_WEAPONS.contains(itemId);
    }

    private static boolean isHeritageRangedWeapon(ItemStack stack) {
        Identifier itemId = getItemId(stack);
        return itemId != null && HERITAGE_RANGED_WEAPONS.contains(itemId);
    }

    private static Identifier getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return Registries.ITEM.getId(stack.getItem());
    }

    private static boolean isHeritageStatLine(ItemStack stack, Text text) {
        if (text == null || !(text.getContent() instanceof TranslatableTextContent content)) {
            return false;
        }

        String translationKey = content.getKey();
        return translationKey != null && translationKey.startsWith(getStatTooltipPrefix(stack));
    }

    private static String getStatTooltipPrefix(ItemStack stack) {
        Identifier itemId = getItemId(stack);
        if (DANZLEIF_AND_TYRFING.equals(itemId)) {
            return "tooltip.heritage.danzleif_tyrfing.stat_";
        }
        if (itemId == null) {
            return "";
        }
        return "tooltip.heritage." + itemId.getPath() + ".stat_";
    }

    private static boolean isEmptyLine(Text text) {
        return text != null && text.getString().isBlank();
    }

    private static double applyHeritageMeleeDamageFormula(PlayerEntity player, ItemStack stack, double currentDamage) {
        double baseDamage = replaceVanillaSharpnessWithHeritageSharpness(stack, currentDamage);

        if (player != null) {
            baseDamage = withHeritageFlatDamage(player, baseDamage, true);
            baseDamage *= getConstantAttackMultiplier(player, true);
        }

        baseDamage = applyDullnessDamagePenalty(stack, baseDamage);
        return sanitizeNonNegative(baseDamage);
    }

    private static double applyHeritageRangedDamageFormula(PlayerEntity player, ItemStack stack, double currentDamage) {
        double baseDamage = sanitizeNonNegative(currentDamage);

        if (player != null) {
            baseDamage = withHeritageFlatDamage(player, baseDamage, false);
            baseDamage *= getConstantAttackMultiplier(player, false);
        }

        baseDamage = applyDullnessDamagePenalty(stack, baseDamage);
        return sanitizeNonNegative(baseDamage);
    }

    private static double replaceVanillaSharpnessWithHeritageSharpness(ItemStack stack, double currentDamage) {
        int sharpnessLevel = getEnchantmentLevel(stack, Enchantments.SHARPNESS);
        if (sharpnessLevel <= 0) {
            return sanitizeNonNegative(currentDamage);
        }

        double vanillaSharpnessBonus = 0.5D * sharpnessLevel + 0.5D;
        double damageWithoutVanillaSharpness = Math.max(0.0D, currentDamage - vanillaSharpnessBonus);
        return damageWithoutVanillaSharpness * (1.0D + valueFromTable(HERITAGE_SHARPNESS_BONUS, sharpnessLevel));
    }

    private static double applyHeritagePower(ItemStack stack, double damage) {
        int powerLevel = getEnchantmentLevel(stack, Enchantments.POWER);
        if (powerLevel <= 0) {
            return sanitizeNonNegative(damage);
        }

        return sanitizeNonNegative(damage) * (1.0D + valueFromTable(HERITAGE_POWER_BONUS, powerLevel));
    }

    private static double withHeritageFlatDamage(PlayerEntity player, double baseDamage, boolean melee) {
        double result = sanitizeNonNegative(baseDamage);
        if (result <= 0.0D) {
            return result;
        }

        result += melee ? getPvpInvestmentMeleeDamageBonus(player) : getPvpInvestmentRangedDamageBonus(player);
        result += baseDamage * Math.max(0.0D, getSamuraiMovementDamageBonusMultiplier(player));
        result = applyWeakeningFlatDamagePenalty(player, result);
        return sanitizeNonNegative(result);
    }

    private static double getConstantAttackMultiplier(PlayerEntity player, boolean melee) {
        double weaponMultiplier = getHeritageWeaponDamageMultiplier(player, melee);
        double comboMultiplier = getComboStrikesAttackMultiplier(player);
        double additiveBonus = Math.max(0.0D, weaponMultiplier) - 1.0D;
        additiveBonus += Math.max(0.0D, comboMultiplier) - 1.0D;
        return Math.max(0.0D, 1.0D + additiveBonus);
    }

    private static double getHeritageWeaponDamageMultiplier(PlayerEntity player, boolean melee) {
        Identifier attributeId = melee ? MELEE_WEAPON_DAMAGE_ATTRIBUTE : RANGED_WEAPON_DAMAGE_ATTRIBUTE;
        Optional<EntityAttribute> attribute = Registries.ATTRIBUTE.getOptionalValue(attributeId);
        if (attribute.isEmpty()) {
            return 1.0D;
        }

        RegistryEntry<EntityAttribute> attributeEntry = Registries.ATTRIBUTE.getEntry(attribute.get());
        EntityAttributeInstance instance = player.getAttributeInstance(attributeEntry);
        if (instance == null) {
            return 1.0D;
        }

        return sanitizeNumber(instance.getValue(), 1.0D);
    }

    private static double getPvpInvestmentMeleeDamageBonus(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return invokeNumber("heritage.talents.one.OneTalentBonuses", "getPvpInvestmentMeleeDamageBonus", 0.0D, serverPlayer);
        }
        return invokeNumber("heritage.talents.one.OneTalentBonuses", "getPvpInvestmentMeleeDamageBonus", 0.0D);
    }

    private static double getPvpInvestmentRangedDamageBonus(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return invokeNumber("heritage.talents.one.OneTalentBonuses", "getPvpInvestmentRangedDamageBonus", 0.0D, serverPlayer);
        }
        return invokeNumber("heritage.talents.one.OneTalentBonuses", "getPvpInvestmentRangedDamageBonus", 0.0D);
    }

    private static double getComboStrikesAttackMultiplier(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return invokeNumber("heritage.talents.one.OneTalentBonuses", "getComboStrikesAttackMultiplier", 1.0D, serverPlayer);
        }
        return 1.0D;
    }

    private static double getSamuraiMovementDamageBonusMultiplier(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return invokeNumber("heritage.rpg.abilities.samurai.SamuraiBladeDisciplinePassive", "getMovementDamageBonusMultiplier", 0.0D, serverPlayer);
        }
        return 0.0D;
    }

    private static double getClassAttackDrawSpeedBonus(PlayerEntity player) {
        if (player == null) {
            return 0.0D;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return invokeNumber("heritage.talents.one.OneTalentBonuses", "getClassAttackDrawSpeedBonus", 0.0D, serverPlayer);
        }
        return invokeNumber("heritage.talents.one.OneTalentBonuses", "getClassAttackDrawSpeedBonus", 0.0D);
    }

    private static double applyWeakeningFlatDamagePenalty(PlayerEntity player, double damage) {
        if (player == null || damage <= 0.0D) {
            return sanitizeNonNegative(damage);
        }
        return invokeNumber(
                "heritage.enchant.HeritageEnchantmentEffects",
                "applyWeakeningFlatDamagePenalty",
                damage,
                player,
                (float) damage
        );
    }

    private static double applyDullnessDamagePenalty(ItemStack stack, double damage) {
        if (stack == null || stack.isEmpty() || damage <= 0.0D) {
            return sanitizeNonNegative(damage);
        }
        return invokeNumber(
                "heritage.enchant.HeritageEnchantmentEffects",
                "applyDullnessDamagePenalty",
                damage,
                stack,
                (float) damage
        );
    }

    private static double getStrengthWeaknessDamageBonus(LivingEntity entity) {
        double bonus = 0.0D;

        StatusEffectInstance strength = entity.getStatusEffect(StatusEffects.STRENGTH);
        if (strength != null) {
            bonus += 3.0D * (strength.getAmplifier() + 1);
        }

        StatusEffectInstance weakness = entity.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness != null) {
            bonus -= 4.0D * (weakness.getAmplifier() + 1);
        }

        return bonus;
    }

    private static double getFreezeSlowPenalty(PlayerEntity player) {
        if (player == null) {
            return 0.0D;
        }

        Optional<StatusEffect> effect = Registries.STATUS_EFFECT.getOptionalValue(FREEZE_EFFECT);
        if (effect.isEmpty()) {
            return 0.0D;
        }

        RegistryEntry<StatusEffect> effectEntry = Registries.STATUS_EFFECT.getEntry(effect.get());
        StatusEffectInstance instance = player.getStatusEffect(effectEntry);
        if (instance == null) {
            return 0.0D;
        }

        return valueFromTable(HERITAGE_FREEZE_SLOW, instance.getAmplifier() + 1);
    }

    private static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantmentKey) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        ItemEnchantmentsComponent enchantments = stack.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(enchantmentKey)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }

    private static double valueFromTable(double[] values, int level) {
        if (values.length == 0 || level <= 0) {
            return 0.0D;
        }
        int index = Math.min(level, values.length - 1);
        return values[index];
    }

    private static double invokeNumber(String className, String methodName, double fallback, Object... args) {
        try {
            Class<?> targetClass = Class.forName(className);
            Method method = findStaticMethod(targetClass, methodName, args);
            if (method == null) {
                return fallback;
            }

            Object value = method.invoke(null, args);
            if (value instanceof Number number) {
                return sanitizeNumber(number.doubleValue(), fallback);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return fallback;
        }

        return fallback;
    }

    private static Method findStaticMethod(Class<?> targetClass, String methodName, Object[] args) {
        for (Method method : targetClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length) {
                continue;
            }
            if (parametersAcceptArgs(parameterTypes, args)) {
                return method;
            }
        }
        return null;
    }

    private static boolean parametersAcceptArgs(Class<?>[] parameterTypes, Object[] args) {
        for (int index = 0; index < parameterTypes.length; index++) {
            Object arg = args[index];
            if (arg == null) {
                continue;
            }

            Class<?> parameterType = wrapPrimitive(parameterTypes[index]);
            if (!parameterType.isAssignableFrom(arg.getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }

    private static double sanitizeNumber(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double sanitizeNonNegative(double value) {
        return Math.max(0.0D, sanitizeNumber(value, 0.0D));
    }

    private static String format(double value) {
        double safeValue = sanitizeNumber(value, 0.0D);
        BigDecimal rounded = BigDecimal.valueOf(safeValue).setScale(2, RoundingMode.HALF_UP);
        return rounded.stripTrailingZeros().toPlainString();
    }

    private static final class HeritageDamageHook implements CombatExtendedDamageHook {
        @Override
        public double modifyMeleeDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
            if (!isLoaded() || !isHeritageMeleeWeapon(stack)) {
                return currentDamage;
            }
            return applyHeritageMeleeDamageFormula(player, stack, currentDamage);
        }

        @Override
        public double modifyRangedDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
            if (!isLoaded() || !isHeritageRangedWeapon(stack)) {
                return currentDamage;
            }
            return applyHeritageRangedDamageFormula(player, stack, currentDamage);
        }
    }

    private static final class HeritageTooltipHook implements CombatExtendedTooltipHook {
        @Override
        public void appendTooltip(ItemStack stack, PlayerEntity player, java.util.function.Consumer<Text> textConsumer, CombatExtendedTooltipPhase phase) {
            if (phase != CombatExtendedTooltipPhase.NORMAL || !isLoaded()) {
                return;
            }

            Identifier itemId = getItemId(stack);
            if (PHOENIX_BOW.equals(itemId)) {
                textConsumer.accept(Text.translatable(
                        "tooltip.combatextended.heritage.phoenix_bow.draw_time",
                        format(getPhoenixBowFullDrawSeconds(player))
                ).formatted(Formatting.DARK_GREEN));
            } else if (HEAVENLY_GUST_CROSSBOW.equals(itemId)) {
                textConsumer.accept(Text.translatable(
                        "tooltip.combatextended.heritage.heavenly_gust_crossbow.reload_time",
                        format(getHeavenlyGustCrossbowReloadSeconds(stack, player))
                ).formatted(Formatting.DARK_GREEN));
            }
        }

        private static double getPhoenixBowFullDrawSeconds(PlayerEntity player) {
            double effectiveTicks = PHOENIX_BOW_FULL_DRAW_TICKS;
            double drawSpeedBonus = getClassAttackDrawSpeedBonus(player);
            if (drawSpeedBonus > 0.0D) {
                effectiveTicks /= 1.0D + drawSpeedBonus;
            }

            double freezeSlow = getFreezeSlowPenalty(player);
            if (freezeSlow > 0.0D) {
                effectiveTicks /= Math.max(0.01D, 1.0D - freezeSlow);
            }

            return effectiveTicks / 20.0D;
        }

        private static double getHeavenlyGustCrossbowReloadSeconds(ItemStack stack, PlayerEntity player) {
            int quickChargeLevel = getEnchantmentLevel(stack, Enchantments.QUICK_CHARGE);
            double reloadTicks = Math.max(1.0D, HEAVENLY_GUST_CROSSBOW_RELOAD_TICKS
                    - HEAVENLY_GUST_CROSSBOW_QUICK_CHARGE_TICK_REDUCTION * quickChargeLevel);

            double drawSpeedBonus = getClassAttackDrawSpeedBonus(player);
            if (drawSpeedBonus > 0.0D) {
                reloadTicks /= 1.0D + drawSpeedBonus;
            }

            double freezeSlow = getFreezeSlowPenalty(player);
            if (freezeSlow > 0.0D) {
                reloadTicks /= Math.max(0.01D, 1.0D - freezeSlow);
            }

            return reloadTicks / 20.0D;
        }
    }
}
