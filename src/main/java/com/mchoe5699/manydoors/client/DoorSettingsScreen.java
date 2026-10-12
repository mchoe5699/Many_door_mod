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
    private int left,top;
    private Component error=Component.empty();
    public DoorSettingsScreen(BlockPos pos,DoorSettings settings){super(Component.translatable("screen.manydoors.settings"));this.pos=pos;original=settings;mode=settings.mode();rightHinge=settings.rightHinge();}
    private EditBox field(int row,String value){EditBox b=new EditBox(font,left+190,top+36+row*25,125,20,Component.empty());b.setMaxLength(10);b.setValue(value);addRenderableWidget(b);return b;}
    @Override protected void init(){
        left=(width-340)/2;top=(height-270)/2;
        widthBox=field(0,Integer.toString(original.width()));heightBox=field(1,Integer.toString(original.height()));
        thicknessBox=field(2,Double.toString(original.thickness()));angleBox=field(3,Double.toString(original.angle()));closeBox=field(4,Double.toString(original.closeTicks()/20.0));
        addRenderableWidget(CycleButton.<DoorSettings.Mode>builder(m->Component.translatable("mode.manydoors."+m.name().toLowerCase(java.util.Locale.ROOT))).withValues(DoorSettings.Mode.values()).withInitialValue(mode).create(left+20,top+167,295,20,Component.translatable("screen.manydoors.mode"),(button,value)->mode=value));
        addRenderableWidget(CycleButton.onOffBuilder(rightHinge).create(left+20,top+191,295,20,Component.translatable("screen.manydoors.right_hinge"),(button,value)->rightHinge=value));
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.apply"),b->apply()).bounds(left+20,top+237,143,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(left+172,top+237,143,20).build());
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
        renderBackground(g);g.fill(left,top,left+340,top+270,0xEE17212A);g.drawCenteredString(font,title,left+170,top+13,0xFFFFFF);
        String[] fields={"width","height","thickness","angle","close"};
        for(int i=0;i<fields.length;i++)g.drawString(font,Component.translatable("screen.manydoors."+fields[i]),left+20,top+42+i*25,0xDDE5ED,false);
        g.drawString(font,error,left+20,top+218,0xFF8888,false);super.render(g,x,y,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
