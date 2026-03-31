package net.Inconnu.badmobs;

import net.Inconnu.badmobs.commands.BadMobsCommand;
import net.Inconnu.badmobs.config.Configuration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class BadMobs implements ModInitializer {

    public static volatile Configuration config = Configuration.createEmpty();

    @Override
    public void onInitialize() {
        Constants.LOG.info("BadMobs initializing");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            config = new Configuration();
            Constants.LOG.info("BadMobs config loaded.");
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                BadMobsCommand.register(dispatcher, registryAccess));
    }
}