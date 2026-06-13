package com.shipovskijkorp.combatextended.combat.tuning;

import net.minecraft.item.ItemStack;

import java.util.Optional;

/**
 * Common-side bridge used by the tooltip renderer to ask client networking for
 * the latest server-authoritative damage preview without importing client classes.
 */
public final class ServerDamagePreviewBridge {
    private static Provider provider = stack -> Optional.empty();

    private ServerDamagePreviewBridge() {
    }

    public static Optional<Result> getPreview(ItemStack stack) {
        return provider.getPreview(stack);
    }

    public static void setProvider(Provider provider) {
        ServerDamagePreviewBridge.provider = provider != null ? provider : stack -> Optional.empty();
    }

    @FunctionalInterface
    public interface Provider {
        Optional<Result> getPreview(ItemStack stack);
    }

    public record Result(double baseAttackDamage, boolean itemCompatible) {
    }
}
