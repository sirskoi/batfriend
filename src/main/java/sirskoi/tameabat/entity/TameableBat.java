package sirskoi.tameabat.entity;

import net.minecraft.world.item.DyeColor;
import java.util.UUID;

public interface TameableBat {
    boolean isTamed();
    void setTamed(boolean tamed);

    UUID getOwnerUuid();
    void setOwnerUuid(UUID uuid);

    int getHoneycombsEaten();
    void addHoneycombEaten();
    int getHoneycombRequirement();

    DyeColor getBatColor();
    void setBatColor(DyeColor color);

    boolean isRelaxed();
    void setRelaxed(boolean relaxed);

    boolean hasVanillaAi();
    void setVanillaAi(boolean vanillaAi);
}