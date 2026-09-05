package sirskoi.tameabat.mixin;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashFenceKnotEntity.class)
public abstract class leashwall {

    @Inject(method = "survives", at = @At("HEAD"), cancellable = true)
    private void allowWalls(CallbackInfoReturnable<Boolean> cir) {
        LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
        BlockState state = knot.level().getBlockState(knot.getPos());
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
            cir.setReturnValue(true);
        }
    }
}