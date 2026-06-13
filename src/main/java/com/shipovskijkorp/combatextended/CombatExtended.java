package com.shipovskijkorp.combatextended;

import com.shipovskijkorp.combatextended.combat.core.CombatModules;
import com.shipovskijkorp.combatextended.combat.compatibility.config.CombatCompatibilityConfig;
import com.shipovskijkorp.combatextended.network.DamagePreviewNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatExtended implements ModInitializer {
    public static final String MOD_ID = "combatextended";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CombatCompatibilityConfig.init();
        CombatModules.init();
        DamagePreviewNetworking.init();
        LOGGER.info("Combat Extended initialized.");
    }
}
