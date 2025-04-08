package dev.cacahuete.minebreach.mixin;

import dev.cacahuete.minebreach.MinebreachController;
import dev.cacahuete.minebreach.MinebreachParty;
import dev.cacahuete.minebreach.items.CustomItem;
import dev.cacahuete.minebreach.items.CustomItems;
import dev.cacahuete.minebreach.items.KeycardCustomItem;
import dev.cacahuete.minebreach.roles.GameRole;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    public void onUse(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!entity.canUsePortals(false) || !(entity instanceof ServerPlayerEntity player)) return;

        MinebreachParty worldParty = MinebreachController.getPartyForWorld((ServerWorld)world);
        if (worldParty == null) return;

        GameRole role = Roles.get(worldParty.getPlayerRole(player));
        if (role.upgradesToId.isEmpty()) {
            ci.cancel();
            return;
        }

        worldParty.upgradePlayer(player);
        ci.cancel();
    }
}
