package com.example.dripclient.modules;

import com.example.dripclient.BoxDraw;
import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Powieksza hitbox innych graczy po stronie klienta. Box jest ustawiany tuz przed wyznaczeniem celu
 * pod celownikiem (mixin), bo pakiety z serwera potrafia go zresetowac w trakcie klatki.
 * Uwaga: serwer przyjmuje trafienie tylko do ok. 4 blokow od prawdziwego hitboxa, wiec
 * powiekszenie powyzej ~1 bloku nie da trafien.
 */
public class HitboxModule extends Module {
    private final Setting.Num size = add(new Setting.Num("Powiększenie", 0.6, 0.1, 1.5, 0.1));
    private final Setting.Bool show = add(new Setting.Bool("Pokaż hitboxy", true));
    private final Setting.Bool skipFriends = add(new Setting.Bool("Pomiń znajomych", true));

    public HitboxModule() {
        super("Hitbox", Category.COMBAT);
    }

    public static void applyNow(MinecraftClient mc) {
        HitboxModule m = ModuleManager.get(HitboxModule.class);
        if (m != null && m.enabled) m.apply(mc);
    }

    private void apply(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        double s = size.value;
        Box mine = mc.player.getBoundingBox();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Box base = p.getDimensions(p.getPose()).getBoxAt(p.getPos());
            boolean friend = skipFriends.value && ModuleManager.isFriend(p.getName().getString());
            Box big = base.expand(s, s, s);
            // gdy powiekszony box dotyka Ciebie, zostaw normalny - inaczej gra by Cie odpychala
            p.setBoundingBox(friend || big.intersects(mine) ? base : big);
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        apply(mc);
    }

    @Override
    public void onFrame(MinecraftClient mc, float delta) {
        apply(mc);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            p.setBoundingBox(p.getDimensions(p.getPose()).getBoxAt(p.getPos()));
        }
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        if (!show.value) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (p.squaredDistanceTo(mc.player) > 96 * 96) continue;
            boolean friend = ModuleManager.isFriend(p.getName().getString());
            Vec3d off = p.getLerpedPos(delta).subtract(p.getPos());
            Box b = p.getBoundingBox().offset(off);
            BoxDraw.draw(mc, ctx, b, friend ? 0xFF55FF55 : 0xFFFFFFFF, 0.8f, false, 0f, true);
        }
    }
}
