package phasico.precise.network.packets;

import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import phasico.precise.Precise;
import phasico.precise.content.precise_staff.PreciseStaffServerHandler;

import java.util.UUID;

public record PreciseStaffDragPacket(
        UUID subLevel,
        Vector3dc targetWorldPos,
        Vector3dc localAnchor,
        Quaterniondc bodyOrientation,
        Quaterniondc targetOrientation
) implements CustomPacketPayload {

    public static final Type<PreciseStaffDragPacket> TYPE =
            new Type<>(ResourceLocation.tryBuild(Precise.MOD_ID, "precise_staff_drag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreciseStaffDragPacket> CODEC =
            StreamCodec.of((buf, v) -> v.write(buf), PreciseStaffDragPacket::read);

    private static PreciseStaffDragPacket read(RegistryFriendlyByteBuf buf) {
        return new PreciseStaffDragPacket(
                buf.readUUID(),
                SableBufferUtils.read(buf, new Vector3d()),
                SableBufferUtils.read(buf, new Vector3d()),
                SableBufferUtils.read(buf, new Quaterniond()),
                SableBufferUtils.read(buf, new Quaterniond())
        );
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(subLevel);
        SableBufferUtils.write(buf, targetWorldPos);
        SableBufferUtils.write(buf, localAnchor);
        SableBufferUtils.write(buf, bodyOrientation);
        SableBufferUtils.write(buf, targetOrientation);
    }

    public void handle(ServerPacketContext context) {
        ServerPlayer player = context.player();
        PreciseStaffServerHandler.get((ServerLevel) player.level())
                .drag(player.getUUID(), subLevel, targetWorldPos, localAnchor, bodyOrientation, targetOrientation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
