package net.Inconnu.badmobs.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.Inconnu.badmobs.BadMobs;
import net.Inconnu.badmobs.config.SpawnConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BadMobsCommand {

    private static final String KEY_CONFIG_NULL    = "commands.badmobs.error.config_null";
    private static final String KEY_RELOAD_SUCCESS = "commands.badmobs.reload.success";
    private static final String KEY_SAVE_SUCCESS   = "commands.badmobs.save.success";
    private static final String KEY_INVALID_TYPE   = "commands.badmobs.error.invalid_spawn_type";
    private static final String KEY_NOT_TRACKED    = "commands.badmobs.error.not_tracked";
    private static final String KEY_SET_SUCCESS    = "commands.badmobs.set.success";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("badmobs")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx.getSource())))

                .then(Commands.literal("save")
                        .executes(ctx -> save(ctx.getSource())))

                .then(Commands.literal("set")
                        .then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .then(Commands.argument("spawnType", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                spawnTypeSuggestions(),
                                                builder
                                        ))
                                        .then(Commands.argument("allowed", BoolArgumentType.bool())
                                                .executes(ctx -> set(
                                                        ctx.getSource(),
                                                        ResourceArgument.getResource(ctx, "entity", Registries.ENTITY_TYPE),
                                                        StringArgumentType.getString(ctx, "spawnType"),
                                                        BoolArgumentType.getBool(ctx, "allowed")
                                                ))))))
        );
    }

    private static Stream<String> spawnTypeSuggestions() {
        return Stream.concat(
                Stream.of("ALL"),
                Arrays.stream(MobSpawnType.values()).map(Enum::name)
        );
    }

    private static int reload(CommandSourceStack source) {
        if (BadMobs.config == null) {
            source.sendFailure(Component.translatable(KEY_CONFIG_NULL));
            return 0;
        }
        BadMobs.config.reload();
        source.sendSuccess(() -> Component.translatable(KEY_RELOAD_SUCCESS), true);
        return 1;
    }

    private static int save(CommandSourceStack source) {
        if (BadMobs.config == null) {
            source.sendFailure(Component.translatable(KEY_CONFIG_NULL));
            return 0;
        }
        BadMobs.config.save();
        source.sendSuccess(() -> Component.translatable(KEY_SAVE_SUCCESS), true);
        return 1;
    }

    private static int set(
            CommandSourceStack source,
            Holder.Reference<EntityType<?>> entityHolder,
            String spawnTypeStr,
            boolean allowed
    ) {
        EntityType<?> entity = entityHolder.value();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString();

        if (BadMobs.config.getConfigs().get(entity) == null) {
            source.sendFailure(Component.translatable(KEY_NOT_TRACKED, entityId));
            return 0;
        }

        Set<MobSpawnType> targetTypes;
        String spawnTypeLabel;

        if (spawnTypeStr.equalsIgnoreCase("ALL")) {
            targetTypes = Arrays.stream(MobSpawnType.values()).collect(Collectors.toSet());
            spawnTypeLabel = "ALL";
        } else {
            try {
                MobSpawnType spawnType = MobSpawnType.valueOf(spawnTypeStr.toUpperCase(Locale.ROOT));
                targetTypes = Set.of(spawnType);
                spawnTypeLabel = spawnType.name();
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.translatable(KEY_INVALID_TYPE, spawnTypeStr));
                return 0;
            }
        }

        applyChange(entity, targetTypes, allowed);
        sendSetSuccess(source, entity, entityId);
        return 1;
    }

    private static void applyChange(EntityType<?> entity, Set<MobSpawnType> types, boolean allowed) {
        SpawnConfig current = BadMobs.config.getConfigs().get(entity);
        Set<MobSpawnType> disallowed = new HashSet<>(current.getDisallowedTypes());
        if (allowed) {
            disallowed.removeAll(types);
        } else {
            disallowed.addAll(types);
        }
        BadMobs.config.setSpawnConfig(entity, new SpawnConfig(disallowed));
    }

    private static void sendSetSuccess(
            CommandSourceStack source,
            EntityType<?> entity,
            String entityId
    ) {
        SpawnConfig updated = BadMobs.config.getConfigs().get(entity);
        Set<MobSpawnType> disallowed = updated.getDisallowedTypes();

        if (disallowed.isEmpty()) {
            source.sendSuccess(
                    () -> Component.translatable("commands.badmobs.set.all_allowed", entityId),
                    true
            );
        } else {
            String typeList = disallowed.stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            source.sendSuccess(
                    () -> Component.translatable(KEY_SET_SUCCESS, entityId, typeList),
                    true
            );
        }
    }
}