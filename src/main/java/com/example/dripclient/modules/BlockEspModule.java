package com.example.dripclient.modules;

import com.example.dripclient.BoxDraw;
import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Biale, zamalowane boxy na wybranych blokach w poblizu (skrzynie, spawnery, cenne rudy). Skan co sekunde. */
public class BlockEspModule extends Module {
    private final Setting.Bool chests = add(new Setting.Bool("Skrzynie i beczki", true));
    private final Setting.Bool spawners = add(new Setting.Bool("Spawnery", true));
    private final Setting.Bool ores = add(new Setting.Bool("Cenne rudy", true));
    private final Setting.Num fill = add(new Setting.Num("Wypełnienie", 0.55, 0.0, 1.0, 0.05));
    private final Setting.Bool outline = add(new Setting.Bool("Obrys", true));
    private final Setting.Num radius = add(new Setting.Num("Zasięg", 16, 8, 24, 4));

    private final List<BlockPos> found = new ArrayList<>();
    private int cooldown = 0;

    public BlockEspModule() {
        super("BlockESP", Category.RENDER);
    }

    @Override
    public void onDisable() {
        found.clear();
    }

    private boolean matches(BlockState st) {
        Block b = st.getBlock();
        if (chests.value && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST
                || b == Blocks.BARREL || b instanceof ShulkerBoxBlock)) return true;
        if (spawners.value && b == Blocks.SPAWNER) return true;
        return ores.value && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE
                || b == Blocks.ANCIENT_DEBRIS || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        if (++cooldown < 20) return;
        cooldown = 0;

        int r = (int) radius.value;
        BlockPos c = mc.player.getBlockPos();
        List<BlockPos> tmp = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(c.add(-r, -r, -r), c.add(r, r, r))) {
            if (matches(mc.world.getBlockState(pos))) tmp.add(pos.toImmutable());
            if (tmp.size() > 300) break;
        }
        Vec3d me = mc.player.getPos();
        tmp.sort(Comparator.comparingDouble(p -> Vec3d.ofCenter(p).squaredDistanceTo(me)));
        found.clear();
        found.addAll(tmp);
    }

    @Override
    public void onHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        int n = 0;
        for (BlockPos pos : found) {
            if (n++ >= 40) break;
            BoxDraw.draw(mc, ctx, new Box(pos).expand(0.01), 0xFFFFFFFF, 0.8f, false, (float) fill.value, outline.value);
        }
    }
}
