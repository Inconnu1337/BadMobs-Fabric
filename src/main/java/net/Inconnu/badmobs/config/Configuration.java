package net.Inconnu.badmobs.config;

import net.Inconnu.badmobs.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;

import java.util.*;

public class Configuration {

    private volatile Map<EntityType<?>, SpawnConfig> configs;

    private final ConfigLoader loader;

    public Configuration() {
        this.loader = new ConfigLoader();
        this.configs = Collections.unmodifiableMap(this.loader.load());
    }

    private Configuration(boolean empty) {
        this.loader = new ConfigLoader();
        this.configs = Collections.emptyMap();
    }

    public static Configuration createEmpty() {
        return new Configuration(true);
    }

    public void reload() {
        this.configs = Collections.unmodifiableMap(this.loader.load());
        Constants.LOG.info("Configuration reloaded successfully.");
    }

    public void save() {
        loader.save(this.configs);
    }

    public synchronized void setSpawnConfig(EntityType<?> entityType, SpawnConfig newConfig) {
        if (!this.configs.containsKey(entityType)) {
            String id = Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))
                    .map(Object::toString)
                    .orElse(entityType.toString());
            throw new IllegalArgumentException("Entity type not tracked by BadMobs: " + id);
        }

        Map<EntityType<?>, SpawnConfig> updated = new LinkedHashMap<>(this.configs);
        updated.put(entityType, newConfig);
        this.configs = Collections.unmodifiableMap(updated);
    }

    public Map<EntityType<?>, SpawnConfig> getConfigs() {
        return configs;
    }

    public boolean allowSpawn(Entity entity, MobSpawnType reason) {
        Map<EntityType<?>, SpawnConfig> snapshot = this.configs;
        SpawnConfig config = snapshot.get(entity.getType());

        if (config == null) {
            Constants.LOG.error(
                    "The entity type {} of {} spawned but has not been registered. SpawnReason={}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), entity, reason
            );
            return true;
        }

        return config.canSpawn(reason);
    }
}