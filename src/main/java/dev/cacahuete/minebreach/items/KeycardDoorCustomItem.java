package dev.cacahuete.minebreach.items;

import dev.cacahuete.minebreach.persistent.MineBreachPersistentState;
import dev.cacahuete.minebreach.persistent.MineBreachStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class KeycardDoorCustomItem extends CustomItem {
    int level;

    public KeycardDoorCustomItem(int level) {
        this.level = level;
    }

    @Override
    public Item getBaseItem() {
        return Items.IRON_DOOR;
    }

    @Override
    public String getTitle() {
        return "Keycard Iron Door (Level " + level + ")";
    }

    @Override
    public String getId() {
        return "iron_door_keycard_" + level;
    }

    @Override
    public void use(ItemStack stack, BlockPos pos, World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        super.use(stack, pos, world, user, hand, cir);

        MineBreachPersistentState state = MineBreachStorage.getOrCreate((ServerWorld)world);
        state.addKeycardDoorEntry(pos, level);
    }
}
