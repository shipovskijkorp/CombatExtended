package com.shipovskijkorp.combatextended.client.mixin;

import com.shipovskijkorp.combatextended.combat.cooldown.MissCooldowns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMissCooldownMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public HitResult crosshairTarget;

    @Shadow
    private int attackCooldown;

    @Unique
    private int combatExtended$attackCooldownBeforeAttack;

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void combatExtended$captureAttackCooldownBeforeAttack(CallbackInfoReturnable<Boolean> cir) {
        this.combatExtended$attackCooldownBeforeAttack = this.attackCooldown;
    }

    @Inject(method = "doAttack", at = @At("RETURN"))
    private void combatExtended$reduceCooldownAfterMiss(CallbackInfoReturnable<Boolean> cir) {
        if (this.combatExtended$attackCooldownBeforeAttack > 0) {
            return;
        }

        if (this.player == null || this.crosshairTarget == null) {
            return;
        }

        if (this.crosshairTarget.getType() != HitResult.Type.MISS) {
            return;
        }

        if (!MissCooldowns.shouldReduceMissCooldown(this.player.getMainHandStack())) {
            return;
        }

        if (this.attackCooldown <= 0) {
            return;
        }

        this.attackCooldown = MissCooldowns.reduceClientMissCooldown(this.attackCooldown);
        MissCooldowns.advanceAttackCooldownProgressAfterMiss(this.player);
    }
}
