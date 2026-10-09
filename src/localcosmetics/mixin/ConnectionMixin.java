package localcosmetics.mixin;

import localcosmetics.LocalCosmetics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "org.polyfrost.polyplus.client.network.websocket.PolyConnection", remap = false)
public abstract class ConnectionMixin {
    @Inject(method = "name=/^sendPacket-IoAF18A$/", at = @At("HEAD"), cancellable = true, remap = false)
    private void local(@Coerce Object packet, CallbackInfoReturnable<Object> cir) {
        if (LocalCosmetics.localPacket(packet)) cir.setReturnValue(LocalCosmetics.unit());
    }
}
