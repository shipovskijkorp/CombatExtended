package com.shipovskijkorp.combatextended.combat.cooldown;

public final class TotemCooldowns {
    private TotemCooldowns() {
    }

    public static final int TOTEM_COOLDOWN_SECONDS = 15;
    public static final int TICKS_PER_SECOND = 20;
    public static final int TOTEM_COOLDOWN_TICKS = TOTEM_COOLDOWN_SECONDS * TICKS_PER_SECOND;
}
