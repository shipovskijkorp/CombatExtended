package com.shipovskijkorp.combatextended.client.input;

import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.combat.tuning.CombatTooltipInputState;
import com.shipovskijkorp.combatextended.network.DamagePreviewRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CombatExtendedKeyBindings {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(CombatExtended.MOD_ID, "keybindings"));

    private static KeyBinding showCeDescriptionKey;
    private static KeyBinding showOriginalValuesKey;

    private CombatExtendedKeyBindings() {
    }

    public static void init() {
        showCeDescriptionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.combatextended.show_ce_description",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                CATEGORY
        ));

        showOriginalValuesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.combatextended.show_original_values",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY
        ));

        CombatTooltipInputState.setRefresher(() -> updateInputState(MinecraftClient.getInstance()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            updateInputState(client);

            CombatTooltipInputState.setSingleplayer(client.isIntegratedServerRunning());

            if (client.player == null || client.getNetworkHandler() == null) {
                CombatTooltipInputState.setServerCalculatorAvailable(client.isIntegratedServerRunning());
                return;
            }

            CombatTooltipInputState.setServerCalculatorAvailable(
                    client.isIntegratedServerRunning() || ClientPlayNetworking.canSend(DamagePreviewRequestPayload.ID)
            );
        });

        CombatExtended.LOGGER.debug("Combat Extended key bindings initialized.");
    }

    /**
     * KeyBinding#isPressed() may stay false for keys that are pressed while an inventory screen is open.
     * The CE tooltip is rendered mostly inside screens, so we also poll the currently bound key directly.
     */
    public static void updateInputState(MinecraftClient client) {
        CombatTooltipInputState.setCeDescriptionDown(isActuallyPressed(client, showCeDescriptionKey));
        CombatTooltipInputState.setOriginalValuesDown(isActuallyPressed(client, showOriginalValuesKey));
        CombatTooltipInputState.setCeDescriptionKeyName(getKeyName(showCeDescriptionKey, "TAB"));
        CombatTooltipInputState.setOriginalValuesKeyName(getKeyName(showOriginalValuesKey, "CTRL"));
    }

    private static boolean isActuallyPressed(MinecraftClient client, KeyBinding keyBinding) {
        if (client == null || keyBinding == null) {
            return false;
        }

        if (keyBinding.isPressed()) {
            return true;
        }

        if (client.getWindow() == null) {
            return false;
        }

        InputUtil.Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
        if (boundKey == null || boundKey.getCode() == -1) {
            return false;
        }

        long handle = client.getWindow().getHandle();
        InputUtil.Type type = boundKey.getCategory();

        if (type == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }

        if (type == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(handle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }

        return false;
    }

    private static String getKeyName(KeyBinding keyBinding, String fallback) {
        if (keyBinding == null) {
            return fallback;
        }

        String name = keyBinding.getBoundKeyLocalizedText().getString();
        if (name == null || name.isBlank()) {
            return fallback;
        }

        return name;
    }
}
