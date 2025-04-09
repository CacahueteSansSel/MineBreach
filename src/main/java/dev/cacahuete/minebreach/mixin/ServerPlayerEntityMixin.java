package dev.cacahuete.minebreach.mixin;

import dev.cacahuete.minebreach.MinebreachController;
import dev.cacahuete.minebreach.MinebreachParty;
import dev.cacahuete.minebreach.items.CustomItem;
import dev.cacahuete.minebreach.items.CustomItems;
import dev.cacahuete.minebreach.roles.Roles;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        MinebreachParty party = MinebreachController.getPartyForWorld(player.getServerWorld());
        if (party == null || !party.isPlayerInParty(player)) return;

        ServerPlayerEntity attacker = damageSource.getAttacker() instanceof ServerPlayerEntity p ? p : null;
        if (!party.isPlayerInParty(attacker)) attacker = null;

        party.setPlayerRole(player, Roles.SPECTATOR.index, false);
        player.changeGameMode(GameMode.SPECTATOR);
        player.setHealth(player.defaultMaxHealth);
        if (attacker != null) player.networkHandler.sendPacket(new SetCameraEntityS2CPacket(attacker));

        ci.cancel();
    }

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    public void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        NbtComponent nbt = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return; // Not a custom item

        String id = nbt.copyNbt().getString("id");
        CustomItem item = CustomItems.get(id);
        if (item == null) return;

        if (!item.canBeDropped()) {
            if (!player.isDead()) player.giveItemStack(stack.copy());

            cir.setReturnValue(null);
            cir.cancel();
        }
    }
}
