package com.mchoe5699.manydoors.door;

/** Fixed bounds keep chunk tags, uploads and dynamic textures predictable. ARGB pixels. */
public final class DoorDesign {
    public static final int WIDTH=64, HEIGHT=96, PIXELS=WIDTH*HEIGHT;
    public static boolean valid(int[] pixels){return pixels!=null&&(pixels.length==0||pixels.length==PIXELS);}
    private DoorDesign(){}
}
