package com.example.dripclient.modules;

import com.example.dripclient.Category;
import com.example.dripclient.Module;
import com.example.dripclient.ModuleManager;
import com.example.dripclient.OwnWater;
import com.example.dripclient.PlaceUtil;
import com.example.dripclient.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Klikasz pajeczyna (albo obsydianem) w dzwignie ALBO w blok tuz obok dzwigni (dziala tez podczas kucania):
 * 1) opcjonalnie zbiera lawe z okolicy pustym wiadrem,
 * 2) wylewa wode w miejsce dzwigni (zmywa ja) i od razu odbiera wode wiadrem
 *    (plynaca woda zniszczylaby pajeczyne),
 * 3) stawia pajeczyne / obsydian w miejscu dzwigni.
 * Uwaga: na kilka tickow obraca kamere, bo wiadro dziala tam, gdzie patrzysz.
 */
public class ErrorKillerModule extends Module {
    private final Setting.Bool web = add(new Setting.Bool("Pajęczyna", true));
    private final Setting.Bool obsidian = add(new Setting.Bool("Obsydian", true));
    private final Setting.Bool pickUp = add(new Setting.Bool("Odbierz wodę", true));
    private final Setting.Bool lava = add(new Setting.Bool("Zabierz lawę", true));
    private final Setting.Num search = add(new Setting.Num("Szukaj obok (bloki)", 1, 0, 2, 1));
    private final Setting.Bool notify = add(new Setting.Bool("Komunikaty", true));

    private enum Phase { IDLE, ACT, WATCH }

    private record Step(Vec3d aim, int slot, int uses) {
    }

    private final List<Step> steps = new ArrayList<>();
    private Phase phase = Phase.IDLE;
    private BlockPos target;
    private int stepIdx, origSlot, timer, msgCooldown;
    private float oldYaw, oldPitch;

    public ErrorKillerModule() {
        super("ErrorKiller", Category.WORLD);
    }

    @Override
    public void onDisable() {
        phase = Phase.IDLE;
        target = null;
        steps.clear();
    }

    private void say(MinecraftClient mc, String msg) {
        if (notify.value && mc.player != null && msgCooldown == 0) {
            mc.player.sendMessage(Text.literal("Veltrix ErrorKiller: " + msg), true);
            msgCooldown = 20;
        }
    }

    // ------------------------------------------------------------------ wyzwalanie klikiem

    public static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
        ErrorKillerModule m = ModuleManager.get(ErrorKillerModule.class);
        if (m == null || !m.enabled || m.phase != Phase.IDLE) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);
        boolean ok = (m.web.value && held.isOf(Items.COBWEB)) || (m.obsidian.value && held.isOf(Items.OBSIDIAN));
        if (!ok) return ActionResult.PASS;

        BlockPos clicked = hit.getBlockPos();
        BlockPos lever = null;
        if (world.getBlockState(clicked).isOf(Blocks.LEVER)) {
            lever = clicked;
        } else if (m.search.value > 0) {
            lever = m.findLeverNear(world, clicked.offset(hit.getSide()), (int) m.search.value);
        }
        if (lever == null) return ActionResult.PASS;

        return m.begin(MinecraftClient.getInstance(), lever, world.getBlockState(lever))
                ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private BlockPos findLeverNear(World world, BlockPos center, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        Vec3d c = Vec3d.ofCenter(center);
        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            if (!world.getBlockState(pos).isOf(Blocks.LEVER)) continue;
            double d = Vec3d.ofCenter(pos).squaredDistanceTo(c);
            if (d < bestD) {
                bestD = d;
                best = pos.toImmutable();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ plan dzialania

    private int emptyBucketSlot(ClientPlayerEntity p, Set<Integer> used) {
        for (int i = 0; i < 9; i++) {
            if (used.contains(i)) continue;
            ItemStack s = p.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(Items.BUCKET)) return i;
        }
        return PlaceUtil.NONE;
    }

    private boolean begin(MinecraftClient mc, BlockPos pos, BlockState st) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return false;

        int waterSlot = PlaceUtil.findSlot(p, s -> s.isOf(Items.WATER_BUCKET));
        if (waterSlot == PlaceUtil.NONE) {
            say(mc, "brak wiadra z wodą w hotbarze");
            return false;
        }

        steps.clear();

        // 1) lawa w poblizu: zbierz pustym wiadrem
        if (lava.value) {
            Set<Integer> used = new HashSet<>();
            for (BlockPos q : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                BlockState s = w.getBlockState(q);
                if (!s.isOf(Blocks.LAVA) || s.get(FluidBlock.LEVEL) != 0) continue;
                int slot = emptyBucketSlot(p, used);
                if (slot == PlaceUtil.NONE) {
                    say(mc, "lawa obok, ale brak pustego wiadra");
                    break;
                }
                used.add(slot);
                steps.add(new Step(Vec3d.ofCenter(q), slot, 1));
                if (steps.size() >= 3) break;
            }
        }

        // 2) woda: wylej w miejsce dzwigni i odbierz
        steps.add(new Step(wallAim(w, p, pos, st), waterSlot, pickUp.value ? 2 : 1));

        target = pos;
        stepIdx = 0;
        origSlot = p.getInventory().selectedSlot;
        oldYaw = p.getYaw();
        oldPitch = p.getPitch();
        OwnWater.mark(w, pos, 80);
        applyAim(p, steps.get(0));
        phase = Phase.ACT;
        return true;
    }

    /** Punkt na scianie za dzwignia (obok dzwigni), tak zeby promien wiadra wylal wode w komorke dzwigni. */
    private Vec3d wallAim(ClientWorld w, ClientPlayerEntity p, BlockPos lever, BlockState st) {
        BlockFace face = st.get(Properties.BLOCK_FACE);
        Direction normal;
        if (face == BlockFace.FLOOR) normal = Direction.UP;
        else if (face == BlockFace.CEILING) normal = Direction.DOWN;
        else normal = st.get(Properties.HORIZONTAL_FACING);
        BlockPos attached = lever.offset(normal.getOpposite());

        Vec3d tangent = normal.getAxis().isVertical()
                ? new Vec3d(1, 0, 0)
                : new Vec3d(-normal.getOffsetZ(), 0, normal.getOffsetX());
        Vec3d bitangent = normal.getAxis().isVertical() ? new Vec3d(0, 0, 1) : new Vec3d(0, 1, 0);
        Vec3d center = Vec3d.ofCenter(attached)
                .add(normal.getOffsetX() * 0.5, normal.getOffsetY() * 0.5, normal.getOffsetZ() * 0.5);

        Vec3d eye = p.getEyePos();
        double[] offs = {0.38, -0.38, 0.0};
        for (double a : offs) {
            for (double b : offs) {
                if (a == 0.0 && b == 0.0) continue;
                Vec3d pt = center.add(tangent.multiply(a)).add(bitangent.multiply(b));
                Vec3d end = eye.add(pt.subtract(eye).normalize().multiply(6.0));
                BlockHitResult r = w.raycast(new RaycastContext(eye, end,
                        RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
                // dobry punkt: promien trafia dokladnie w sciane, do ktorej przylega dzwignia
                if (r.getType() == HitResult.Type.BLOCK && r.getBlockPos().equals(attached) && r.getSide() == normal) {
                    return pt;
                }
            }
        }
        return Vec3d.ofCenter(lever); // zapasowo: celuj w sama dzwignie
    }

    private void applyAim(ClientPlayerEntity p, Step s) {
        Vec3d eye = p.getEyePos();
        double dx = s.aim().x - eye.x, dy = s.aim().y - eye.y, dz = s.aim().z - eye.z;
        double hd = Math.sqrt(dx * dx + dz * dz);
        p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        p.setPitch((float) -Math.toDegrees(Math.atan2(dy, hd)));
        p.getInventory().selectedSlot = s.slot();
    }

    // ------------------------------------------------------------------ wykonanie

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || mc.interactionManager == null) return;
        if (msgCooldown > 0) msgCooldown--;
        ClientPlayerEntity p = mc.player;

        if (phase == Phase.ACT) {
            // serwer ma juz nowy obrot i wybrane wiadro
            Step s = steps.get(stepIdx);
            for (int i = 0; i < s.uses(); i++) mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            OwnWater.mark(mc.world, target, 80);
            stepIdx++;

            if (stepIdx < steps.size()) {
                applyAim(p, steps.get(stepIdx)); // nastepny krok: obrot teraz, uzycie w nastepnym ticku
                return;
            }
            // koniec wiader: postaw blok w miejscu dzwigni i przywroc kamere
            PlaceUtil.placeAt(mc, target, origSlot);
            p.getInventory().selectedSlot = origSlot;
            p.setYaw(oldYaw);
            p.setPitch(oldPitch);
            phase = Phase.WATCH;
            timer = 0;
        } else if (phase == Phase.WATCH) {
            BlockState st = mc.world.getBlockState(target);
            if (st.isOf(Blocks.COBWEB) || st.isOf(Blocks.OBSIDIAN)) {
                phase = Phase.IDLE;
            } else if (!st.isOf(Blocks.LEVER) && timer < 6) {
                PlaceUtil.placeAt(mc, target, origSlot); // dzwignia znikla, blok jeszcze nie stoi - ponow
                timer++;
            } else if (++timer > 8) {
                say(mc, "nie udało się (sprawdź czy wiadro jest pełne i blok w ręce)");
                phase = Phase.IDLE;
            }
        }
    }
}
