package com.shipovskijkorp.combatextended.combat.compatibility.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.shipovskijkorp.combatextended.CombatExtended;
import com.shipovskijkorp.combatextended.api.CombatExtendedApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class CombatCompatibilityConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(CombatExtended.MOD_ID)
            .resolve("compatibility.json");

    private static final ConfigData DATA = new ConfigData();
    private static boolean loaded;

    private CombatCompatibilityConfig() {
    }

    public static synchronized void init() {
        load();
    }

    public static synchronized void load() {
        DATA.mods.clear();
        DATA.items.clear();
        DATA.version = 1;

        if (!Files.exists(CONFIG_PATH)) {
            loaded = true;
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                loaded = true;
                save();
                return;
            }

            if (root.has("version")) {
                DATA.version = Math.max(1, root.get("version").getAsInt());
            }

            readRules(root, "mods", DATA.mods);
            readRules(root, "items", DATA.items);
            loaded = true;
        } catch (Exception exception) {
            CombatExtended.LOGGER.warn("Failed to load Combat Extended compatibility config, using defaults.", exception);
            loaded = true;
        }
    }

    public static synchronized void save() {
        ensureLoaded();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(DATA.toJson(), writer);
            }
        } catch (IOException exception) {
            CombatExtended.LOGGER.warn("Failed to save Combat Extended compatibility config.", exception);
        }
    }

    public static synchronized Map<String, CompatibilityListType> getModRules() {
        ensureLoaded();
        return Collections.unmodifiableMap(new TreeMap<>(DATA.mods));
    }

    public static synchronized Map<String, CompatibilityListType> getItemRules() {
        ensureLoaded();
        return Collections.unmodifiableMap(new TreeMap<>(DATA.items));
    }

    public static synchronized Optional<CompatibilityListType> getModRule(String modId) {
        ensureLoaded();
        return Optional.ofNullable(DATA.mods.get(normalizeKey(modId)));
    }

    public static synchronized Optional<CompatibilityListType> getItemRule(Identifier itemId) {
        ensureLoaded();
        return Optional.ofNullable(DATA.items.get(itemId.toString()));
    }

    public static synchronized void setModRule(String modId, CompatibilityListType type) {
        ensureLoaded();
        DATA.mods.put(normalizeKey(modId), type);
        save();
    }

    public static synchronized void setItemRule(Identifier itemId, CompatibilityListType type) {
        ensureLoaded();
        DATA.items.put(itemId.toString(), type);
        save();
    }

    public static synchronized void clearModRule(String modId) {
        ensureLoaded();
        DATA.mods.remove(normalizeKey(modId));
        save();
    }

    public static synchronized void clearItemRule(Identifier itemId) {
        ensureLoaded();
        DATA.items.remove(itemId.toString());
        save();
    }

    public static CompatibilityDecision resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompatibilityDecision(CompatibilityListType.NEUTRAL, CompatibilityDecisionSource.DEFAULT_NEUTRAL);
        }

        return resolve(Registries.ITEM.getId(stack.getItem()));
    }

    public static synchronized CompatibilityDecision resolve(Identifier itemId) {
        ensureLoaded();

        String itemKey = itemId.toString();
        String namespace = itemId.getNamespace();
        CompatibilityListType itemRule = DATA.items.get(itemKey);
        CompatibilityListType modRule = DATA.mods.get(normalizeKey(namespace));
        boolean internalNeutralItem = CombatExtendedApi.isCompatibilityNeutralItem(itemId);
        boolean registeredCompatibleItem = CombatExtendedApi.isCompatibleItem(itemId);
        boolean lockedByInternalRule = internalNeutralItem || registeredCompatibleItem;

        if (itemRule == CompatibilityListType.BLACKLIST) {
            return new CompatibilityDecision(CompatibilityListType.BLACKLIST, CompatibilityDecisionSource.USER_ITEM_RULE);
        }

        if (itemRule != null && !lockedByInternalRule) {
            return new CompatibilityDecision(itemRule, CompatibilityDecisionSource.USER_ITEM_RULE);
        }

        if (modRule == CompatibilityListType.BLACKLIST) {
            return new CompatibilityDecision(CompatibilityListType.BLACKLIST, CompatibilityDecisionSource.USER_MOD_RULE);
        }

        if (internalNeutralItem) {
            return new CompatibilityDecision(CompatibilityListType.NEUTRAL, CompatibilityDecisionSource.INTERNAL_NEUTRAL_ITEM);
        }

        if (registeredCompatibleItem) {
            return new CompatibilityDecision(CompatibilityListType.WHITELIST, CompatibilityDecisionSource.BUILT_IN_COMPATIBILITY);
        }

        if (itemRule != null) {
            return new CompatibilityDecision(itemRule, CompatibilityDecisionSource.USER_ITEM_RULE);
        }


        if (modRule != null) {
            return new CompatibilityDecision(modRule, CompatibilityDecisionSource.USER_MOD_RULE);
        }

        return new CompatibilityDecision(CompatibilityListType.NEUTRAL, CompatibilityDecisionSource.DEFAULT_NEUTRAL);
    }

    public static synchronized CompatibilityDecision resolveMod(String modId) {
        ensureLoaded();

        String normalizedModId = normalizeKey(modId);
        CompatibilityListType modRule = DATA.mods.get(normalizedModId);
        if (modRule == CompatibilityListType.BLACKLIST) {
            return new CompatibilityDecision(CompatibilityListType.BLACKLIST, CompatibilityDecisionSource.USER_MOD_RULE);
        }

        if (CombatExtendedApi.isCompatibleMod(normalizedModId)) {
            return new CompatibilityDecision(CompatibilityListType.WHITELIST, CompatibilityDecisionSource.BUILT_IN_COMPATIBILITY);
        }

        if (modRule != null) {
            return new CompatibilityDecision(modRule, CompatibilityDecisionSource.USER_MOD_RULE);
        }

        return new CompatibilityDecision(CompatibilityListType.NEUTRAL, CompatibilityDecisionSource.DEFAULT_NEUTRAL);
    }

    public static boolean shouldShowCustomTooltip(ItemStack stack) {
        return !resolve(stack).isBlacklisted();
    }

    public static boolean hasCeCompatibility(ItemStack stack) {
        return resolve(stack).isCompatibleForTooltipStatus();
    }

    public static boolean hasRegisteredCompatibility(String modId) {
        return CombatExtendedApi.isCompatibleMod(modId);
    }

    public static boolean hasRegisteredCompatibility(Identifier itemId) {
        return CombatExtendedApi.isCompatibleItem(itemId);
    }

    public static boolean isInternalNeutralItem(Identifier itemId) {
        return CombatExtendedApi.isCompatibilityNeutralItem(itemId);
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void readRules(JsonObject root, String name, Map<String, CompatibilityListType> target) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            return;
        }

        JsonObject object = root.getAsJsonObject(name);
        object.entrySet().forEach(entry -> {
            CompatibilityListType type = CompatibilityListType.fromConfig(entry.getValue().getAsString(), null);
            if (type != null) {
                target.put(normalizeKey(entry.getKey()), type);
            }
        });
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ConfigData {
        private int version = 1;
        private final Map<String, CompatibilityListType> mods = new HashMap<>();
        private final Map<String, CompatibilityListType> items = new HashMap<>();

        private JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("version", version);
            root.add("mods", mapToJson(mods));
            root.add("items", mapToJson(items));
            return root;
        }

        private JsonObject mapToJson(Map<String, CompatibilityListType> source) {
            JsonObject object = new JsonObject();
            new TreeMap<>(source).forEach((key, value) -> object.addProperty(key, value.name()));
            return object;
        }
    }
}
