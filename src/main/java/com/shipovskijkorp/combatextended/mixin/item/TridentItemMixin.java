package com.shipovskijkorp.combatextended.mixin.item;

import com.shipovskijkorp.combatextended.combat.tuning.TridentTuning;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void combatextended$replaceTridentAttributes(CallbackInfoReturnable<ItemAttributeModifiers> cir) {
        cir.setReturnValue(TridentTuning.createAttributeModifiers());
    }
}
