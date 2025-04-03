package dev.cacahuete.minebreach.roles;

import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class Roles {
    public static final ArrayList<GameRole> ROLES = new ArrayList<>();
    public static final GameRole WAITING = add(new GameRole("waiting", "Waiting", Formatting.BLUE, GameRole.Team.Special, "Waiting for players", false));
    public static final GameRole SPECTATOR = add(new GameRole("spectator", "Spectator", Formatting.DARK_GRAY, GameRole.Team.Special, "Spectate other players", false));
    public static final GameRole NITWIT = add(new GameRole("nitwit", "Nitwit", Formatting.GOLD, GameRole.Team.Humans, "Escape the building (but with nothing) !", true).withCustomHeadModel().withSpawnKitLootTable());
    public static final GameRole LIBRARIAN = add(new GameRole("librarian", "Librarian", Formatting.YELLOW, GameRole.Team.Humans, "Escape the building !", true).withCustomHeadModel().withSpawnKitLootTable());
    public static final GameRole PLAYER = add(new GameRole("player", "Player", Formatting.AQUA, GameRole.Team.Humans, "Save the villagers while avoiding", false));
    public static final GameRole CREEPER = add(new GameRole("creeper", "Creeper", Formatting.RED, GameRole.Team.Creatures, "Kill anyone and (maybe) coop with the Pillagers", true).withCustomHead(Items.CREEPER_HEAD));
    public static final GameRole PILLAGER = add(new GameRole("pillager", "Pillager", Formatting.GREEN, GameRole.Team.Creatures, "Coop with the monsters and save ...", false).withCustomHeadModel().withSpawnKitLootTable());
    public static final GameRole ENDERMAN = add(new GameRole("enderman", "Enderman", Formatting.RED, GameRole.Team.Creatures, "Kill anyone and (maybe) coop with the Pillagers", true).withCustomHeadModel().withSpawnKitLootTable());

    private static GameRole add(GameRole role) {
        role.index = ROLES.size();
        ROLES.add(role);

        return role;
    }

    public static void registerTeams(MinecraftServer server) {
        for (GameRole role : ROLES) role.addTeam(server);
    }

    public static GameRole get(int index) {
        return ROLES.get(index);
    }

    public static GameRole getRandomFromTeam(Random rng, GameRole.Team team) {
        List<GameRole> roles = ROLES.stream().filter(role -> role.team == team).toList();

        return roles.get(rng.nextInt(roles.size()));
    }

    public static GameRole getRandomFromTeamNaturallySpawnable(Random rng, GameRole.Team team) {
        List<GameRole> roles = ROLES.stream().filter(role -> role.team == team && role.naturallySpawns).toList();

        return roles.get(rng.nextInt(roles.size()));
    }
}
