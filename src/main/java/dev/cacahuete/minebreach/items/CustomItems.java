package dev.cacahuete.minebreach.items;

import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class CustomItems {
    public static final ArrayList<CustomItem> ITEMS = new ArrayList<>();

    public static final CustomItem KEYCARD_WOODEN = add(new KeycardCustomItem("Wooden", "wooden", 1, Items.BROWN_DYE));
    public static final CustomItem KEYCARD_COBBLESTONE = add(new KeycardCustomItem("Cobblestone", "cobblestone", 2, Items.GRAY_DYE));
    public static final CustomItem KEYCARD_IRON = add(new KeycardCustomItem("Iron", "iron", 3, Items.LIGHT_GRAY_DYE));
    public static final CustomItem KEYCARD_GOLDEN = add(new KeycardCustomItem("Golden", "golden", 4, Items.ORANGE_DYE));
    public static final CustomItem KEYCARD_DIAMOND = add(new KeycardCustomItem("Diamond", "diamond", 5, Items.CYAN_DYE));
    public static final CustomItem KEYCARD_EMERALD = add(new KeycardCustomItem("Emerald", "emerald", 6, Items.GREEN_DYE));
    public static final CustomItem KEYCARD_NETHERITE = add(new KeycardCustomItem("Netherite", "netherite", 7, Items.BLACK_DYE));
    public static final CustomItem IRON_DOOR_KEYCARD_0 = add(new KeycardDoorCustomItem(0));
    public static final CustomItem IRON_DOOR_KEYCARD_1 = add(new KeycardDoorCustomItem(1));
    public static final CustomItem IRON_DOOR_KEYCARD_2 = add(new KeycardDoorCustomItem(2));
    public static final CustomItem IRON_DOOR_KEYCARD_3 = add(new KeycardDoorCustomItem(3));
    public static final CustomItem IRON_DOOR_KEYCARD_4 = add(new KeycardDoorCustomItem(4));
    public static final CustomItem IRON_DOOR_KEYCARD_5 = add(new KeycardDoorCustomItem(5));
    public static final CustomItem IRON_DOOR_KEYCARD_6 = add(new KeycardDoorCustomItem(6));
    public static final CustomItem ABILITY_CREEPER_EXPLODE = add(new AbilityCreeperExplodeCustomItem());

    private static CustomItem add(CustomItem item) {
        ITEMS.add(item);

        return item;
    }

    public static @Nullable CustomItem get(String id) {
        for (CustomItem item : ITEMS) {
            if (item.getId().equals(id))
                return item;
        }

        return null;
    }

    public static void register() {
        System.out.println("Registered " + ITEMS.stream().count() + " custom items ! Use /minebreach inventory to see all custom items");
    }
}