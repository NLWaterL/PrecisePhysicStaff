package phasico.precise.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllSpecialTextures;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler;
import dev.simulated_team.simulated.index.SimRenderTypes;
import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;
import phasico.precise.content.precise_staff.PreciseStaffClientHandler;
import phasico.precise.content.precise_staff.PreciseStaffItem;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class PreciseStaffRenderHandler {

    private final PreciseStaffClientHandler handler;

    @Nullable
    private BlockPos hoverBlockPos = null;

    // Cached reflection accessor for the protected PhysicsStaffClientHandler.getLocks(Level).
    @Nullable
    private static Method getLocks;

    static {
        try {
            getLocks = PhysicsStaffClientHandler.class.getDeclaredMethod("getLocks", Level.class);
            getLocks.setAccessible(true);
        } catch (Exception e) {
            getLocks = null;
        }
    }

    public PreciseStaffRenderHandler(PreciseStaffClientHandler handler) {
        this.handler = handler;
    }

    public void onRenderLevelStage(VeilRenderLevelStageEvent.Stage stage, LevelRenderer levelRenderer,
                                   MultiBufferSource.BufferSource bufferSource, MatrixStack matrixStack,
                                   Matrix4fc projectionMatrix, Matrix4fc frustumMatrix,
                                   int ticks, DeltaTracker deltaTracker, Camera camera, Frustum frustum) {
        if (stage != VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (Minecraft.getInstance().options.hideGui) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (!PreciseStaffItem.isHolding(player)) return;

        renderAllLocks(bufferSource, matrixStack, player.level(), camera.getPosition());

        updateHoverPos(minecraft, player);

        if (hoverBlockPos != null) {
            Color color = new Color(191.0f / 255.0f, 191.0f / 255.0f, 191.0f / 255.0f, 1.0f);
            Outliner.getInstance().showCluster("preciseStaffSelection", List.of(hoverBlockPos))
                    .colored(color.rgb())
                    .disableLineNormals()
                    .lineWidth(1 / 32f)
                    .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
    }

    // Copied verbatim from Simulated's PhysicsStaffRenderHandler.renderAllLocks().
    private static void renderAllLocks(MultiBufferSource.BufferSource bufferSource, MatrixStack ps,
                                       Level level, Vec3 cameraPos) {
        List<UUID> locks = getSimulatedLocks(level);
        if (locks == null || locks.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        SubLevelContainer container = SubLevelContainer.getContainer(level);

        for (UUID lock : locks) {
            SubLevel subLevel = container.getSubLevel(lock);
            if (!(subLevel instanceof ClientSubLevel clientSubLevel)) continue;

            ps.matrixPush();
            Vector3dc renderPos = clientSubLevel.renderPose().position();
            ps.translate(renderPos.x() - cameraPos.x, renderPos.y() - cameraPos.y, renderPos.z() - cameraPos.z);
            ps.rotate(client.getEntityRenderDispatcher().cameraOrientation());

            VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.lock());
            PoseStack.Pose pose = ps.pose();
            int color = 0xffffffff;
            buffer.addVertex(pose, 0.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 1.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 0.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 0.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 0.0f).setLight(LightTexture.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 1.0f).setLight(LightTexture.FULL_BRIGHT);

            ps.matrixPop();
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static List<UUID> getSimulatedLocks(Level level) {
        if (getLocks == null) return null;
        try {
            return (List<UUID>) getLocks.invoke(SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER, level);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateHoverPos(Minecraft minecraft, LocalPlayer player) {
        final ClientLevel level = minecraft.level;
        final float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        hoverBlockPos = null;

        PreciseStaffClientHandler.ClientDragSession dragSession = handler.getSession();

        if (dragSession != null) {
            hoverBlockPos = BlockPos.containing(
                    dragSession.localAnchor.x, dragSession.localAnchor.y, dragSession.localAnchor.z);
            return;
        }

        final LevelPoseProviderExtension extension = (LevelPoseProviderExtension) level;
        extension.sable$pushPoseSupplier(x -> ((ClientSubLevel) x).renderPose());
        final HitResult hit = player.pick(PreciseStaffItem.RANGE, partialTicks, false);
        extension.sable$popPoseSupplier();

        if (!(hit instanceof final BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        final Vec3 hitLocation = hit.getLocation();
        final SubLevel subLevel = Sable.HELPER.getContaining(level, hitLocation);
        if (subLevel == null) return;

        hoverBlockPos = blockHitResult.getBlockPos();
    }
}
