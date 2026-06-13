package com.shipovskijkorp.combatextended.combat.tuning;

/**
 * Client-owned tooltip input/network state exposed to common tooltip code without
 * making common classes depend on Minecraft client-only classes.
 */
public final class CombatTooltipInputState {
    private static volatile boolean ceDescriptionDown;
    private static volatile boolean originalValuesDown;
    private static volatile boolean serverCalculatorAvailable;
    private static volatile boolean singleplayer;
    private static volatile String ceDescriptionKeyName = "TAB";
    private static volatile String originalValuesKeyName = "CTRL";
    private static volatile Runnable refresher = () -> { };

    private CombatTooltipInputState() {
    }

    public static void refresh() {
        refresher.run();
    }

    public static void setRefresher(Runnable refresher) {
        CombatTooltipInputState.refresher = refresher != null ? refresher : () -> { };
    }

    public static boolean isCeDescriptionDown() {
        return ceDescriptionDown;
    }

    public static void setCeDescriptionDown(boolean ceDescriptionDown) {
        CombatTooltipInputState.ceDescriptionDown = ceDescriptionDown;
    }

    public static boolean isOriginalValuesDown() {
        return originalValuesDown;
    }

    public static void setOriginalValuesDown(boolean originalValuesDown) {
        CombatTooltipInputState.originalValuesDown = originalValuesDown;
    }

    public static boolean isOriginalValuesTooltipMode() {
        return ceDescriptionDown && originalValuesDown;
    }

    public static boolean isServerCalculatorAvailable() {
        return singleplayer || serverCalculatorAvailable;
    }

    public static void setServerCalculatorAvailable(boolean serverCalculatorAvailable) {
        CombatTooltipInputState.serverCalculatorAvailable = serverCalculatorAvailable;
    }

    public static boolean isSingleplayer() {
        return singleplayer;
    }

    public static void setSingleplayer(boolean singleplayer) {
        CombatTooltipInputState.singleplayer = singleplayer;
    }

    public static String getCeDescriptionKeyName() {
        return ceDescriptionKeyName;
    }

    public static void setCeDescriptionKeyName(String keyName) {
        CombatTooltipInputState.ceDescriptionKeyName = sanitizeKeyName(keyName, "TAB");
    }

    public static String getOriginalValuesKeyName() {
        return originalValuesKeyName;
    }

    public static void setOriginalValuesKeyName(String keyName) {
        CombatTooltipInputState.originalValuesKeyName = sanitizeKeyName(keyName, "CTRL");
    }

    public static void reset() {
        ceDescriptionDown = false;
        originalValuesDown = false;
        serverCalculatorAvailable = false;
        singleplayer = false;
        ceDescriptionKeyName = "TAB";
        originalValuesKeyName = "CTRL";
    }

    private static String sanitizeKeyName(String keyName, String fallback) {
        if (keyName == null || keyName.isBlank()) {
            return fallback;
        }

        return keyName;
    }
}
