package dev.cacahuete.minebreach.mixin;

import dev.cacahuete.minebreach.MinebreachController;
import dev.cacahuete.minebreach.MinebreachParty;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    public void remove(ServerPlayerEntity player, CallbackInfo ci) {
        MinebreachParty party = MinebreachController.getPartyForWorld(player.getServerWorld());
        if (party == null || !party.isPlayerInParty(player)) return;

        party.removePlayer(player, true);
        System.out.println("[Minebreach] Player " + player.getDisplayName().getString() + " disconnected : removed from Minebreach party");
    }
}
