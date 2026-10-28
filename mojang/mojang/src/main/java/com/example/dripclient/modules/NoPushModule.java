package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.client.MinecraftClient;

/**
 * NoPush: gra nie wypycha Cie z bloku (np. po perle). Przez mixin w ClientPlayerEntity.
 * Opcja "Przechodz przez bloki" wylacza kolizje, gdy jestes w srodku bloku.
 * Serwer moze cofnac taki ruch.
 */
public class NoPushModule extends Module {
    private final Setting.Bool passThrough = add(new Setting.Bool("Przechodź przez bloki", true));

    private boolean setNoClip = false;

    public NoPushModule() {
        super("NoPush", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        boolean inside = passThrough.value && mc.player.isInsideWall();
        if (inside != setNoClip) {
            mc.player.noClip = inside;
            setNoClip = inside;
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (setNoClip && mc.player != null) mc.player.noClip = false;
        setNoClip = false;
    }
}
