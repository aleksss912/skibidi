package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.mixin.LivingEntityAccessor;
import net.minecraft.client.MinecraftClient;

/** Brak opoznienia skoku: mozna skakac co tick, trzymajac spacje. */
public class NoJumpDelayModule extends Module {
    public NoJumpDelayModule() {
        super("NoJumpDelay", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        try {
            if (mc.player instanceof LivingEntityAccessor acc) acc.dripclient$setJumpingCooldown(0);
        } catch (Throwable ignored) {
        }
    }
}
