package sirskoi.tameabat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sirskoi.tameabat.entity.TameableBat;

import java.util.List;

@Mixin(LeadItem.class)
public class batlead {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player != null && (level.getBlockState(pos).is(BlockTags.FENCES) || level.getBlockState(pos).is(BlockTags.WALLS))) {
            List<Bat> bats = level.getEntitiesOfClass(Bat.class, new AABB(pos).inflate(7.0D));
            boolean hasBatToTie = false;

            for (Bat bat : bats) {
                if (bat instanceof TameableBat t && t.isTamed() && bat.getLeashHolder() == player) {
                    hasBatToTie = true;
                    break;
                }
            }

            if (hasBatToTie) {
                if (!level.isClientSide()) {
                    LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(level, pos);
                    knot.playPlacementSound();
                    for (Bat bat : bats) {
                        if (bat instanceof TameableBat t && t.isTamed() && bat.getLeashHolder() == player) {
                            bat.setLeashedTo(knot, true);
                        }
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}