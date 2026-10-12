package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.door.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class DesignTextures {
    private record Entry(int hash,ResourceLocation location){}
    private static final Map<UUID,Entry> CACHE=new LinkedHashMap<>(16,.75f,true);
    public static ResourceLocation get(DoorBlockEntity door,ResourceLocation fallback){
        if(!door.hasDesign())return fallback;
        Entry entry=CACHE.get(door.identity());if(entry!=null&&entry.hash==door.designHash())return entry.location;
        if(entry!=null)Minecraft.getInstance().getTextureManager().release(entry.location);
        int[] pixels=door.designPixels();NativeImage image=new NativeImage(DoorDesign.WIDTH,DoorDesign.HEIGHT,false);
        for(int y=0;y<DoorDesign.HEIGHT;y++)for(int x=0;x<DoorDesign.WIDTH;x++){int argb=pixels[y*DoorDesign.WIDTH+x];image.setPixelRGBA(x,y,(argb&0xFF00FF00)|((argb>>>16)&255)|((argb&255)<<16));}
        ResourceLocation location=new ResourceLocation("manydoors","custom/"+door.identity());Minecraft.getInstance().getTextureManager().register(location,new DynamicTexture(image));CACHE.put(door.identity(),new Entry(door.designHash(),location));
        while(CACHE.size()>128){UUID oldest=CACHE.keySet().iterator().next();Minecraft.getInstance().getTextureManager().release(CACHE.remove(oldest).location);}
        return location;
    }
    private DesignTextures(){}
}
