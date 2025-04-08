package dev.cacahuete.minebreach.items;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class AbilityCreeperExplodeCustomItem extends CustomItem {
    private static final ExplosionBehavior EXPLOSION_BEHAVIOR = new ExplosionBehavior() {
        @Override
        public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
            return false;
        }
    };

    @Override
    public Item getBaseItem() {
        return Items.LEATHER;
    }

    @Override
    public String getTitle() {
        return "[Creeper Ability] Explosion";
    }

    @Override
    public String getId() {
        return "ability_creeper_explosion";
    }

    @Override
    public void use(ItemStack stack, BlockPos pos, World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        world.createExplosion(user, Explosion.createDamageSource(world, user), EXPLOSION_BEHAVIOR, user.getX(), user.getY(), user.getZ(), 3.0F, false, World.ExplosionSourceType.MOB);
        user.getItemCooldownManager().set(stack, 100);
    }
}
