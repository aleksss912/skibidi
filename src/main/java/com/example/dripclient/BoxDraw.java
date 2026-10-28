package com.example.dripclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Rysuje box 3D na ekranie (przez sciany): wypelnienie i/lub obrys, opcjonalnie ze swieceniem. */
public final class BoxDraw {
    private static final int[][] EDGES = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},
            {4, 5}, {5, 7}, {7, 6}, {6, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private BoxDraw() {
    }

    public static void draw(MinecraftClient mc, DrawContext ctx, Box b, int color, float th,
                            boolean glow, float fillAlpha, boolean outline) {
        Vec3d[] c = new Vec3d[8];
        int i = 0;
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++)
                    c[i++] = new Vec3d(x == 0 ? b.minX : b.maxX, y == 0 ? b.minY : b.maxY, z == 0 ? b.minZ : b.maxZ);

        double[][] segs = new double[EDGES.length][];
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (int k = 0; k < EDGES.length; k++) {
            double[] s = Projection.segment(c[EDGES[k][0]], c[EDGES[k][1]]);
            segs[k] = s;
            if (s == null) continue;
            any = true;
            minX = Math.min(minX, Math.min(s[0], s[2]));
            maxX = Math.max(maxX, Math.max(s[0], s[2]));
            minY = Math.min(minY, Math.min(s[1], s[3]));
            maxY = Math.max(maxY, Math.max(s[1], s[3]));
        }
        if (!any) return;

        if (fillAlpha > 0.01f) {
            int w = (int) Projection.width(), h = (int) Projection.height();
            int x1 = Math.max(0, (int) Math.floor(minX)), y1 = Math.max(0, (int) Math.floor(minY));
            int x2 = Math.min(w, (int) Math.ceil(maxX)), y2 = Math.min(h, (int) Math.ceil(maxY));
            // gdy kamera jest w srodku boxa, nie zamalowuj pol ekranu
            if (x2 > x1 && y2 > y1 && (long) (x2 - x1) * (y2 - y1) < 0.55 * w * h) {
                ctx.fill(x1, y1, x2, y2, Draw.withAlpha(color, fillAlpha));
            }
        }

        if (!outline) return;
        for (double[] s : segs) {
            if (s == null) continue;
            if (glow) {
                Draw.line(ctx, s[0], s[1], s[2], s[3], th + 3.5f, Draw.withAlpha(color, 0.10f));
                Draw.line(ctx, s[0], s[1], s[2], s[3], th + 1.8f, Draw.withAlpha(color, 0.22f));
            }
            Draw.line(ctx, s[0], s[1], s[2], s[3], th, color);
        }
    }
}
