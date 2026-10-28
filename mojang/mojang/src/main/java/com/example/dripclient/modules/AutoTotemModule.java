package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Trzyma totem w drugiej rece. Nie ruszaj drugiej reki, gdy jest w niej dzwignia albo tarcza
 * (opcje do wylaczenia).
 */
public class AutoTotemModule extends Module {
    private final Setting.Bool keepLever = add(new Setting.Bool("Zostaw dźwignię", true));
    private final Setting.Bool keepShield = add(new Setting.Bool("Zostaw tarczę", true));
    private final Setting.Num hp = add(new Setting.Num("HP poniżej", 20, 1, 20, 1));
    private final Setting.Num delay = add(new Setting.Num("Opóźnienie (ticki)", 1, 0, 10, 1));

    private int cooldown = 0;

    public AutoTotemModule() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        ItemStack off = p.getOffHandStack();
        if (off.isOf(Items.TOTEM_OF_UNDYING)) return;
        if (keepLever.value && off.isOf(Items.LEVER)) return;
        if (keepShield.value && off.isOf(Items.SHIELD)) return;
        if (p.getHealth() + p.getAbsorptionAmount() > hp.value) return;

        ScreenHandler h = p.playerScreenHandler;
        int src = -1;
        for (int s = 9; s <= 44; s++) {
            if (h.getSlot(s).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                src = s;
                break;
            }
        }
        if (src < 0) return;

        // przycisk 40 = druga reka
        mc.interactionManager.clickSlot(h.syncId, src, 40, SlotActionType.SWAP, p);
        cooldown = (int) delay.value;
    }
}
