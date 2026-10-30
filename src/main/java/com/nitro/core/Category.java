package com.nitro.core;

public enum Category {
    COMBAT("Combat"), VISUALS("Render"), UNSEEN("Unseen"), HUD("HUD"), CLIENT("Client");
    public final String label;
    Category(String l) { label = l; }
}
