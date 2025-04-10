package dev.cacahuete.minebreach;

import dev.cacahuete.minebreach.laboratory.LaboratoryFloors;
import dev.cacahuete.minebreach.laboratory.LaboratoryGenerator;
import dev.cacahuete.minebreach.laboratory.LaboratoryLayout;
import dev.cacahuete.minebreach.roles.GameRole;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import java.util.*;

public class MinebreachParty {
    Dictionary<UUID, Integer> roleMap = new Hashtable<>();
    ArrayList<ServerPlayerEntity> players = new ArrayList<>();
    State gameState;
    ServerWorld world;
    LaboratoryGenerator overworldGenerator;
    LaboratoryGenerator netherGenerator;
    LaboratoryGenerator endGenerator;
    Random rng;
    Dictionary<Integer, ArrayList<BlockPos>> playerSpawnPoints;
    int endTickTimeout = 100;
    
    public MinebreachParty(ServerWorld world) {
        this.world = world;
        this.rng = Random.create();

        overworldGenerator = LaboratoryFloors.OVERWORLD.createGenerator();
        netherGenerator = LaboratoryFloors.NETHER.createGenerator();
        endGenerator = LaboratoryFloors.END.createGenerator();

        gameState = State.WaitingForPlayers;
    }

    protected void sendSystemMessage(int progress, int progressMax, String status) {
        for (ServerPlayerEntity player : players) {
            if (!player.hasPermissionLevel(2)) continue;

            player.sendMessage(Text.literal("[MineBreach] " + progress + "/" + progressMax + " : " + status));
        }
    }

    public boolean isPlayerInParty(ServerPlayerEntity player) {
        return players.contains(player);
    }

    public void assignPlayerRoles() {
        Integer[] array = new Integer[players.size()];

        if (array.length == 1) {
            array[0] = Roles.LIBRARIAN.index;
        } else {
            int monsterCount = Math.min(array.length / 2, 4);

            // Spawn monsters/creatures
            for (int i = 0; i < monsterCount; i++) {
                array[i] = Roles.getRandomFromTeamNaturallySpawnable(rng, GameRole.Team.Creatures).index;
            }

            // Spawn humans
            for (int i = monsterCount; i < array.length; i++) {
                array[i] = Roles.getRandomFromTeamNaturallySpawnable(rng, GameRole.Team.Humans).index;
            }
        }

        for (int i = 0; i < array.length; i++)
            setPlayerRole(players.get(i), array[i], true);
    }

    public void generateAndSpawnPlayers(BlockPos origin) {
        sendSystemMessage(1, 4, "Cleaning before generation");

        // Clear all entities before generation
        Box genBounds = Box.enclosing(new BlockPos(-320, 0, -320), new BlockPos(320 * 2, 128, 320));
        for (Entity e : world.getEntitiesByClass(Entity.class, genBounds, e -> !(e instanceof ServerPlayerEntity))) {
            e.discard();
        }

        // Create the player spawn point table
        playerSpawnPoints = new Hashtable<>();
        for (GameRole role : Roles.ROLES) {
            if (role.staticSpawnPoints != null && !role.staticSpawnPoints.isEmpty()) {
                ArrayList<BlockPos> spawnPoints = new ArrayList<>();
                for (BlockPos pos : role.staticSpawnPoints) {
                    spawnPoints.add(pos.add(origin));
                }
                playerSpawnPoints.put(role.index, spawnPoints);
            }
        }

        // Generate all floors
        sendSystemMessage(2, 4, "Generating Floor OVERWORLD");
        overworldGenerator.generate(origin, rng, world, playerSpawnPoints);
        sendSystemMessage(3, 4, "Generating Floor NETHER");
        netherGenerator.generate(origin.up(12), rng, world, playerSpawnPoints);
        sendSystemMessage(3, 4, "Generating Floor END");
        endGenerator.generate(origin.up(12).south(LaboratoryLayout.SIZE * 16), rng, world, playerSpawnPoints);

        sendSystemMessage(4, 4, "Final Cleanup");
        // Kill ItemEntities left over by the generation
        for (Entity e : world.getEntitiesByClass(ItemEntity.class, genBounds, Objects::nonNull)) {
            e.discard();
        }
    }
    
    public void start() {
        if (gameState != State.WaitingForPlayers) return;
        gameState = State.InGame;

        assignPlayerRoles();

        for (ServerPlayerEntity player : players) {
            player.changeGameMode(GameMode.ADVENTURE);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, StatusEffectInstance.INFINITE, 1, true, false));
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE);
        }
    }

    public boolean addPlayer(ServerPlayerEntity player) {
        if (gameState != State.WaitingForPlayers)
            return false;

        players.add(player);
        roleMap.put(player.getUuid(), 0);
        return true;
    }

    public int getPlayerRole(ServerPlayerEntity player) {
        return roleMap.get(player.getUuid());
    }

    public void setPlayerRole(ServerPlayerEntity player, int roleId, boolean teleportToSpawnPoint) {
        roleMap.put(player.getUuid(), roleId);

        GameRole role = Roles.get(roleId);
        Scoreboard scoreboard = world.getScoreboard();
        Team roleTeam = scoreboard.getTeam(role.team.getTeamId());
        Team currentPlayerTeam = scoreboard.getScoreHolderTeam(player.getNameForScoreboard());

        if (currentPlayerTeam != roleTeam) {
            if (currentPlayerTeam != null)
                scoreboard.removeScoreHolderFromTeam(player.getNameForScoreboard(), scoreboard.getScoreHolderTeam(player.getNameForScoreboard()));

            scoreboard.addScoreHolderToTeam(player.getNameForScoreboard(), roleTeam);
        }

        role.postProcessPlayer(player);
        if (teleportToSpawnPoint) {
            int roleIndex = getPlayerRole(player);
            ArrayList<BlockPos> rolePositions = playerSpawnPoints.get(roleIndex);
            if (rolePositions == null || rolePositions.isEmpty()) rolePositions = playerSpawnPoints.get(2);
            if (rolePositions != null && !rolePositions.isEmpty()) {
                BlockPos pos = rolePositions.get(rng.nextInt(rolePositions.size()));
                rolePositions.remove(pos);

                player.teleportTo(new TeleportTarget(world, pos.toCenterPos(), Vec3d.ZERO, 0f, 0f, TeleportTarget.NO_OP));
            }
        }

        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(role.description)));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(role.name).formatted(Formatting.BOLD, role.color)));
    }

    public void stop() {
        for (ServerPlayerEntity player : players) {
            removePlayer(player, false);
        }

        players.clear();
    }

    public void end(GameRole.Team winnerTeam) {
        Text title = Text.of("Unknown winners");
        Text subtitle = Text.of("The party is ending in 5 seconds");

        switch (winnerTeam) {
            case Humans -> {
                title = Text.literal("The humans won").formatted(Formatting.GOLD);
            }
            case Creatures -> {
                title = Text.literal("The creatures won").formatted(Formatting.RED);
            }
            case Insurgents -> {
                title = Text.literal("The insurgents won").formatted(Formatting.GREEN);
            }
            default -> {
                title = Text.literal("This is a tie").formatted(Formatting.WHITE);
            }
        }

        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
        }

        gameState = State.Ended;
    }

    public void removePlayer(ServerPlayerEntity player, boolean removeFromList) {
        ServerWorld overworld = world.getServer().getWorld(ServerWorld.OVERWORLD);
        BlockPos worldSpawn = overworld.getSpawnPos();
        Scoreboard scoreboard = overworld.getServer().getScoreboard();

        player.teleportTo(new TeleportTarget(overworld, worldSpawn.toCenterPos(), Vec3d.ZERO, 0f, 0f, TeleportTarget.NO_OP));
        player.getInventory().clear();
        player.removeStatusEffect(StatusEffects.SATURATION);
        scoreboard.removeScoreHolderFromTeam(player.getNameForScoreboard(), scoreboard.getScoreHolderTeam(player.getNameForScoreboard()));

        if (removeFromList) players.remove(player);
    }

    public void upgradePlayer(ServerPlayerEntity player) {
        int roleId = getPlayerRole(player);
        GameRole role = Roles.get(roleId);

        if (role == null || role.upgradesToId.isEmpty()) return;

        setPlayerRole(player, Objects.requireNonNull(Roles.get(role.upgradesToId.get())).index, true);
    }

    void checkForPartyEnd() {
        if (players.isEmpty()) {
            end(null);
            return;
        }
        if (players.size() < 2) return;

        int creaturesCount = 0;
        int humansCount = 0;
        int insurgentsCount = 0;
        for (ServerPlayerEntity player : players) {
            GameRole role = Roles.get(getPlayerRole(player));

            switch (role.team) {
                case Creatures -> creaturesCount++;
                case Humans -> humansCount++;
                case Insurgents -> insurgentsCount++;
            }
        }

        if (insurgentsCount == 0 && creaturesCount == 0 && humansCount > 0) {
            end(GameRole.Team.Humans);
        } else if (humansCount == 0 && creaturesCount == 0 && insurgentsCount > 0) {
            end(GameRole.Team.Insurgents);
        } else if (insurgentsCount == 0 && humansCount == 0 && creaturesCount > 0) {
            end(GameRole.Team.Creatures);
        }
    }

    public void tick() {
        switch (gameState) {
            case WaitingForPlayers -> {
                // todo ?
            }
            case InGame -> checkForPartyEnd();
            case Ended -> {
                endTickTimeout--;
                if (endTickTimeout <= 0) {
                    MinebreachController.stopPartyForWorld(world);
                }
            }
        }
    }

    public enum State {
        WaitingForPlayers,
        InGame,
        Ended
    }
}
