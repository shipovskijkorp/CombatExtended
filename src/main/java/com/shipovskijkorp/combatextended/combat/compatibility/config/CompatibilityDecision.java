package com.shipovskijkorp.combatextended.combat.compatibility.config;

public record CompatibilityDecision(
        CompatibilityListType type,
        CompatibilityDecisionSource source
) {
    public boolean isBlacklisted() {
        return type == CompatibilityListType.BLACKLIST;
    }

    public boolean isBuiltInCompatible() {
        return source == CompatibilityDecisionSource.BUILT_IN_COMPATIBILITY;
    }

    public boolean isInternalNeutralItem() {
        return source == CompatibilityDecisionSource.INTERNAL_NEUTRAL_ITEM;
    }

    public boolean isUserWhitelisted() {
        return type == CompatibilityListType.WHITELIST
                && (source == CompatibilityDecisionSource.USER_ITEM_RULE
                || source == CompatibilityDecisionSource.USER_MOD_RULE);
    }

    public boolean isCompatibleForTooltipStatus() {
        return isBuiltInCompatible() || isUserWhitelisted();
    }

    public boolean isUserItemRule() {
        return source == CompatibilityDecisionSource.USER_ITEM_RULE;
    }

    public boolean isUserModRule() {
        return source == CompatibilityDecisionSource.USER_MOD_RULE;
    }
}
