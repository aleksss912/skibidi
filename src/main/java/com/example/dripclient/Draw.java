package com.example.dripclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class Draw {
    private Draw() {
    }

    /** Linia o dowolnej (takze ulamkowej) grubosci: jeden prostokat obrocony i sciesniony macierza. */
    public static void line(DrawContext ctx, double x1, double y1, double x2, double y2, float thickness, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 0.5 || len > 6000) return;
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate((float) x1, (float) y1, 0f);
        m.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
        m.scale(1f, thickness, 1f);
        m.translate(0f, -0.5f, 0f);
        ctx.fill(0, 0, (int) Math.ceil(len), 1, color);
        m.pop();
    }

    /** Wypelnia wypukly wielokat poziomymi paskami (krok 2 px - oszczedza FPS). */
    public static void fillPolygon(DrawContext ctx, double[] xs, double[] ys, int color) {
        int n = xs.length;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        int h = ctx.getScaledWindowHeight();
        int w = ctx.getScaledWindowWidth();
        int startY = Math.max(0, (int) Math.ceil(minY));
        int endY = Math.min(h, (int) Math.floor(maxY));
        for (int y = startY; y < endY; y += 2) {
            double xl = Double.MAX_VALUE, xr = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double y1 = ys[i], y2 = ys[j];
                if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
                    double t = (y - y1) / (y2 - y1);
                    double x = xs[i] + t * (xs[j] - xs[i]);
                    xl = Math.min(xl, x);
                    xr = Math.max(xr, x);
                }
            }
            if (xr > xl) {
                int a = Math.max(0, (int) xl), b = Math.min(w, (int) xr);
                if (b > a) ctx.fill(a, y, b, Math.min(h, y + 2), color);
            }
        }
    }

    public static int withAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0xFFFFFF);
    }
}
