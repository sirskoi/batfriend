package sirskoi.tameabat.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sirskoi.tameabat.ai.batfollow;
import sirskoi.tameabat.entity.TameableBat;

import java.util.UUID;

@Mixin(Bat.class)
@SuppressWarnings({"AddedMixinMembersNamePattern", "EntityClassMismatch"})
public abstract class batentity extends Mob implements TameableBat {

    @Shadow public abstract void setResting(boolean resting);

    @Unique
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Integer> BAT_COLOR = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Boolean> RELAXED = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Boolean> VANILLA_AI = SynchedEntityData.defineId(Bat.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private UUID ownerUuid = null;

    @Unique
    private int honeycombsEaten = 0;

    @Unique
    private int honeycombRequirement = 0;

    protected batentity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityType<? extends Bat> entityType, Level level, CallbackInfo ci) {
        this.goalSelector.addGoal(1, new batfollow((Bat) (Object) this));

        if (!level.isClientSide()) {
            this.honeycombRequirement = this.getRandom().nextInt(20) + 1;
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void onCustomServerAiStep(ServerLevel level, CallbackInfo ci) {
        if (this.isTamed()) {
            if (this.getCustomName() == null && this.ownerUuid != null) {
                Player player = level.getPlayerByUUID(this.ownerUuid);
                if (player != null) {
                    this.setCustomName(Component.literal(player.getName().getString() + "'s Bat"));
                }
            }

            // sugar activation
            if (this.hasVanillaAi()) {
                return;
            }

            // wake up when unleashed
            if (!this.isLeashed() && this.isRelaxed()) {
                this.setRelaxed(false);
                this.setResting(false);
            }

            // relax
            if (this.isRelaxed()) {
                this.setResting(false);
                if (!this.onGround()) {
                    this.setDeltaMovement(0.0D, -0.15D, 0.0D);
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }

                if (this.isLeashed()) {
                    Entity holder = this.getLeashHolder();
                    if (holder instanceof Player player) {
                        if (this.distanceToSqr(player) <= 25.0D) {
                            Vec3 toPlayer = player.getEyePosition().subtract(this.getEyePosition());
                            float yRot = (float)(Math.atan2(toPlayer.z, toPlayer.x) * (180.0D / Math.PI)) - 90.0F;
                            float xRot = (float)(-(Math.atan2(toPlayer.y, toPlayer.horizontalDistance()) * (180.0D / Math.PI)));

                            this.setXRot(xRot);
                            this.setYRot(yRot);
                            this.yBodyRot = yRot;
                            this.yHeadRot = yRot;
                        }
                    } else if (holder != null) {
                        Vec3 awayFromFence = this.position().subtract(holder.position());
                        if (awayFromFence.horizontalDistanceSqr() > 0.001D) {
                            float yRot = (float)(Math.atan2(awayFromFence.z, awayFromFence.x) * (180.0D / Math.PI)) - 90.0F;

                            this.setXRot(0.0F);
                            this.setYRot(yRot);
                            this.yBodyRot = yRot;
                            this.yHeadRot = yRot;
                        }
                    }
                }

                ci.cancel();
                return;
            }

            // rotate logic
            if (this.isLeashed()) {
                Entity holder = this.getLeashHolder();

                if (holder instanceof Player player) {
                    if (this.distanceToSqr(player) <= 25.0D) {
                        Vec3 toPlayer = player.getEyePosition().subtract(this.getEyePosition());
                        float yRot = (float)(Math.atan2(toPlayer.z, toPlayer.x) * (180.0D / Math.PI)) - 90.0F;
                        float xRot = (float)(-(Math.atan2(toPlayer.y, toPlayer.horizontalDistance()) * (180.0D / Math.PI)));

                        this.setXRot(xRot);
                        this.setYRot(yRot);
                        this.yBodyRot = yRot;
                        this.yHeadRot = yRot;
                    }
                } else if (holder != null) {
                    Vec3 awayFromFence = this.position().subtract(holder.position());
                    if (awayFromFence.horizontalDistanceSqr() > 0.001D) {
                        float yRot = (float)(Math.atan2(awayFromFence.z, awayFromFence.x) * (180.0D / Math.PI)) - 90.0F;

                        this.setXRot(0.0F);
                        this.setYRot(yRot);
                        this.yBodyRot = yRot;
                        this.yHeadRot = yRot;
                    }
                }
            }

            super.customServerAiStep(level);
            ci.cancel();
        }
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void onDefineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(TAMED, false);
        builder.define(BAT_COLOR, DyeColor.WHITE.getId());
        builder.define(RELAXED, false);
        builder.define(VANILLA_AI, false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("IsTamed", this.isTamed());
        output.putInt("HoneycombsEaten", this.honeycombsEaten);
        output.putInt("HoneycombRequirement", this.honeycombRequirement);
        output.putInt("BatColor", this.getBatColor().getId());
        output.putBoolean("IsRelaxed", this.isRelaxed());
        output.putBoolean("VanillaAi", this.hasVanillaAi());

        if (this.ownerUuid != null) {
            output.putString("OwnerUuid", this.ownerUuid.toString());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        this.setTamed(input.getBooleanOr("IsTamed", false));
        this.honeycombsEaten = input.getIntOr("HoneycombsEaten", 0);
        this.honeycombRequirement = input.getIntOr("HoneycombRequirement", 0);
        if (this.honeycombRequirement <= 0) {
            this.honeycombRequirement = this.getRandom().nextInt(20) + 1;
        }
        this.setBatColor(DyeColor.byId(input.getIntOr("BatColor", DyeColor.WHITE.getId())));
        this.setRelaxed(input.getBooleanOr("IsRelaxed", false));
        this.setVanillaAi(input.getBooleanOr("VanillaAi", false));

        String savedUuid = input.getStringOr("OwnerUuid", "");
        if (!savedUuid.isEmpty()) {
            try {
                this.ownerUuid = UUID.fromString(savedUuid);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (this.isTamed()) {
            this.setPersistenceRequired();
            var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(18.0D);
            }
        }
    }

    @Override
    public boolean isTamed() {
        return this.getEntityData().get(TAMED);
    }

    @Override
    public void setTamed(boolean tamed) {
        this.getEntityData().set(TAMED, tamed);
        if (tamed) {
            this.setPersistenceRequired();
            var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(18.0D);
                this.setHealth(18.0F);
            }
        }
    }

    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Override
    public int getHoneycombsEaten() {
        return this.honeycombsEaten;
    }

    @Override
    public void addHoneycombEaten() {
        this.honeycombsEaten++;
    }

    @Override
    public int getHoneycombRequirement() {
        if (this.honeycombRequirement <= 0) {
            this.honeycombRequirement = this.getRandom().nextInt(20) + 1;
        }
        return this.honeycombRequirement;
    }

    @Override
    public DyeColor getBatColor() {
        return DyeColor.byId(this.getEntityData().get(BAT_COLOR));
    }

    @Override
    public void setBatColor(DyeColor color) {
        this.getEntityData().set(BAT_COLOR, color.getId());
    }

    @Override
    public boolean isRelaxed() {
        return this.getEntityData().get(RELAXED);
    }

    @Override
    public void setRelaxed(boolean relaxed) {
        this.getEntityData().set(RELAXED, relaxed);
    }

    @Override
    public boolean hasVanillaAi() {
        return this.getEntityData().get(VANILLA_AI);
    }

    @Override
    public void setVanillaAi(boolean vanillaAi) {
        this.getEntityData().set(VANILLA_AI, vanillaAi);
    }
}