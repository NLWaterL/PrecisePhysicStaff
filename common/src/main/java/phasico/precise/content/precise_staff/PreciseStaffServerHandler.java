package phasico.precise.content.precise_staff;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PreciseStaffServerHandler {

    // High stiffness + matching damping — body snaps to target with no spring oscillation.
    private static final float LINEAR_STIFFNESS  = 50_000f;
    private static final float LINEAR_DAMPING    = 10_000f;
    private static final float ANGULAR_STIFFNESS = 50_000f;
    private static final float ANGULAR_DAMPING   = 10_000f;

    private static final Map<ServerLevel, PreciseStaffServerHandler> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final ServerLevel level;
    private final Map<UUID, DragSession> sessions = new HashMap<>();

    private PreciseStaffServerHandler(ServerLevel level) {
        this.level = level;
    }

    public static PreciseStaffServerHandler get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, PreciseStaffServerHandler::new);
    }

    // ── Packet handlers ───────────────────────────────────────────────────────

    public void drag(UUID playerUUID, UUID subLevelUUID,
                     Vector3dc targetWorldPos, Vector3dc localAnchor,
                     Quaterniondc bodyOrientation, Quaterniondc targetOrientation) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        SubLevel subLevel = container.getSubLevel(subLevelUUID);
        if (subLevel == null) return;

        // Delegate to Simulated's lock system so the lock is removed, broadcast to clients,
        // and the padlock icon disappears — same behaviour as the original physics staff.
        PhysicsStaffServerHandler.get(level).removeLock(subLevel);

        DragSession session = sessions.computeIfAbsent(playerUUID,
                id -> new DragSession(playerUUID, (ServerSubLevel) subLevel));
        session.targetWorldPos.set(targetWorldPos);
        session.localAnchor.set(localAnchor);
        session.bodyOrientation.set(bodyOrientation);
        session.targetOrientation.set(targetOrientation);
    }

    public void stopDrag(UUID playerUUID) {
        DragSession session = sessions.remove(playerUUID);
        if (session != null) session.removeConstraint();
    }

    // ── Game tick (called via SubLevelObserver) ───────────────────────────────

    public void tick() {
        sessions.entrySet().removeIf(entry -> {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(entry.getKey());
            if (player == null || !PreciseStaffItem.isHolding(player) || entry.getValue().subLevel.isRemoved()) {
                entry.getValue().removeConstraint();
                return true;
            }
            return false;
        });
    }

    // ── Physics tick (called via SableEventPlatform.onPhysicsTick) ───────────

    public void physicsTick(SubLevelPhysicsSystem physicsSystem) {
        sessions.values().forEach(s -> {
            if (!s.subLevel.isRemoved()) s.tick(physicsSystem);
        });
    }

    // ── Drag session ──────────────────────────────────────────────────────────

    private static final class DragSession {

        final UUID          playerUUID;
        final ServerSubLevel subLevel;

        final Vector3d   targetWorldPos    = new Vector3d();
        final Vector3d   localAnchor       = new Vector3d();
        final Quaterniond bodyOrientation   = new Quaterniond();
        final Quaterniond targetOrientation = new Quaterniond();

        private FreeConstraintHandle constraint;

        DragSession(UUID playerUUID, ServerSubLevel subLevel) {
            this.playerUUID = playerUUID;
            this.subLevel   = subLevel;
        }

        void tick(SubLevelPhysicsSystem physicsSystem) {
            removeConstraint();
            attachConstraint(physicsSystem);
            if (constraint == null) return;

            // Transform the world-space target into the constraint frame (targetOrientation).
            Vector3d localGoal = new Vector3d(targetWorldPos);
            targetOrientation.transformInverse(localGoal);

            constraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);
            constraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);
            constraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0);

            // Angular motors at 0 hold the body at targetOrientation (the constraint frame).
            for (ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
                constraint.setMotor(axis, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0);
            }
        }

        private void attachConstraint(SubLevelPhysicsSystem physicsSystem) {
            FreeConstraintConfiguration config = new FreeConstraintConfiguration(
                    new Vector3d(), localAnchor, targetOrientation);
            constraint = physicsSystem.getPipeline().addConstraint(null, subLevel, config);
        }

        void removeConstraint() {
            if (constraint != null) {
                constraint.remove();
                constraint = null;
            }
        }
    }
}
