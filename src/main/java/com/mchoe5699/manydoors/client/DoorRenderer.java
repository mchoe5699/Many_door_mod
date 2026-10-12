package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.door.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity>{
    private static final ResourceLocation METAL=tex("steel"),BLACK=tex("black");
    private static ResourceLocation tex(String name){return new ResourceLocation("manydoors","textures/block/"+name+".png");}
    public DoorRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(DoorBlockEntity d,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(d.isRemoved())return;
        double w=d.settings().width(),h=d.settings().height(),t=d.settings().thickness();
        poses.pushPose();poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotation((float)DoorGeometry.yaw(d.facing())));poses.translate(-.5,0,-.5);
        boolean lobby=d.style()==DoorStyle.LOBBY_AUTOMATIC;
        if(lobby){
            DetailMesh.box(poses,buffers,METAL,0,0,0,1,h,1,1,light,overlay);
            DetailMesh.rounded(poses,buffers,BLACK,.1,.12,.9,h-.12,-.004,.004,.05,light,overlay);
            for(double x:new double[]{.07,.92})DetailMesh.box(poses,buffers,METAL,x,.08,-.008,x+.012,h-.08,.002,1,light,overlay);
        }
        double start=lobby?1:0;
        DetailMesh.box(poses,buffers,METAL,start-.045,0,.5-t/2-.03,start+.035,h+.08,.5+t/2+.03,1,light,overlay);
        DetailMesh.box(poses,buffers,METAL,w-.035,0,.5-t/2-.03,w+.045,h+.08,.5+t/2+.03,1,light,overlay);
        DetailMesh.box(poses,buffers,METAL,start-.045,h-.01,.5-t/2-.03,w+.045,h+.08,.5+t/2+.03,1,light,overlay);
        if(d.style().motion==DoorStyle.Motion.SLIDE||d.style().motion==DoorStyle.Motion.DOUBLE_SLIDE||d.style().motion==DoorStyle.Motion.TELESCOPIC){
            DetailMesh.box(poses,buffers,METAL,start,h,.32,w,h+.2,.7,1,light,overlay);DetailMesh.box(poses,buffers,BLACK,start+.06,h+.035,.316,w-.06,h+.045,.32,1,light,overlay);
        }
        var panels=DoorGeometry.panels(d.style(),d.settings(),d.progress(partial));var closed=DoorGeometry.panels(d.style(),d.settings(),0);
        ResourceLocation base=tex(d.style().id),design=DesignTextures.get(d,base);
        for(int i=0;i<panels.size();i++){
            var p=panels.get(i);poses.pushPose();poses.translate(p.x(),p.y(),p.z());poses.mulPose(Axis.YP.rotation((float)p.yaw()));poses.mulPose(Axis.XP.rotation((float)p.pitch()));
            double x0=Math.min(0,p.width()),x1=Math.max(0,p.width()),y0=Math.min(0,p.height()),y1=Math.max(0,p.height());
            boolean glass=p.surface()==DoorStyle.Surface.GLASS||p.surface()==DoorStyle.Surface.SCREEN,custom=d.hasDesign()&&p.surface()==d.style().surface&&Math.abs(p.height())>.25;
            double trim=Math.min(.045,Math.min(Math.abs(p.width()),Math.abs(p.height()))/4);
            ResourceLocation surface=custom?design:p.surface()==d.style().surface?base:tex(p.surface().name().toLowerCase(java.util.Locale.ROOT));
            if(custom){
                var original=closed.get(Math.min(i,closed.size()-1));float u0=(float)Math.max(0,Math.min(1,(original.x()+x0)/w)),u1=(float)Math.max(0,Math.min(1,(original.x()+x1)/w));
                DetailMesh.box(poses,buffers,surface,x0,y0,-p.depth()/2,x1,y1,p.depth()/2,1,light,overlay,u0,u1,0,1);
            }else DetailMesh.box(poses,buffers,surface,x0+(glass?trim:0),y0+(glass?trim:0),-p.depth()*(glass?.12:.5),x1-(glass?trim:0),y1-(glass?trim:0),p.depth()*(glass?.12:.5),glass?.3f:1,light,overlay);
            if(glass){
                for(ResourceLocation layer:new ResourceLocation[]{BLACK,METAL}){
                    double inset=layer==BLACK?trim:.0,band=layer==BLACK?.015:trim;
                    DetailMesh.box(poses,buffers,layer,x0+inset,y0,-p.depth()*.55,x0+inset+band,y1,p.depth()*.55,1,light,overlay);
                    DetailMesh.box(poses,buffers,layer,x1-inset-band,y0,-p.depth()*.55,x1-inset,y1,p.depth()*.55,1,light,overlay);
                    DetailMesh.box(poses,buffers,layer,x0,y0+inset,-p.depth()*.55,x1,y0+inset+band,p.depth()*.55,1,light,overlay);
                    DetailMesh.box(poses,buffers,layer,x0,y1-inset-band,-p.depth()*.55,x1,y1-inset,p.depth()*.55,1,light,overlay);
                }
            }else if(!custom&&(d.style()==DoorStyle.PANEL||d.style()==DoorStyle.WOODEN||d.style()==DoorStyle.TRADITIONAL||d.style()==DoorStyle.APARTMENT_ENTRANCE)){
                for(int section=0;section<2;section++){
                    double bottom=y0+(y1-y0)*(.08+.48*section),top=bottom+(y1-y0)*.36;
                    DetailMesh.rounded(poses,buffers,surface,x0+(x1-x0)*.12,bottom,x1-(x1-x0)*.12,top,-p.depth()/2-.012,-p.depth()/2,.018,light,overlay);
                    DetailMesh.box(poses,buffers,surface,x0+(x1-x0)*.15,bottom+.04,p.depth()/2,x1-(x1-x0)*.15,top-.04,p.depth()/2+.012,1,light,overlay);
                }
            }
            boolean hinged=d.style().motion==DoorStyle.Motion.SWING||d.style().motion==DoorStyle.Motion.DOUBLE_SWING||d.style().motion==DoorStyle.Motion.PIVOT;
            if(hinged){
                double hx=x0+(x1-x0)*.84,hy=y0+(y1-y0)*.47;
                for(double face:new double[]{-1,1}){
                    poses.pushPose();if(face==1)poses.mulPose(Axis.YP.rotation((float)Math.PI));double xx=face==1?-hx:hx;
                    DetailMesh.disk(poses,buffers,METAL,xx,hy,-p.depth()/2-.012,.035,light,overlay);
                    DetailMesh.rounded(poses,buffers,METAL,xx-.015,hy-.017,xx+.13,hy+.017,-p.depth()/2-.055,-p.depth()/2-.036,.016,light,overlay);
                    poses.popPose();
                }
                double hinge=p.width()<0?x1:x0;
                for(double y:new double[]{y0+(y1-y0)*.18,y0+(y1-y0)*.5,y0+(y1-y0)*.82})DetailMesh.box(poses,buffers,METAL,hinge-.018,y-.045,-p.depth()/2-.008,hinge+.018,y+.045,p.depth()/2+.008,1,light,overlay);
                if(d.style()==DoorStyle.APARTMENT_ENTRANCE||d.style()==DoorStyle.ARMORED_ENTRANCE){DetailMesh.disk(poses,buffers,METAL,(x0+x1)/2,y0+(y1-y0)*.73,-p.depth()/2-.008,.017,light,overlay);DetailMesh.disk(poses,buffers,BLACK,(x0+x1)/2,y0+(y1-y0)*.73,-p.depth()/2-.012,.009,light,overlay);}
            }
            poses.popPose();
        }
        poses.popPose();
    }
    @Override public boolean shouldRenderOffScreen(DoorBlockEntity d){return true;}
    @Override public int getViewDistance(){return 96;}
}
