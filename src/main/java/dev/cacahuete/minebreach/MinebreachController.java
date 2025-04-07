package dev.cacahuete.minebreach;

import dev.cacahuete.minebreach.roles.GameRole;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class MinebreachController {
    static Dictionary<RegistryKey<World>, MinebreachParty> parties = new Hashtable<>();
    static List<MinebreachParty> partyList = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MinebreachController::tick);
    }

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
        partyList.add(party);

        return party;
    }

    public static boolean endPartyForWorld(ServerWorld world, GameRole.Team winnerTeam) {
        MinebreachParty party = getPartyForWorld(world);
        if (party == null) return false;

        party.end(winnerTeam);

        return true;
    }

    public static boolean stopPartyForWorld(ServerWorld world) {
        MinebreachParty party = getPartyForWorld(world);
        if (party == null) return false;

        party.stop();
        parties.remove(world.getRegistryKey());
        partyList.remove(party);

        return true;
    }

    public static void buildExterior(MinecraftServer server) {
        ServerWorld world = (ServerWorld)getMainGameWorld(server);
        StructureTemplateManager structureManager = world.getStructureTemplateManager();
        if (world == null) return;

        BlockPos position = new BlockPos(-3, 47, 237);
        for (int x = 0; x < 4; x++) {
            Identifier structureId = Identifier.of("minebreach", "exterior/e_" + x);

            int finalX = x;
            structureManager.getTemplate(structureId).ifPresent(template -> {
                template.place(world, position.add(finalX * 48, 0, 0), BlockPos.ORIGIN, new StructurePlacementData(), world.getRandom(), 2);
            });
        }

        structureManager.getTemplate(Identifier.of("minebreach:exterior/e_3_1")).ifPresent(template -> {
            template.place(world, position.add(3 * 48, 0, -48), BlockPos.ORIGIN, new StructurePlacementData(), world.getRandom(), 2);
        });
    }

    static void tick(MinecraftServer server) {
        for (int i = 0, partyListSize = partyList.size(); i < partyListSize; i++) {
            MinebreachParty party = partyList.get(i);
            party.tick();
        }
    }
}
