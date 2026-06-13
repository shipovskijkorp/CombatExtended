package com.shipovskijkorp.combatextended.mixin;

import com.shipovskijkorp.combatextended.combat.tuning.CombatWeaponTooltip;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "appendAttributeModifiersTooltip", at = @At("HEAD"), cancellable = true)
    private void combatextended$replaceWeaponAttributeTooltip(
            Consumer<Text> textConsumer,
            TooltipDisplayComponent displayComponent,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        if (!CombatWeaponTooltip.shouldReplaceAttributeTooltip(stack)) {
            return;
        }

        if (CombatWeaponTooltip.shouldShowOriginalValues()) {
            CombatWeaponTooltip.appendOriginalValuesHeader(textConsumer);
            return;
        }

        CombatWeaponTooltip.appendVanillaStyleAttributes(stack, player, textConsumer);
        ci.cancel();
    }
}
