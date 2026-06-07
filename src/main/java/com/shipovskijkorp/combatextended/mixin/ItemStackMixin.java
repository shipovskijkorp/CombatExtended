package com.shipovskijkorp.combatextended.mixin;

import com.shipovskijkorp.combatextended.combat.tuning.CombatWeaponTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void combatextended$replaceWeaponAttributeTooltip(
            Consumer<Component> textConsumer,
            TooltipDisplay displayComponent,
            Player player,
            CallbackInfo ci
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        if (!CombatWeaponTooltip.shouldReplaceAttributeTooltip(stack)) {
            return;
        }

        CombatWeaponTooltip.appendVanillaStyleAttributes(stack, player, textConsumer);
        ci.cancel();
    }
}
