# Nitro Client (Fabric, Minecraft 1.21.11)

Original utility client with a Purxiy-style layout (icon tab bar, card grid, search, settings popup, restyled HUD), own code, own branding.

## Build
1. Generate a blank 1.21.11 Fabric project at https://fabricmc.net/develop (gives you the Gradle wrapper),
   copy this project's `src/`, `build.gradle`, `gradle.properties` over it (or run `gradle wrapper` with Gradle 9.2+).
2. `./gradlew build` with JDK 21.
3. Put `build/libs/nitro-client-1.0.0.jar` (not -sources) in `mods/` with Fabric Loader >= 0.18.1 and Fabric API 0.141.1+1.21.11.

## Use
- **Right Shift** opens the menu. Left-click a module = toggle, right-click or `>` = settings, click the key box then press a key = bind (Backspace clears).
- **HUD Editor** button (top right of menu): drag HUD elements, they snap to edges/center.
- Zoom is bound to **C** (hold). Freecam: WASD / Space / Shift / Ctrl(fast).
- Config: `config/nitroclient3.json` (menuKey, staffKeywords, every module/setting/HUD position).

## Modules
Combat: Target HUD (auto PvP mode: Sword/Axe/Mace/Crystal/Bow/Crossbow/Trident/Potion/Elytra/Fist), W-Tap, Ripple, Hitboxes, Combo Counter, Totem Counter, Hit Splash, Custom Crosshair (6 styles + hit confirm + cooldown), Radiance.
Unseen: Staff List (+vanished dimmed), Anti Vanish, Freecam, Player ESP, Storage ESP, Tracers, Radar, Chunk Finder.
Visuals: Skies (8 palettes incl. Galaxy), Weather, Ambience, Afterglow, Block Outline (spring glide), Name Tags, Zoom, Fullbright, Low Fire, Cute World, Cyberspace, Bokeh, Nick Hider.
HUD: FPS, Coordinates, Direction, Ping, Time, Profile, CPS, Keystrokes, Armor Status, Potion Effects, Active Modules, Notifications (+ Target/Combo/Totem/Staff/Radar above).
Client: Interface (accent colour, opacity, language Auto/English/Русский).

## Not included (needs shaders / external APIs / deeper mixins)
Procedural shader skies (Skies is colour-only), true glass-blur renderer, Spotify player, hand-movement tweaks. Cute World / Cyberspace / Bokeh are screen-overlay approximations, not shaders.
Name Tags adds a plate above the vanilla tag (vanilla tag is not hidden). Nick Hider covers chat + Nitro HUD only.

## If the build fails
Written without compiling. Errors should point at one of these:
- `core/Compat.java` (tick progress, GameProfile name/id, `face()` skin drawing) - rename calls. If `face()` will not compile, make its body empty: everything falls back to a letter tile.
- `NitroClient.java` HUD registry (`HudElementRegistry.addLast/replaceElement`, `VanillaHudElements.CROSSHAIR`).
- `Entity.getCameraPosVec`, `ParticleManager.addParticle`, `PlayerListEntry`, `ClientReceiveMessageEvents.MODIFY_GAME`.
Both mixins are optional (`require=0`): if their target changed, only totem-pop counting / hiding vanilla fire stop working.
ESP/tracers/outlines are drawn on the HUD with my own projection, so they use your Options FOV; sprint/effect FOV changes can offset them slightly at screen edges.

## Testing Skies
Enable Skies > pick a palette. If the sky stays vanilla, the 1.21.11 sky-colour method has moved (mixin is optional, so nothing crashes): check `logs/latest.log` for 'ClientWorldMixin' / 'getSkyColor' and send it to me.

## Build the jar on GitHub (no local setup)
1. Create a GitHub repo and upload everything in this folder (including the hidden `.github` folder).
2. Open the repo > **Actions** tab > the "Build jar" run (starts on every push, or press "Run workflow").
3. When it turns green, open the run and download **nitro-client-jar** under Artifacts. Unzip it: that's your `.jar`.
If it fails (red), open the run and copy the compile errors to me.

## v1.1 changes
- New menu: icon tab bar, 4-column module cards (click = toggle, right-click or the sliders icon = settings), search (press `/`), HUD Editor button.
- Restyled HUD: FPS pill with graph, Keystrokes, Target HUD (mode pill, damage taken, hp, armour segments, pop/totem/armor chips), Staff List, toast-style Notifications / Anti Vanish.
- ESP, Tracers, Radar, Name Tags now include mobs by default so you can see them work in singleplayer.
- Skies now use Purxiy's eight names: Galaxy, Aether, Nocturne, Amethyst, Opaline, Rain, Snow, Fire (colour palettes, not shaders).
- A module that throws an error is logged (`logs/latest.log`, search "[Nitro]") and switched off after 3 failures instead of crashing.
- Config moved to `config/nitroclient2.json` (fresh defaults).

## v1.2 changes
- Whole UI rebuilt on rounded shapes (panel, tabs, cards, round toggles, pill toasts) with a soft accent glow on enabled cards.
- Every module card now has a real glyph icon instead of a letter.
- Target HUD, Staff List and Anti Vanish / Staff toasts show the player's skin face.
- Custom Crosshair defaults to the pink glowing X (Cross, thickness 2).
- New module: Fullbright (client-side night vision, removed again when you turn it off).
- Zoom puts your real FOV back when the game closes (it used to stay zoomed in options.txt if you quit while holding C).
- Config moved to `config/nitroclient3.json` so the new defaults apply.

## v1.4.2
- Fixed startup crash in Base Finder (static initialisation order).

## v1.4.1
- New module: Base Finder (Unseen). Scores loaded chunks by clues of player building (machines, storage, farming, built blocks, underground torches, redstone, decor, portals), damps natural structures, outlines suspects with a % tag, optional chat alert and tracer.

## v1.4
- Skies now works on 1.21.11: hooks SkyRendering.renderTopSky and FogRenderer.getFogColor (found from the real class dump).
- Freecam uses the real lastX/lastYaw fields, so the view no longer jitters.
- Menu chime restored via PositionedSoundInstance.ui(...).

## v1.3.3
- Menu chime disabled until the correct 1.21.11 sound call is known (build was failing on it).

## v1.3.1
- Fixed the 6 compile errors from the first CI run (Freecam camera fields, menu chime overload).

## v1.3 changes
- HUD Editor moved out of the header into **Client → HUD Editor** (it is a button card, not a toggle).
- HUD Editor can now resize: drag the square on an element's bottom-right corner, or scroll over it. Right-click resets to 100%. Sizes are saved.
- GUI opening animation (backdrop fade + pop-in).
- Soft amethyst chime when you hover cards (notes follow a pentatonic scale, so sweeping across cards sounds musical), plus click/toggle/tab sounds. Switch off or change volume in Client → Interface.
- Freecam: no more shaking (the camera entity's previous-position fields were stale, so the renderer interpolated back and forth every tick) and added smooth acceleration.
- Build workflow now also uploads a `nitro-signatures` artifact with the real 1.21.11 method names (used to fix Skies).
