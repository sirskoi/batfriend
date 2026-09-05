package sirskoi.tameabat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.AABB;
import sirskoi.tameabat.entity.TameableBat;

import java.util.List;

public class Tameabat implements ModInitializer {

    private int tickCounter = 0;

    @Override
    @SuppressWarnings("resource")
    public void onInitialize() {
        // night vision checks every 10 seconds
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= 100) {
                tickCounter = 0;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    AABB searchBox = player.getBoundingBox().inflate(10.0D);
                    List<Bat> nearbyBats = player.level().getEntitiesOfClass(Bat.class, searchBox, bat ->
                            bat instanceof TameableBat t && t.isTamed() &&
                                    t.getOwnerUuid() != null && t.getOwnerUuid().equals(player.getUUID())
                    );

                    if (!nearbyBats.isEmpty()) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
                    }
                }
            }
        });
    }
}