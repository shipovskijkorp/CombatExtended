package com.shipovskijkorp.combatextended;

import com.shipovskijkorp.combatextended.combat.core.CombatModules;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatExtended implements ModInitializer {
    public static final String MOD_ID = "combatextended";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CombatModules.init();
        LOGGER.info("Combat Extended initialized.");
    }
}
