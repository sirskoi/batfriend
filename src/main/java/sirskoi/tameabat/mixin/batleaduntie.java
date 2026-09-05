package sirskoi.tameabat.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sirskoi.tameabat.entity.TameableBat;

@Mixin(Mob.class)
public abstract class batleaduntie {

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void allowBatLeash(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Bat bat) {
            TameableBat tameable = (TameableBat) (Object) bat;
            if (tameable.isTamed()) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("resource")
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if ((Object) this instanceof Bat bat) {
            TameableBat tameable = (TameableBat) (Object) bat;
            ItemStack itemstack = player.getItemInHand(hand);
            Level level = player.level();

            // taming
            if (!tameable.isTamed()) {
                if (itemstack.is(Items.HONEYCOMB)) {
                    if (!level.isClientSide()) {
                        tameable.addHoneycombEaten();
                        if (!player.isCreative()) {
                            itemstack.shrink(1);
                        }

                        if (tameable.getHoneycombsEaten() >= tameable.getHoneycombRequirement()) {
                            tameable.setTamed(true);
                            tameable.setOwnerUuid(player.getUUID());
                            bat.setPersistenceRequired();

                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                        ParticleTypes.HEART,
                                        bat.getX(), bat.getY() + 0.3D, bat.getZ(),
                                        7, 0.2D, 0.2D, 0.2D, 0.1D
                                );
                            }
                        } else {
                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                        ParticleTypes.COMPOSTER,
                                        bat.getX(), bat.getY() + 0.2D, bat.getZ(),
                                        4, 0.1D, 0.1D, 0.1D, 0.05D
                                );
                            }
                        }
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
                return;
            }

            // honeycomb calms vanilla ai, wakes up and heals
            if (itemstack.is(Items.HONEYCOMB) && (tameable.isRelaxed() || tameable.hasVanillaAi() || bat.getHealth() < bat.getMaxHealth())) {
                if (!level.isClientSide()) {
                    tameable.setRelaxed(false);
                    tameable.setVanillaAi(false);
                    bat.setResting(false);

                    if (bat.getHealth() < bat.getMaxHealth()) {
                        bat.heal(4.0F);
                    }

                    bat.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.2F);
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.HEART,
                                bat.getX(), bat.getY() + 0.3D, bat.getZ(),
                                6, 0.2D, 0.2D, 0.2D, 0.1D
                        );
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // sugar triggers vanilla ai
            if (bat.isLeashed() && itemstack.is(Items.SUGAR) && !tameable.hasVanillaAi()) {
                if (!level.isClientSide()) {
                    tameable.setVanillaAi(true);
                    tameable.setRelaxed(false);
                    bat.setResting(false);

                    bat.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.4F);
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.HAPPY_VILLAGER,
                                bat.getX(), bat.getY() + 0.2D, bat.getZ(),
                                7, 0.2D, 0.2D, 0.2D, 0.05D
                        );
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // spidereye relaxes bat from flying
            if (bat.isLeashed() && itemstack.is(Items.SPIDER_EYE) && !tameable.isRelaxed()) {
                if (!level.isClientSide()) {
                    tameable.setRelaxed(true);
                    tameable.setVanillaAi(false);
                    bat.setResting(false);

                    bat.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.8F);
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.SMOKE,
                                bat.getX(), bat.getY() + 0.2D, bat.getZ(),
                                6, 0.15D, 0.15D, 0.15D, 0.02D
                        );
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // dye bat
            String itemName = itemstack.getItem().toString().toLowerCase();
            DyeColor appliedColor = null;

            if (itemName.contains("light_gray_dye") || itemName.contains("light_grey_dye")) {
                appliedColor = DyeColor.LIGHT_GRAY;
            } else if (itemName.contains("gray_dye") || itemName.contains("grey_dye")) {
                appliedColor = DyeColor.GRAY;
            } else {
                for (DyeColor color : DyeColor.values()) {
                    if (itemName.contains(color.getName() + "_dye")) {
                        appliedColor = color;
                        break;
                    }
                }
            }

            if (appliedColor != null) {
                if (appliedColor != tameable.getBatColor()) {
                    if (!level.isClientSide()) {
                        tameable.setBatColor(appliedColor);
                        if (!player.isCreative()) {
                            itemstack.shrink(1);
                        }
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // lead attach
            if (itemstack.is(Items.LEAD) && !bat.isLeashed()) {
                if (!level.isClientSide()) {
                    bat.setLeashedTo(player, true);
                    if (!player.isCreative()) {
                        itemstack.shrink(1);
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // lead detach
            if (bat.isLeashed()) {
                if (!level.isClientSide()) {
                    bat.dropLeash();
                    tameable.setRelaxed(false);
                    tameable.setVanillaAi(false);
                    bat.setResting(false);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}