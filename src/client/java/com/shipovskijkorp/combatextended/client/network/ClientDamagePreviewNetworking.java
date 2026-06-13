package com.shipovskijkorp.combatextended.client.network;

import com.shipovskijkorp.combatextended.combat.tuning.CombatTooltipInputState;
import com.shipovskijkorp.combatextended.combat.tuning.ServerDamagePreviewBridge;
import com.shipovskijkorp.combatextended.network.DamagePreviewRequestPayload;
import com.shipovskijkorp.combatextended.network.DamagePreviewResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientDamagePreviewNetworking {
    private static final int REQUEST_INTERVAL_TICKS = 10;

    private static final Map<Integer, CacheEntry> CACHE = new HashMap<>();
    private static final Map<Integer, Integer> REQUEST_TO_STACK_KEY = new HashMap<>();
    private static int nextRequestId = 1;

    private ClientDamagePreviewNetworking() {
    }

    public static void init() {
        ServerDamagePreviewBridge.setProvider(ClientDamagePreviewNetworking::getPreview);

        ClientPlayNetworking.registerGlobalReceiver(DamagePreviewResponsePayload.ID, (payload, context) -> context.client().execute(() -> {
            Integer stackKey = REQUEST_TO_STACK_KEY.remove(payload.requestId());
            if (stackKey == null) {
                return;
            }

            CacheEntry entry = CACHE.computeIfAbsent(stackKey, ignored -> new CacheEntry());
            entry.result = new ServerDamagePreviewBridge.Result(
                    payload.baseAttackDamage(),
                    payload.minimumRangedDamage(),
                    payload.maximumRangedDamage(),
                    payload.rangedWeapon(),
                    payload.itemCompatible()
            );
            entry.lastResponseTick = getClientWorldTime();
        }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            clear();
            CombatTooltipInputState.setSingleplayer(client.isIntegratedServerRunning());
            CombatTooltipInputState.setServerCalculatorAvailable(
                    client.isIntegratedServerRunning() || ClientPlayNetworking.canSend(DamagePreviewRequestPayload.ID)
            );
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clear();
            CombatTooltipInputState.reset();
        });
    }

    private static Optional<ServerDamagePreviewBridge.Result> getPreview(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null || stack.isEmpty()) {
            return Optional.empty();
        }

        if (!CombatTooltipInputState.isServerCalculatorAvailable()) {
            return Optional.empty();
        }

        int stackKey = stackKey(stack);
        CacheEntry entry = CACHE.computeIfAbsent(stackKey, ignored -> new CacheEntry());
        long worldTime = getClientWorldTime();

        if (worldTime - entry.lastRequestTick >= REQUEST_INTERVAL_TICKS && ClientPlayNetworking.canSend(DamagePreviewRequestPayload.ID)) {
            int requestId = nextRequestId++;
            entry.lastRequestTick = worldTime;
            REQUEST_TO_STACK_KEY.put(requestId, stackKey);
            ClientPlayNetworking.send(new DamagePreviewRequestPayload(requestId, stack.copy()));
        }

        return Optional.ofNullable(entry.result);
    }

    private static int stackKey(ItemStack stack) {
        int result = Registries.ITEM.getId(stack.getItem()).hashCode();
        result = 31 * result + stack.getCount();
        result = 31 * result + stack.getComponents().hashCode();
        return result;
    }

    private static long getClientWorldTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null ? client.world.getTime() : 0L;
    }

    private static void clear() {
        CACHE.clear();
        REQUEST_TO_STACK_KEY.clear();
    }

    private static final class CacheEntry {
        private long lastRequestTick = Long.MIN_VALUE / 2L;
        private long lastResponseTick = Long.MIN_VALUE / 2L;
        private ServerDamagePreviewBridge.Result result;
    }
}
