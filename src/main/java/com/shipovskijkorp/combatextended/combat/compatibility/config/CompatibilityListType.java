package com.shipovskijkorp.combatextended.combat.compatibility.config;

public enum CompatibilityListType {
    WHITELIST,
    NEUTRAL,
    BLACKLIST;

    public static CompatibilityListType fromConfig(String value, CompatibilityListType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return CompatibilityListType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
