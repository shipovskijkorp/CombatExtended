package com.shipovskijkorp.combatextended.combat.damage;

import com.shipovskijkorp.combatextended.mixin.accessor.PersistentProjectileEntityWeaponAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class MultishotProjectileDamage {
    private static final Identifier MULTISHOT_ID = Identifier.withDefaultNamespace("multishot");

    private MultishotProjectileDamage() {
    }

    public static boolean shouldBypassDamageCooldown(DamageSource source) {
        Entity sourceEntity = source.getDirectEntity();
        if (!(sourceEntity instanceof AbstractArrow projectile)) {
            return false;
        }

        ItemStack weaponStack = ((PersistentProjectileEntityWeaponAccessor) projectile).combatExtended$getWeaponStack();
        return weaponStack != null
                && !weaponStack.isEmpty()
                && weaponStack.getItem() instanceof CrossbowItem
                && hasMultishot(weaponStack);
    }

    private static boolean hasMultishot(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(MULTISHOT_ID) && entry.getIntValue() > 0) {
                return true;
            }
        }

        return false;
    }
}
