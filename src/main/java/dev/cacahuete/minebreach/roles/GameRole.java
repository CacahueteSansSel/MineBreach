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

import java.util.Map;
import java.util.Optional;

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

    public GameRole(String id, String name, Formatting color, Team team, String description, boolean naturallySpawns) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.team = team;
        this.description = description;
        this.naturallySpawns = naturallySpawns;
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

    public boolean addTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        if (scoreboard.getTeam("mb-" + id) != null)
            return false;

        var team = scoreboard.addTeam("mb-" + id);
        team.setDisplayName(Text.literal(name));
        team.setColor(color);
        team.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OWN_TEAM);
        team.setFriendlyFireAllowed(false);
        team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        team.setPrefix(Text.literal("[" + name + "] "));

        return true;
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
            // Minecraft's code is so overcomplicated that running the loot command from the code is the easy way to
            // do this
            player.getServer().getCommandManager()
                    .executeWithPrefix(player.getServer().getCommandSource(),
                            "loot give " + player.getCommandSource().getName() + " loot " + spawnKitLootTable.get());
        }
    }

    public static enum Team {
        Special,
        Humans,
        Creatures
    }
}
