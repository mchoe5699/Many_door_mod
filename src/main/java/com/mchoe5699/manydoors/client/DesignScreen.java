package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.door.*;
import com.mchoe5699.manydoors.network.DoorNetwork;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import javax.imageio.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DesignScreen extends Screen {
    private final Screen parent;private final BlockPos pos;private final UUID identity;
    private final int[] pixels;private EditBox hex;private int zoom,left,top,side,brush=1,lastX=-1,lastY=-1;
    private String error="";
    public DesignScreen(Screen parent,DoorBlockEntity door){super(Component.translatable("screen.manydoors.design"));this.parent=parent;pos=door.getBlockPos();identity=door.identity();int[] original=door.designPixels();pixels=original.length==DoorDesign.PIXELS?original:new int[DoorDesign.PIXELS];if(original.length==0)Arrays.fill(pixels,0xFFAC8055);}
    @Override protected void init(){
        zoom=Math.max(1,Math.min(3,Math.min((height-75)/DoorDesign.HEIGHT,(width-197)/DoorDesign.WIDTH)));int total=DoorDesign.WIDTH*zoom+185;left=(width-total)/2;top=Math.max(28,(height-Math.max(DoorDesign.HEIGHT*zoom,204)-32)/2+24);side=left+DoorDesign.WIDTH*zoom+12;
        hex=new EditBox(font,side,top+23,160,20,Component.translatable("screen.manydoors.color"));hex.setMaxLength(7);hex.setValue("#795438");addRenderableWidget(hex);
        addRenderableWidget(CycleButton.<Integer>builder(v->Component.literal(v+" px")).withValues(1,2,4,8).withInitialValue(brush).create(side,top+49,160,20,Component.translatable("screen.manydoors.brush"),(b,v)->brush=v));
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.import_photo"),b->choose()).bounds(side,top+75,160,20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.fill"),b->{try{Arrays.fill(pixels,color());error="";}catch(IllegalArgumentException e){error="Invalid color / 색상 오류";}}).bounds(side,top+101,160,20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.reset_design"),b->{send(new int[0]);onClose();}).bounds(side,top+127,160,20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.apply"),b->{send(pixels);onClose();}).bounds(side,top+153,160,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(side,top+179,160,20).build());
    }
    private void send(int[] data){DoorNetwork.CHANNEL.sendToServer(new DoorNetwork.DesignApply(pos,identity,data));}
    private int color(){String text=hex.getValue().replace("#","");if(!text.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException();return 0xFF000000|Integer.parseInt(text,16);}
    private boolean inside(double x,double y){return x>=left&&x<left+DoorDesign.WIDTH*zoom&&y>=top&&y<top+DoorDesign.HEIGHT*zoom;}
    private void paint(int x,int y,int button){int value=button==1?0:color();for(int yy=y-brush/2;yy<y+(brush+1)/2;yy++)for(int xx=x-brush/2;xx<x+(brush+1)/2;xx++)if(xx>=0&&xx<DoorDesign.WIDTH&&yy>=0&&yy<DoorDesign.HEIGHT)pixels[yy*DoorDesign.WIDTH+xx]=value;}
    private void stroke(double x,double y,int button){
        if(!inside(x,y)){lastX=-1;return;}
        int px=(int)((x-left)/zoom),py=(int)((y-top)/zoom);
        try{if(lastX<0)paint(px,py,button);else {int steps=Math.max(Math.abs(px-lastX),Math.abs(py-lastY));for(int i=0;i<=steps;i++){double p=steps==0?0:(double)i/steps;paint((int)Math.round(lastX+(px-lastX)*p),(int)Math.round(lastY+(py-lastY)*p),button);}}lastX=px;lastY=py;error="";}catch(IllegalArgumentException e){error="Invalid color / 색상 오류";}
    }
    @Override public boolean mouseClicked(double x,double y,int button){if((button==0||button==1)&&inside(x,y)){lastX=-1;stroke(x,y,button);return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0||button==1){stroke(x,y,button);if(inside(x,y))return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){lastX=-1;return super.mouseReleased(x,y,b);}
    private void choose(){
        try(MemoryStack stack=MemoryStack.stackPush()){
            var filters=stack.mallocPointer(3);filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).flip();
            String selected=TinyFileDialogs.tinyfd_openFileDialog("Door design PNG/JPEG", "", filters, "PNG / JPEG", false);
            if(selected!=null)importPhoto(Path.of(selected));
        }catch(Exception e){error="Drag PNG/JPEG here / 사진을 이 화면에 드래그하세요";}
    }
    public boolean importPhoto(Path path){
        try{
            if(Files.size(path)>16*1024*1024)throw new IllegalArgumentException("File limit: 16 MB");
            try(var input=ImageIO.createImageInputStream(path.toFile())){
                var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("PNG/JPEG only");var reader=readers.next();
                try{
                    reader.setInput(input,true,true);String format=reader.getFormatName();if(!format.equalsIgnoreCase("png")&&!format.equalsIgnoreCase("jpeg")&&!format.equalsIgnoreCase("jpg"))throw new IllegalArgumentException("PNG/JPEG only");
                    int w=reader.getWidth(0),h=reader.getHeight(0);if(w<=0||h<=0||w>16000||h>16000||(long)w*h>64_000_000L)throw new IllegalArgumentException("Image dimensions too large");
                    var params=reader.getDefaultReadParam();int sample=Math.max(1,(int)Math.ceil(Math.max(w,h)/1024.0));params.setSourceSubsampling(sample,sample,0,0);
                    BufferedImage decoded=reader.read(0,params),resized=new BufferedImage(DoorDesign.WIDTH,DoorDesign.HEIGHT,BufferedImage.TYPE_INT_ARGB);
                    var g=resized.createGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(decoded,0,0,DoorDesign.WIDTH,DoorDesign.HEIGHT,null);}finally{g.dispose();}
                    resized.getRGB(0,0,DoorDesign.WIDTH,DoorDesign.HEIGHT,pixels,0,DoorDesign.WIDTH);error="";return true;
                }finally{reader.dispose();}
            }
        }catch(Exception e){error="Import failed / PNG·JPEG, 16 MB 이내";return false;}
    }
    @Override public void onFilesDrop(List<Path> paths){if(!paths.isEmpty())importPhoto(paths.get(0));}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);g.fill(left-7,top-28,side+170,Math.max(top+207,top+DoorDesign.HEIGHT*zoom+8),0xEE17212A);g.drawCenteredString(font,title,(left+side+160)/2,top-20,0xFFFFFF);
        for(int y=0;y<DoorDesign.HEIGHT;y++)for(int x=0;x<DoorDesign.WIDTH;x++){int c=pixels[y*DoorDesign.WIDTH+x];if((c>>>24)==0)c=((x/4+y/4)%2==0)?0xFFDDDDDD:0xFF999999;g.fill(left+x*zoom,top+y*zoom,left+(x+1)*zoom,top+(y+1)*zoom,c);}
        g.drawString(font,Component.translatable("screen.manydoors.color"),side,top+9,0xEEEEEE,false);g.drawString(font,error,left,Math.min(height-11,Math.max(top+202,top+DoorDesign.HEIGHT*zoom+8)),0xFF8888,false);super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
