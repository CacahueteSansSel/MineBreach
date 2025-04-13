package dev.cacahuete.minebreach;

import dev.cacahuete.minebreach.core.Shuffler;
import dev.cacahuete.minebreach.core.TickScheduler;
import dev.cacahuete.minebreach.laboratory.LaboratoryFloors;
import dev.cacahuete.minebreach.laboratory.LaboratoryGenerator;
import dev.cacahuete.minebreach.laboratory.LaboratoryLayout;
import dev.cacahuete.minebreach.roles.GameRole;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
    boolean nuclearBombEnabled = false;
    NuclearBombState nuclearBombState = NuclearBombState.Off;
    int nuclearBombTimeout = 2400;
    CommandBossBar nuclearBombBossBar;
    
    public MinebreachParty(ServerWorld world) {
        this.world = world;
        this.rng = Random.create();

        nuclearBombBossBar = world.getServer().getBossBarManager().get(Identifier.of("minebreach:nuclear_bomb"));
        if (nuclearBombBossBar == null) {
            nuclearBombBossBar = world.getServer().getBossBarManager()
                    .add(Identifier.of("minebreach:nuclear_bomb"), Text.literal("Nuclear Bomb is Ongoing"));
            nuclearBombBossBar.setStyle(BossBar.Style.PROGRESS);
            nuclearBombBossBar.setColor(BossBar.Color.RED);
            nuclearBombBossBar.setMaxValue(120);
        }
        nuclearBombBossBar.clearPlayers();
        nuclearBombBossBar.setName(Text.literal("Nuclear Bomb is Ongoing"));
        nuclearBombBossBar.setValue(120);

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

    public boolean isNuclearBombEnabled() {
        return nuclearBombEnabled;
    }

    public NuclearBombState getNuclearBombState() {
        return nuclearBombState;
    }

    public void setNuclearBombEnabled(boolean enabled) {
        this.nuclearBombEnabled = enabled;
    }

    public void assignPlayerRoles() {
        int[] array = new int[players.size()];

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

        Shuffler.shuffle(array);

        for (int i = 0; i < array.length; i++)
            setPlayerRole(players.get(i), array[i], true);
    }

    public void generateAndSpawnPlayers(BlockPos origin) {
        sendSystemMessage(1, 5, "Cleaning before generation");

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
        sendSystemMessage(2, 5, "Generating Floor OVERWORLD");
        overworldGenerator.generate(origin, rng, world, playerSpawnPoints);
        sendSystemMessage(3, 5, "Generating Floor NETHER");
        netherGenerator.generate(origin.up(12), rng, world, playerSpawnPoints);
        sendSystemMessage(4, 5, "Generating Floor END");
        endGenerator.generate(origin.up(12).south(LaboratoryLayout.SIZE * 16), rng, world, playerSpawnPoints);

        sendSystemMessage(5, 5, "Final Cleanup");
        // Kill ItemEntities left over by the generation
        for (Entity e : world.getEntitiesByClass(ItemEntity.class, genBounds, Objects::nonNull)) {
            e.discard();
        }
    }
    
    public void start() {
        if (gameState != State.WaitingForPlayers) return;
        gameState = State.InGame;

        assignPlayerRoles();

        nuclearBombBossBar.setVisible(false);
        nuclearBombBossBar.clearPlayers();
        nuclearBombBossBar.addPlayers(players);
        for (ServerPlayerEntity player : players) {
            player.changeGameMode(GameMode.ADVENTURE);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, StatusEffectInstance.INFINITE, 1, true, false));
            player.playSound(SoundEvents.BLOCK_BELL_RESONATE);
        }

        TickScheduler.schedule(120, () -> {
            playSoundToPlayers(Identifier.of("minebreach:ambient.announcement.start"));
        });
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

        nuclearBombBossBar.setVisible(false);
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
            case InGame -> {
                updateNuclearBomb();
                checkForPartyEnd();
            }
            case Ended -> {
                endTickTimeout--;
                if (endTickTimeout <= 0) {
                    MinebreachController.stopPartyForWorld(world);
                }
            }
        }
    }

    public void explodeNuclearBomb() {
        if (nuclearBombState == NuclearBombState.Exploded) return;

        nuclearBombBossBar.setName(Text.literal("Nuclear Bomb Detonated"));
        for (ServerPlayerEntity player : players) {
            if (player.getBlockY() < 47)
                player.kill(world);
        }

        playSoundToPlayers(Identifier.of("minebreach:ambient.nuclear_bomb.explosion"));

        nuclearBombState = NuclearBombState.Exploded;
    }

    void updateNuclearBomb() {
        if (nuclearBombState != NuclearBombState.Ticking) return;

        if (nuclearBombTimeout <= 0) explodeNuclearBomb();
        else nuclearBombTimeout--;

        nuclearBombBossBar.setValue(nuclearBombTimeout / 20);
    }

    public void startNuclearBomb(ServerPlayerEntity starterPlayer) {
        if (nuclearBombState != NuclearBombState.Off) {
            starterPlayer.sendMessage(Text.literal("The Nuclear Bomb is ongoing !"), true);
            return;
        }
        if (!nuclearBombEnabled) {
            starterPlayer.sendMessage(Text.literal("The Nuclear Bomb is not enabled !"), true);
            return;
        }
        nuclearBombState = NuclearBombState.Ticking;

        Text title = Text.literal("Nuclear Bomb Started").formatted(Formatting.DARK_RED, Formatting.BOLD);
        Text subtitle = Text.literal("Please exit the building ASAP").formatted(Formatting.BOLD);

        nuclearBombBossBar.setVisible(true);
        nuclearBombBossBar.setValue(nuclearBombTimeout / 20);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
        }

        playSoundToPlayers(Identifier.of("minebreach:ambient.nuclear_bomb.start"));

        if (nuclearBombTimeout < 100) {
            nuclearBombTimeout = 100;
        }

        starterPlayer.sendMessage(Text.literal("The Nuclear Bomb is now ongoing !"), true);
    }

    public void playSoundToPlayers(Identifier identifier) {
        for (ServerPlayerEntity player : players) {
            Vec3d pos = player.getPos();
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(identifier)), SoundCategory.MASTER, pos.getX(), pos.getY(), pos.getZ(), 1f, 1f, 5));
        }
    }

    public void playSoundToPlayers(SoundEvent event) {
        playSoundToPlayers(event.id());
    }

    public void stopSoundForPlayers(Identifier identifier) {
        StopSoundS2CPacket stopSoundS2CPacket = new StopSoundS2CPacket(identifier, SoundCategory.MASTER);

        for (ServerPlayerEntity serverPlayerEntity : players) {
            serverPlayerEntity.networkHandler.sendPacket(stopSoundS2CPacket);
        }
    }

    public void stopNuclearBomb() {
        if (nuclearBombState != NuclearBombState.Ticking) return;
        nuclearBombState = NuclearBombState.Off;

        Text title = Text.literal("Nuclear Bomb Stopped").formatted(Formatting.DARK_RED, Formatting.BOLD);
        Text subtitle = Text.literal("Everything is back to normal").formatted(Formatting.BOLD);

        nuclearBombBossBar.setVisible(false);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
        }

        stopSoundForPlayers(Identifier.of("minebreach:ambient.nuclear_bomb.start"));
        playSoundToPlayers(SoundEvents.ENTITY_VILLAGER_NO);
    }

    public enum State {
        WaitingForPlayers,
        InGame,
        Ended
    }

    public enum NuclearBombState {
        Off,
        Ticking,
        Exploded
    }
}
