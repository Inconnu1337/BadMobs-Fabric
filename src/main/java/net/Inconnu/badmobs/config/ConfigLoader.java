package net.Inconnu.badmobs.config;

import com.google.gson.*;
import net.Inconnu.badmobs.Constants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configFile;

    public ConfigLoader() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve("badmobs.json");
    }

    public Map<EntityType<?>, SpawnConfig> load() {
        Map<EntityType<?>, SpawnConfig> configs = new LinkedHashMap<>();
        JsonObject existing = null;

        if (configFile.toFile().exists()) {
            try (Reader reader = new FileReader(configFile.toFile())) {
                existing = GSON.fromJson(reader, JsonObject.class);
            } catch (Exception e) {
                Constants.LOG.error("Failed to read badmobs.json", e);
            }
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!shouldInclude(type, id))
                continue;

            String key = id.toString();
            Set<MobSpawnType> disallowed = new HashSet<>();

            if (existing != null && existing.has(key)) {
                JsonObject entry = existing.getAsJsonObject(key);

                if (entry.has("disallowedTypes")) {
                    JsonArray array = entry.getAsJsonArray("disallowedTypes");
                    for (JsonElement el : array) {
                        try {
                            disallowed.add(MobSpawnType.valueOf(el.getAsString().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            Constants.LOG.warn("Unknown spawn type: {}", el.getAsString());
                        }
                    }
                } else {
                    migrateOldFormat(entry, disallowed);
                }
            }
            configs.put(type, new SpawnConfig(disallowed));
        }

        save(configs);
        return configs;
    }

    public void save(Map<EntityType<?>, SpawnConfig> configs) {
        JsonObject root = new JsonObject();

        JsonObject info = new JsonObject();
        info.addProperty("description", "List types you want to block in 'disallowedTypes'.");

        JsonObject typeHelp = new JsonObject();
        for (MobSpawnType type : MobSpawnType.values()) {
            typeHelp.addProperty(type.name(), getHelpText(type));
        }
        info.add("available_types_help", typeHelp);
        root.add("_comment", info);

        for (Map.Entry<EntityType<?>, SpawnConfig> entry : configs.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey());

            JsonObject obj = new JsonObject();
            JsonArray array = new JsonArray();
            entry.getValue().getDisallowedTypes().stream()
                    .map(Enum::name)
                    .sorted()
                    .forEach(array::add);

            obj.add("disallowedTypes", array);
            root.add(id.toString(), obj);
        }

        try {
            configFile.getParent().toFile().mkdirs();
            try (Writer writer = new FileWriter(configFile.toFile())) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }

    private String getHelpText(MobSpawnType type) {
        return switch (type) {
            case NATURAL -> "Normal world spawning (darkness, grass, etc.)";
            case CHUNK_GENERATION -> "Spawned when a new chunk is created";
            case SPAWNER -> "Monster Spawner blocks";
            case STRUCTURE -> "Spawned as part of a structure (Witch huts, etc.)";
            case BREEDING -> "Animals falling in love";
            case MOB_SUMMONED -> "Summoned by other mobs (Illagers, Vexes)";
            case JOCKEY -> "Spider jockeys and similar";
            case EVENT -> "Raids or special world events";
            case REINFORCEMENT -> "Zombie reinforcements";
            case PATROL -> "Illager patrols";
            case SPAWN_EGG -> "Creative mode spawn eggs";
            case COMMAND -> "Spawned via /summon or command blocks";
            case DISPENSER -> "Spawned from a dispenser";
            case CONVERSION -> "Zombie villagers curing, piglins turning to zombified";
            default -> "No description available.";
        };
    }

    private void migrateOldFormat(JsonObject entry, Set<MobSpawnType> disallowed) {
        if (isLegacyFalse(entry, "allowNormalSpawning")) {
            disallowed.addAll(List.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.STRUCTURE));
        }
        if (isLegacyFalse(entry, "allowSpawners")) disallowed.add(MobSpawnType.SPAWNER);
        if (isLegacyFalse(entry, "allowSpawnEggs")) disallowed.add(MobSpawnType.SPAWN_EGG);
        if (isLegacyFalse(entry, "allowConversions")) disallowed.add(MobSpawnType.CONVERSION);
    }

    private boolean isLegacyFalse(JsonObject entry, String key) {
        return entry.has(key) && !entry.get(key).getAsBoolean();
    }

    public boolean shouldInclude(EntityType<?> type, ResourceLocation id) {
        return !"minecraft".equals(id.getNamespace()) || (type.getCategory() != MobCategory.MISC || List.of("villager", "snow_golem", "iron_golem").contains(id.getPath()));
    }
}