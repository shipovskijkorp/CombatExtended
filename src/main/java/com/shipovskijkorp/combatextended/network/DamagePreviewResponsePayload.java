package com.shipovskijkorp.combatextended.network;

import com.shipovskijkorp.combatextended.CombatExtended;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record DamagePreviewResponsePayload(
        int requestId,
        double baseAttackDamage,
        boolean itemCompatible
) implements CustomPayload {
    public static final Id<DamagePreviewResponsePayload> ID = new Id<>(CombatExtended.id("damage_preview_response"));
    public static final PacketCodec<RegistryByteBuf, DamagePreviewResponsePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            DamagePreviewResponsePayload::requestId,
            PacketCodecs.DOUBLE,
            DamagePreviewResponsePayload::baseAttackDamage,
            PacketCodecs.BOOLEAN,
            DamagePreviewResponsePayload::itemCompatible,
            DamagePreviewResponsePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
