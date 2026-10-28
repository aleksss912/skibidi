package com.example.dripclient.mixin;

import com.example.dripclient.ModuleManager;
import com.example.dripclient.modules.NoPushModule;
import com.example.dripclient.modules.NoSignGuiModule;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    /** NoPush: gra nie wypycha Cie z bloku (np. po perle). */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void dripclient$noPush(double x, double z, CallbackInfo ci) {
        NoPushModule m = ModuleManager.get(NoPushModule.class);
        if (m != null && m.enabled) ci.cancel();
    }

    /** NoSignGUI: nie otwieraj okna edycji tabliczki. */
    @Inject(method = "openEditSignScreen", at = @At("HEAD"), cancellable = true)
    private void dripclient$noSignGui(SignBlockEntity sign, boolean front, CallbackInfo ci) {
        NoSignGuiModule m = ModuleManager.get(NoSignGuiModule.class);
        if (m != null && m.enabled) ci.cancel();
    }
}
