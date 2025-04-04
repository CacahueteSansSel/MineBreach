package dev.cacahuete.minebreach.mixin;

import dev.cacahuete.minebreach.MinebreachController;
import dev.cacahuete.minebreach.core.TickScheduler;
import dev.cacahuete.minebreach.items.CustomItem;
import dev.cacahuete.minebreach.items.CustomItems;
import dev.cacahuete.minebreach.items.KeycardCustomItem;
import dev.cacahuete.minebreach.persistent.MineBreachPersistentState;
import dev.cacahuete.minebreach.persistent.MineBreachStorage;
import dev.cacahuete.minebreach.roles.GameRole;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static net.minecraft.block.DoorBlock.OPEN;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {
    @Shadow
    protected abstract void playOpenCloseSound(@Nullable Entity entity, World world, BlockPos pos, boolean open);

    void openDoor(PlayerEntity player, BlockState state, World world, BlockPos pos, boolean automatic, DoorBlock doorBlock) {
        player.sendMessage(Text.literal(automatic ? "Access granted for 5 seconds" : "Access granted").formatted(Formatting.GREEN), true);
        state = state.cycle(OPEN);
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
        playOpenCloseSound(null, world, pos, state.get(OPEN));
        world.emitGameEvent(player, doorBlock.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);

        final BlockState finalState = state;
        if (automatic) {
            TickScheduler.schedule(100, () -> {
                if (world.getBlockState(pos).getBlock() != doorBlock) return;

                world.setBlockState(pos, finalState.with(OPEN, false), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
                playOpenCloseSound(null, world, pos, false);
                world.emitGameEvent(player, GameEvent.BLOCK_CLOSE, pos);
            });
        }
    }

    @Inject(method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        DoorBlock doorBlock = (DoorBlock)(Object)this;
        if (doorBlock.getBlockSetType() != BlockSetType.IRON) return;

        Optional<Integer> level = Optional.empty();
        boolean automatic = false;
        CommandBlockBlockEntity cm;
        if (world.getBlockEntity(pos.down()) instanceof CommandBlockBlockEntity commandBlock)
            cm = commandBlock;
        else if (world.getBlockEntity(pos.down(2)) instanceof CommandBlockBlockEntity commandBlock)
            cm = commandBlock;
        else return;

        String command = cm.getCommandExecutor().getCommand();
        List<String> prefix = Arrays.stream(command.split(" ")).toList();

        if (prefix.size() >= 2 && Objects.equals(prefix.getFirst(), "access"))
        {
            level = Optional.of(Integer.parseInt(prefix.get(1)));

            automatic = prefix.contains("auto");
        }

        int playerRoleIndex = MinebreachController.getPartyForWorld((ServerWorld)MinebreachController.getMainGameWorld(player.getServer())).getPlayerRole((ServerPlayerEntity)player);
        if (Roles.get(playerRoleIndex).team == GameRole.Team.Creatures)
        {
            openDoor(player, state, world, pos, automatic, doorBlock);
            cir.setReturnValue(ActionResult.SUCCESS);

            return;
        }

        ItemStack currentItem = player.getStackInHand(player.getActiveHand());
        NbtComponent itemNbt = currentItem.get(DataComponentTypes.CUSTOM_DATA);
        if (itemNbt == null) return; // Not a custom item

        String id = itemNbt.copyNbt().getString("id");
        CustomItem item = CustomItems.get(id);
        if (item == null) return;
        if (!(item instanceof KeycardCustomItem keycard)) return;

        if (level.get() > keycard.getLevel()) {
            player.sendMessage(Text.literal("Access denied").formatted(Formatting.RED), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        openDoor(player, state, world, pos, automatic, doorBlock);

        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
