package phasico.precise.neoforge;

import com.simibubi.create.CreateClient;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.index.SimClickInteractions;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import phasico.precise.content.precise_staff.PreciseStaffClientHandler;
import phasico.precise.content.precise_staff.PreciseStaffItem;
import phasico.precise.index.PreciseKeys;
import phasico.precise.index.PreciseSoundEvents;

public class PreciseClient {

    public static final PreciseStaffClientHandler STAFF_HANDLER = new PreciseStaffClientHandler();
    private static LoopingSoundInstance idleSound = null;

    public static void init(IEventBus modBus) {
        modBus.addListener(PreciseClient::registerKeys);
        SimClickInteractions.register(new PreciseStaffClientHandler.MouseHandler(STAFF_HANDLER));
        PreciseStaffRenderHandler renderHandler = new PreciseStaffRenderHandler(STAFF_HANDLER);
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(renderHandler::onRenderLevelStage);

        STAFF_HANDLER.grabEffectCallback = (SubLevel subLevel, net.minecraft.world.phys.Vec3 hitLocation) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            InteractionHand hand = mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof PreciseStaffItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            CreateClient.ZAPPER_RENDER_HANDLER.shoot(hand, subLevel.logicalPose().transformPosition(hitLocation));

            RandomSource random = mc.level.getRandom();
            for (int i = 0; i < 10; i++) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        hitLocation.x, hitLocation.y, hitLocation.z,
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.2,
                        (random.nextDouble() - 0.5) * 0.2);
            }
        };
    }

    public static void tickIdleSound(Minecraft mc) {
        if (!STAFF_HANDLER.isDragging()) {
            if (idleSound != null) idleSound.setVolume(0.0f);
            return;
        }
        if (idleSound == null) {
            idleSound = new LoopingSoundInstance((LocalPlayer) mc.player, mc.level.getRandom());
        }
        if (!mc.getSoundManager().isActive(idleSound)) {
            mc.getSoundManager().play(idleSound);
        }
        idleSound.setVolume(1.0f);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        PreciseKeys.registerTo(event::register);
    }

    public static class LoopingSoundInstance extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        public LoopingSoundInstance(LocalPlayer player, RandomSource random) {
            super(PreciseSoundEvents.STAFF_IDLE.get(), SoundSource.PLAYERS, random);
            this.player = player;
        }

        public void setVolume(float volume) { this.volume = volume; }

        @Override public double getX() { return player.position().x(); }
        @Override public double getY() { return player.position().y(); }
        @Override public double getZ() { return player.position().z(); }
        @Override public void tick() {}
    }
}
