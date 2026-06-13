package com.shipovskijkorp.combatextended.client;

import com.shipovskijkorp.combatextended.client.input.CombatExtendedKeyBindings;
import com.shipovskijkorp.combatextended.client.network.ClientDamagePreviewNetworking;
import net.fabricmc.api.ClientModInitializer;

public class CombatExtendedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CombatExtendedKeyBindings.init();
		ClientDamagePreviewNetworking.init();
	}
}