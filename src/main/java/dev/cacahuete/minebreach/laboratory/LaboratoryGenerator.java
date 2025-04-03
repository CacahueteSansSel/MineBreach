package dev.cacahuete.minebreach.laboratory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.*;

public class LaboratoryGenerator {
    LaboratoryFloors.FloorLaboratoryManager manager;

    public LaboratoryLayout layout;

    public LaboratoryGenerator(LaboratoryFloors.FloorLaboratoryManager manager) {
        this.manager = manager;
    }

    public Dictionary<Integer, ArrayList<BlockPos>> generate(BlockPos origin, Random rng, ServerWorld world, Dictionary<Integer, ArrayList<BlockPos>> spawnPoints) {
        return generate(origin, rng, manager.getLayout(rng), world, spawnPoints);
    }

    public Dictionary<Integer, ArrayList<BlockPos>> generate(BlockPos origin, Random rng, LaboratoryLayout layout, ServerWorld world, Dictionary<Integer, ArrayList<BlockPos>> spawnPoints) {
        this.layout = layout;

        for (int y = 0; y < LaboratoryLayout.SIZE; y++) {
            for (int x = 0; x < LaboratoryLayout.SIZE; x++) {
                BlockPos tilePos = origin.add(x * 16, 0, y * 16);
                StructureTemplate structure = manager.getTileTypeStructure(layout.get(x, y), rng);
                if (structure == null) {
                    continue;
                }

                for (int bx = 0; bx < 16; bx++) {
                    for (int by = 0; by < 16; by++) {
                        for (int bz = 0; bz < 16; bz++) {
                            BlockPos pos = new BlockPos(tilePos.getX() + bx, tilePos.getY() + by, tilePos.getZ() + bz);
                            if (world.isAir(pos)) continue;

                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.SKIP_DROPS | Block.REDRAW_ON_MAIN_THREAD);
                        }
                    }
                }

                structure.place(world, tilePos, tilePos, new StructurePlacementData().setIgnoreEntities(false), rng, Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);

                for (int bx = 0; bx < 16; bx++) {
                    for (int by = 0; by < 16; by++) {
                        for (int bz = 0; bz < 16; bz++) {
                            BlockPos pos = new BlockPos(tilePos.getX() + bx, tilePos.getY() + by, tilePos.getZ() + bz);

                            if (!(world.getBlockEntity(pos) instanceof CommandBlockBlockEntity commandBlock))
                                continue;

                            System.out.println(manager.floorId + " : at " + pos + " : " + commandBlock.getCommandExecutor().getCommand());

                            String[] tokens = commandBlock.getCommandExecutor().getCommand().split(" ");
                            if (tokens.length != 2 || !Objects.equals(tokens[0], "spawn"))
                                continue;

                            int roleId = Integer.parseInt(tokens[1]);
                            if (spawnPoints.get(roleId) == null) spawnPoints.put(roleId, new ArrayList<>());

                            spawnPoints.get(roleId).add(pos);

                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
                        }
                    }
                }
            }
        }

        return spawnPoints;
    }
}
