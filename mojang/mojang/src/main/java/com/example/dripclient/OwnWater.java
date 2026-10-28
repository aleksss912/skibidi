package com.example.dripclient;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Zapamietuje miejsca, w ktorych wode wylewamy MY, zeby AutoLever jej nie ruszal. */
public final class OwnWater {
    private record Mark(BlockPos pos, long until) {
    }

    private static final List<Mark> MARKS = new ArrayList<>();

    private OwnWater() {
    }

    public static void mark(ClientWorld w, BlockPos pos, int ticks) {
        if (w == null) return;
        MARKS.add(new Mark(pos.toImmutable(), w.getTime() + ticks));
    }

    public static boolean isOwn(ClientWorld w, BlockPos pos) {
        if (w == null) return false;
        long now = w.getTime();
        MARKS.removeIf(m -> m.until() < now);
        for (Mark m : MARKS) {
            if (m.pos().getSquaredDistance(pos) <= 3.6 * 3.6) return true;
        }
        return false;
    }

    public static void clear() {
        MARKS.clear();
    }
}
