package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Swiecace gwiazdki lataja po okolicy (rozsiane po calej mapie wokol, a nie przy graczu) oraz
 * od czasu do czasu przelatuje spadajaca gwiazda. Ilosc ustawiasz suwakiem (FPS).
 */
public class StarsModule extends Module {
    private final Setting.Num amount = add(new Setting.Num("Ilość", 0.6, 0.1, 1.0, 0.1));
    private final Setting.Num range = add(new Setting.Num("Zasięg", 28, 12, 30, 2));
    private final Setting.Bool shooting = add(new Setting.Bool("Spadające gwiazdy", true));

    private final Random rng = new Random();

    public StarsModule() {
        super("Gwiazdki", Category.RENDER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        Vec3d me = mc.player.getPos();

        int n = 1 + (int) Math.round(5 * amount.value);
        for (int i = 0; i < n; i++) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double dist = 8 + rng.nextDouble() * (range.value - 8);
            double x = me.x + Math.cos(ang) * dist;
            double z = me.z + Math.sin(ang) * dist;
            double y = me.y - 3 + rng.nextDouble() * 26;
            ParticleEffect type = rng.nextInt(6) == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD;
            mc.world.addParticle(type, x, y, z,
                    (rng.nextDouble() - 0.5) * 0.07, (rng.nextDouble() - 0.4) * 0.05, (rng.nextDouble() - 0.5) * 0.07);
        }

        if (shooting.value && rng.nextInt(60) == 0) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double dist = 14 + rng.nextDouble() * 12;
            Vec3d start = new Vec3d(me.x + Math.cos(ang) * dist, me.y + 18 + rng.nextDouble() * 10, me.z + Math.sin(ang) * dist);
            Vec3d dir = new Vec3d(rng.nextDouble() - 0.5, -0.35, rng.nextDouble() - 0.5).normalize();
            for (int k = 0; k < 14; k++) {
                Vec3d at = start.add(dir.multiply(k * 0.45));
                mc.world.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z,
                        dir.x * 0.55, dir.y * 0.55, dir.z * 0.55);
            }
        }
    }
}
