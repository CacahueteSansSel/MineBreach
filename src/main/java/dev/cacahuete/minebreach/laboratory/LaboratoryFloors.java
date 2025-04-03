package dev.cacahuete.minebreach.laboratory;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LaboratoryFloors {
    public static final FloorLaboratoryManager OVERWORLD = new FloorLaboratoryManager("overworld");
    public static final FloorLaboratoryManager NETHER = new FloorLaboratoryManager("nether");
    public static final FloorLaboratoryManager END = new FloorLaboratoryManager("end");

    public static void loadAllStructures(ServerWorld world) {
        OVERWORLD.loadStructures(world);
        NETHER.loadStructures(world);
        END.loadStructures(world);
    }

    public static class FloorLaboratoryManager {
        String floorId;
        ArrayList<LaboratoryLayout> layouts = new ArrayList<>();
        Map<LaboratoryLayout.TileType, ArrayList<StructureTemplate>> structures = new HashMap<>();

        public FloorLaboratoryManager(String floorId) {
            this.floorId = floorId;
        }

        public void registerLayout(LaboratoryLayout layout) {
            layouts.add(layout);
        }

        public LaboratoryLayout getLayout(Random rng) {
            return layouts.get(rng.nextInt(layouts.size()));
        }

        public void loadStructures(ServerWorld world) {
            StructureTemplateManager structureManager = world.getStructureTemplateManager();

            for (LaboratoryLayout.TileType tileType : LaboratoryLayout.TileType.values()) {
                ArrayList<StructureTemplate> structureTemplates = new ArrayList<>();

                for (int i = 0; i < 64; i++) {
                    Identifier structureId = Identifier.of("minebreach", floorId + "/" + tileType.getStructureName() + "/" + i);
                    StructureTemplate template = structureManager.getTemplate(structureId).orElse(null);
                    if (template == null) break;

                    structureTemplates.add(template);
                }

                structures.put(tileType, structureTemplates);
            }
        }

        public StructureTemplate getTileTypeStructure(LaboratoryLayout.TileType type, Random rng) {
            ArrayList<StructureTemplate> structures = this.structures.get(type);
            if (structures == null || structures.isEmpty()) return null;

            return structures.get(rng.nextInt(this.structures.get(type).size()));
        }

        public LaboratoryGenerator createGenerator() {
            return new LaboratoryGenerator(this);
        }
    }
}
