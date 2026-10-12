package com.mchoe5699.manydoors.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Textured cuboids and bevelled, rounded extrusions; no per-frame dynamic texture allocation. */
public final class DetailMesh {
    public static void box(PoseStack p,MultiBufferSource b,ResourceLocation tex,double x0,double y0,double z0,double x1,double y1,double z1,float alpha,int light,int overlay){box(p,b,tex,x0,y0,z0,x1,y1,z1,alpha,light,overlay,0,1,0,1);}
    public static void box(PoseStack p,MultiBufferSource b,ResourceLocation tex,double x0,double y0,double z0,double x1,double y1,double z1,float alpha,int light,int overlay,float u0,float u1,float v0,float v1){
        VertexConsumer v=b.getBuffer(alpha<1?RenderType.entityTranslucent(tex):RenderType.entityCutoutNoCull(tex));
        double[][][] f={{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}},{{x1,y0,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}},{{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}},{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}},{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}},{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}}};
        float[][] ns={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
        for(int face=0;face<6;face++)for(int i=0;i<4;i++)vertex(p,v,f[face][i],i<2?u0:u1,i==0||i==3?v1:v0,ns[face],alpha,light,overlay);
    }
    private static void vertex(PoseStack p,VertexConsumer v,double[] xyz,float u,float w,float[] normal,float alpha,int light,int overlay){v.vertex(p.last().pose(),(float)xyz[0],(float)xyz[1],(float)xyz[2]).color(1f,1f,1f,alpha).uv(u,w).overlayCoords(overlay).uv2(light).normal(p.last().normal(),normal[0],normal[1],normal[2]).endVertex();}
    private static double[][] ring(double x0,double y0,double x1,double y1,double radius,double z){
        double[][] points=new double[32][3];double[] cx={x1-radius,x1-radius,x0+radius,x0+radius},cy={y0+radius,y1-radius,y1-radius,y0+radius};
        for(int corner=0;corner<4;corner++)for(int i=0;i<8;i++){double a=-Math.PI/2+corner*Math.PI/2+i*Math.PI/14;points[corner*8+i]=new double[]{cx[corner]+Math.cos(a)*radius,cy[corner]+Math.sin(a)*radius,z};}return points;
    }
    public static void rounded(PoseStack p,MultiBufferSource b,ResourceLocation tex,double x0,double y0,double x1,double y1,double z0,double z1,double radius,int light,int overlay){
        radius=Math.min(radius,Math.min(x1-x0,y1-y0)/2);double bevel=Math.min(.008,radius/3);
        double[][] back=ring(x0,y0,x1,y1,radius,z1),body=ring(x0,y0,x1,y1,radius,z0+bevel),front=ring(x0+bevel,y0+bevel,x1-bevel,y1-bevel,Math.max(.001,radius-bevel),z0);
        VertexConsumer v=b.getBuffer(RenderType.entityCutoutNoCull(tex));
        for(double[][][] pair:new double[][][][]{{back,body},{body,front}})for(int i=0;i<32;i++){
            int j=(i+1)%32;double[][] quad={pair[0][i],pair[0][j],pair[1][j],pair[1][i]};
            double nx=quad[1][1]-quad[0][1],ny=-(quad[1][0]-quad[0][0]);double length=Math.max(.00001,Math.sqrt(nx*nx+ny*ny));float[] n={(float)(nx/length),(float)(ny/length),0};
            for(int k=0;k<4;k++)vertex(p,v,quad[k],k<2?0:1,k==0||k==3?1:0,n,1,light,overlay);
        }
        double[] center={(x0+x1)/2,(y0+y1)/2,z0};
        for(int i=0;i<32;i++){int j=(i+1)%32;double[][] q={center,front[i],front[j],front[j]};for(double[] point:q)vertex(p,v,point,(float)((point[0]-x0)/(x1-x0)),(float)(1-(point[1]-y0)/(y1-y0)),new float[]{0,0,-1},1,light,overlay);}
    }
    public static void disk(PoseStack p,MultiBufferSource b,ResourceLocation tex,double x,double y,double z,double radius,int light,int overlay){rounded(p,b,tex,x-radius,y-radius,x+radius,y+radius,z,z+.004,radius,light,overlay);}
    private DetailMesh(){}
}
