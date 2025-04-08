package dev.cacahuete.minebreach.roles;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class GameRole {
    public int index;
    public String id;
    public String name;
    public Formatting color;
    public Team team;
    public String description;
    public boolean hasCustomHead;
    public boolean naturallySpawns;
    public Optional<Item> headItem = Optional.empty();
    Optional<Identifier> spawnKitLootTable = Optional.empty();
    public Optional<String> upgradesToId = Optional.empty();
    public ArrayList<BlockPos> staticSpawnPoints = new ArrayList<>();

    public GameRole(String id, String name, Formatting color, Team team, String description, boolean naturallySpawns) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.team = team;
        this.description = description;
        this.naturallySpawns = naturallySpawns;
    }

    public GameRole withStaticSpawnPoint(BlockPos pos) {
        staticSpawnPoints.add(pos);

        return this;
    }

    public GameRole withStaticSpawnPoints(List<BlockPos> positions) {
        staticSpawnPoints.addAll(positions);

        return this;
    }

    public GameRole upgradesTo(String id) {
        upgradesToId = Optional.of(id);
        return this;
    }

    public GameRole withSpawnKitLootTable(Identifier lootTableIdentifier) {
        spawnKitLootTable = Optional.of(lootTableIdentifier);

        return this;
    }

    public GameRole withSpawnKitLootTable() {
        return withSpawnKitLootTable(Identifier.of("minebreach:start_kit/" + id));
    }

    public GameRole withCustomHeadModel() {
        hasCustomHead = true;
        return this;
    }

    public GameRole withCustomHead(Item item) {
        hasCustomHead = true;
        headItem = Optional.of(item);

        return this;
    }

    public void addCosmetics(ServerPlayerEntity player) {
        if (hasCustomHead) {
            ItemStack head;

            if (headItem.isPresent()) {
                head = new ItemStack(headItem.get());
            } else {
                head = new ItemStack(Items.PAPER);

                head.set(DataComponentTypes.ITEM_MODEL, Identifier.of("minebreach:" + id + "_head"));
                head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name + " Head").setStyle(Style.EMPTY.withItalic(false)));
            }

            player.equipStack(EquipmentSlot.HEAD, head);
        }
    }

    public void postProcessPlayer(ServerPlayerEntity player) {
        player.getInventory().clear();

        addCosmetics(player);

        if (spawnKitLootTable.isPresent()) {
            // Minecraft's code is so overcomplicated that running the loot command from the code is the easiest way to
            // do this
            player.getServer().getCommandManager()
                    .executeWithPrefix(player.getServer().getCommandSource(),
                            "loot give " + player.getCommandSource().getName() + " loot " + spawnKitLootTable.get());
        }
    }

    public static enum Team {
        Special("special", "Special"),
        Humans("humans", "Humans"),
        Creatures("creatures", "Creatures"),
        Insurgents("insurgents", "Insurgents");

        final String displayName;
        final String slug;
        final String teamId;

        public String getSlug() {
            return slug;
        }

        public String getTeamId() {
            return teamId;
        }

        Team(String slug, String displayName) {
            this.slug = slug;
            this.displayName = displayName;
            this.teamId = "mb-t-" + slug;
        }

        public void addTeam(MinecraftServer server) {
            Scoreboard scoreboard = server.getScoreboard();
            net.minecraft.scoreboard.Team team = scoreboard.addTeam(teamId);

            team.setDisplayName(Text.literal(displayName));
            team.setColor(Formatting.WHITE);
            team.setFriendlyFireAllowed(false);
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.ALWAYS);
        }
    }
}
