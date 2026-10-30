package com.nitro.modules;

import com.nitro.core.*;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/**
 * Base Finder: rates every loaded chunk on how likely it is that a player built something in it and outlines the
 * suspicious ones with a percentage tag.
 *
 * It only reads chunk data the server already sent you (no packets, nothing extra requested). Each kind of evidence
 * (machines, storage, farming, built blocks, underground torches, redstone, decoration, portals) becomes a 0..1
 * strength; the strengths are combined like independent clues, so several different kinds push the score up much
 * faster than one kind alone. The 8 neighbouring chunks add a little because bases cross chunk borders. The number is
 * a heuristic score, not a real probability. Natural structures (villages, mineshafts, strongholds, ancient cities,
 * trial chambers, bastions, desert temples) are damped so they do not flood the list.
 */
final class SusChunkFinder extends Mod {
    private enum Sig {
        //            label           reliability  k (count that gives ~63% strength)
        MACHINE ("machines",      0.80, 3),
        STORAGE ("storage",       0.65, 4),
        FARM    ("farming",       0.50, 8),
        PLACED  ("built blocks",  0.50, 14),
        LIGHT   ("torches",       0.45, 8),
        REDSTONE("redstone",      0.70, 6),
        DECOR   ("decor",         0.45, 10),
        PORTAL  ("portals",       0.55, 3);
        final String label; final double rel, k;
        Sig(String label, double rel, double k) { this.label = label; this.rel = rel; this.k = k; }
    }

    // marker bits used to recognise natural structures
    private static final int M_BELL = 1, M_PATH = 2, M_RAIL = 4, M_WEB = 8, M_END = 16, M_ANCIENT = 32, M_TRIAL = 64,
            M_GILDED = 128, M_SPAWNER = 256, M_CHSAND = 512, M_TNT = 1024;

    private static final Map<Block, Sig> CLASSES = new HashMap<>();
    private static final Map<Block, Integer> MARKS = new HashMap<>();
    private static final Set<String> MACHINE_IDS = Set.of("hopper", "dropper", "dispenser", "observer", "piston", "sticky_piston",
            "repeater", "comparator", "tnt", "daylight_detector", "beacon", "enchanting_table", "redstone_lamp", "note_block", "jukebox", "crafter");
    private static final Set<String> REDSTONE_IDS = Set.of("redstone_wire", "lever", "tripwire_hook", "target", "slime_block", "honey_block",
            "lightning_rod", "sculk_sensor", "redstone_torch", "redstone_wall_torch", "powered_rail", "activator_rail");
    private static final Set<String> FARM_IDS = Set.of("farmland", "wheat", "carrots", "potatoes", "beetroots", "nether_wart", "composter",
            "melon_stem", "pumpkin_stem");
    private static final Set<String> PLACED_IDS = Set.of("crafting_table", "furnace", "blast_furnace", "smoker", "smithing_table", "anvil",
            "ladder", "scaffolding", "glass", "glass_pane", "iron_block", "gold_block", "diamond_block", "emerald_block", "netherite_block",
            "lapis_block", "redstone_block");
    private static final Set<String> DECOR_IDS = Set.of("flower_pot", "bookshelf", "chiseled_bookshelf", "lectern", "brewing_stand", "cauldron",
            "grindstone", "lodestone", "respawn_anchor", "conduit", "bricks", "smooth_stone", "quartz_block", "quartz_pillar", "polished_andesite",
            "polished_diorite", "polished_granite", "decorated_pot", "beehive", "jack_o_lantern", "carved_pumpkin", "cake", "end_crystal");

    // must come AFTER the id sets above: static initialisers run top to bottom
    static {
        for (Block b : Registries.BLOCK) {
            String p = Registries.BLOCK.getId(b).getPath();
            Sig s = classify(p);
            if (s != null) CLASSES.put(b, s);
            int m = mark(p);
            if (m != 0) MARKS.put(b, m);
        }
    }

    private static Sig classify(String p) {
        if (MACHINE_IDS.contains(p)) return Sig.MACHINE;
        if (REDSTONE_IDS.contains(p) || p.endsWith("_button") || p.endsWith("_pressure_plate")) return Sig.REDSTONE;
        if (p.equals("nether_portal")) return Sig.PORTAL;
        if (DECOR_IDS.contains(p) || p.startsWith("stripped_") || p.endsWith("_candle") || p.equals("candle")
                || p.endsWith("_skull") || p.endsWith("_head") || p.endsWith("_carpet")) return Sig.DECOR;
        if (FARM_IDS.contains(p)) return Sig.FARM;
        if (PLACED_IDS.contains(p) || p.endsWith("planks") || p.endsWith("_bed") || p.endsWith("_wool") || p.endsWith("_concrete")
                || p.endsWith("glazed_terracotta") || p.endsWith("_door") || p.endsWith("_trapdoor") || p.endsWith("_sign")
                || p.endsWith("_fence_gate") || (p.endsWith("_fence") && !p.startsWith("nether_brick")) || p.endsWith("_stained_glass")
                || p.endsWith("_stained_glass_pane") || p.endsWith("_banner")) return Sig.PLACED;
        if (p.endsWith("torch") || p.endsWith("lantern") || p.equals("glowstone") || p.equals("shroomlight") || p.equals("end_rod")
                || p.equals("campfire") || p.equals("soul_campfire")) return Sig.LIGHT;
        return null;
    }

    private static int mark(String p) {
        return switch (p) {
            case "bell" -> M_BELL;
            case "dirt_path" -> M_PATH;
            case "rail" -> M_RAIL;
            case "cobweb" -> M_WEB;
            case "end_portal_frame" -> M_END;
            case "sculk_shrieker", "reinforced_deepslate" -> M_ANCIENT;
            case "trial_spawner", "vault", "heavy_core" -> M_TRIAL;
            case "gilded_blackstone" -> M_GILDED;
            case "spawner" -> M_SPAWNER;
            case "chiseled_sandstone" -> M_CHSAND;
            case "tnt" -> M_TNT;
            default -> 0;
        };
    }

    // ---- settings ----
    final Setting.Num range = add(new Setting.Num("Range (chunks)", 6, 2, 12, 1, 0));
    final Setting.Num budget = add(new Setting.Num("Scan budget ms/tick", 3, 0.5, 10, 0.5, 1));
    final Setting.Num minPct = add(new Setting.Num("Min % to show", 30, 10, 90, 5, 0));
    final Setting.Num maxShow = add(new Setting.Num("Max shown", 40, 5, 100, 5, 0));
    final Setting.Num maxDist = add(new Setting.Num("Max distance", 800, 100, 3000, 100, 0));
    final Setting.Bool ignoreStructures = add(new Setting.Bool("Ignore natural structures", true));
    final Setting.Bool chatAlert = add(new Setting.Bool("Chat alert", true));
    final Setting.Bool soundAlert = add(new Setting.Bool("Alert sound", false));
    final Setting.Num alertAt = add(new Setting.Num("Alert at %", 60, 20, 95, 5, 0));
    final Setting.Bool tracer = add(new Setting.Bool("Tracer to best", false));
    final Setting.Bool sMachines = add(new Setting.Bool("Sig: machines", true));
    final Setting.Bool sStorage = add(new Setting.Bool("Sig: storage", true));
    final Setting.Bool sFarm = add(new Setting.Bool("Sig: farming", true));
    final Setting.Bool sPlaced = add(new Setting.Bool("Sig: built blocks", true));
    final Setting.Bool sLight = add(new Setting.Bool("Sig: torches", true));
    final Setting.Bool sRedstone = add(new Setting.Bool("Sig: redstone", true));
    final Setting.Bool sDecor = add(new Setting.Bool("Sig: decor", true));
    final Setting.Bool sPortal = add(new Setting.Bool("Sig: portals", true));

    private boolean on(Sig s) {
        return switch (s) {
            case MACHINE -> sMachines.v; case STORAGE -> sStorage.v; case FARM -> sFarm.v; case PLACED -> sPlaced.v;
            case LIGHT -> sLight.v; case REDSTONE -> sRedstone.v; case DECOR -> sDecor.v; case PORTAL -> sPortal.v;
        };
    }

    // ---- data ----
    private static final class Info {
        final int cx, cz;
        final double[] counts = new double[Sig.values().length];
        int marks, path, rail;
        double hx, hy, hz, hw, minY = 1e9, maxY = -1e9, own;
        int pct;
        Info(int cx, int cz) { this.cx = cx; this.cz = cz; }
        void add(Sig s, double n, double x, double y, double z) {
            counts[s.ordinal()] += n;
            double w = n * switch (s) { case MACHINE, STORAGE, REDSTONE -> 3.0; case PORTAL -> 0.5; case FARM -> 2.0; default -> 1.0; };
            hx += x * w; hy += y * w; hz += z * w; hw += w;
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        double midX() { return hw > 0 ? hx / hw : cx * 16 + 8; }
        double midZ() { return hw > 0 ? hz / hw : cz * 16 + 8; }
        double midY(double fallback) { return hw > 0 ? hy / hw : fallback; }
        long key() { return ChunkPos.toLong(cx, cz); }
    }

    private final Map<Long, Info> infos = new HashMap<>();
    private final Map<Long, Integer> scanned = new HashMap<>();   // chunk -> identity of the loaded WorldChunk we scanned
    private final Set<Long> alerted = new HashSet<>();
    private final ArrayDeque<long[]> queue = new ArrayDeque<>();
    private volatile List<Info> display = List.of();
    private Object lastWorld;
    private int timer;
    private long tick;

    SusChunkFinder() { super("Base Finder", "Rates chunks by how likely a player base is in them", Category.UNSEEN); }

    @Override public void onEnable() { timer = 1000; }
    @Override public void onWorldChange() { reset(); }
    private void reset() { infos.clear(); scanned.clear(); alerted.clear(); queue.clear(); display = List.of(); timer = 1000; }

    @Override public void onTick(MinecraftClient m) {
        if (m.player == null || m.world == null) return;
        if (m.world != lastWorld) { lastWorld = m.world; reset(); }
        tick++;

        if (queue.isEmpty()) {
            if (++timer >= 40) { timer = 0; beginPass(m); }
        }
        long t0 = System.nanoTime();
        boolean changed = false;
        while (!queue.isEmpty() && (System.nanoTime() - t0) / 1e6 < budget.v) {
            long[] c = queue.poll();
            WorldChunk ch = m.world.getChunkManager().getWorldChunk((int) c[0], (int) c[1]);
            if (ch == null) continue;
            Info i = scan(ch);
            scanned.put(i.key(), System.identityHashCode(ch));
            if (i.hw > 0) infos.put(i.key(), i); else infos.remove(i.key());
            changed = true;
        }
        if (changed || tick % 40 == 0) rebuild(m);   // also re-rate when a setting changed
    }

    private void beginPass(MinecraftClient m) {
        ChunkPos pc = m.player.getChunkPos();
        int r = range.i();
        var cm = m.world.getChunkManager();
        scanned.keySet().removeIf(k -> cm.getWorldChunk(ChunkPos.getPackedX(k), ChunkPos.getPackedZ(k)) == null);   // unloaded: scan again if it returns
        List<long[]> l = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int x = pc.x + dx, z = pc.z + dz;
            WorldChunk ch = cm.getWorldChunk(x, z);
            if (ch == null) continue;
            Integer seen = scanned.get(ChunkPos.toLong(x, z));
            if (seen != null && seen == System.identityHashCode(ch)) continue;   // already scanned this loaded chunk
            l.add(new long[]{x, z});
        }
        l.sort(Comparator.comparingLong(a -> (a[0] - pc.x) * (a[0] - pc.x) + (a[1] - pc.z) * (a[1] - pc.z)));   // nearest first
        queue.addAll(l);
    }

    private static boolean interesting(Block b) { return CLASSES.containsKey(b) || MARKS.containsKey(b); }

    private Info scan(WorldChunk ch) {
        Info in = new Info(ch.getPos().x, ch.getPos().z);
        int bx = ch.getPos().getStartX(), bz = ch.getPos().getStartZ(), bottom = ch.getBottomY();
        ChunkSection[] secs = ch.getSectionArray();
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty() || !s.hasAny(st -> interesting(st.getBlock()))) continue;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                Block b = s.getBlockState(x, y, z).getBlock();
                Integer mk = MARKS.get(b);
                if (mk != null) { in.marks |= mk; if (mk == M_PATH) in.path++; else if (mk == M_RAIL) in.rail++; }
                Sig sg = CLASSES.get(b);
                if (sg == null) continue;
                int wy = bottom + i * 16 + y;
                if (sg == Sig.LIGHT && wy >= 55) continue;       // only torches underground count
                double c = (sg == Sig.FARM && wy < 45) ? 2 : 1;  // underground farming counts double
                in.add(sg, c, bx + x + 0.5, wy + 0.5, bz + z + 0.5);
            }
        }
        int stores = 0; List<BlockEntity> list = new ArrayList<>();
        for (BlockEntity be : ch.getBlockEntities().values()) {
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity || be instanceof EnderChestBlockEntity) {
                stores++; list.add(be);
            }
        }
        if (!((in.marks & M_SPAWNER) != 0 && stores <= 2)) {      // a lone dungeon chest is natural
            for (BlockEntity be : list) in.add(Sig.STORAGE, 1, be.getPos().getX() + 0.5, be.getPos().getY() + 0.5, be.getPos().getZ() + 0.5);
        }
        return in;
    }

    private double structMult(Info i) {
        int m = i.marks;
        double f = 1;
        if ((m & (M_END | M_ANCIENT | M_TRIAL)) != 0) f = Math.min(f, 0.10);
        if ((m & M_BELL) != 0 || i.path >= 10) f = Math.min(f, 0.30);          // village
        if (i.rail >= 4 && (m & M_WEB) != 0) f = Math.min(f, 0.20);            // mineshaft
        if ((m & M_GILDED) != 0) f = Math.min(f, 0.30);                        // bastion
        if ((m & M_CHSAND) != 0 && (m & M_TNT) != 0) f = Math.min(f, 0.30);    // desert temple trap
        return f;
    }

    private double own(Info i) {
        double q = 1;
        for (Sig s : Sig.values()) {
            if (!on(s)) continue;
            double n = i.counts[s.ordinal()];
            if (n > 0) q *= 1 - s.rel * (1 - Math.exp(-n / s.k));
        }
        double p = 1 - q;
        return ignoreStructures.v ? p * structMult(i) : p;
    }

    private String reasons(Info i) {
        List<Sig> l = new ArrayList<>();
        for (Sig s : Sig.values()) if (on(s) && i.counts[s.ordinal()] > 0) l.add(s);
        l.sort(Comparator.comparingDouble((Sig s) -> -s.rel * (1 - Math.exp(-i.counts[s.ordinal()] / s.k))));
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < Math.min(3, l.size()); k++) {
            if (k > 0) sb.append(", ");
            sb.append(l.get(k).label).append(" x").append((int) i.counts[l.get(k).ordinal()]);
        }
        return sb.toString();
    }

    private void rebuild(MinecraftClient m) {
        for (Info i : infos.values()) i.own = own(i);
        List<Info> out = new ArrayList<>();
        for (Info i : infos.values()) {
            double nb = 0;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Info n = infos.get(ChunkPos.toLong(i.cx + dx, i.cz + dz));
                if (n != null) nb = Math.max(nb, n.own);
            }
            i.pct = (int) Math.round((1 - (1 - i.own) * (1 - 0.25 * nb)) * 100);
            if (i.pct >= minPct.i()) out.add(i);
        }
        display = out;
        for (Info i : out) {
            if (i.pct < alertAt.i() || !alerted.add(i.key())) continue;
            String where = "X " + (int) i.midX() + ", Y " + (int) i.midY(m.player.getY()) + ", Z " + (int) i.midZ();
            if (chatAlert.v) m.player.sendMessage(Text.literal("[Base Finder] Possible base " + i.pct + "% near " + where + "  (" + reasons(i) + ")"), false);
            Notifications.push("Base Finder", "Possible base " + i.pct + "%", where);
            if (soundAlert.v) Compat.chime(1.7f, (float) Theme.SOUND_VOL.v);
        }
    }

    private static int tier(int pct) { return pct >= 60 ? 0xFFFF4D6D : pct >= 40 ? 0xFFFF9A3C : 0xFFFFD25A; }

    @Override public void onHud(DrawContext c, float td) {
        List<Info> all = display;
        if (!Proj.ready || all.isEmpty()) return;
        List<Info> vis = new ArrayList<>();
        for (Info i : all) {
            double dx = i.midX() - Proj.camX, dz = i.midZ() - Proj.camZ;
            if (Math.sqrt(dx * dx + dz * dz) <= maxDist.v) vis.add(i);
        }
        vis.sort(Comparator.comparingInt((Info i) -> -i.pct));
        int n = 0;
        for (Info i : vis) {
            if (n >= maxShow.i()) break;
            int col = tier(i.pct);
            double y = i.midY(Proj.camY);
            double x0 = i.cx * 16.0, z0 = i.cz * 16.0, x1 = x0 + 16, z1 = z0 + 16;
            int w = n < 6 ? 2 : 1;
            Proj.line3(c, new double[]{x0, y, z0}, new double[]{x1, y, z0}, col, w);
            Proj.line3(c, new double[]{x1, y, z0}, new double[]{x1, y, z1}, col, w);
            Proj.line3(c, new double[]{x1, y, z1}, new double[]{x0, y, z1}, col, w);
            Proj.line3(c, new double[]{x0, y, z1}, new double[]{x0, y, z0}, col, w);
            double[] p = Proj.project(i.midX(), y + 1.5, i.midZ());
            if (p != null) {
                double dx = i.midX() - Proj.camX, dz = i.midZ() - Proj.camZ;
                String head = i.pct + "%  " + (int) Math.sqrt(dx * dx + dz * dz) + "m";
                String sub = Gfx.fit(reasons(i), 170);
                int tw = Math.max(Gfx.tw(head), Gfx.tw(sub)), sx = (int) p[0], sy = (int) p[1];
                int bw = tw + 10, bh = sub.isEmpty() ? 14 : 24;
                Gfx.rrect(c, sx - bw / 2, sy - bh, bw, bh, Theme.bg());
                Gfx.border(c, sx - bw / 2, sy - bh, bw, bh, Gfx.alpha(col, 0xA0));
                Gfx.ctext(c, head, sx, sy - bh + 3, col);
                if (!sub.isEmpty()) Gfx.ctext(c, sub, sx, sy - bh + 13, 0xFFB9B2D6);
                if (n == 0 && tracer.v) Gfx.line(c, c.getScaledWindowWidth() / 2.0, c.getScaledWindowHeight(), sx, sy, 1, Gfx.alpha(col, 0xB0));
            }
            n++;
        }
    }
}
