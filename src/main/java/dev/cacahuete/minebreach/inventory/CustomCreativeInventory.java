package dev.cacahuete.minebreach.inventory;

import dev.cacahuete.minebreach.items.CustomItem;
import dev.cacahuete.minebreach.items.CustomItems;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CustomCreativeInventory {
    public static void openFor(ServerPlayerEntity player) {
        SimpleInventory inventory = new SimpleInventory(54);

        for (CustomItem item : CustomItems.ITEMS) {
            inventory.addStack(item.buildStack(1));
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, p) -> GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, inventory), Text.of("Minebreach Creative Inventory")));
    }
}
