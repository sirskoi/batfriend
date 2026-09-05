package sirskoi.mixin;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sirskoi.tameabat.entity.TameableBat;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(BatRenderer.class)
public class battexture {

    @Unique
    private static final Map<DyeColor, Identifier> TEXTURES = new EnumMap<>(DyeColor.class);

    @Unique
    private static final Map<BatRenderState, DyeColor> BAT_COLORS = new WeakHashMap<>();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(Bat bat, BatRenderState state, float partialTicks, CallbackInfo ci) {
        if (bat instanceof TameableBat tameable && tameable.isTamed()) {
            DyeColor color = tameable.getBatColor();
            BAT_COLORS.put(state, color != null ? color : DyeColor.WHITE);
        } else {
            BAT_COLORS.remove(state);
        }
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(BatRenderState state, CallbackInfoReturnable<Identifier> cir) {
        DyeColor color = BAT_COLORS.get(state);
        if (color != null) {
            Identifier texture = TEXTURES.computeIfAbsent(color, c ->
                    Identifier.fromNamespaceAndPath("tameabat", "textures/entity/tamed_bat_" + c.getName() + ".png")
            );
            cir.setReturnValue(texture);
        }
    }
}