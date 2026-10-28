package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import net.minecraft.client.MinecraftClient;

/** Auto-sprint: wystarczy trzymac W, a gra sama wlacza sprint. */
public class SprintModule extends Module {
    private boolean forced = false;

    public SprintModule() {
        super("Sprint", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        boolean forward = mc.options.forwardKey.isPressed();
        mc.options.sprintKey.setPressed(forward);
        forced = true;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (forced) mc.options.sprintKey.setPressed(false);
        forced = false;
    }
}
