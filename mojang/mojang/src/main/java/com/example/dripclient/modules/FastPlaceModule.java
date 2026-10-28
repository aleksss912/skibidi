package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import com.example.dripclient.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;

/**
 * Szybkie stawianie blokow. Suwak Szybkosc: 1 = co ok. 5 tickow, 5 = co tick.
 */
public class FastPlaceModule extends Module {
    private final Setting.Num speed = add(new Setting.Num("Szybkość", 5, 1, 5, 1));
    private final Setting.Bool onlyBlocks = add(new Setting.Bool("Tylko bloki", true));

    private int counter = 0;

    public FastPlaceModule() {
        super("FastPlace", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (onlyBlocks.value && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        int delay = 5 - (int) speed.value; // 5 -> 0 tickow przerwy, 1 -> 4 ticki
        try {
            if ((Object) mc instanceof MinecraftClientAccessor acc) {
                if (delay <= 0 || counter >= delay) {
                    acc.dripclient$setItemUseCooldown(0); // wolno stawiac w nastepnym ticku
                    counter = 0;
                } else {
                    acc.dripclient$setItemUseCooldown(1); // jeszcze blokuj
                    counter++;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
