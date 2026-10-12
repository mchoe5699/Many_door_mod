package com.mchoe5699.manydoors.door;

public enum DoorStyle {
    WOODEN("wooden_door", Motion.SWING, Surface.WOOD, 1, 2),
    PANEL("panel_door", Motion.SWING, Surface.WOOD, 1, 2),
    FLUSH("flush_door", Motion.SWING, Surface.WHITE, 1, 2),
    TRADITIONAL("traditional_door", Motion.DOUBLE_SWING, Surface.WOOD, 2, 2),
    STEEL_ENTRANCE("steel_entrance", Motion.SWING, Surface.STEEL, 1, 2),
    APARTMENT_ENTRANCE("apartment_entrance", Motion.SWING, Surface.BROWN, 1, 2),
    ARMORED_ENTRANCE("armored_entrance", Motion.SWING, Surface.DARK, 1, 2),
    FIRE_ENTRANCE("fire_entrance", Motion.SWING, Surface.RED, 1, 2),
    DOUBLE_ENTRANCE("double_entrance", Motion.DOUBLE_SWING, Surface.BROWN, 2, 2),
    GLASS_ENTRANCE("glass_entrance", Motion.SWING, Surface.GLASS, 1, 2),
    ALUMINUM_ENTRANCE("aluminum_entrance", Motion.SWING, Surface.STEEL, 1, 2),
    PVC_ENTRANCE("pvc_entrance", Motion.SWING, Surface.WHITE, 1, 2),
    SOUNDPROOF("soundproof_door", Motion.SWING, Surface.DARK, 1, 2),
    SCREEN("screen_door", Motion.SWING, Surface.SCREEN, 1, 2),
    STORM("storm_door", Motion.SWING, Surface.GLASS, 1, 2),
    DOUBLE_SWING("double_swing_door", Motion.DOUBLE_SWING, Surface.GLASS, 2, 2),
    SALOON("saloon_door", Motion.DOUBLE_SWING, Surface.WOOD, 2, 2),
    PIVOT("pivot_door", Motion.PIVOT, Surface.DARK, 2, 3),
    SLIDING("sliding_door", Motion.SLIDE, Surface.WOOD, 1, 2),
    POCKET("pocket_door", Motion.SLIDE, Surface.WHITE, 1, 2),
    GLASS_SLIDING("glass_sliding_door", Motion.SLIDE, Surface.GLASS, 2, 2),
    AUTOMATIC_SLIDING("automatic_sliding_door", Motion.DOUBLE_SLIDE, Surface.GLASS, 2, 3),
    DOUBLE_SLIDING("double_sliding_door", Motion.DOUBLE_SLIDE, Surface.GLASS, 2, 2),
    TELESCOPIC("telescopic_door", Motion.TELESCOPIC, Surface.GLASS, 3, 3),
    FOLDING("folding_door", Motion.FOLD, Surface.WHITE, 2, 2),
    BIFOLD("bifold_door", Motion.BIFOLD, Surface.WOOD, 2, 2),
    ACCORDION("accordion_door", Motion.FOLD, Surface.BROWN, 2, 2),
    REVOLVING("revolving_door", Motion.REVOLVE, Surface.GLASS, 3, 3),
    ROLLER_SHUTTER("roller_shutter", Motion.ROLL, Surface.STEEL, 3, 3),
    SECTIONAL_GARAGE("sectional_garage", Motion.SECTIONAL, Surface.WHITE, 3, 3),
    UP_AND_OVER("up_and_over_garage", Motion.OVERHEAD, Surface.STEEL, 3, 3),
    VERTICAL_LIFT("vertical_lift_door", Motion.LIFT, Surface.STEEL, 2, 3);

    public enum Motion { SWING, DOUBLE_SWING, PIVOT, SLIDE, DOUBLE_SLIDE, TELESCOPIC, FOLD, BIFOLD, REVOLVE, ROLL, SECTIONAL, OVERHEAD, LIFT }
    public enum Surface { WOOD, WHITE, STEEL, BROWN, DARK, RED, GLASS, SCREEN }
    public final String id;
    public final Motion motion;
    public final Surface surface;
    public final int width, height;
    DoorStyle(String id, Motion motion, Surface surface, int width, int height) {
        this.id = id; this.motion = motion; this.surface = surface; this.width = width; this.height = height;
    }
}
