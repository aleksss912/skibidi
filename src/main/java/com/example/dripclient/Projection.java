package com.example.dripclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * Rzutowanie punktow swiata na ekran (GUI). Parametry kamery liczone raz na klatke (beginFrame).
 * Odcinki sa przycinane do plaszczyzny kamery i do ekranu, wiec nic nie "ucieka" w nieskonczonosc.
 */
public final class Projection {
    public static final class P {
        public double x, y;
        public boolean behind;
    }

    private static final double NEAR = 0.05;

    private static double fx, fy, fz, rx, rz, ux, uy, uz, cpx, cpy, cpz;
    private static double tanHalf = 0.75, aspect = 1.78, W = 854, H = 480;

    private Projection() {
    }

    public static double width() {
        return W;
    }

    public static double height() {
        return H;
    }

    /** Wywolywane raz na poczatku klatki HUD. */
    public static void beginFrame(MinecraftClient mc, DrawContext ctx, float delta) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        cpx = cp.x;
        cpy = cp.y;
        cpz = cp.z;

        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        fx = -Math.sin(yaw) * Math.cos(pitch);
        fy = -Math.sin(pitch);
        fz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw);
        rz = -Math.sin(yaw);
        ux = -rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy;

        W = ctx.getScaledWindowWidth();
        H = ctx.getScaledWindowHeight();
        aspect = W / H;

        double fov = mc.options.getFov().getValue();
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
    }

    private static double[] toCam(Vec3d w) {
        double dx = w.x - cpx, dy = w.y - cpy, dz = w.z - cpz;
        return new double[]{
                dx * rx + dz * rz,
                dx * ux + dy * uy + dz * uz,
                dx * fx + dy * fy + dz * fz
        };
    }

    private static double sx(double[] c) {
        return W / 2.0 * (1.0 + c[0] / (c[2] * tanHalf * aspect));
    }

    private static double sy(double[] c) {
        return H / 2.0 * (1.0 - c[1] / (c[2] * tanHalf));
    }

    /** Pojedynczy punkt. behind = true, gdy jest za kamera (wtedy x/y wskazuja kierunek obrotu). */
    public static P project(MinecraftClient mc, DrawContext ctx, Vec3d world) {
        double[] c = toCam(world);
        P p = new P();
        if (c[2] > NEAR) {
            p.x = sx(c);
            p.y = sy(c);
            p.behind = false;
        } else {
            double len = Math.hypot(c[0], c[1]);
            double nx = len < 1e-6 ? 0 : c[0] / len;
            double ny = len < 1e-6 ? -1 : c[1] / len;
            double big = Math.max(W, H);
            p.x = W / 2.0 + nx * big;
            p.y = H / 2.0 - ny * big;
            p.behind = true;
        }
        return p;
    }

    /** Punkt tylko jesli jest wyraznie przed kamera (minDepth w blokach), inaczej null. */
    public static double[] projectStrict(Vec3d world, double minDepth) {
        double[] c = toCam(world);
        if (c[2] < minDepth) return null;
        return new double[]{sx(c), sy(c)};
    }

    /** Odcinek 3D -> odcinek 2D przyciety do plaszczyzny kamery i ekranu: {x1,y1,x2,y2} albo null. */
    public static double[] segment(Vec3d a, Vec3d b) {
        double[] A = toCam(a), B = toCam(b);
        if (A[2] < NEAR && B[2] < NEAR) return null;
        if (A[2] < NEAR) A = atNear(A, B);
        else if (B[2] < NEAR) B = atNear(B, A);
        return clipLine(sx(A), sy(A), sx(B), sy(B), 24);
    }

    private static double[] atNear(double[] behind, double[] front) {
        double t = (NEAR - behind[2]) / (front[2] - behind[2]);
        return new double[]{
                behind[0] + (front[0] - behind[0]) * t,
                behind[1] + (front[1] - behind[1]) * t,
                NEAR
        };
    }

    /** Liang-Barsky: przycina odcinek do prostokata ekranu (z marginesem). null, jesli nic nie widac. */
    public static double[] clipLine(double x1, double y1, double x2, double y2, double margin) {
        double xmin = -margin, ymin = -margin, xmax = W + margin, ymax = H + margin;
        double dx = x2 - x1, dy = y2 - y1;
        double t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x1 - xmin, xmax - x1, y1 - ymin, ymax - y1};
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i]) < 1e-12) {
                if (q[i] < 0) return null;
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0) {
                    if (r > t1) return null;
                    if (r > t0) t0 = r;
                } else {
                    if (r < t0) return null;
                    if (r < t1) t1 = r;
                }
            }
        }
        return new double[]{x1 + t0 * dx, y1 + t0 * dy, x1 + t1 * dx, y1 + t1 * dy};
    }
}
