package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Draw;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.Setting;
import com.example.dripclient.UiConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lista wlaczonych cheatow po lewej stronie ekranu. Ten modul mozna wylaczyc (lub zbindowac). */
public class HudModule extends Module {
    private final Setting.Bool showBind = add(new Setting.Bool("Pokaż bind", false));
    private final Setting.Bool bar = add(new Setting.Bool("Pasek z boku", true));
    private final Setting.Num scale = add(new Setting.Num("Rozmiar", 1.0, 0.6, 1.6, 0.1));

    private final Map<String, Float> anim = new HashMap<>();
    private long lastNs = 0;

    public HudModule() {
        super("HUD", Category.RENDER);
        enabled = true; // domyslnie wlaczony
    }

    private String label(Module m) {
        String s = m.name;
        if (showBind.value && m.bind.code > 0) {
            s += " [" + InputUtil.Type.KEYSYM.createFromCode(m.bind.code).getLocalizedText().getString() + "]";
        }
        return s;
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : MathHelper.clamp((now - lastNs) / 1_000_000_000f, 0f, 0.1f);
        lastNs = now;

        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.enabled && m != this) list.add(m);
        }
        list.sort(Comparator.comparingInt((Module m) -> -tr.getWidth(label(m))));

        int pri = UiConfig.primary(), sec = UiConfig.secondary();
        float s = (float) scale.value;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(4f, 4f, 0f);
        ms.scale(s, s, 1f);

        int y = 0;
        int count = Math.max(1, list.size());
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            float v = anim.getOrDefault(m.name, 0f);
            v += (1f - v) * Math.min(1f, dt * 10f);
            anim.put(m.name, v);

            String text = label(m);
            int tw = tr.getWidth(text);
            int col = lerp(pri, sec, i / (float) count);
            int slide = Math.round((1f - v) * -14f);

            ctx.fill(slide, y, slide + tw + 8, y + 11, Draw.withAlpha(0xFF0A0B10, 0.55f * v));
            if (bar.value) ctx.fill(slide, y, slide + 2, y + 11, Draw.withAlpha(col, v));
            if (v > 0.12f) ctx.drawTextWithShadow(tr, text, slide + 5, y + 2, Draw.withAlpha(col, v));
            y += 11;
        }
        ms.pop();
        if (list.isEmpty()) anim.clear();
    }

    private static int lerp(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
