package com.nitro.modules;

import com.nitro.core.*;
import com.nitro.gui.HudEditor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.regex.Pattern;

// ======================================================================= Freecam
final class Freecam extends Mod {
    static boolean on;
    final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.1, 5, 0.1, 1));
    final Setting.Bool smooth = add(new Setting.Bool("Smooth movement", true));
    private ArmorStandEntity cam;
    private double x, y, z, vx, vy, vz, eh; private long last;

    Freecam() { super("Freecam", "Leave your body and fly the camera anywhere (WASD, Space, Shift, Ctrl = fast)", Category.UNSEEN); }
    @Override public boolean persistEnabled() { return false; }

    @Override public void onEnable() {
        if (mc.player == null || mc.world == null) { enabled = false; return; }
        cam = new ArmorStandEntity(EntityType.ARMOR_STAND, mc.world);
        cam.refreshPositionAndAngles(0, 0, 0, 0, 0);
        eh = cam.getEyeY() - cam.getY();
        x = mc.player.getX(); y = mc.player.getEyeY(); z = mc.player.getZ();
        vx = vy = vz = 0;
        place();
        mc.setCameraEntity(cam);
        last = System.nanoTime(); on = true;
    }
    @Override public void onDisable() {
        on = false;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
        restoreKeys();
    }
    @Override public void onWorldChange() { if (enabled) setEnabled(false); }

    private KeyBinding[] keys() {
        return new KeyBinding[]{mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey,
                                mc.options.jumpKey, mc.options.sneakKey, mc.options.sprintKey};
    }
    private void restoreKeys() { for (KeyBinding k : keys()) k.setPressed(Keys.isDown(k)); }

    /** Keep the real player from walking while the camera flies. */
    @Override public void onStartTick(MinecraftClient m) { for (KeyBinding k : keys()) k.setPressed(false); }

    /**
     * Put the camera entity exactly where we want it. The shaking came from the entity's "previous" position/rotation
     * (which the renderer interpolates with) staying stale, so the view slid back and forth every tick.
     * We now overwrite prev* and lastRender* with the current values every frame, so there is nothing to interpolate.
     */
    private void place() {
        cam.refreshPositionAndAngles(x, y - eh, z, mc.player.getYaw(), mc.player.getPitch());
        // overwrite the "previous" values too so the renderer has nothing to interpolate between
        cam.lastX = cam.lastRenderX = cam.getX();
        cam.lastY = cam.lastRenderY = cam.getY();
        cam.lastZ = cam.lastRenderZ = cam.getZ();
        cam.lastYaw = cam.getYaw();
        cam.lastPitch = cam.getPitch();
    }

    /** Per-frame so the camera moves smoothly (not at 20 Hz). */
    @Override public void onHud(DrawContext c, float td) {
        if (cam == null || mc.player == null) return;
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1e9); last = now;
        double tx = 0, ty = 0, tz = 0;
        if (mc.currentScreen == null) {
            double yr = Math.toRadians(mc.player.getYaw());
            double fx = -Math.sin(yr), fz = Math.cos(yr), rx = -Math.cos(yr), rz = -Math.sin(yr);
            double mx = 0, mz = 0, my = 0;
            if (Keys.down(87)) { mx += fx; mz += fz; }
            if (Keys.down(83)) { mx -= fx; mz -= fz; }
            if (Keys.down(68)) { mx += rx; mz += rz; }
            if (Keys.down(65)) { mx -= rx; mz -= rz; }
            if (Keys.down(32)) my += 1;
            if (Keys.down(340)) my -= 1;
            double len = Math.sqrt(mx * mx + mz * mz);
            if (len > 0) { mx /= len; mz /= len; }
            double sp = speed.v * 10 * (Keys.down(341) ? 2.5 : 1);   // blocks per second
            tx = mx * sp; tz = mz * sp; ty = my * sp;
        }
        if (smooth.v) {
            double k = 1 - Math.exp(-dt * 14);
            vx += (tx - vx) * k; vy += (ty - vy) * k; vz += (tz - vz) * k;
        } else { vx = tx; vy = ty; vz = tz; }
        x += vx * dt; y += vy * dt; z += vz * dt;
        place();
    }
}

// ======================================================================= Player ESP
final class PlayerEsp extends Mod {
    final Setting.Num range = add(new Setting.Num("Range", 128, 16, 512, 16, 0));
    final Setting.Num fill = add(new Setting.Num("Fill opacity", 40, 0, 120, 5, 0));
    final Setting.Bool mobs = add(new Setting.Bool("Include mobs", true));
    final Setting.Bool names = add(new Setting.Bool("Name + health", true));
    final Setting.Col color = add(new Setting.Col("Color", 0xB48CFF));

    PlayerEsp() { super("Player ESP", "Every player lit through walls in soft filled light", Category.UNSEEN); }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.wanted(e, true, mobs.v, false) || Targets.dist(e) > range.v) continue;
            double[] r = Proj.rect(Proj.box(e, td));
            if (r == null) continue;
            int x1 = (int) r[0], y1 = (int) r[1], x2 = (int) r[2], y2 = (int) r[3];
            if (x2 < -50 || y2 < -50 || x1 > c.getScaledWindowWidth() + 50 || y1 > c.getScaledWindowHeight() + 50) continue;
            int rgb = e instanceof PlayerEntity ? color.rgb() : 0xFFC040;
            c.fill(x1, y1, x2, y2, Gfx.alpha(rgb, fill.i()));
            Gfx.outline(c, x1, y1, x2, y2, Gfx.alpha(rgb, 230));
            if (names.v) {
                String s = NickHider.apply(e.getName().getString());
                if (e instanceof LivingEntity l) s += "  " + String.format("%.0f", l.getHealth() + l.getAbsorptionAmount());
                Gfx.text(c, s, (x1 + x2) / 2 - Gfx.tw(s) / 2, y1 - 11, Theme.TEXT);
            }
        }
    }
}

// ======================================================================= Tracers
final class Tracers extends Mod {
    final Setting.Bool mobs = add(new Setting.Bool("Include mobs", true));
    final Setting.Mode origin = add(new Setting.Mode("Origin", new String[]{"Bottom", "Crosshair"}, 0));
    final Setting.Num range = add(new Setting.Num("Range", 192, 16, 512, 16, 0));
    final Setting.Num width = add(new Setting.Num("Width", 1, 1, 3, 1, 0));
    final Setting.Col color = add(new Setting.Col("Color", 0xB48CFF));

    Tracers() { super("Tracers", "A thin line to every player", Category.UNSEEN); }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        int ox = c.getScaledWindowWidth() / 2, oy = origin.idx == 0 ? c.getScaledWindowHeight() : c.getScaledWindowHeight() / 2;
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.wanted(e, true, mobs.v, false)) continue;
            double d = Targets.dist(e);
            if (d > range.v) continue;
            Box b = Proj.box(e, td);
            double[] p = Proj.project((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, (b.minZ + b.maxZ) / 2);
            if (p == null) continue;
            int a = (int) Math.max(70, 230 - d);
            Gfx.line(c, ox, oy, p[0], p[1], width.i(), color.argb(a));
        }
    }
}

// ======================================================================= Storage ESP
final class StorageEsp extends Mod {
    final Setting.Num radius = add(new Setting.Num("Scan radius (chunks)", 6, 2, 12, 1, 0));
    final Setting.Bool chests = add(new Setting.Bool("Chests", true));
    final Setting.Bool barrels = add(new Setting.Bool("Barrels", true));
    final Setting.Bool shulkers = add(new Setting.Bool("Shulkers", true));
    final Setting.Bool enders = add(new Setting.Bool("Ender chests", true));
    final Setting.Bool fillBox = add(new Setting.Bool("Soft fill", true));

    private record Hit(BlockPos pos, int rgb) {}
    private List<Hit> hits = new ArrayList<>();
    private final Set<BlockPos> opened = new HashSet<>();
    private BlockPos looked; private int t;

    StorageEsp() { super("Storage ESP", "Chests, barrels and shulkers glow through the ground", Category.UNSEEN); }

    @Override public void onWorldChange() { hits.clear(); opened.clear(); }

    @Override public void onTick(MinecraftClient m) {
        if (m.player == null || m.world == null) return;
        if (m.currentScreen == null) {
            HitResult hr = m.crosshairTarget;
            looked = (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) ? bh.getBlockPos() : null;
        } else if (m.currentScreen instanceof HandledScreen<?> && looked != null) opened.add(looked);

        if (++t % 10 != 0) return;
        List<Hit> out = new ArrayList<>();
        ChunkPos pc = m.player.getChunkPos(); int r = radius.i();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            WorldChunk ch = m.world.getChunkManager().getWorldChunk(pc.x + dx, pc.z + dz);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                int rgb;
                if (be instanceof ChestBlockEntity && chests.v) rgb = 0xFFA040;
                else if (be instanceof BarrelBlockEntity && barrels.v) rgb = 0xC08050;
                else if (be instanceof ShulkerBoxBlockEntity && shulkers.v) rgb = 0xFF70D0;
                else if (be instanceof EnderChestBlockEntity && enders.v) rgb = 0xB060FF;
                else continue;
                out.add(new Hit(be.getPos(), rgb));
            }
        }
        hits = out;
    }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (Hit h : hits) {
            double dx = h.pos().getX() + 0.5 - Proj.camX, dy = h.pos().getY() + 0.5 - Proj.camY, dz = h.pos().getZ() + 0.5 - Proj.camZ;
            if (dx * dx + dy * dy + dz * dz > 96 * 96) continue;
            boolean seen = opened.contains(h.pos());
            Box b = new Box(h.pos()).contract(0.06);
            if (fillBox.v) {
                double[] r = Proj.rect(b);
                if (r != null) c.fill((int) r[0], (int) r[1], (int) r[2], (int) r[3], Gfx.alpha(h.rgb(), seen ? 0x10 : 0x2A));
            }
            Gfx.wire(c, b, Gfx.alpha(h.rgb(), seen ? 0x70 : 0xE6), 1);
        }
    }
}

// ======================================================================= Radar
final class Radar extends HudMod {
    final Setting.Num range = add(new Setting.Num("Range", 64, 16, 256, 8, 0));
    final Setting.Num size = add(new Setting.Num("Size", 44, 24, 80, 2, 0));
    final Setting.Bool mobs = add(new Setting.Bool("Show mobs", true));

    Radar() { super("Radar", "Players around you on a round glass radar", Category.UNSEEN, 6, 330); }

    public void draw(DrawContext c, float td) {
        int r = size.i(); w = r * 2 + 4; h = r * 2 + 4;
        int cx = x + r + 2, cy = y + r + 2;
        Gfx.circle(c, cx, cy, r, Theme.bg());
        Gfx.circle(c, cx, cy, r / 2, 0x14FFFFFF);
        Gfx.ring(c, cx, cy, r, 1, Theme.accentA(0xB0));
        c.fill(cx - r, cy, cx + r, cy + 1, 0x14FFFFFF); c.fill(cx, cy - r, cx + 1, cy + r, 0x14FFFFFF);
        double yr = Math.toRadians(Proj.ready ? Proj.yaw : mc.player.getYaw());
        double fx = -Math.sin(yr), fz = Math.cos(yr), rx = -Math.cos(yr), rz = -Math.sin(yr);
        Entity self = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        for (Entity e : mc.world.getEntities()) {
            if (!Targets.wanted(e, true, mobs.v, false)) continue;
            double dx = e.getX() - self.getX(), dz = e.getZ() - self.getZ();
            double px = dx * rx + dz * rz, pf = dx * fx + dz * fz;
            double d = Math.hypot(px, pf);
            if (d > range.v) { px *= range.v / d; pf *= range.v / d; }
            int sx = cx + (int) (px / range.v * (r - 3)), sy = cy - (int) (pf / range.v * (r - 3));
            Gfx.circle(c, sx, sy, 2, e instanceof PlayerEntity ? 0xFFFF5080 : 0xFFFFC040);
        }
        Gfx.circle(c, cx, cy, 2, Theme.accent());
    }
}

// ======================================================================= Staff list + Anti vanish
final class StaffList extends HudMod {
    private static final class S { String name, rank; UUID id; long seen; boolean online; }
    private final Map<String, S> staff = new LinkedHashMap<>();
    private Pattern pat; private int t;

    StaffList() { super("Staff List", "Every staff member online, with their rank", Category.UNSEEN, 300, 270); }

    private Pattern pattern() {
        List<String> esc = new ArrayList<>();
        for (String k : ModuleManager.staffKeywords) esc.add(Pattern.quote(k));
        return Pattern.compile("\\b(" + String.join("|", esc) + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    @Override public void onWorldChange() { staff.clear(); }

    @Override public void onMessage(Text msg) {
        String s = msg.getString().toLowerCase();
        if (!s.contains("left the game")) return;
        staff.values().removeIf(x -> x.name != null && s.contains(x.name.toLowerCase()));
    }

    @Override public void onTick(MinecraftClient m) {
        if (m.getNetworkHandler() == null || ++t % 20 != 0) return;
        if (pat == null || t % 400 == 0) pat = pattern();
        Set<String> present = new HashSet<>();
        long now = System.currentTimeMillis();
        for (PlayerListEntry e : m.getNetworkHandler().getPlayerList()) {
            String name = Compat.name(e.getProfile());
            String disp = e.getDisplayName() != null ? e.getDisplayName().getString() : "";
            Team team = e.getScoreboardTeam();
            if (team != null) disp = team.getPrefix().getString() + disp;
            var mt = pat.matcher(disp);
            if (!mt.find()) continue;
            present.add(name);
            S s = staff.computeIfAbsent(name, k -> new S());
            UUID pid = Compat.id(e.getProfile());
            if (s.name == null) Notifications.push("Staff", name, mt.group(1) + " is online", pid);
            s.name = name; s.id = pid; s.rank = mt.group(1); s.seen = now; s.online = true;
        }
        for (S s : staff.values()) if (!present.contains(s.name)) s.online = false;
        staff.values().removeIf(s -> !s.online && now - s.seen > 600_000);
    }

    private static int rankColor(String r) {
        String l = r.toLowerCase();
        if (l.contains("owner") || l.contains("admin") || l.contains("manager")) return 0xFFFF6B7D;
        if (l.contains("mod")) return 0xFF45D6C4;
        if (l.contains("helper") || l.contains("support")) return 0xFF6BE08A;
        return 0xFFB48CFF;
    }

    public void draw(DrawContext c, float td) {
        List<S> list = new ArrayList<>(staff.values());
        if (list.isEmpty()) {
            if (!HudEditor.editing) { w = 0; h = 0; return; }
            S a = new S(); a.name = "Alex"; a.rank = "Admin"; a.online = true; list.add(a);
            S b = new S(); b.name = "Kai"; b.rank = "Mod"; b.online = true; list.add(b);
        }
        int online = 0; for (S s : list) if (s.online) online++;
        w = 200; h = 40 + list.size() * 26;
        Gfx.card(c, x, y, w, h);
        Gfx.rgrad(c, x + 8, y + 8, 22, 22, Gfx.bright(), Gfx.deep());
        Icons.shield(c, x + 19, y + 19, 0xFFFFFFFF);
        Gfx.bold(c, "Staff", x + 38, y + 14, 0xFFFFFF);
        String ol = online + " online";
        int pw = Gfx.tw(ol) + 22;
        Gfx.rrect(c, x + w - 8 - pw, y + 10, pw, 18, 0x30FF5FA8); Gfx.border(c, x + w - 8 - pw, y + 10, pw, 18, 0xA0FF5FA8);
        Gfx.circle(c, x + w - pw, y + 19, 2, 0xFFFF5FA8);
        Gfx.text(c, ol, x + w - pw + 8, y + 15, 0xFFFFFFFF);
        int yy = y + 36;
        for (S s : list) {
            int a = s.online ? 255 : 110;
            Gfx.rrect(c, x + 8, yy, w - 16, 22, 0x24FFFFFF);
            Gfx.rrect(c, x + 12, yy + 3, 16, 16, Gfx.alpha(0x3A2A66, a));
            if (s.id != null) { Compat.face(c, s.id, x + 13, yy + 4, 14); if (!s.online) Gfx.rrect(c, x + 12, yy + 3, 16, 16, 0x99100B1E); }
            else Gfx.ctext(c, s.name.substring(0, 1).toUpperCase(), x + 20, yy + 7, s.online ? 0xFFFFFF : 0x8A84A6);
            Gfx.text(c, Gfx.fit(s.name, 90), x + 34, yy + 7, s.online ? 0xFFFFFFFF : 0xFF7F789A);
            String rk = s.rank.toUpperCase(); int rw = Gfx.tw(rk) + 12; int col = rankColor(s.rank);
            int rcol = s.online ? col : Gfx.alpha(col, 110);
            Gfx.rrect(c, x + w - 14 - rw, yy + 3, rw, 16, Gfx.alpha(col, 0x28)); Gfx.border(c, x + w - 14 - rw, yy + 3, rw, 16, rcol);
            Gfx.text(c, rk, x + w - 8 - rw, yy + 7, rcol);
            yy += 26;
        }
    }
}

final class AntiVanish extends Mod {
    private final Set<UUID> seen = new HashSet<>(); private int t;
    AntiVanish() { super("Anti Vanish", "A soft alert when someone watches from spectator", Category.UNSEEN); }
    @Override public void onWorldChange() { seen.clear(); }
    @Override public void onTick(MinecraftClient m) {
        if (m.getNetworkHandler() == null || m.player == null || ++t % 10 != 0) return;
        Set<UUID> now = new HashSet<>();
        for (PlayerListEntry e : m.getNetworkHandler().getPlayerList()) {
            UUID id = Compat.id(e.getProfile());
            if (id.equals(m.player.getUuid()) || e.getGameMode() != GameMode.SPECTATOR) continue;
            now.add(id);
            if (seen.add(id)) Notifications.push("Vanish detected", Compat.name(e.getProfile()), "Switched to spectator", id);
        }
        seen.retainAll(now);
    }
}

// ======================================================================= Chunk Finder
final class ChunkFinder extends Mod {
    final Setting.Num radius = add(new Setting.Num("Scan radius (chunks)", 8, 2, 16, 1, 0));
    final Setting.Num maxY = add(new Setting.Num("Only below Y", 60, -60, 320, 4, 0));
    final Setting.Bool portals = add(new Setting.Bool("Lit portals", true));
    final Setting.Bool wither = add(new Setting.Bool("Wither farms", true));
    final Setting.Bool ways = add(new Setting.Bool("Ways down", true));
    final Setting.Num dist = add(new Setting.Num("Label distance", 400, 64, 1000, 16, 0));
    final Setting.Num budget = add(new Setting.Num("Budget ms/tick", 2, 0.5, 8, 0.5, 1));

    private record Find(String type, BlockPos pos, int rgb) {}
    private final Map<Long, List<Find>> found = new HashMap<>();
    private final Map<Long, Long> scanned = new HashMap<>();
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
    private long tick;

    ChunkFinder() { super("Chunk Finder", "Signs of bases hidden underground: portals, wither farms, ways down", Category.UNSEEN); }

    @Override public void onWorldChange() { found.clear(); scanned.clear(); queue.clear(); }

    @Override public void onTick(MinecraftClient m) {
        if (m.player == null || m.world == null) return;
        tick++;
        if (queue.isEmpty() && tick % 40 == 0) {
            ChunkPos pc = m.player.getChunkPos(); int r = radius.i();
            List<ChunkPos> l = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                ChunkPos p = new ChunkPos(pc.x + dx, pc.z + dz);
                Long last = scanned.get(p.toLong());
                if (last == null || tick - last > 1200) l.add(p);
            }
            l.sort(Comparator.comparingInt(p -> (p.x - pc.x) * (p.x - pc.x) + (p.z - pc.z) * (p.z - pc.z)));
            queue.addAll(l);
        }
        long start = System.nanoTime();
        while (!queue.isEmpty() && (System.nanoTime() - start) / 1e6 < budget.v) scan(m, queue.poll());
    }

    private static boolean interesting(Block b) {
        return b == Blocks.NETHER_PORTAL || b == Blocks.WITHER_SKELETON_SKULL || b == Blocks.WITHER_SKELETON_WALL_SKULL
            || b == Blocks.SOUL_SAND || b == Blocks.LADDER;
    }

    private void scan(MinecraftClient m, ChunkPos p) {
        WorldChunk ch = m.world.getChunkManager().getWorldChunk(p.x, p.z);
        if (ch == null) return;
        scanned.put(p.toLong(), tick);
        int portal = 0, skulls = 0, sand = 0, ladders = 0;
        BlockPos pp = null, sp = null, lp = null;
        ChunkSection[] secs = ch.getSectionArray();
        int bottom = ch.getBottomY(), limit = maxY.i();
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || s.isEmpty() || bottom + i * 16 > limit) continue;
            if (!s.hasAny(st -> interesting(st.getBlock()))) continue;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                Block b = s.getBlockState(x, y, z).getBlock();
                if (!interesting(b)) continue;
                int wy = bottom + i * 16 + y;
                if (wy > limit) continue;
                BlockPos pos = new BlockPos(p.getStartX() + x, wy, p.getStartZ() + z);
                if (b == Blocks.NETHER_PORTAL) { portal++; if (pp == null) pp = pos; }
                else if (b == Blocks.SOUL_SAND) sand++;
                else if (b == Blocks.LADDER) { ladders++; if (lp == null) lp = pos; }
                else { skulls++; if (sp == null) sp = pos; }
            }
        }
        List<Find> out = new ArrayList<>();
        if (portals.v && portal > 0) out.add(new Find("Portal", pp, 0xFFB060FF));
        if (wither.v && skulls >= 2 && sand >= 8) out.add(new Find("Wither farm", sp, 0xFFFF5060));
        if (ways.v && ladders >= 10) out.add(new Find("Way down", lp, 0xFF60E0FF));
        if (out.isEmpty()) found.remove(p.toLong()); else found.put(p.toLong(), out);
    }

    @Override public void onHud(DrawContext c, float td) {
        if (!Proj.ready) return;
        for (List<Find> l : found.values()) for (Find f : l) {
            double dx = f.pos().getX() + 0.5 - Proj.camX, dy = f.pos().getY() + 0.5 - Proj.camY, dz = f.pos().getZ() + 0.5 - Proj.camZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > dist.v) continue;
            double[] p = Proj.project(f.pos().getX() + 0.5, f.pos().getY() + 0.5, f.pos().getZ() + 0.5);
            if (p == null) continue;
            int sx = (int) p[0], sy = (int) p[1];
            Gfx.line(c, sx, sy - 4, sx + 4, sy, 1, f.rgb()); Gfx.line(c, sx + 4, sy, sx, sy + 4, 1, f.rgb());
            Gfx.line(c, sx, sy + 4, sx - 4, sy, 1, f.rgb()); Gfx.line(c, sx - 4, sy, sx, sy - 4, 1, f.rgb());
            String s = f.type() + "  " + (int) d + "m";
            int tw = Gfx.tw(s);
            Gfx.rrect(c, sx - tw / 2 - 4, sy - 20, tw + 8, 12, Theme.bg());
            Gfx.text(c, s, sx - tw / 2, sy - 18, f.rgb());
        }
    }
}
