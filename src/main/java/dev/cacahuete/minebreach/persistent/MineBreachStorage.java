package dev.cacahuete.minebreach.persistent;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Objects;

public class MineBreachStorage {
    public static MineBreachPersistentState getOrCreate(ServerWorld world) {
        PersistentStateManager stateManager = world.getPersistentStateManager();
        return stateManager.getOrCreate(MineBreachPersistentState.TYPE, "minebreach");
    }
}
