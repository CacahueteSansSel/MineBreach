package dev.cacahuete.minebreach;

import dev.cacahuete.minebreach.commands.MinebreachCommand;
import dev.cacahuete.minebreach.core.TickScheduler;
import dev.cacahuete.minebreach.items.CustomItems;
import dev.cacahuete.minebreach.laboratory.LaboratoryFloors;
import dev.cacahuete.minebreach.resources.MinebreachReloadResourceListener;
import dev.cacahuete.minebreach.roles.Roles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class Minebreach implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("Initializing MineBreach");

        CustomItems.register();
        TickScheduler.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MinebreachCommand.register(dispatcher);
        });

        ServerWorldEvents.LOAD.register(((minecraftServer, serverWorld) -> {
            Roles.registerTeams(minecraftServer);
            LaboratoryFloors.loadAllStructures(serverWorld);
        }));


        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new MinebreachReloadResourceListener());
    }
}
