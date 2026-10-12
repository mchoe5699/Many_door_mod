package com.mchoe5699.manydoors.door;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.AABB;

/** Cuboids in anchor-local coordinates. Rendering and collision share this geometry. */
public final class DoorGeometry {
    public record Panel(double x, double y, double z, double width, double height, double depth,
                        double yaw, double pitch, DoorStyle.Surface surface) {}
    private static void leaf(List<Panel> out, double pivotX, double pivotY, double pivotZ,
                             double width, double height, double thickness, double yaw, double pitch, DoorStyle.Surface surface) {
        out.add(new Panel(pivotX, pivotY, pivotZ, width, height, thickness, yaw, pitch, surface));
    }
    public static List<Panel> panels(DoorStyle style, DoorSettings s, double p) {
        List<Panel> out = new ArrayList<>();
        double w=s.width(), h=s.height(), t=s.thickness(), a=Math.toRadians(style.motion==DoorStyle.Motion.REVOLVE?90:s.angle())*p;
        var mat=style.surface;
        switch (style.motion) {
            case SWING -> leaf(out, s.rightHinge()?w:0, 0, .5, s.rightHinge()?-w:w, h, t, s.rightHinge()?-a:a, 0, mat);
            case DOUBLE_SWING -> {
                double bottom=style==DoorStyle.SALOON?h*.25:0, leafHeight=style==DoorStyle.SALOON?h*.5:h;
                leaf(out,0,bottom,.5,w/2,leafHeight,t,a,0,mat); leaf(out,w,bottom,.5,-w/2,leafHeight,t,-a,0,mat);
            }
            case PIVOT -> leaf(out,w/2,0,.5,-w/2,h,t,a,0,mat);
            case SLIDE -> leaf(out,p*(w+.05),0,.5,w,h,t,0,0,mat);
            case DOUBLE_SLIDE -> {
                double start=style==DoorStyle.LOBBY_AUTOMATIC?1:0, opening=w-start;
                leaf(out,start-p*(opening/2+.05),0,.5,opening/2,h,t,0,0,mat);
                leaf(out,start+opening/2+p*(opening/2+.05),0,.5,opening/2,h,t,0,0,mat);
            }
            case TELESCOPIC -> {
                for(int i=0;i<3;i++) leaf(out,i*w/3 + p*(w-i*w/3),0,.5+i*t*1.1,w/3,h,t,0,0,mat);
            }
            case FOLD, BIFOLD -> {
                int count=style.motion==DoorStyle.Motion.BIFOLD?2:6;
                double segment=w/count, angle=Math.toRadians(85)*p;
                double x=0,z=.5;
                for(int i=0;i<count;i++) {
                    double yaw=(i%2==0?angle:-angle);
                    leaf(out,x,0,z,segment,h,t,yaw,0,mat);
                    x+=Math.cos(yaw)*segment; z-=Math.sin(yaw)*segment;
                }
            }
            case REVOLVE -> {
                for(int i=0;i<4;i++) leaf(out,w/2,0,.5,w/2,h,t,i*Math.PI/2+a,0,mat);
            }
            case ROLL -> {
                if(p<.999) leaf(out,0,h*p,.5,w,h*(1-p),t,0,0,mat);
                leaf(out,0,h,.5,w,.2,.3,0,0,DoorStyle.Surface.STEEL);
            }
            case SECTIONAL -> {
                double segment=h/6;
                for(int i=0;i<6;i++) {
                    double y=i*segment+p*h;
                    if(y+segment<=h) leaf(out,0,y,.5,w,segment,t,0,0,mat);
                    else {
                        double turn=Math.min(1,(y+segment-h)/segment);
                        leaf(out,0,Math.min(h,y),.5-Math.max(0,y-h),w,segment,t,0,-turn*Math.PI/2,mat);
                    }
                }
            }
            case OVERHEAD -> leaf(out,0,h,.5,w,-h,t,0,-p*Math.PI/2,mat);
            case LIFT -> leaf(out,0,h*p,.5,w,h,t,0,0,mat);
        }
        if(style.motion==DoorStyle.Motion.PIVOT) leaf(out,w/2,0,.5,w/2,h,t,a,0,mat);
        return out;
    }
    public static double yaw(Direction facing) {
        return switch(facing) { case EAST -> -Math.PI/2; case SOUTH -> Math.PI; case WEST -> Math.PI/2; default -> 0; };
    }
    private static double[] vertex(Panel p,double x,double y,double z,Direction facing) {
        double yp=y*Math.cos(p.pitch)-z*Math.sin(p.pitch), zp=y*Math.sin(p.pitch)+z*Math.cos(p.pitch);
        double xp=x*Math.cos(p.yaw)+zp*Math.sin(p.yaw), zz=-x*Math.sin(p.yaw)+zp*Math.cos(p.yaw);
        double a=yaw(facing), lx=p.x+xp-.5, lz=p.z+zz-.5;
        return new double[]{.5+lx*Math.cos(a)+lz*Math.sin(a),p.y+yp,.5-lx*Math.sin(a)+lz*Math.cos(a)};
    }
    public static List<AABB> boxes(DoorStyle style, DoorSettings settings,double progress,Direction facing) {
        List<AABB> out=new ArrayList<>();
        for(Panel p:panels(style,settings,progress)) {
            int slices=Math.max(1,(int)Math.ceil(Math.abs(p.width)*8));
            for(int i=0;i<slices;i++) {
                double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
                for(double x:new double[]{p.width*i/slices,p.width*(i+1)/slices})
                    for(double y:new double[]{0,p.height}) for(double z:new double[]{-p.depth/2,p.depth/2}) {
                        double[] v=vertex(p,x,y,z,facing);
                        minX=Math.min(minX,v[0]);minY=Math.min(minY,v[1]);minZ=Math.min(minZ,v[2]);
                        maxX=Math.max(maxX,v[0]);maxY=Math.max(maxY,v[1]);maxZ=Math.max(maxZ,v[2]);
                    }
                out.add(new AABB(minX,minY,minZ,maxX,maxY,maxZ));
            }
        }
        return out;
    }
    public static Set<BlockPos> footprint(BlockPos anchor, Direction facing, DoorStyle style, DoorSettings s) {
        Set<BlockPos> result=new HashSet<>();
        for(int step=0;step<=36;step++) for(AABB b:boxes(style,s,step/36.0*(style.motion==DoorStyle.Motion.REVOLVE?4:1),facing)) {
            for(int x=(int)Math.floor(b.minX);x<Math.ceil(b.maxX);x++)
                for(int y=(int)Math.floor(b.minY);y<Math.ceil(b.maxY);y++)
                    for(int z=(int)Math.floor(b.minZ);z<Math.ceil(b.maxZ);z++) result.add(anchor.offset(x,y,z));
        }
        result.add(anchor.immutable()); return result;
    }
    private DoorGeometry() {}
}
