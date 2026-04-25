package com.shipovskijkorp.combatextended.combat.core;

import com.shipovskijkorp.combatextended.CombatExtended;

public final class CombatModules {
    private CombatModules() {
    }

    public static void init() {
        CombatExtended.LOGGER.info("Combat modules loaded: tuning, cooldowns, damage, weapons, anti-abuse.");
    }
}
