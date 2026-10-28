package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;

/**
 * Znajomi: lista nickow, ktorych cheaty nie ruszaja (AimAssist, TriggerBot, Hitbox, Visuals...).
 * Wylaczenie tego modulu wylacza ochrone znajomych. Dodawaj tu nicki albo srodkowym przyciskiem myszy na graczu.
 */
public class FriendsModule extends Module {
    public FriendsModule() {
        super("Znajomi", Category.PLAYER);
        hasFriends = true;
        enabled = true; // ochrona znajomych domyslnie wlaczona
    }
}
