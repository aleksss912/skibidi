package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Freecam: kamera odlatuje od postaci, a postac stoi w miejscu. Kliknieciem niszczysz i stawiasz bloki
 * tam, gdzie patrzy kamera - ale serwer przyjmie to tylko w normalnym zasiegu od ciala.
 */
public class FreecamModule extends Module {
    private final Setting.Num speed = add(new Setting.Num("Prędkość", 8, 1, 40, 1));

    private ArmorStandEntity cam;
    private Input savedInput;
    private Vec3d pos;
    private long last = 0;

    public FreecamModule() {
        super("Freecam", Category.RENDER);
    }

    @Override
    public void onLoad() {
        enabled = false; // freecam nigdy nie startuje wlaczony
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            enabled = false;
            return;
        }
        pos = mc.player.getEyePos();
        cam = new ArmorStandEntity(mc.world, pos.x, pos.y, pos.z);
        savedInput = mc.player.input;
        mc.player.input = new Input(); // postac przestaje reagowac na klawisze
        mc.setCameraEntity(cam);
        last = System.nanoTime();
        place(mc.player.getYaw(), mc.player.getPitch());
    }

    @Override
    public void onDisable() {
        stop();
    }

    private void stop() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            if (savedInput != null) mc.player.input = savedInput;
            mc.setCameraEntity(mc.player);
        }
        cam = null;
        savedInput = null;
    }

    private void place(float yaw, float pitch) {
        double y = pos.y - cam.getStandingEyeHeight();
        cam.setPosition(pos.x, y, pos.z);
        cam.prevX = pos.x;
        cam.prevY = y;
        cam.prevZ = pos.z;
        cam.setYaw(yaw);
        cam.setPitch(pitch);
        cam.prevYaw = yaw;
        cam.prevPitch = pitch;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            // swiat zniknal (wyjscie z gry) - wylacz bez ruszania nieistniejacego gracza
            enabled = false;
            cam = null;
            savedInput = null;
        }
    }

    @Override
    public void onFrame(MinecraftClient mc, float delta) {
        if (cam == null || mc.player == null) return;
        long now = System.nanoTime();
        float dt = MathHelper.clamp((now - last) / 1_000_000_000f, 0f, 0.1f);
        last = now;

        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        Vec3d forward = Vec3d.fromPolar(pitch, yaw);
        double yawRad = Math.toRadians(yaw);
        Vec3d right = new Vec3d(-Math.cos(yawRad), 0, -Math.sin(yawRad));

        Vec3d move = Vec3d.ZERO;
        if (mc.options.forwardKey.isPressed()) move = move.add(forward);
        if (mc.options.backKey.isPressed()) move = move.subtract(forward);
        if (mc.options.rightKey.isPressed()) move = move.add(right);
        if (mc.options.leftKey.isPressed()) move = move.subtract(right);
        if (mc.options.jumpKey.isPressed()) move = move.add(0, 1, 0);
        if (mc.options.sneakKey.isPressed()) move = move.add(0, -1, 0);
        if (move.lengthSquared() > 1e-6) {
            pos = pos.add(move.normalize().multiply(speed.value * dt));
        }
        place(yaw, pitch);
    }
}
