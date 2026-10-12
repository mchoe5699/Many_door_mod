package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.control.*;
import com.mchoe5699.manydoors.door.DoorGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public final class ControlRenderer implements BlockEntityRenderer<ControlBlockEntity>{
    private static final ResourceLocation METAL=tex("steel"),BLACK=tex("black"),WHITE=tex("white"),BLUE=tex("blue"),RED=tex("red"),GREEN=tex("green");
    private static ResourceLocation tex(String name){return new ResourceLocation("manydoors","textures/block/"+name+".png");}
    public ControlRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(ControlBlockEntity be,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
        if(!(be.getBlockState().getBlock() instanceof ControlBlock block))return;
        if(block.kind==ControlBlock.Kind.PRESSURE_SENSOR){DetailMesh.box(p,b,METAL,.06,0,.06,.94,.065,.94,1,light,overlay);DetailMesh.box(p,b,BLACK,.12,.065,.12,.88,.072,.88,1,light,overlay);return;}
        var m=be.mount();p.pushPose();p.translate(.5,.5,.5);p.mulPose(Axis.YP.rotation((float)DoorGeometry.yaw(be.getBlockState().getValue(ControlBlock.FACING))));p.translate(m.x(),m.y(),.5-m.depth());p.scale((float)m.scale(),(float)m.scale(),(float)m.scale());p.translate(-.5,-.5,-1);
        boolean powered=be.getBlockState().getValue(ControlBlock.POWERED);
        if(block.kind==ControlBlock.Kind.MOTION_SENSOR){
            DetailMesh.rounded(p,b,METAL,.06,.44,.94,.64,.88,1,.075,light,overlay);
            DetailMesh.rounded(p,b,BLACK,.12,.478,.88,.603,.876,.89,.022,light,overlay);
            DetailMesh.box(p,b,WHITE,.14,.595,.874,.86,.598,.876,1,light,overlay);
            DetailMesh.disk(p,b,powered?GREEN:BLUE,.84,.493,.871,.007,light,overlay);
        }else if(block.kind==ControlBlock.Kind.BUTTON||block.kind==ControlBlock.Kind.EMERGENCY_BUTTON){
            DetailMesh.rounded(p,b,METAL,.4,.08,.6,.92,.91,1,.034,light,overlay);
            DetailMesh.rounded(p,b,BLACK,.427,.615,.573,.902,.904,.918,.006,light,overlay);
            DetailMesh.rounded(p,b,WHITE,.429,.098,.571,.611,.907,.919,.008,light,overlay);
            DetailMesh.disk(p,b,WHITE,.5,.798,.899,.044,light,overlay);DetailMesh.disk(p,b,BLACK,.5,.798,.893,.039,light,overlay);
            DetailMesh.disk(p,b,METAL,.5,.798,.889,.031,light,overlay);DetailMesh.disk(p,b,block.kind==ControlBlock.Kind.EMERGENCY_BUTTON?RED:BLUE,.5,.798,.884,.025,light,overlay);
            for(double y:new double[]{.581,.561,.541})DetailMesh.rounded(p,b,METAL,.446,y,.554,y+.006,.9,.906,.003,light,overlay);
            text(p,b,"PUSH",.5,.701,.896,.0035f,0xFFFFFF,light);
            String[] words={"눌","러","주","세","요"};for(int i=0;i<words.length;i++)text(p,b,words[i],.5,.51-i*.077,.895,.0078f,0x111111,light);
            text(p,b,"AUTO",.5,.137,.895,.0038f,0x111111,light);
        }else{
            DetailMesh.rounded(p,b,METAL,.4,.3,.6,.7,.9,1,.035,light,overlay);
            DetailMesh.rounded(p,b,BLACK,.427,.327,.573,.673,.895,.905,.025,light,overlay);
            DetailMesh.disk(p,b,powered?GREEN:RED,.5,.52,.89,.045,light,overlay);
        }
        p.popPose();
    }
    private void text(PoseStack p,MultiBufferSource b,String text,double x,double y,double z,float scale,int color,int light){p.pushPose();p.translate(x,y,z);p.scale(-scale,-scale,scale);var f=Minecraft.getInstance().font;var label=net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.ChatFormatting.BOLD);f.drawInBatch(label.getVisualOrderText(),-f.width(label)/2f,0,color,false,p.last().pose(),b,Font.DisplayMode.NORMAL,0,light);p.popPose();}
}
