package com.mchoe5699.manydoors.control;

import com.mchoe5699.manydoors.door.DoorGeometry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public record MountSettings(double x,double y,double depth,double scale,int range){
    public static MountSettings defaults(){return new MountSettings(0,0,0,1,3);}
    public boolean valid(){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(depth)&&Double.isFinite(scale)&&Math.abs(x)<=.45&&Math.abs(y)<=.45&&depth>=0&&depth<=.3&&scale>=.4&&scale<=1.4&&range>=1&&range<=8;}
    public CompoundTag save(){CompoundTag t=new CompoundTag();t.putDouble("X",x);t.putDouble("Y",y);t.putDouble("Depth",depth);t.putDouble("Scale",scale);t.putInt("Range",range);return t;}
    public static MountSettings load(CompoundTag t){var s=new MountSettings(t.getDouble("X"),t.getDouble("Y"),t.getDouble("Depth"),t.getDouble("Scale"),t.getInt("Range"));return s.valid()?s:defaults();}
    public AABB bounds(Direction facing,ControlBlock.Kind kind){
        if(kind==ControlBlock.Kind.PRESSURE_SENSOR)return new AABB(.0625,0,.0625,.9375,.08,.9375);
        AABB b=kind==ControlBlock.Kind.MOTION_SENSOR?new AABB(.06,.44,.88,.94,.64,1):new AABB(.4,.08,.89,.6,.92,1);
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX,a=DoorGeometry.yaw(facing);
        for(double vx:new double[]{b.minX,b.maxX})for(double vy:new double[]{b.minY,b.maxY})for(double vz:new double[]{b.minZ,b.maxZ}){
            double xx=(vx-.5)*scale+x,yy=(vy-.5)*scale+.5+y,zz=(vz-1)*scale+.5-depth;
            double wx=.5+xx*Math.cos(a)+zz*Math.sin(a),wz=.5-xx*Math.sin(a)+zz*Math.cos(a);
            minX=Math.min(minX,wx);minY=Math.min(minY,yy);minZ=Math.min(minZ,wz);maxX=Math.max(maxX,wx);maxY=Math.max(maxY,yy);maxZ=Math.max(maxZ,wz);
        }return new AABB(minX,minY,minZ,maxX,maxY,maxZ);
    }
}
