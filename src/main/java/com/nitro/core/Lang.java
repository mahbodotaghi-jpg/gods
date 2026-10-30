package com.nitro.core;

import net.minecraft.client.MinecraftClient;

import java.util.Map;

public final class Lang {
    public static final Setting.Mode MODE = new Setting.Mode("Language", new String[]{"Auto", "English", "Русский"}, 0);
    private static final Map<String, String> RU = Map.ofEntries(
        Map.entry("Combat", "Бой"), Map.entry("Render", "Рендер"), Map.entry("Unseen", "Скрытое"),
        Map.entry("HUD", "HUD"), Map.entry("Client", "Клиент"),
        Map.entry("HUD Editor", "Редактор HUD"), Map.entry("Reset all settings", "Сбросить настройки"),
        Map.entry("Right-click a module for settings", "ПКМ по модулю - настройки"),
        Map.entry("Drag HUD elements. Esc to go back.", "Перетаскивайте элементы. Esc - назад."),
        Map.entry("Scanned", "Просканировано"));

    private Lang() {}

    public static boolean ru() {
        if (MODE.idx == 2) return true;
        if (MODE.idx == 1) return false;
        String l = MinecraftClient.getInstance().options.language;
        return l != null && l.startsWith("ru");
    }
    public static String t(String s) { return ru() ? RU.getOrDefault(s, s) : s; }
}
