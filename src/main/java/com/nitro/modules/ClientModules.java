package com.nitro.modules;

import com.nitro.core.*;
import com.nitro.gui.HudEditor;

/** Client → Interface: language, accent colour, panel opacity, menu sounds. */
final class InterfaceMod extends Mod {
    InterfaceMod() {
        super("Interface", "Menu look, accent colour, sounds and language", Category.CLIENT);
        add(Theme.ACCENT); add(Theme.OPACITY); add(Theme.SOUNDS); add(Theme.SOUND_VOL); add(Lang.MODE);
        enabled = true;
    }
    @Override public boolean persistEnabled() { return false; }
}

/** Client → HUD Editor: a button card that opens the drag / resize editor. */
final class HudEditorMod extends Mod {
    HudEditorMod() { super("HUD Editor", "Drag, resize and arrange every HUD element", Category.CLIENT); }
    @Override public boolean isAction() { return true; }
    @Override public boolean persistEnabled() { return false; }
    @Override public void setEnabled(boolean b) { }
    @Override public void toggle() { run(); }
    @Override public void run() {
        ModuleManager.save();
        mc.setScreen(new HudEditor());
    }
}
