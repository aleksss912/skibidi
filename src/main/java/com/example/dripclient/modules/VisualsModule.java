package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Draw;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.Projection;
import com.example.dripclient.Setting;
import com.example.dripclient.UiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Efekty PvP w kolorach motywu (fioletowo-niebieskie): swiecace czasteczki wokol graczy,
 * blyskawice przy trafieniu, blysk ekranu i pulsujacy glow na krawedziach ekranu.
 * Liczba efektow jest ograniczona (suwak Gestosc), zeby nie zabijac FPS.
 */
public class VisualsModule extends Module {
    private final Setting.Bool particles = add(new Setting.Bool("Cząsteczki", true));
    private final Setting.Bool bolts = add(new Setting.Bool("Błyskawice", true));
    private final Setting.Bool vignette = add(new Setting.Bool("Glow ekranu", true));
    private final Setting.Bool flash = add(new Setting.Bool("Błysk przy trafieniu", true));
    private final Setting.Bool storm = add(new Setting.Bool("Burza w tle", true));
    private final Setting.Bool self = add(new Setting.Bool("Aura na mnie", false));
    private final Setting.Num density = add(new Setting.Num("Gęstość", 0.6, 0.1, 1.0, 0.1));
    private final Setting.Num range = add(new Setting.Num("Zasięg", 40, 10, 80, 10));

    private record Bolt(Vec3d pos, long startNs, long seed, double height) {
    }

    private static final float BOLT_LIFE = 0.45f;
    private static final ParticleEffect[] AURA = {
            ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.WITCH, ParticleTypes.END_ROD,
            ParticleTypes.ELECTRIC_SPARK, ParticleTypes.PORTAL
    };

    private final Map<UUID, Integer> lastHurt = new HashMap<>();
    private final List<Bolt> active = new ArrayList<>();
    private final Random rng = new Random();
    private float flashAmount = 0f;
    private long lastNs = 0;
    private int auraIndex = 0;

    public VisualsModule() {
        super("Visuals PvP", Category.RENDER);
    }

    @Override
    public void onDisable() {
        active.clear();
        lastHurt.clear();
        flashAmount = 0f;
    }

    // ------------------------------------------------------------------ tick: czasteczki i wykrywanie trafien

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;

        Vec3d me = mc.player.getPos();
        double r2 = range.value * range.value;
        List<AbstractClientPlayerEntity> near = new ArrayList<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !self.value) continue;
            if (p != mc.player && ModuleManager.isFriend(p.getName().getString())) continue;
            if (p.squaredDistanceTo(me) > r2) continue;
            near.add(p);
        }
        near.sort(Comparator.comparingDouble(p -> p.squaredDistanceTo(me)));

        int perPlayer = Math.max(1, Math.round(2f * (float) density.value));
        int n = 0;
        for (AbstractClientPlayerEntity p : near) {
            if (n++ >= 6) break; // limit graczy z efektami (FPS)

            int prev = lastHurt.getOrDefault(p.getUuid(), 0);
            int cur = p.hurtTime;
            if (cur > prev && p != mc.player) onHit(mc, p);
            lastHurt.put(p.getUuid(), cur);

            if (particles.value) {
                for (int i = 0; i < perPlayer; i++) {
                    ParticleEffect type = AURA[auraIndex++ % AURA.length];
                    double x = p.getX() + (rng.nextDouble() - 0.5) * 0.9;
                    double y = p.getY() + rng.nextDouble() * 1.9;
                    double z = p.getZ() + (rng.nextDouble() - 0.5) * 0.9;
                    mc.world.addParticle(type, x, y, z, 0.0, 0.02 + rng.nextDouble() * 0.03, 0.0);
                }
            }
        }
        if (lastHurt.size() > 64) lastHurt.clear();

        // odlegle blyskawice w tle (rzadko, dla klimatu)
        if (storm.value && bolts.value && rng.nextInt(90) == 0) {
            double ang = rng.nextDouble() * Math.PI * 2;
            double dist = 14 + rng.nextDouble() * 24;
            Vec3d at = new Vec3d(me.x + Math.cos(ang) * dist, me.y, me.z + Math.sin(ang) * dist);
            if (active.size() >= 6) active.remove(0);
            active.add(new Bolt(at, System.nanoTime(), rng.nextLong(), 18.0));
            if (flash.value) flashAmount = Math.max(flashAmount, 0.30f);
        }
    }

    private void onHit(MinecraftClient mc, AbstractClientPlayerEntity p) {
        if (bolts.value) {
            if (active.size() >= 6) active.remove(0);
            active.add(new Bolt(p.getPos(), System.nanoTime(), rng.nextLong(), 7.0));
        }
        if (particles.value) {
            int sparks = Math.round(14f * (float) density.value) + 4;
            for (int i = 0; i < sparks; i++) {
                mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        p.getX(), p.getY() + 1.0, p.getZ(),
                        (rng.nextDouble() - 0.5) * 0.5, (rng.nextDouble() - 0.2) * 0.4, (rng.nextDouble() - 0.5) * 0.5);
            }
            for (int i = 0; i < 6; i++) {
                mc.world.addParticle(ParticleTypes.END_ROD,
                        p.getX(), p.getY() + 1.0, p.getZ(),
                        (rng.nextDouble() - 0.5) * 0.3, rng.nextDouble() * 0.3, (rng.nextDouble() - 0.5) * 0.3);
            }
        }
        if (flash.value && mc.player.distanceTo(p) <= 6f) flashAmount = 1f;
    }

    // ------------------------------------------------------------------ HUD: glow, blysk, blyskawice

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : MathHelper.clamp((now - lastNs) / 1_000_000_000f, 0f, 0.1f);
        lastNs = now;
        flashAmount = Math.max(0f, flashAmount - dt * 3.5f);

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int pri = UiConfig.primary(), sec = UiConfig.secondary();

        if (vignette.value) drawVignette(ctx, w, h, now, pri, sec);
        if (flashAmount > 0.01f) ctx.fill(0, 0, w, h, Draw.withAlpha(sec, 0.20f * flashAmount));

        if (!active.isEmpty()) {
            active.removeIf(b -> (now - b.startNs()) / 1_000_000_000f > BOLT_LIFE);
            for (Bolt b : active) drawBolt(mc, ctx, b, now, pri, sec);
        }
    }

    private void drawVignette(DrawContext ctx, int w, int h, long now, int pri, int sec) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 1_000_000_000.0 * 2.0);
        float a = 0.10f + 0.07f * pulse;
        int ph = (int) (h * 0.16f);
        int pw = (int) (w * 0.10f);
        ctx.fillGradient(0, 0, w, ph, Draw.withAlpha(pri, a), 0x00000000);
        ctx.fillGradient(0, h - ph, w, h, 0x00000000, Draw.withAlpha(pri, a));
        for (int i = 0; i < pw; i += 4) {
            float f = 1f - i / (float) pw;
            int c = Draw.withAlpha(sec, a * f * f);
            ctx.fill(i, 0, i + 4, h, c);
            ctx.fill(w - i - 4, 0, w - i, h, c);
        }
    }

    private void drawBolt(MinecraftClient mc, DrawContext ctx, Bolt b, long now, int pri, int sec) {
        float age = (now - b.startNs()) / 1_000_000_000f;
        float fade = MathHelper.clamp(1f - age / BOLT_LIFE, 0f, 1f);
        long slot = (now - b.startNs()) / 55_000_000L; // zmiana ksztaltu ~18 razy/s = migotanie
        Random r = new Random(b.seed() + slot * 31L);

        Vec3d top = b.pos().add((r.nextDouble() - 0.5) * 1.2, b.height(), (r.nextDouble() - 0.5) * 1.2);
        Vec3d bottom = b.pos().add(0, 0.9, 0);
        int segs = 8;

        Vec3d prev = top;
        Vec3d fork = null;
        for (int i = 1; i <= segs; i++) {
            Vec3d base = top.lerp(bottom, i / (double) segs);
            Vec3d pt = i == segs ? bottom
                    : base.add((r.nextDouble() - 0.5) * 0.9, 0, (r.nextDouble() - 0.5) * 0.9);
            segment(mc, ctx, prev, pt, fade, 1f, pri, sec);
            if (i == 3) fork = pt;
            prev = pt;
        }
        // odnoga
        if (fork != null) {
            Vec3d p0 = fork;
            for (int i = 1; i <= 3; i++) {
                Vec3d p1 = p0.add((r.nextDouble() - 0.2) * 0.9, -0.9 - r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.9);
                segment(mc, ctx, p0, p1, fade, 0.55f, pri, sec);
                p0 = p1;
            }
        }
    }

    private void segment(MinecraftClient mc, DrawContext ctx, Vec3d a, Vec3d b, float fade, float scale, int pri, int sec) {
        double[] sg = Projection.segment(a, b);
        if (sg == null) return;
        Draw.line(ctx, sg[0], sg[1], sg[2], sg[3], 6f * scale, Draw.withAlpha(sec, 0.10f * fade));   // szeroki glow
        Draw.line(ctx, sg[0], sg[1], sg[2], sg[3], 3f * scale, Draw.withAlpha(pri, 0.30f * fade));   // bloom
        Draw.line(ctx, sg[0], sg[1], sg[2], sg[3], 1.1f * scale, Draw.withAlpha(0xFFFFFFFF, 0.95f * fade)); // rdzen
    }
}
