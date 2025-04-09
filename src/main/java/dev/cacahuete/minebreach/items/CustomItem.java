package dev.cacahuete.minebreach.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public abstract class CustomItem {
    public abstract Item getBaseItem();
    public abstract String getTitle();
    public abstract String getId();
    public boolean canBeDropped() {
        return true;
    }

    public ItemStack buildStackCustomPass(ItemStack stack) {
        return stack;
    }

    public ItemStack buildStack(int count) {
        ItemStack item = new ItemStack(getBaseItem(), count);
        NbtCompound nbt = new NbtCompound();

        addCustomNbtData(nbt);

        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal(getTitle()).setStyle(Style.EMPTY.withItalic(false)));
        item.set(DataComponentTypes.ITEM_MODEL, Identifier.of("minebreach", getId()));
        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        return buildStackCustomPass(item);
    }

    public void addCustomNbtData(NbtCompound nbt) {
        nbt.putString("id", getId());
    }

    public void use(ItemStack stack, BlockPos pos, World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {

    }

    @Override
    public String toString() {
        return getTitle() + " (" + getId() + ")";
    }
}
