package dev.cacahuete.minebreach;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Dictionary;
import java.util.Hashtable;

public class MinebreachController {
    static Dictionary<RegistryKey<World>, MinebreachParty> parties = new Hashtable<>();

    public static RegistryKey<World> getMainGameWorldKey() {
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minebreach:minebreach"));
    }

    public static World getMainGameWorld(MinecraftServer server) {
        return server.getWorld(getMainGameWorldKey());
    }

    public static MinebreachParty getPartyForWorld(ServerWorld world) {
        return parties.get(world.getRegistryKey());
    }

    public static MinebreachParty createPartyForWorld(ServerWorld world) {
        MinebreachParty party = new MinebreachParty(world);
        parties.put(world.getRegistryKey(), party);

        return party;
    }

    public static boolean endPartyForWorld(ServerWorld world) {
        MinebreachParty party = getPartyForWorld(world);
        if (party == null) return false;

        party.end();
        parties.remove(world.getRegistryKey());
        return true;
    }
}
