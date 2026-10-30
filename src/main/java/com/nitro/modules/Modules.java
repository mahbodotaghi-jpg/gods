package com.nitro.modules;

import com.nitro.core.Mod;

import java.util.ArrayList;
import java.util.List;

/** Registry of every module. Order = draw order (full-screen effects first, HUD last). */
public final class Modules {
    private Modules() {}

    public static List<Mod> create() {
        List<Mod> l = new ArrayList<>();
        // screen effects first so everything else draws on top
        l.add(new CuteWorld()); l.add(new Cyberspace()); l.add(new Bokeh()); l.add(new LowFire());
        // world overlays
        l.add(new BlockOutline()); l.add(new Hitboxes()); l.add(new Radiance());
        l.add(new PlayerEsp()); l.add(new StorageEsp()); l.add(new Tracers()); l.add(new ChunkFinder()); l.add(new SusChunkFinder());
        l.add(new NameTags());
        // behaviour / particles
        l.add(new WTap()); l.add(new Ripple()); l.add(new HitSplash()); l.add(new Afterglow());
        l.add(new Skies()); l.add(new Weather()); l.add(new Ambience()); l.add(new Fullbright()); l.add(new Zoom()); l.add(new Freecam());
        l.add(new AntiVanish()); l.add(new NickHider()); l.add(new CustomCrosshair());
        // HUD elements
        l.add(new TargetHud()); l.add(new ComboHud()); l.add(new TotemHud());
        l.add(new StaffList()); l.add(new Radar());
        l.add(new FpsHud()); l.add(new CoordsHud()); l.add(new DirectionHud()); l.add(new PingHud());
        l.add(new TimeHud()); l.add(new ProfileHud()); l.add(new CpsHud()); l.add(new Keystrokes());
        l.add(new ArmorHud()); l.add(new PotionHud()); l.add(new ActiveModulesHud()); l.add(new Notifications());
        // client
        l.add(new HudEditorMod()); l.add(new InterfaceMod());

        // sensible first-run defaults (so something is visible straight away)
        for (Mod m : l) {
            if (m instanceof FpsHud || m instanceof Keystrokes || m instanceof ArmorHud || m instanceof Notifications
                || m instanceof TargetHud || m instanceof ComboHud || m instanceof CustomCrosshair || m instanceof BlockOutline
                || m instanceof NameTags || m instanceof Radiance || m instanceof CoordsHud || m instanceof ActiveModulesHud) m.enabled = true;
        }
        return l;
    }
}
