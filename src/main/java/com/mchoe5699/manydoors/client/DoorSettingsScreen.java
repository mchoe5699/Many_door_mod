package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.door.DoorSettings;
import com.mchoe5699.manydoors.network.DoorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DoorSettingsScreen extends Screen {
    private final BlockPos pos;
    private final DoorSettings original;
    private EditBox widthBox,heightBox,thicknessBox,angleBox,closeBox;
    private DoorSettings.Mode mode;
    private boolean rightHinge;
    private int left,top,formWidth;
    private Component error=Component.empty();
    public DoorSettingsScreen(BlockPos pos,DoorSettings settings){super(Component.translatable("screen.manydoors.settings"));this.pos=pos;original=settings;mode=settings.mode();rightHinge=settings.rightHinge();}
    private EditBox field(int row,String value){EditBox b=new EditBox(font,left+formWidth-145,top+26+row*21,125,18,Component.empty());b.setMaxLength(10);b.setValue(value);addRenderableWidget(b);return b;}
    @Override protected void init(){
        formWidth=Math.min(340,width-12);left=(width-formWidth)/2;top=(height-236)/2;
        widthBox=field(0,widthBox==null?Integer.toString(original.width()):widthBox.getValue());heightBox=field(1,heightBox==null?Integer.toString(original.height()):heightBox.getValue());
        thicknessBox=field(2,thicknessBox==null?Double.toString(original.thickness()):thicknessBox.getValue());angleBox=field(3,angleBox==null?Double.toString(original.angle()):angleBox.getValue());closeBox=field(4,closeBox==null?Double.toString(original.closeTicks()/20.0):closeBox.getValue());
        addRenderableWidget(CycleButton.<DoorSettings.Mode>builder(m->Component.translatable("mode.manydoors."+m.name().toLowerCase(java.util.Locale.ROOT))).withValues(DoorSettings.Mode.values()).withInitialValue(mode).create(left+20,top+133,formWidth-40,18,Component.translatable("screen.manydoors.mode"),(button,value)->mode=value));
        addRenderableWidget(CycleButton.onOffBuilder(rightHinge).create(left+20,top+155,formWidth-40,18,Component.translatable("screen.manydoors.right_hinge"),(button,value)->rightHinge=value));
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.apply"),b->apply()).bounds(left+20,top+213,(formWidth-50)/2,18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(left+30+(formWidth-50)/2,top+213,(formWidth-50)/2,18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.design"),b->{if(minecraft.level!=null&&minecraft.level.getBlockEntity(pos) instanceof com.mchoe5699.manydoors.door.DoorBlockEntity d)minecraft.setScreen(new DesignScreen(this,d));}).bounds(left+20,top+177,formWidth-40,18).build());
        setInitialFocus(widthBox);
    }
    private void apply(){
        try{
            double seconds=Double.parseDouble(closeBox.getValue());
            if(!Double.isFinite(seconds)||seconds<0||seconds>60)throw new IllegalArgumentException();
            DoorSettings s=new DoorSettings(Integer.parseInt(widthBox.getValue()),Integer.parseInt(heightBox.getValue()),Double.parseDouble(thicknessBox.getValue()),Double.parseDouble(angleBox.getValue()),mode,(int)Math.round(seconds*20),rightHinge);
            if(!s.valid())throw new IllegalArgumentException();
            DoorNetwork.CHANNEL.sendToServer(new DoorNetwork.Apply(pos,s));onClose();
        }catch(IllegalArgumentException e){error=Component.translatable("message.manydoors.invalid_settings");}
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g);g.fill(left,top,left+formWidth,top+236,0xEE17212A);g.drawCenteredString(font,title,left+formWidth/2,top+8,0xFFFFFF);
        String[] fields={"width","height","thickness","angle","close"};
        for(int i=0;i<fields.length;i++)g.drawString(font,Component.translatable("screen.manydoors."+fields[i]),left+20,top+31+i*21,0xDDE5ED,false);
        g.drawString(font,error,left+20,top+200,0xFF8888,false);super.render(g,x,y,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
