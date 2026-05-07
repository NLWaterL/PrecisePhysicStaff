package phasico.precise.neoforge.mixin;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import phasico.precise.neoforge.PreciseClient;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void precise$suppressMovementWhenDragging(CallbackInfo ci) {
        if (PreciseClient.STAFF_HANDLER.isDragging()) {
            this.forwardImpulse = 0;
            this.leftImpulse = 0;
            this.jumping = false;
            this.shiftKeyDown = false;
        }
    }
}
