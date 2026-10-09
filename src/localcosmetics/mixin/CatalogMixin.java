package localcosmetics.mixin;

import localcosmetics.LocalCosmetics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "org.polyfrost.polyplus.client.cosmetics.CosmeticCatalog", remap = false)
public abstract class CatalogMixin {
    @Inject(method = {"ownedIds", "ownedCosmeticIds", "ownedEmoteIds", "ownedCosmeticGroupIds"}, at = @At("HEAD"), remap = false)
    private void expose(CallbackInfoReturnable<Object> cir) { LocalCosmetics.exposeCatalog(this); }

    @Inject(method = "setSelectedEmote", at = @At("HEAD"), remap = false)
    private void exposeEmotes(CallbackInfo ci) { LocalCosmetics.exposeCatalog(this); }

    @Inject(method = "localEquipped", at = @At("HEAD"), cancellable = true, remap = false)
    private void equipped(CallbackInfoReturnable<Object> cir) { cir.setReturnValue(LocalCosmetics.localEquipped(this)); }

    @Inject(method = "getRemoteEquipped", at = @At("HEAD"), cancellable = true, remap = false)
    private void localPlayer(java.util.UUID uuid, CallbackInfoReturnable<Object> cir) {
        if (LocalCosmetics.overridesPlayer(uuid)) cir.setReturnValue(LocalCosmetics.localMap(this));
    }

    @Inject(method = "getActiveId", at = @At("HEAD"), cancellable = true, remap = false)
    private void active(java.util.UUID uuid, @Coerce Object slot, CallbackInfoReturnable<Integer> cir) {
        if (LocalCosmetics.overridesPlayer(uuid)) cir.setReturnValue((Integer)((java.util.Map<?,?>)LocalCosmetics.localMap(this)).get(slot));
    }

    @Inject(method = "name=/^setEquipped-gIAlu-s$/", at = @At("HEAD"), cancellable = true, remap = false)
    private void equip(@Coerce Object request, @Coerce Object continuation, CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(LocalCosmetics.equip(this, request));
    }

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    private void reset(CallbackInfo ci) { LocalCosmetics.reset(); }
}
