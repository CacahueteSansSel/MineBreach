package dev.cacahuete.minebreach.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

public class KeycardCustomItem extends CustomItem {
    String title;
    String id;
    int level;
    Item baseItem;

    public KeycardCustomItem(String title, String id, int level, Item baseItem) {
        this.title = title;
        this.id = id;
        this.level = level;
        this.baseItem = baseItem;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public Item getBaseItem() {
        return baseItem;
    }

    @Override
    public String getTitle() {
        return title + " Keycard";
    }

    @Override
    public String getId() {
        return "keycard_" + id;
    }

    @Override
    public void addCustomNbtData(NbtCompound nbt) {
        nbt.putInt("level", level);

        super.addCustomNbtData(nbt);
    }
}
