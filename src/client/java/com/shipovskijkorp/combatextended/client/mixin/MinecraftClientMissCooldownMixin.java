package com.shipovskijkorp.combatextended.client.mixin;

import com.shipovskijkorp.combatextended.combat.cooldown.MissCooldowns;
import com.shipovskijkorp.combatextended.mixin.accessor.LivingEntityAttackCooldownAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMissCooldownMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    public HitResult hitResult;

    @Shadow
    protected int missTime;

    @Unique
    private int combatExtended$missTimeBeforeAttack;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void combatExtended$captureMissTimeBeforeAttack(CallbackInfoReturnable<Boolean> cir) {
        this.combatExtended$missTimeBeforeAttack = this.missTime;
    }

    @Inject(method = "startAttack", at = @At("RETURN"))
    private void combatExtended$reduceCooldownAfterMiss(CallbackInfoReturnable<Boolean> cir) {
        if (this.combatExtended$missTimeBeforeAttack > 0) {
            return;
        }

        if (this.player == null || this.hitResult == null) {
            return;
        }

        if (this.hitResult.getType() != HitResult.Type.MISS) {
            return;
        }

        if (!MissCooldowns.shouldReduceMissCooldown(this.player.getMainHandItem())) {
            return;
        }

        if (this.missTime <= 0) {
            return;
        }

        this.missTime = MissCooldowns.reduceClientMissCooldown(this.missTime);
        MissCooldowns.advanceAttackCooldownProgressAfterMiss(this.player);
    }
}
