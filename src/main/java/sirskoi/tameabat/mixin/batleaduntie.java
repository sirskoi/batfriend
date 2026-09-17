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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sirskoi.tameabat.entity.TameableBat;

@Mixin(Mob.class)
public abstract class batleaduntie {

    @Unique
    @SuppressWarnings("resource")
    private void awardAdvancement(Player player, String advancementId) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.server.MinecraftServer server = serverPlayer.level().getServer();

            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.fromNamespaceAndPath("tameabat", advancementId);
            net.minecraft.advancements.AdvancementHolder adv = server.getAdvancements().get(id);
            if (adv != null) {
                net.minecraft.advancements.AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    for (String crit : progress.getRemainingCriteria()) {
                        serverPlayer.getAdvancements().award(adv, crit);
                    }
                }
            }
        }
    }

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void allowBatLeash(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Bat bat) {
            TameableBat tameable = (TameableBat) bat;
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
            TameableBat tameable = (TameableBat) bat;
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

                            awardAdvancement(player, "bat_friend");
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

                    boolean healed = false;
                    if (bat.getHealth() < bat.getMaxHealth()) {
                        bat.heal(4.0F);
                        healed = true;
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

                    if (healed) {
                        awardAdvancement(player, "flesh_wound");
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

                    awardAdvancement(player, "mach_6");
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // check if the item is a valid relaxing fruit
            boolean isRelaxFruit = itemstack.is(Items.APPLE) || itemstack.is(Items.GLOW_BERRIES) ||
                    itemstack.is(Items.CHORUS_FRUIT) || itemstack.is(Items.SWEET_BERRIES) ||
                    itemstack.is(Items.MELON_SLICE);

            // fruit relaxes bat from flying
            if (bat.isLeashed() && isRelaxFruit && !tameable.isRelaxed()) {
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

                    awardAdvancement(player, "spa_day");
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
                        awardAdvancement(player, "battastic");
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