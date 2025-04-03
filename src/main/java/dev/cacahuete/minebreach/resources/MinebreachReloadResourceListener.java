package dev.cacahuete.minebreach.resources;

import dev.cacahuete.minebreach.laboratory.LaboratoryLayout;
import dev.cacahuete.minebreach.laboratory.LaboratoryFloors;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MinebreachReloadResourceListener implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return Identifier.of("minebreach:reload");
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, LaboratoryLayout> layouts = new HashMap<>();

            for (Identifier id : manager.findResources("laboratory/layout/overworld", path ->
                    path.getPath().endsWith(".csv")).keySet()) {
                try (InputStream stream = manager.getResourceOrThrow(id).getInputStream()) {
                    layouts.put("overworld", LaboratoryLayout.fromStream(stream));
                } catch(Exception e) {
                    System.err.println("Error occurred while loading resource json " + id.toString());
                }
            }

            for (Identifier id : manager.findResources("laboratory/layout/nether", path ->
                    path.getPath().endsWith(".csv")).keySet()) {
                try (InputStream stream = manager.getResourceOrThrow(id).getInputStream()) {
                    layouts.put("nether", LaboratoryLayout.fromStream(stream));
                } catch(Exception e) {
                    System.err.println("Error occurred while loading resource json " + id.toString());
                }
            }

            for (Identifier id : manager.findResources("laboratory/layout/end", path ->
                    path.getPath().endsWith(".csv")).keySet()) {
                try (InputStream stream = manager.getResourceOrThrow(id).getInputStream()) {
                    layouts.put("end", LaboratoryLayout.fromStream(stream));
                } catch(Exception e) {
                    System.err.println("Error occurred while loading resource json " + id.toString());
                }
            }

            return layouts;
        }, prepareExecutor).thenCompose(synchronizer::whenPrepared).thenAcceptAsync(loadedData -> {
            for (var layout : loadedData.entrySet()) {
                switch (layout.getKey()) {
                    case "overworld":
                        LaboratoryFloors.OVERWORLD.registerLayout(layout.getValue());
                        break;
                    case "nether":
                        LaboratoryFloors.NETHER.registerLayout(layout.getValue());
                        break;
                    case "end":
                        LaboratoryFloors.END.registerLayout(layout.getValue());
                        break;
                }
            }

            System.out.println("[Minebreach/Laboratory] registered " + loadedData.size() + " layout(s)");
        }, applyExecutor);
    }
}
