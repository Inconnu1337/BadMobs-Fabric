package net.Inconnu.badmobs.config;

import net.minecraft.world.entity.MobSpawnType;
import java.util.HashSet;
import java.util.Set;

public class SpawnConfig {
    private final Set<MobSpawnType> disallowedTypes;

    public SpawnConfig(Set<MobSpawnType> disallowedTypes) {
        this.disallowedTypes = disallowedTypes;
    }

    public boolean canSpawn(MobSpawnType reason) {
        return !disallowedTypes.contains(reason);
    }

    public Set<MobSpawnType> getDisallowedTypes() {
        return disallowedTypes;
    }

    public static SpawnConfig defaultConfig() {
        return new SpawnConfig(new HashSet<>());
    }
}