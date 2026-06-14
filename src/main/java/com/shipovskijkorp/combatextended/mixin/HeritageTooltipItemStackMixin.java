package com.shipovskijkorp.combatextended.mixin;

import com.shipovskijkorp.combatextended.combat.compatibility.heritage.HeritageOfGodsCompatibility;
import com.shipovskijkorp.combatextended.combat.tuning.CombatWeaponTooltip;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class HeritageTooltipItemStackMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void combatextended$replaceHeritageStatTooltip(
            Item.TooltipContext context,
            @Nullable PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!HeritageOfGodsCompatibility.shouldPatchTooltip(stack)) {
            return;
        }

        if (CombatWeaponTooltip.shouldShowOriginalValues()) {
            return;
        }

        List<Text> tooltip = new ArrayList<>(cir.getReturnValue());
        if (HeritageOfGodsCompatibility.applyTooltipPatch(stack, player, tooltip)) {
            cir.setReturnValue(tooltip);
        }
    }
}
