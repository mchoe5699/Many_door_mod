package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.door.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    private static final ResourceLocation METAL=texture(DoorStyle.Surface.STEEL);
    public DoorRenderer(BlockEntityRendererProvider.Context c){}
    private static ResourceLocation texture(DoorStyle.Surface s){return new ResourceLocation(ManyDoors.MOD_ID,"textures/block/"+s.name().toLowerCase(java.util.Locale.ROOT)+".png");}
    @Override public void render(DoorBlockEntity door,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotation((float)DoorGeometry.yaw(door.facing())));poses.translate(-.5,0,-.5);
        for(DoorGeometry.Panel p:DoorGeometry.panels(door.style(),door.settings(),door.progress(partial))){
            poses.pushPose();poses.translate(p.x(),p.y(),p.z());poses.mulPose(Axis.YP.rotation((float)p.yaw()));poses.mulPose(Axis.XP.rotation((float)p.pitch()));
            double x0=Math.min(0,p.width()),x1=Math.max(0,p.width()),y0=Math.min(0,p.height()),y1=Math.max(0,p.height());
            boolean glass=p.surface()==DoorStyle.Surface.GLASS||p.surface()==DoorStyle.Surface.SCREEN;
            cube(poses,buffers,p.surface()==door.style().surface?new ResourceLocation(ManyDoors.MOD_ID,"textures/block/"+door.style().id+".png"):texture(p.surface()),x0,y0,-p.depth()/2,x1,y1,p.depth()/2,glass?.42f:1,light,overlay);
            double trim=Math.min(.055,Math.min(Math.abs(p.width()),Math.abs(p.height()))/4);
            if(glass){
                cube(poses,buffers,METAL,x0,y0,-p.depth()*.6,x0+trim,y1,p.depth()*.6,1,light,overlay);
                cube(poses,buffers,METAL,x1-trim,y0,-p.depth()*.6,x1,y1,p.depth()*.6,1,light,overlay);
                cube(poses,buffers,METAL,x0,y0,-p.depth()*.6,x1,y0+trim,p.depth()*.6,1,light,overlay);
                cube(poses,buffers,METAL,x0,y1-trim,-p.depth()*.6,x1,y1,p.depth()*.6,1,light,overlay);
            }
            if(door.style().motion==DoorStyle.Motion.SWING||door.style().motion==DoorStyle.Motion.DOUBLE_SWING||door.style().motion==DoorStyle.Motion.PIVOT){
                double hx=x0+(x1-x0)*.85,hy=y0+(y1-y0)*.48;
                cube(poses,buffers,METAL,hx-.08,hy-.025,p.depth()/2,hx+.08,hy+.025,p.depth()/2+.06,1,light,overlay);
                cube(poses,buffers,METAL,hx-.08,hy-.025,-p.depth()/2-.06,hx+.08,hy+.025,-p.depth()/2,1,light,overlay);
            }
            poses.popPose();
        }
        poses.popPose();
    }
    private static void cube(PoseStack p,MultiBufferSource b,ResourceLocation tex,double x0,double y0,double z0,double x1,double y1,double z1,float alpha,int light,int overlay){
        VertexConsumer v=b.getBuffer(alpha<1?RenderType.entityTranslucent(tex):RenderType.entityCutoutNoCull(tex));
        float[][][] faces={
            {{(float)x0,(float)y0,(float)z0},{(float)x0,(float)y1,(float)z0},{(float)x1,(float)y1,(float)z0},{(float)x1,(float)y0,(float)z0}},
            {{(float)x1,(float)y0,(float)z1},{(float)x1,(float)y1,(float)z1},{(float)x0,(float)y1,(float)z1},{(float)x0,(float)y0,(float)z1}},
            {{(float)x0,(float)y0,(float)z1},{(float)x0,(float)y1,(float)z1},{(float)x0,(float)y1,(float)z0},{(float)x0,(float)y0,(float)z0}},
            {{(float)x1,(float)y0,(float)z0},{(float)x1,(float)y1,(float)z0},{(float)x1,(float)y1,(float)z1},{(float)x1,(float)y0,(float)z1}},
            {{(float)x0,(float)y1,(float)z0},{(float)x0,(float)y1,(float)z1},{(float)x1,(float)y1,(float)z1},{(float)x1,(float)y1,(float)z0}},
            {{(float)x0,(float)y0,(float)z1},{(float)x0,(float)y0,(float)z0},{(float)x1,(float)y0,(float)z0},{(float)x1,(float)y0,(float)z1}}
        };
        float[][] normals={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        Matrix4f matrix=p.last().pose();Matrix3f normal=p.last().normal();
        for(int face=0;face<6;face++)for(int i=0;i<4;i++){
            float[] xyz=faces[face][i],n=normals[face];
            v.vertex(matrix,xyz[0],xyz[1],xyz[2]).color(1,1,1,alpha).uv(i<2?0:1,i==0||i==3?1:0).overlayCoords(overlay).uv2(light).normal(normal,n[0],n[1],n[2]).endVertex();
        }
    }
    @Override public boolean shouldRenderOffScreen(DoorBlockEntity d){return true;}
    @Override public int getViewDistance(){return 128;}
}
