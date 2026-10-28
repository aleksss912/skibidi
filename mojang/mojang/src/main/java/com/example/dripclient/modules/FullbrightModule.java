package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Pelna jasnosc: lokalny efekt widzenia w ciemnosci (tylko po Twojej stronie). */
public class FullbrightModule extends Module {
    private int cooldown = 0;

    public FullbrightModule() {
        super("Fullbright", Category.RENDER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (cooldown-- > 0 && mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) return;
        cooldown = 100;
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
