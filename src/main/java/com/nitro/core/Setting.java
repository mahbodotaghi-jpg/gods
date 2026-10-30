package com.nitro.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);
    public abstract void reset();

    public static final class Bool extends Setting {
        public boolean v; private final boolean def;
        public Bool(String n, boolean d) { super(n); v = d; def = d; }
        public JsonElement save() { return new JsonPrimitive(v); }
        public void load(JsonElement e) { v = e.getAsBoolean(); }
        public void reset() { v = def; }
    }

    public static final class Num extends Setting {
        public double v; public final double min, max, step; public final int dec; private final double def;
        public Num(String n, double d, double min, double max, double step, int dec) {
            super(n); v = d; def = d; this.min = min; this.max = max; this.step = step; this.dec = dec;
        }
        public int i() { return (int) Math.round(v); }
        public float f() { return (float) v; }
        public JsonElement save() { return new JsonPrimitive(v); }
        public void load(JsonElement e) { v = Math.max(min, Math.min(max, e.getAsDouble())); }
        public void reset() { v = def; }
    }

    public static final class Mode extends Setting {
        public final String[] opts; public int idx; private final int def;
        public Mode(String n, String[] opts, int d) { super(n); this.opts = opts; idx = d; def = d; }
        public String get() { return opts[idx]; }
        public boolean is(String s) { return opts[idx].equals(s); }
        public void next(int dir) { idx = (idx + dir + opts.length) % opts.length; }
        public JsonElement save() { return new JsonPrimitive(idx); }
        public void load(JsonElement e) { int i = e.getAsInt(); if (i >= 0 && i < opts.length) idx = i; }
        public void reset() { idx = def; }
    }

    public static final class Col extends Setting {
        public int r, g, b; private final int def;
        public Col(String n, int rgb) { super(n); set(rgb); def = rgb; }
        public void set(int rgb) { r = (rgb >> 16) & 255; g = (rgb >> 8) & 255; b = rgb & 255; }
        public int rgb() { return (r << 16) | (g << 8) | b; }
        public int argb(int a) { return (a << 24) | rgb(); }
        public JsonElement save() { return new JsonPrimitive(rgb()); }
        public void load(JsonElement e) { set(e.getAsInt()); }
        public void reset() { set(def); }
    }
}
