package phasico.precise.content.precise_staff;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;
import phasico.precise.index.PreciseKeys;
import phasico.precise.index.PreciseSoundEvents;
import phasico.precise.network.packets.PreciseStaffActionPacket;
import phasico.precise.network.packets.PreciseStaffDragPacket;

import java.util.function.BiConsumer;

public class PreciseStaffClientHandler {

    // ── Step sizes ───────────────────────────────────────────────────────────
    private static final double STEP_FINE    = 1.0 / 128.0; // Tab
    private static final double STEP_DEFAULT = 1.0 / 16.0;

    private static final double ANGLE_FINE    = 0.1;  // Tab
    private static final double ANGLE_DEFAULT = 1.0;
    private static final double ANGLE_COARSE  = 90.0; // Ctrl

    // ── State ────────────────────────────────────────────────────────────────
    private ClientDragSession session;
    private boolean rotateMode = false;
    public BiConsumer<SubLevel, Vec3> grabEffectCallback = (s, h) -> {};
    // Previous-tick key states for edge-detection. Order: W, S, D, A, Space, Shift
    private final boolean[] prevKeys = new boolean[6];
    // Per-key committed directions (locked at first press, cleared on release).
    // Translation [0]=W,[1]=S,[2]=D,[3]=A — the cardinal forward/right at press time.
    // Rotation    [0]=W,[1]=S,[2]=D,[3]=A — the world axis to rotate around at press time.
    private final Vec3[] committedMoveDir = new Vec3[4];
    private final Vec3[] committedRotAxis = new Vec3[4];
    // Set by translation commands so tick() doesn't re-anchor targetPos before the body moves.
    private boolean hasManualTarget = false;

    public boolean isDragging() { return session != null; }
    public boolean isRotateMode() { return rotateMode; }
    public ClientDragSession getSession() { return session; }

    // ── Client tick ───────────────────────────────────────────────────────────
    public void tick() {
        Minecraft mc     = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || !PreciseStaffItem.isHolding(player)) {
            stopDragging();
            return;
        }
        if (session == null) return;
        if (session.subLevel.isRemoved()) { stopDragging(); return; }

        boolean[] cur = {
            mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
            mc.options.keyRight.isDown(), mc.options.keyLeft.isDown(),
            mc.options.keyJump.isDown(), mc.options.keyShift.isDown()
        };

        if (rotateMode) {
            applyRotationNudge(player, cur);
        } else {
            applyTranslationNudge(player, cur);
        }
        System.arraycopy(cur, 0, prevKeys, 0, 6);
        sendDragPacket();
    }

    private void applyTranslationNudge(LocalPlayer player, boolean[] cur) {
        double step = computeStep();

        // Always derive horizontal forward from yaw — valid even when looking straight up/down.
        double yawRad = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = forward.cross(new Vec3(0, 1, 0));

        // Commit direction on first press; clear on release.
        if (cur[0] && !prevKeys[0]) committedMoveDir[0] = forward; else if (!cur[0]) committedMoveDir[0] = null;
        if (cur[1] && !prevKeys[1]) committedMoveDir[1] = forward; else if (!cur[1]) committedMoveDir[1] = null;
        if (cur[2] && !prevKeys[2]) committedMoveDir[2] = right;   else if (!cur[2]) committedMoveDir[2] = null;
        if (cur[3] && !prevKeys[3]) committedMoveDir[3] = right;   else if (!cur[3]) committedMoveDir[3] = null;

        boolean moved = false;
        Vec3 d;
        if (cur[0] && (d = committedMoveDir[0]) != null) { nudgePos( d.x * step, 0,  d.z * step); moved = true; }
        if (cur[1] && (d = committedMoveDir[1]) != null) { nudgePos(-d.x * step, 0, -d.z * step); moved = true; }
        if (cur[2] && (d = committedMoveDir[2]) != null) { nudgePos( d.x * step, 0,  d.z * step); moved = true; }
        if (cur[3] && (d = committedMoveDir[3]) != null) { nudgePos(-d.x * step, 0, -d.z * step); moved = true; }
        if (cur[4]) { nudgePos(0,  step, 0); moved = true; }
        if (cur[5]) { nudgePos(0, -step, 0); moved = true; }

        if (moved) {
            hasManualTarget = false;
        } else if (!hasManualTarget) {
            // Re-anchor so the motor sees zero error while idle — no coasting.
            Vec3 anchor = session.subLevel.logicalPose().transformPosition(
                    new Vec3(session.localAnchor.x, session.localAnchor.y, session.localAnchor.z));
            session.targetPos.set(anchor.x, anchor.y, anchor.z);
        }
    }

    private void applyRotationNudge(LocalPlayer player, boolean[] cur) {
        double a       = Math.toRadians(computeAngleDeg());
        boolean coarse = Screen.hasControlDown();
        boolean shift  = cur[5];

        Vec3 look = player.getLookAngle();
        Vec3 forward = Math.abs(look.x) >= Math.abs(look.z)
                ? new Vec3(Math.signum(look.x), 0, 0)
                : new Vec3(0, 0, Math.signum(look.z));
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        Vec3 up    = new Vec3(0, 1, 0);

        // W/S pitch around view-right axis; A/D yaw (Y) or roll (view-forward) with Shift.
        // Commit axis on first press; Shift state at press time decides A/D axis.
        if (cur[0] && !prevKeys[0]) committedRotAxis[0] = right;              else if (!cur[0]) committedRotAxis[0] = null;
        if (cur[1] && !prevKeys[1]) committedRotAxis[1] = right;              else if (!cur[1]) committedRotAxis[1] = null;
        if (cur[2] && !prevKeys[2]) committedRotAxis[2] = shift ? forward : up; else if (!cur[2]) committedRotAxis[2] = null;
        if (cur[3] && !prevKeys[3]) committedRotAxis[3] = shift ? forward : up; else if (!cur[3]) committedRotAxis[3] = null;

        Vec3 ax;
        if (fires(coarse, cur[0], prevKeys[0]) && (ax = committedRotAxis[0]) != null)
            rotateWorldAxis(session.targetOrientation, -a, ax);
        if (fires(coarse, cur[1], prevKeys[1]) && (ax = committedRotAxis[1]) != null)
            rotateWorldAxis(session.targetOrientation,  a, ax);
        if (fires(coarse, cur[2], prevKeys[2]) && (ax = committedRotAxis[2]) != null)
            rotateWorldAxis(session.targetOrientation,  a, ax);
        if (fires(coarse, cur[3], prevKeys[3]) && (ax = committedRotAxis[3]) != null)
            rotateWorldAxis(session.targetOrientation, -a, ax);
        // Space unassigned in rotation mode.
    }

    // Rotate q around a world-space axis: new_q = rot(angle, axis) * q.
    private static void rotateWorldAxis(Quaterniond q, double angle, Vec3 axis) {
        new Quaterniond().rotateAxis(angle, axis.x, axis.y, axis.z).mul(q, q);
    }

    // Coarse mode: only fires on the tick the key first goes down (one step per press).
    // Fine / default: fires every tick while held (continuous movement).
    private static boolean fires(boolean coarse, boolean isDown, boolean wasDown) {
        return coarse ? (isDown && !wasDown) : isDown;
    }

    private void nudgePos(double dx, double dy, double dz) {
        session.targetPos.add(dx, dy, dz);
    }

    private void sendDragPacket() {
        if (session == null) return;
        VeilPacketManager.server().sendPacket(new PreciseStaffDragPacket(
                session.subLevel.getUniqueId(),
                new Vector3d(session.targetPos),
                new Vector3d(session.localAnchor),
                new Quaterniond(session.bodyOrientation),
                new Quaterniond(session.targetOrientation)
        ));
    }

    // ── Mouse / key handlers ──────────────────────────────────────────────────

    // RMB: grab in movement mode; if already dragging, release.
    public void onRightClick() {
        if (session != null) {
            stopDragging();
            return;
        }
        startDrag();
    }

    // LMB: lock/unlock.
    // While dragging: lock the grabbed body and release.
    // Otherwise: lock/unlock whichever physics body is being aimed at.
    // Returns true if the click was consumed.
    public boolean onLeftClick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        if (session != null) {
            player.playSound(PreciseSoundEvents.STAFF_MODE_SWITCH.get());
            sendLockPacket(session.subLevel.getUniqueId());
            grabEffectCallback.accept(session.subLevel,
                    session.subLevel.logicalPose().transformPosition(
                            new Vec3(session.localAnchor.x, session.localAnchor.y, session.localAnchor.z)));
            stopDragging();
            return true;
        }

        HitResult hit = player.pick(PreciseStaffItem.RANGE, 1.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) return false;
        SubLevel subLevel = Sable.HELPER.getContainingClient(hit.getLocation());
        if (subLevel == null) return false;

        player.playSound(PreciseSoundEvents.STAFF_MODE_SWITCH.get());
        sendLockPacket(subLevel.getUniqueId());
        grabEffectCallback.accept(subLevel, hit.getLocation());
        return true;
    }

    // R key (toggle): switch rotate/move mode while dragging, show action bar message.
    public void toggleMode() {
        if (session == null) return;
        rotateMode = !rotateMode;
        // Reset committed directions/axes so the new mode starts clean.
        java.util.Arrays.fill(committedMoveDir, null);
        java.util.Arrays.fill(committedRotAxis, null);
        java.util.Arrays.fill(prevKeys, false);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(PreciseSoundEvents.STAFF_MODE_SWITCH.get());
            player.displayClientMessage(
                    Component.translatable(rotateMode
                            ? "precise.mode.rotate"
                            : "precise.mode.move"),
                    true);
        }
    }

    // ── Command mode ──────────────────────────────────────────────────────────

    // Called by CommandInputScreen when Enter is pressed.
    // Parses [command][value] and applies transformation to the target.
    // On syntax error: shows red action-bar message and returns without doing anything.
    public void executeCommand(String raw) {
        if (session == null) return;
        LocalPlayer player = Minecraft.getInstance().player;

        String input = raw.trim().toUpperCase();
        String cmd = null;
        String valStr = null;
        // Longer prefixes must come before shorter ones (RX/RY/RZ before R, GRID before G).
        for (String c : new String[]{"RX", "RY", "RZ", "GRID", "R", "G", "X", "Y", "Z", "W", "A", "S", "D"}) {
            if (input.startsWith(c)) {
                cmd = c;
                valStr = input.substring(c.length()).trim();
                break;
            }
        }

        if (cmd == null) {
            showCommandError(player, "Unknown command: " + raw.trim());
            return;
        }

        //No value commands
        if (cmd.equals("G") || cmd.equals("GRID")) {

            if(!valStr.isEmpty()){

                showCommandError(player, "The command \"" + cmd + "\" does not accept any parameters.");

            }

            snapToGrid();
            sendDragPacket();
            return;

        }

        if (valStr.isEmpty()) {

            showCommandError(player, "The syntax of \"" + cmd +  "\" is: " + cmd + " [value]");
            return;
        }


        double value;
        try {
            value = Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            showCommandError(player, "Cannot understand \"" + valStr + "\"");
            return;
        }

        double limit = cmd.startsWith("R") ? 360.0 : 128.0;
        if (value < -limit || value > limit) {
            showCommandError(player, "Value out of range [-" + (int) limit + ", " + (int) limit + "]");
            return;
        }

        switch (cmd) {
            // Positive value = clockwise: from +X for RX, from above for RY, from +Z for RZ.
            case "RX" -> session.targetOrientation.rotateAxis(Math.toRadians(value), 1, 0, 0);
            case "RY" -> session.targetOrientation.rotateAxis(Math.toRadians(-value), 0, 1, 0);
            case "RZ" -> session.targetOrientation.rotateAxis(Math.toRadians(value), 0, 0, 1);
            // Positive value = clockwise from player's view.
            case "R"  -> applyViewAlignedRotation(player, Math.toRadians(value));
            // Translation: move along world absolute axis by the given number of blocks.
            case "X"  -> { session.targetPos.add(value, 0, 0); hasManualTarget = true; }
            case "Y"  -> { session.targetPos.add(0, value, 0); hasManualTarget = true; }
            case "Z"  -> { session.targetPos.add(0, 0, value); hasManualTarget = true; }
            // Translation: move relative to player's view direction.
            case "W", "S", "A", "D" -> { applyViewAlignedTranslation(player, value, cmd); hasManualTarget = true; }
        }
        sendDragPacket();
    }

    // Rotate around whichever local body axis best aligns with the player's look direction.
    // Behaves like the corresponding RX/RY/RZ: positive = clockwise in that axis's natural direction.
    private void applyViewAlignedRotation(LocalPlayer player, double rad) {
        if (player == null) return;
        Vec3 look = player.getLookAngle();

        // Body's three local axes expressed in world space.
        Vector3d xWorld = session.targetOrientation.transform(new Vector3d(1, 0, 0), new Vector3d());
        Vector3d yWorld = session.targetOrientation.transform(new Vector3d(0, 1, 0), new Vector3d());
        Vector3d zWorld = session.targetOrientation.transform(new Vector3d(0, 0, 1), new Vector3d());
        Vector3d[] worldAxes = {xWorld, yWorld, zWorld};

        int best = 0;
        double bestAbsDot = 0;
        for (int i = 0; i < 3; i++) {
            Vector3d a = worldAxes[i];
            double d = Math.abs(look.x * a.x + look.y * a.y + look.z * a.z);
            if (d > bestAbsDot) { bestAbsDot = d; best = i; }
        }

        // If the best axis points into the screen (dot > 0), use rad as-is.
        // If it points out of the screen (dot < 0), flip so the rotation is still
        // clockwise from the player's perspective.
        Vector3d bw = worldAxes[best];
        double dot = look.x * bw.x + look.y * bw.y + look.z * bw.z;
        double signedRad = rad * Math.signum(dot);

        switch (best) {
            case 0 -> session.targetOrientation.rotateAxis(signedRad, 1, 0, 0);
            case 1 -> session.targetOrientation.rotateAxis(signedRad, 0, 1, 0);
            case 2 -> session.targetOrientation.rotateAxis(signedRad, 0, 0, 1);
        }

        String axisName = best == 0 ? "X" : best == 1 ? "Y" : "Z";
        String directionName = Math.signum(rad) != Math.signum(signedRad) ? "Negative" : "Positive";
        player.displayClientMessage(Component.literal(directionName + " " + axisName), true);
    }

    private void applyViewAlignedTranslation(LocalPlayer player, double value, String dir) {
        if (player == null) return;
        Vec3 look = player.getLookAngle();
        Vec3 forward = Math.abs(look.x) >= Math.abs(look.z)
                ? new Vec3(Math.signum(look.x), 0, 0)
                : new Vec3(0, 0, Math.signum(look.z));
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        switch (dir) {
            case "W" -> session.targetPos.add( forward.x * value, 0,  forward.z * value);
            case "S" -> session.targetPos.add(-forward.x * value, 0, -forward.z * value);
            case "D" -> session.targetPos.add( right.x   * value, 0,  right.z   * value);
            case "A" -> session.targetPos.add(-right.x   * value, 0, -right.z   * value);
        }
    }

    private void snapToGrid() {
        // targetPos is the world position of the grabbed block's center (localAnchor = blockPos + 0.5).
        // A grid-aligned position has components n + 0.5, so round to nearest such value.
        session.targetPos.set(
                Math.rint(session.targetPos.x - 0.5) + 0.5,
                Math.rint(session.targetPos.y - 0.5) + 0.5,
                Math.rint(session.targetPos.z - 0.5) + 0.5);
        session.targetOrientation.set(snapedOrientation(session.targetOrientation));
        hasManualTarget = true;
    }

    // Returns the axis-aligned quaternion closest to q.
    // Greedily maps each local body axis to the world axis it most resembles,
    // then rebuilds the rotation matrix and converts back to a quaternion.
    private static Quaterniond snapedOrientation(Quaterniond q) {
        Vector3d[] local = {
            q.transform(new Vector3d(1, 0, 0), new Vector3d()),
            q.transform(new Vector3d(0, 1, 0), new Vector3d()),
            q.transform(new Vector3d(0, 0, 1), new Vector3d())
        };

        int[]     worldAxis = {-1, -1, -1};
        int[]     sign      = new int[3];
        boolean[] used      = new boolean[3];

        for (int iter = 0; iter < 3; iter++) {
            int bestLocal = -1, bestWorld = -1, bestSign = 1;
            double bestAbs = -1;
            for (int li = 0; li < 3; li++) {
                if (worldAxis[li] != -1) continue;
                double[] c = {local[li].x, local[li].y, local[li].z};
                for (int wi = 0; wi < 3; wi++) {
                    if (used[wi]) continue;
                    if (Math.abs(c[wi]) > bestAbs) {
                        bestAbs   = Math.abs(c[wi]);
                        bestLocal = li;
                        bestWorld = wi;
                        bestSign  = c[wi] >= 0 ? 1 : -1;
                    }
                }
            }
            worldAxis[bestLocal] = bestWorld;
            sign[bestLocal]      = bestSign;
            used[bestWorld]      = true;
        }

        Matrix3d m = new Matrix3d();
        for (int i = 0; i < 3; i++) {
            Vector3d col = new Vector3d();
            col.setComponent(worldAxis[i], sign[i]);
            m.setColumn(i, col);
        }
        return m.getNormalizedRotation(new Quaterniond());
    }

    private static void showCommandError(LocalPlayer player, String msg) {
        if (player != null)
            player.displayClientMessage(
                    Component.literal(msg).withStyle(s -> s.withColor(0xFF5555)), true);
    }

    private void sendLockPacket(java.util.UUID subLevelUUID) {
        VeilPacketManager.server().sendPacket(
                new PreciseStaffActionPacket(PreciseStaffAction.LOCK, subLevelUUID));
    }

    private void startDrag() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        HitResult hit = player.pick(PreciseStaffItem.RANGE, 1.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) return;

        SubLevel subLevel = Sable.HELPER.getContainingClient(hit.getLocation());
        if (subLevel == null) return;

        Vector3d localAnchor = new Vector3d(
                blockHit.getBlockPos().getX() + 0.5,
                blockHit.getBlockPos().getY() + 0.5,
                blockHit.getBlockPos().getZ() + 0.5);
        Vec3 worldAnchor  = subLevel.logicalPose().transformPosition(new Vec3(localAnchor.x, localAnchor.y, localAnchor.z));
        Vector3d targetPos = new Vector3d(worldAnchor.x, worldAnchor.y, worldAnchor.z);
        Quaterniond bodyOri  = new Quaterniond(subLevel.logicalPose().orientation());
        Quaterniond targetOri = new Quaterniond(bodyOri);
        java.util.Arrays.fill(prevKeys, false);
        java.util.Arrays.fill(committedMoveDir, null);
        java.util.Arrays.fill(committedRotAxis, null);
        rotateMode = false;
        hasManualTarget = false;
        session = new ClientDragSession(subLevel, localAnchor, targetPos, bodyOri, targetOri);
        player.playSound(PreciseSoundEvents.STAFF_GRAB.get());
        grabEffectCallback.accept(subLevel, hit.getLocation());
    }

    public void stopDragging() {
        if (session == null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.playSound(PreciseSoundEvents.STAFF_RELEASE.get());
        rotateMode = false;
        hasManualTarget = false;
        VeilPacketManager.server().sendPacket(
                new PreciseStaffActionPacket(PreciseStaffAction.STOP_DRAG, session.subLevel.getUniqueId()));
        session = null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static double computeStep() {
        if (PreciseKeys.FINE_MODE.isDown()) return STEP_FINE;
        return STEP_DEFAULT;
    }

    private static double computeAngleDeg() {
        if (Screen.hasControlDown())        return ANGLE_COARSE;
        if (PreciseKeys.FINE_MODE.isDown()) return ANGLE_FINE;
        return ANGLE_DEFAULT;
    }

    // ── Drag session ──────────────────────────────────────────────────────────
    public static class ClientDragSession {
        public final SubLevel     subLevel;
        public final Vector3d     localAnchor;
        public final Vector3d     targetPos;
        public final Quaterniond  bodyOrientation;
        public final Quaterniond  targetOrientation;

        public ClientDragSession(SubLevel subLevel, Vector3d localAnchor,
                                 Vector3d targetPos,
                                 Quaterniond bodyOrientation, Quaterniond targetOrientation) {
            this.subLevel          = subLevel;
            this.localAnchor       = localAnchor;
            this.targetPos         = targetPos;
            this.bodyOrientation   = bodyOrientation;
            this.targetOrientation = targetOrientation;
        }
    }

    // ── InteractCallback — registered from PreciseClient (neoforge module) ────
    public static class MouseHandler implements InteractCallback {

        private final PreciseStaffClientHandler handler;

        public MouseHandler(PreciseStaffClientHandler handler) {
            this.handler = handler;
        }

        @Override
        public Result onUse(int modifiers, int action, KeyMapping rightKey) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !PreciseStaffItem.isHolding(mc.player)) return Result.empty();
            if (action == GLFW.GLFW_PRESS) {
                handler.onRightClick();
                return new Result(true);
            }
            return Result.empty();
        }

        @Override
        public Result onAttack(int modifiers, int action, KeyMapping leftKey) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !PreciseStaffItem.isHolding(mc.player)) return Result.empty();
            if (action == GLFW.GLFW_PRESS) {
                return handler.onLeftClick() ? new Result(true) : Result.empty();
            }
            return Result.empty();
        }
    }
}
