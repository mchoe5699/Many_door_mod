package com.mchoe5699.manydoors.door;

import net.minecraft.nbt.CompoundTag;

public record DoorSettings(int width, int height, double thickness, double angle, Mode mode, int closeTicks, boolean rightHinge) {
    public enum Mode { MANUAL, AUTOMATIC, BOTH;
        public boolean allowsClick() { return this != AUTOMATIC; }
        public boolean allowsSensor() { return this != MANUAL; }
    }
    public static DoorSettings defaults(DoorStyle style) {
        return new DoorSettings(style.width, style.height, 0.125, 90, Mode.BOTH, 60, false);
    }
    public boolean valid() {
        return width >= 1 && width <= 6 && height >= 1 && height <= 6
            && Double.isFinite(thickness) && thickness >= 0.0625 && thickness <= 0.5
            && Double.isFinite(angle) && angle >= 1 && angle <= 180
            && mode != null && closeTicks >= 0 && closeTicks <= 1200;
    }
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Width", width); t.putInt("Height", height); t.putDouble("Thickness", thickness);
        t.putDouble("Angle", angle); t.putString("Mode", mode.name()); t.putInt("CloseTicks", closeTicks);
        t.putBoolean("RightHinge", rightHinge); return t;
    }
    public static DoorSettings load(CompoundTag t, DoorStyle style) {
        try {
            DoorSettings s = new DoorSettings(t.getInt("Width"), t.getInt("Height"), t.getDouble("Thickness"),
                t.getDouble("Angle"), Mode.valueOf(t.getString("Mode")), t.getInt("CloseTicks"), t.getBoolean("RightHinge"));
            return s.valid() ? s : defaults(style);
        } catch (IllegalArgumentException e) { return defaults(style); }
    }
}
