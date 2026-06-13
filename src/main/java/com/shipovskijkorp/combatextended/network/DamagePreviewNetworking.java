package com.shipovskijkorp.combatextended.network;

import com.shipovskijkorp.combatextended.combat.tuning.CombatWeaponTooltip;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DamagePreviewNetworking {
    private DamagePreviewNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(DamagePreviewRequestPayload.ID, DamagePreviewRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DamagePreviewResponsePayload.ID, DamagePreviewResponsePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DamagePreviewRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack stack = payload.stack().copy();

            context.server().execute(() -> {
                boolean rangedWeapon = CombatWeaponTooltip.isRangedWeapon(stack);
                ServerPlayNetworking.send(
                        player,
                        new DamagePreviewResponsePayload(
                                payload.requestId(),
                                CombatWeaponTooltip.calculateServerBaseAttackDamage(stack, player),
                                rangedWeapon ? CombatWeaponTooltip.calculateServerRangedMinimumDamage(stack, player) : 0.0D,
                                rangedWeapon ? CombatWeaponTooltip.calculateServerRangedMaximumDamage(stack, player) : 0.0D,
                                rangedWeapon,
                                CombatWeaponTooltip.hasCeCompatibility(stack)
                        )
                );
            });
        });
    }
}
