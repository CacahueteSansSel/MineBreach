package dev.cacahuete.minebreach.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.cacahuete.minebreach.MinebreachController;
import dev.cacahuete.minebreach.MinebreachParty;
import dev.cacahuete.minebreach.inventory.CustomCreativeInventory;
import dev.cacahuete.minebreach.laboratory.LaboratoryLayout;
import dev.cacahuete.minebreach.roles.GameRole;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Objects;

public class MinebreachCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("minebreach")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("inventory")
                        .executes(context -> {
                            if (!context.getSource().isExecutedByPlayer()) {
                                context.getSource().sendFeedback(() -> Text.literal("Command must be executed by a player"), false);
                                return 1;
                            }

                            CustomCreativeInventory.openFor(Objects.requireNonNull(context.getSource().getPlayer()));
                            return 1;
                        }))
                .then(CommandManager.literal("party")
                        .then(CommandManager.literal("start")
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(context -> {
                                            ServerWorld partyWorld = (ServerWorld) MinebreachController.getMainGameWorld(context.getSource().getServer());
                                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");

                                            MinebreachParty party = MinebreachController.getPartyForWorld(partyWorld);
                                            if (party != null) {
                                                context.getSource().sendError(Text.literal("A party is already running ! Use /minebreach party end to end the party (gracefully)"));

                                                return 2;
                                            }

                                            party = MinebreachController.createPartyForWorld(partyWorld);
                                            for (ServerPlayerEntity player : players)
                                                party.addPlayer(player);

                                            context.getSource().sendFeedback(() -> Text.literal("Created a Minebreach party on " + partyWorld.getRegistryKey().getValue() + ": generation in progress"), true);

                                            party.generateAndSpawnPlayers(BlockPos.ORIGIN);
                                            party.start();

                                            context.getSource().sendFeedback(() -> Text.literal("Minebreach party on " + partyWorld.getRegistryKey().getValue() + " finished generation"), true);

                                            return 1;
                                        })))
                        .then(CommandManager.literal("end")
                                .executes(context -> {
                                    ServerWorld partyWorld = (ServerWorld) MinebreachController.getMainGameWorld(context.getSource().getServer());
                                    MinebreachParty party = MinebreachController.getPartyForWorld(partyWorld);
                                    if (party == null) {
                                        context.getSource().sendError(Text.literal("There isn't any party running ! Use /minebreach party start to start a party"));

                                        return 2;
                                    }

                                    if (MinebreachController.endPartyForWorld(partyWorld, GameRole.Team.Humans)) {
                                        context.getSource().sendFeedback(() -> Text.literal("Ended the Minebreach party on " + partyWorld.getRegistryKey().getValue()), true);
                                    } else {
                                        context.getSource().sendError(Text.literal("Failed to end the party"));

                                        return 2;
                                    }

                                    return 1;
                                }))
                        .then(CommandManager.literal("stop")
                                .executes(context -> {
                                    ServerWorld partyWorld = (ServerWorld) MinebreachController.getMainGameWorld(context.getSource().getServer());
                                    MinebreachParty party = MinebreachController.getPartyForWorld(partyWorld);
                                    if (party == null) {
                                        context.getSource().sendError(Text.literal("There isn't any party running ! Use /minebreach party start to start a party"));

                                        return 2;
                                    }

                                    if (MinebreachController.stopPartyForWorld(partyWorld)) {
                                        context.getSource().sendFeedback(() -> Text.literal("Ended the Minebreach party on " + partyWorld.getRegistryKey().getValue()), true);
                                    } else {
                                        context.getSource().sendError(Text.literal("Failed to end the party"));

                                        return 2;
                                    }

                                    return 1;
                                })))
                .then(CommandManager.literal("forcerole")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.argument("role", IntegerArgumentType.integer(0, Roles.ROLES.size() - 1))
                                        .executes(context -> {
                                            ServerWorld partyWorld = (ServerWorld) MinebreachController.getMainGameWorld(context.getSource().getServer());
                                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "targets");
                                            int roleId = IntegerArgumentType.getInteger(context, "role");
                                            MinebreachParty party = MinebreachController.getPartyForWorld(partyWorld);
                                            if (party == null) {
                                                context.getSource().sendError(Text.literal("You must create a party first for this world ! Use /minebreach party start"));

                                                return 2;
                                            }

                                            for (ServerPlayerEntity player : players)
                                                party.setPlayerRole(player, roleId);

                                            context.getSource().sendFeedback(() -> Text.literal("Set role of " + players.size() + " player(s) to #" + roleId + " (" + Roles.get(roleId).name + ")"), true);

                                            return 1;
                                        })
                                ))
                )
                .then(CommandManager.literal("generation")
                        .then(CommandManager.literal("clean")
                                .executes(context -> {
                                    ServerWorld partyWorld = (ServerWorld) MinebreachController.getMainGameWorld(context.getSource().getServer());

                                    for (int bx = 0; bx < LaboratoryLayout.SIZE * 16; bx++) {
                                        for (int by = 0; by < 128; by++) {
                                            for (int bz = 0; bz < LaboratoryLayout.SIZE * 16; bz++) {
                                                BlockPos pos = new BlockPos(bx, by, bz);
                                                if (partyWorld.isAir(pos)) continue;

                                                partyWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS);
                                            }
                                        }
                                    }

                                    context.getSource().sendFeedback(() -> Text.literal("Cleaned Laboratory generation area"), true);

                                    return 1;
                                }))
                )
        );
    }
}
