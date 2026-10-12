package com.mchoe5699.manydoors.client;
import com.mchoe5699.manydoors.door.DoorColliderEntity;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class DoorColliderRenderer extends EntityRenderer<DoorColliderEntity>{
 public DoorColliderRenderer(EntityRendererProvider.Context c){super(c);}
 @Override public ResourceLocation getTextureLocation(DoorColliderEntity e){return new ResourceLocation("manydoors","textures/block/steel.png");}
 @Override public boolean shouldRender(DoorColliderEntity e,net.minecraft.client.renderer.culling.Frustum f,double x,double y,double z){return false;}
}
