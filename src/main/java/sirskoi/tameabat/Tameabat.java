package sirskoi.tameabat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.AABB;
import sirskoi.tameabat.entity.TameableBat;

import java.util.List;

public class Tameabat implements ModInitializer {

    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            return InteractionResult.PASS;
        });

        // 15 second night vision with 10 second check
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= 200) {
                tickCounter = 0;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    AABB searchBox = player.getBoundingBox().inflate(10.0D);
                    List<Bat> nearbyBats = player.level().getEntitiesOfClass(Bat.class, searchBox, bat ->
                            bat instanceof TameableBat t && t.isTamed() &&
                                    t.getOwnerUuid() != null && t.getOwnerUuid().equals(player.getUUID())
                    );

                    if (!nearbyBats.isEmpty()) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
                    }
                }
            }
        });
    }
}