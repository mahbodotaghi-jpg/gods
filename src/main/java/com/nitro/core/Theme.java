package com.nitro.core;

public final class Theme {
    public static final Setting.Col ACCENT = new Setting.Col("Accent", 0xA855F7);
    public static final Setting.Num OPACITY = new Setting.Num("Panel opacity", 0.80, 0.3, 1.0, 0.02, 2);
    public static final Setting.Bool SOUNDS = new Setting.Bool("Menu sounds", true);
    public static final Setting.Num SOUND_VOL = new Setting.Num("Menu volume", 0.35, 0.0, 1.0, 0.05, 2);
    public static final int TEXT = 0xFFF2EEFF, DIM = 0xFF9A93B8;

    private Theme() {}
    public static int accent() { return ACCENT.argb(255); }
    public static int accentA(int a) { return ACCENT.argb(a); }
    public static int bg() { return (((int) (OPACITY.v * 255)) << 24) | 0x0E0A1A; }
}
