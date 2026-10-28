package com.example.dripclient;

import com.example.dripclient.modules.AimAssistModule;
import com.example.dripclient.modules.AntiKostkaModule;
import com.example.dripclient.modules.AntiTrapModule;
import com.example.dripclient.modules.AutoLeverModule;
import com.example.dripclient.modules.AutoTotemModule;
import com.example.dripclient.modules.BetterPaintingModule;
import com.example.dripclient.modules.BlockEspModule;
import com.example.dripclient.modules.EspModule;
import com.example.dripclient.modules.ErrorKillerModule;
import com.example.dripclient.modules.FastPlaceModule;
import com.example.dripclient.modules.FreecamModule;
import com.example.dripclient.modules.FriendsModule;
import com.example.dripclient.modules.FullbrightModule;
import com.example.dripclient.modules.HitboxModule;
import com.example.dripclient.modules.HudModule;
import com.example.dripclient.modules.ItemEspModule;
import com.example.dripclient.modules.NameTagModule;
import com.example.dripclient.modules.NoJumpDelayModule;
import com.example.dripclient.modules.NoPushModule;
import com.example.dripclient.modules.NoSignGuiModule;
import com.example.dripclient.modules.PuddlesModule;
import com.example.dripclient.modules.StarsModule;
import com.example.dripclient.modules.SprintModule;
import com.example.dripclient.modules.TracerModule;
import com.example.dripclient.modules.TriggerBotModule;
import com.example.dripclient.modules.VisualsModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();
    public static final Set<String> FRIENDS = new LinkedHashSet<>();

    private ModuleManager() {
    }

    public static void init() {
        // Render
        MODULES.add(new NameTagModule());
        MODULES.add(new TracerModule());
        MODULES.add(new EspModule());
        MODULES.add(new ItemEspModule());
        MODULES.add(new BlockEspModule());
        MODULES.add(new FullbrightModule());
        MODULES.add(new FreecamModule());
        MODULES.add(new VisualsModule());
        MODULES.add(new StarsModule());
        MODULES.add(new PuddlesModule());
        MODULES.add(new HudModule());
        // Combat
        MODULES.add(new HitboxModule());
        MODULES.add(new AimAssistModule());
        MODULES.add(new TriggerBotModule());
        MODULES.add(new AutoTotemModule());
        // Gracz
        MODULES.add(new AntiKostkaModule());
        MODULES.add(new FriendsModule());
        MODULES.add(new SprintModule());
        MODULES.add(new NoJumpDelayModule());
        MODULES.add(new FastPlaceModule());
        MODULES.add(new NoPushModule());
        MODULES.add(new NoSignGuiModule());
        // World
        MODULES.add(new BetterPaintingModule());
        MODULES.add(new AutoLeverModule());
        MODULES.add(new ErrorKillerModule());
        MODULES.add(new AntiTrapModule());
        load();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
        return null;
    }

    /** Czy nick jest na liscie znajomych (niezaleznie od tego, czy modul Znajomi jest wlaczony). */
    public static boolean inList(String name) {
        return FRIENDS.contains(name.toLowerCase(Locale.ROOT));
    }

    /** Znajomy chroniony przed cheatami: jest na liscie i modul Znajomi jest wlaczony. */
    public static boolean isFriend(String name) {
        if (!inList(name)) return false;
        FriendsModule fm = get(FriendsModule.class);
        return fm == null || fm.enabled;
    }

    public static void addFriend(String name) {
        if (name != null && !name.isBlank()) FRIENDS.add(name.trim().toLowerCase(Locale.ROOT));
    }

    public static void removeFriend(String name) {
        FRIENDS.remove(name.toLowerCase(Locale.ROOT));
    }

    /** Zwraca true, jesli dodano, false jesli usunieto. */
    public static boolean toggleFriend(String name) {
        if (inList(name)) {
            removeFriend(name);
            return false;
        }
        addFriend(name);
        return true;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("dripclient.properties");
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : MODULES) {
            p.setProperty("mod." + m.name + ".enabled", String.valueOf(m.enabled));
            for (Setting s : m.settings) {
                p.setProperty("mod." + m.name + "." + s.name, s.save());
            }
            p.setProperty("mod." + m.name + ".bind", String.valueOf(m.bind.code));
            m.saveExtra(p);
        }
        p.setProperty("friends", String.join(",", FRIENDS));
        UiConfig.save(p);
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Veltrix");
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        Path f = file();
        Properties p = new Properties();
        if (Files.exists(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            } catch (IOException ignored) {
            }
        }
        for (Module m : MODULES) {
            // domyslnie zostaje stan z konstruktora (np. HUD i Znajomi sa wlaczone od poczatku)
            m.enabled = Boolean.parseBoolean(p.getProperty("mod." + m.name + ".enabled", String.valueOf(m.enabled)));
            for (Setting s : m.settings) {
                String v = p.getProperty("mod." + m.name + "." + s.name);
                if (v != null) s.load(v);
            }
            m.bind.load(p.getProperty("mod." + m.name + ".bind", "-1"));
            m.loadExtra(p);
            m.onLoad();
        }
        UiConfig.load(p);
        String fr = p.getProperty("friends", "");
        for (String n : fr.split(",")) addFriend(n);
    }
}
