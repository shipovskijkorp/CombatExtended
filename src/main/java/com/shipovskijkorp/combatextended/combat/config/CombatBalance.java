package com.shipovskijkorp.combatextended.combat.config;

public final class CombatBalance {
    private CombatBalance() {
    }

    /** Desired final melee damage for the trident in vanilla-style combat math. */
    public static final double TRIDENT_MELEE_DAMAGE = 9.0D;

    /** Desired attacks per second shown/used by the vanilla attack speed attribute. */
    public static final double TRIDENT_ATTACK_SPEED = 1.2D;

    /** Desired entity attack/interact reach while the trident is held in the main hand. */
    public static final double TRIDENT_ATTACK_RANGE = 4D;

    /** Vanilla ranged damage tooltip values. These do not change projectile damage. */
    public static final double BOW_MINIMUM_ARROW_DAMAGE = 1.0D;
    public static final double BOW_ARROW_BASE_DAMAGE = 2.0D;
    public static final double BOW_FULL_DRAW_ARROW_SPEED = 3.0D;
    public static final double CROSSBOW_MINIMUM_ARROW_DAMAGE = 6.0D;
    public static final double CROSSBOW_MAXIMUM_ARROW_DAMAGE = 11.0D;

    /** Vanilla-style attack damage modifiers from status effects. */
    public static final double STRENGTH_ATTACK_DAMAGE_PER_LEVEL = 3.0D;
    public static final double WEAKNESS_ATTACK_DAMAGE_PENALTY_PER_LEVEL = 4.0D;

    /** Player damage multiplier for end crystal, bed and respawn anchor explosions. */
    public static final float SPECIAL_EXPLOSION_PLAYER_DAMAGE_MULTIPLIER = 1.0F / 3.0F;

    /** A quarter-second cooldown at 20 TPS. */
    public static final int BOW_COOLDOWN_TICKS = 5;

    /** Missed melee attacks recover 30% faster, so only 70% of the delay remains. */
    public static final double MISSED_ATTACK_COOLDOWN_REDUCTION = 0.30D;
    public static final double MISSED_ATTACK_COOLDOWN_MULTIPLIER = 1.0D - MISSED_ATTACK_COOLDOWN_REDUCTION;

    public static final double VANILLA_PLAYER_BASE_ATTACK_DAMAGE = 1.0D;
    public static final double VANILLA_PLAYER_BASE_ATTACK_SPEED = 4.0D;
    public static final double VANILLA_PLAYER_ENTITY_INTERACTION_RANGE = 3.0D;

    public static double tridentAttackDamageModifier() {
        return TRIDENT_MELEE_DAMAGE - VANILLA_PLAYER_BASE_ATTACK_DAMAGE;
    }

    public static double tridentAttackSpeedModifier() {
        return TRIDENT_ATTACK_SPEED - VANILLA_PLAYER_BASE_ATTACK_SPEED;
    }

    public static double tridentAttackRangeModifier() {
        return TRIDENT_ATTACK_RANGE - VANILLA_PLAYER_ENTITY_INTERACTION_RANGE;
    }
}
