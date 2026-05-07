package phasico.precise.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import phasico.precise.Precise;

public class PreciseSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Precise.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_GRAB =
            register("item.precise_staff.grab");

    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_RELEASE =
            register("item.precise_staff.release");

    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_MODE_SWITCH =
            register("item.precise_staff.mode_switch");

    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_IDLE =
            register("item.precise_staff.idle");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Precise.path(name)));
    }
}
