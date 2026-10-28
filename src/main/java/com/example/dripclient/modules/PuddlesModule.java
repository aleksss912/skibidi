package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Draw;
import com.example.dripclient.Module;
import com.example.dripclient.Projection;
import com.example.dripclient.Setting;
import com.example.dripclient.UiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Za Toba zostaje slad wody: polprzezroczyste kaluze na ziemi. Gdy chodzisz po nich (i obok nich),
 * na wodzie pojawiaja sie rozchodzace sie kola i pluski. Rysowane jako nakladka (z ukrywaniem za blokami).
 */
public class PuddlesModule extends Module {
    private final Setting.Bool ripples = add(new Setting.Bool("Kręgi na wodzie", true));
    private final Setting.Bool splash = add(new Setting.Bool("Pluski", true));
    private final Setting.Num life = add(new Setting.Num("Czas kałuż (s)", 6, 2, 15, 1));
    private final Setting.Num size = add(new Setting.Num("Rozmiar", 0.7, 0.4, 1.2, 0.1));
    private final Setting.Num step = add(new Setting.Num("Odstęp (bloki)", 0.5, 0.3, 1.5, 0.1));

    private static final int MAX_PUDDLES = 18;
    private static final int MAX_RIPPLES = 14;

    private static final class Puddle {
        final double x, y, z;
        final long born;
        boolean visible = true;

        Puddle(double x, double y, double z, long born) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.born = born;
        }
    }

    private record Ripple(double x, double y, double z, long born) {
    }

    private final List<Puddle> puddles = new ArrayList<>();
    private final List<Ripple> ring = new ArrayList<>();
    private final Random rng = new Random();
    private Vec3d lastPos = null;
    private Vec3d lastPuddle = null;
    private int tickN = 0;

    public PuddlesModule() {
        super("Kałuże", Category.RENDER);
    }

    @Override
    public void onDisable() {
        puddles.clear();
        ring.clear();
        lastPos = null;
        lastPuddle = null;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        tickN++;

        long now = System.nanoTime();
        Vec3d pos = p.getPos();
        double moved = lastPos == null ? 0 : Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z);
        boolean moving = moved > 0.04 && p.isOnGround();
        lastPos = pos;

        if (moving) {
            if (lastPuddle == null || lastPuddle.squaredDistanceTo(pos) >= step.value * step.value) {
                puddles.add(new Puddle(pos.x, pos.y + 0.02, pos.z, now));
                lastPuddle = pos;
                if (puddles.size() > MAX_PUDDLES) puddles.remove(0);
            }
            if (ripples.value && tickN % 4 == 0) {
                ring.add(new Ripple(pos.x, pos.y + 0.03, pos.z, now));
                if (ring.size() > MAX_RIPPLES) ring.remove(0);
            }
            if (splash.value && tickN % 3 == 0) {
                for (int i = 0; i < 2; i++) {
                    mc.world.addParticle(ParticleTypes.SPLASH,
                            pos.x + (rng.nextDouble() - 0.5) * 0.5, pos.y + 0.05, pos.z + (rng.nextDouble() - 0.5) * 0.5,
                            0.0, 0.06, 0.0);
                }
            }
        }

        double maxAge = life.value;
        puddles.removeIf(q -> (now - q.born) / 1_000_000_000.0 > maxAge);
        ring.removeIf(r -> (now - r.born()) / 1_000_000_000.0 > 1.0);

        // widocznosc (czy kaluza nie jest za blokiem) liczona rzadko - to tylko kilka promieni
        if (tickN % 4 == 0) {
            Vec3d eye = p.getEyePos();
            for (Puddle q : puddles) {
                Vec3d c = new Vec3d(q.x, q.y + 0.05, q.z);
                q.visible = mc.world.raycast(new RaycastContext(eye, c,
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p))
                        .getType() == HitResult.Type.MISS;
            }
        }
    }

    private boolean ringPoints(double cx, double cy, double cz, double r, int n, double[] xs, double[] ys) {
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            double[] sp = Projection.projectStrict(new Vec3d(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r), 0.4);
            if (sp == null) return false;
            xs[i] = sp[0];
            ys[i] = sp[1];
        }
        return true;
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.nanoTime();
        int water = UiConfig.secondary();
        double r = size.value;
        double[] xs = new double[20], ys = new double[20];

        for (Puddle q : puddles) {
            if (!q.visible) continue;
            double age = (now - q.born) / 1_000_000_000.0 / life.value;
            if (age >= 1.0) continue;
            if (mc.player.squaredDistanceTo(q.x, q.y, q.z) > 26 * 26) continue;
            if (!ringPoints(q.x, q.y, q.z, r, 20, xs, ys)) continue;

            float a = (float) (1.0 - age * age);
            Draw.fillPolygon(ctx, xs, ys, Draw.withAlpha(water, 0.30f * a));
            for (int i = 0; i < 20; i++) {
                int j = (i + 1) % 20;
                Draw.line(ctx, xs[i], ys[i], xs[j], ys[j], 0.7f, Draw.withAlpha(0xFFFFFFFF, 0.35f * a));
            }
        }

        double[] rx = new double[24], ry = new double[24];
        for (Ripple rp : ring) {
            double age = (now - rp.born()) / 1_000_000_000.0;
            if (age >= 1.0) continue;
            double rad = 0.1 + age * 0.85 * r;
            if (!ringPoints(rp.x(), rp.y(), rp.z(), rad, 24, rx, ry)) continue;
            int col = Draw.withAlpha(0xFFFFFFFF, (float) (0.85 * (1.0 - age)));
            for (int i = 0; i < 24; i++) {
                int j = (i + 1) % 24;
                Draw.line(ctx, rx[i], ry[i], rx[j], ry[j], 0.8f, col);
            }
        }
    }
}
