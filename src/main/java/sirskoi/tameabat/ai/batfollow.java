package sirskoi.tameabat.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import sirskoi.tameabat.entity.TameableBat;

import java.util.EnumSet;
import java.util.UUID;

public class batfollow extends Goal {

    private final Bat bat;
    private final TameableBat tameableBat;
    private Player owner;

    private int flutterCooldown = 0;
    private int playerIdleTicks = 0;
    private Vec3 lastPlayerPos = null;

    public batfollow(Bat bat) {
        this.bat = bat;
        this.tameableBat = (TameableBat) bat;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.tameableBat.isTamed()) {
            return false;
        }

        UUID ownerUuid = this.tameableBat.getOwnerUuid();
        if (ownerUuid == null) {
            return false;
        }

        this.owner = this.bat.level().getPlayerByUUID(ownerUuid);
        if (this.owner == null || this.owner.isSpectator()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.owner != null && this.owner.isAlive() && !this.owner.isSpectator();
    }

    @Override
    public void start() {
        if (this.owner != null) {
            this.lastPlayerPos = this.owner.position();
        }
        this.playerIdleTicks = 0;
        this.bat.setNoGravity(true);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.bat.setNoGravity(false);
    }

    @Override
    public void tick() {
        if (this.owner == null) {
            return;
        }

        this.bat.setNoGravity(true);
        this.bat.setOnGround(false);

        if (this.bat.isLeashed()) {
            handleLeashedHovering();
            return;
        }

        Vec3 currentPos = this.owner.position();
        boolean moved = this.lastPlayerPos == null || currentPos.distanceToSqr(this.lastPlayerPos) > 0.01D;
        boolean interacted = this.owner.swinging || this.owner.attackAnim > 0.0F;

        if (!moved && !interacted) {
            this.playerIdleTicks++;
        } else {
            this.playerIdleTicks = 0;
            this.lastPlayerPos = currentPos;
        }

        if (this.playerIdleTicks >= 200) {
            handleIdleHovering();
        } else {
            handleActiveFollow();
        }
    }

    private void handleLeashedHovering() {
        Entity leashHolder = this.bat.getLeashHolder();
        if (leashHolder != null) {
            double targetX = leashHolder.getX();
            double targetY = leashHolder.getY();
            double targetZ = leashHolder.getZ();

            double diffX = this.bat.getX() - leashHolder.getX();
            double diffZ = this.bat.getZ() - leashHolder.getZ();
            double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            double flutter = Math.sin(this.bat.tickCount * 0.25D) * 0.08D;

            if (leashHolder instanceof Player p) {
                double offsetX = horizontalDist > 0.1D ? (diffX / horizontalDist) * 2.0D : 2.0D;
                double offsetZ = horizontalDist > 0.1D ? (diffZ / horizontalDist) * 2.0D : 0.0D;

                targetX += offsetX;
                targetY = p.getEyeY() - 0.15D + flutter;
                targetZ += offsetZ;
            } else {
                double offsetX = horizontalDist > 0.1D ? (diffX / horizontalDist) * 1.0D : 1.0D;
                double offsetZ = horizontalDist > 0.1D ? (diffZ / horizontalDist) * 1.0D : 0.0D;

                targetX += offsetX;
                targetY += 0.25D + flutter;
                targetZ += offsetZ;
            }

            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            Vec3 toTarget = targetPos.subtract(this.bat.position());

            double distSq = toTarget.lengthSqr();
            double speedFactor = (leashHolder instanceof Player p && p.isSprinting()) ? 0.48D : 0.28D;

            if (distSq > 0.35D) {
                if (--this.flutterCooldown <= 0) {
                    this.flutterCooldown = 2 + this.bat.getRandom().nextInt(2);
                    Vec3 impulse = toTarget.normalize().scale(speedFactor);
                    Vec3 currentVel = this.bat.getDeltaMovement();
                    this.bat.setDeltaMovement(currentVel.scale(0.35D).add(impulse));
                }
            } else {
                this.bat.setDeltaMovement(this.bat.getDeltaMovement().scale(0.65D));
            }

            if (leashHolder instanceof Player p) {
                faceTarget(p.getEyePosition());
            } else {
                // face away from wall/fence
                Vec3 awayPos = new Vec3(
                        this.bat.getX() + (this.bat.getX() - leashHolder.getX()),
                        this.bat.getY(),
                        this.bat.getZ() + (this.bat.getZ() - leashHolder.getZ())
                );
                faceTarget(awayPos);
            }
        }
    }

    private void handleActiveFollow() {
        double distSq = this.bat.distanceToSqr(this.owner);

        if (distSq > 256.0D) {
            this.bat.teleportTo(this.owner.getX(), this.owner.getEyeY() - 0.15D, this.owner.getZ());
            return;
        }

        double diffX = this.bat.getX() - this.owner.getX();
        double diffZ = this.bat.getZ() - this.owner.getZ();
        double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        double offsetX = horizontalDist > 0.1D ? (diffX / horizontalDist) * 2.0D : 2.0D;
        double offsetZ = horizontalDist > 0.1D ? (diffZ / horizontalDist) * 2.0D : 0.0D;

        double flutter = Math.sin(this.bat.tickCount * 0.25D) * 0.08D;
        double targetY = this.owner.getEyeY() - 0.15D + flutter;
        Vec3 targetPos = new Vec3(this.owner.getX() + offsetX, targetY, this.owner.getZ() + offsetZ);

        Vec3 toTarget = targetPos.subtract(this.bat.position());

        double distance = Math.sqrt(distSq);
        double speedFactor = this.owner.isSprinting() ? 0.48D : 0.28D;
        if (distance > 6.0D) {
            speedFactor = Math.min(0.85D, speedFactor + (distance - 6.0D) * 0.06D);
        }

        if (toTarget.lengthSqr() > 0.35D) {
            if (--this.flutterCooldown <= 0) {
                this.flutterCooldown = 2 + this.bat.getRandom().nextInt(2);

                Vec3 impulse = toTarget.normalize().scale(speedFactor);
                Vec3 currentVel = this.bat.getDeltaMovement();
                this.bat.setDeltaMovement(currentVel.scale(0.35D).add(impulse));
            }
        } else {
            this.bat.setDeltaMovement(this.bat.getDeltaMovement().scale(0.65D));
        }

        faceTarget(this.owner.getEyePosition());
    }

    private void handleIdleHovering() {
        double distSq = this.bat.distanceToSqr(this.owner);

        if (distSq > 256.0D) {
            this.bat.teleportTo(this.owner.getX(), this.owner.getEyeY() - 0.15D, this.owner.getZ());
            return;
        }

        double diffX = this.bat.getX() - this.owner.getX();
        double diffZ = this.bat.getZ() - this.owner.getZ();
        double horizontalDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        double offsetX = horizontalDist > 0.1D ? (diffX / horizontalDist) * 1.5D : 1.5D;
        double offsetZ = horizontalDist > 0.1D ? (diffZ / horizontalDist) * 1.5D : 0.0D;

        double flutter = Math.sin(this.bat.tickCount * 0.2D) * 0.1D;
        double targetY = this.owner.getEyeY() - 0.15D + flutter;
        Vec3 targetPos = new Vec3(this.owner.getX() + offsetX, targetY, this.owner.getZ() + offsetZ);

        Vec3 toTarget = targetPos.subtract(this.bat.position());

        if (toTarget.lengthSqr() > 0.2D) {
            Vec3 impulse = toTarget.normalize().scale(0.15D);
            Vec3 currentVel = this.bat.getDeltaMovement();
            this.bat.setDeltaMovement(currentVel.scale(0.4D).add(impulse));
        } else {
            this.bat.setDeltaMovement(this.bat.getDeltaMovement().scale(0.5D));
        }

        faceTarget(this.owner.getEyePosition());
    }

    private void faceTarget(Vec3 target) {
        double dx = target.x - this.bat.getX();
        double dy = target.y - this.bat.getY();
        double dz = target.z - this.bat.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * (180.0D / Math.PI)));

        this.bat.setYRot(Mth.rotLerp(0.2F, this.bat.getYRot(), targetYaw));
        this.bat.setXRot(Mth.rotLerp(0.2F, this.bat.getXRot(), targetPitch));
        this.bat.setYHeadRot(this.bat.getYRot());
        this.bat.setYBodyRot(this.bat.getYRot());
    }
}