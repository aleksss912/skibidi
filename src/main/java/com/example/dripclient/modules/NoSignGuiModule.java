package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;

/**
 * NoSignGUI: okno edycji tabliczki sie nie otwiera, wiec mozna klikac ja bez przerwy.
 * Glowna robota jest w mixinie (ClientPlayerEntity), a tu jest zapasowe zamykanie okna.
 */
public class NoSignGuiModule extends Module {
    public NoSignGuiModule() {
        super("NoSignGUI", Category.PLAYER);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.currentScreen instanceof AbstractSignEditScreen) mc.setScreen(null);
    }
}
