package com.shipovskijkorp.combatextended.api;

import com.shipovskijkorp.combatextended.CombatExtended;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Public integration API for Combat Extended tooltip and damage preview support.
 *
 * <p>External mods can call this during their initializer to mark their items as
 * natively compatible with CE or to contribute extra calculator/tooltip logic.</p>
 */
public final class CombatExtendedApi {
    private static final Set<String> COMPATIBLE_MODS = new LinkedHashSet<>();
    private static final Set<Identifier> COMPATIBLE_ITEMS = new LinkedHashSet<>();
    private static final Set<Identifier> COMPATIBILITY_NEUTRAL_ITEMS = new LinkedHashSet<>();
    private static final CopyOnWriteArrayList<CombatExtendedDamageHook> DAMAGE_HOOKS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CombatExtendedTooltipHook> TOOLTIP_HOOKS = new CopyOnWriteArrayList<>();

    static {
        registerCompatibleMod("minecraft");
        registerCompatibleMod(CombatExtended.MOD_ID);
        registerCompatibleMod("scythes");
        registerCompatibleMod("advancednetherite");
    }

    private CombatExtendedApi() {
    }

    /**
     * Marks every item from the given mod id as natively compatible with CE.
     * User whitelist/neutral rules do not change this status; user blacklist rules still disable CE tooltips.
     */
    public static void registerCompatibleMod(String modId) {
        String normalized = normalizeModId(modId);
        if (!normalized.isBlank()) {
            COMPATIBLE_MODS.add(normalized);
        }
    }

    /**
     * Marks one item as natively compatible with CE, even if the whole mod is not marked compatible.
     */
    public static void registerCompatibleItem(Identifier itemId) {
        if (itemId != null) {
            COMPATIBLE_ITEMS.add(itemId);
        }
    }

    /**
     * Forces one item to be shown as not natively compatible, while keeping the custom CE tooltip enabled.
     * This is useful for one broken item inside an otherwise compatible mod.
     */
    public static void registerCompatibilityNeutralItem(Identifier itemId) {
        if (itemId != null) {
            COMPATIBILITY_NEUTRAL_ITEMS.add(itemId);
        }
    }

    public static boolean isCompatibleMod(String modId) {
        return COMPATIBLE_MODS.contains(normalizeModId(modId));
    }

    public static boolean isCompatibleItem(Identifier itemId) {
        if (itemId == null || isCompatibilityNeutralItem(itemId)) {
            return false;
        }

        return COMPATIBLE_ITEMS.contains(itemId) || isCompatibleMod(itemId.getNamespace());
    }

    public static boolean isCompatibilityNeutralItem(Identifier itemId) {
        return itemId != null && COMPATIBILITY_NEUTRAL_ITEMS.contains(itemId);
    }

    public static Set<String> getCompatibleMods() {
        return Collections.unmodifiableSet(COMPATIBLE_MODS);
    }

    public static Set<Identifier> getCompatibleItems() {
        return Collections.unmodifiableSet(COMPATIBLE_ITEMS);
    }

    public static Set<Identifier> getCompatibilityNeutralItems() {
        return Collections.unmodifiableSet(COMPATIBILITY_NEUTRAL_ITEMS);
    }

    public static void registerDamageHook(CombatExtendedDamageHook hook) {
        if (hook != null && !DAMAGE_HOOKS.contains(hook)) {
            DAMAGE_HOOKS.add(hook);
        }
    }

    public static void registerTooltipHook(CombatExtendedTooltipHook hook) {
        if (hook != null && !TOOLTIP_HOOKS.contains(hook)) {
            TOOLTIP_HOOKS.add(hook);
        }
    }

    public static double modifyMeleeDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
        double result = currentDamage;
        for (CombatExtendedDamageHook hook : DAMAGE_HOOKS) {
            result = sanitize(hook.modifyMeleeDamage(player, stack, result), result);
        }
        return result;
    }

    public static double modifyRangedDamage(PlayerEntity player, ItemStack stack, double currentDamage) {
        double result = currentDamage;
        for (CombatExtendedDamageHook hook : DAMAGE_HOOKS) {
            result = sanitize(hook.modifyRangedDamage(player, stack, result), result);
        }
        return result;
    }

    public static void appendTooltip(ItemStack stack, PlayerEntity player, Consumer<Text> textConsumer, CombatExtendedTooltipPhase phase) {
        for (CombatExtendedTooltipHook hook : TOOLTIP_HOOKS) {
            hook.appendTooltip(stack, player, textConsumer, phase);
        }
    }

    private static double sanitize(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static String normalizeModId(String modId) {
        return modId == null ? "" : modId.trim().toLowerCase(Locale.ROOT);
    }
}
