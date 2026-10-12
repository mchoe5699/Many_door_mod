package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.control.MountSettings;
import com.mchoe5699.manydoors.network.DoorNetwork;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DeviceSettingsScreen extends Screen {
    private final BlockPos pos;private final MountSettings mount;
    private final EditBox[] inputs=new EditBox[5];private int left,top,formWidth;private Component error=Component.empty();
    public DeviceSettingsScreen(BlockPos p,MountSettings s){super(Component.translatable("screen.manydoors.device"));pos=p;mount=s;}
    @Override protected void init(){
        formWidth=Math.min(330,width-12);left=(width-formWidth)/2;top=(height-225)/2;
        String[] values={""+mount.x(),""+mount.y(),""+mount.depth(),""+mount.scale(),""+mount.range()};
        for(int i=0;i<5;i++){inputs[i]=new EditBox(font,left+formWidth-145,top+36+i*25,125,20,Component.empty());inputs[i].setValue(values[i]);inputs[i].setMaxLength(10);addRenderableWidget(inputs[i]);}
        addRenderableWidget(Button.builder(Component.translatable("screen.manydoors.apply"),b->apply()).bounds(left+20,top+191,(formWidth-50)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),b->onClose()).bounds(left+30+(formWidth-50)/2,top+191,(formWidth-50)/2,20).build());
    }
    private void apply(){try{
        var s=new MountSettings(Double.parseDouble(inputs[0].getValue()),Double.parseDouble(inputs[1].getValue()),Double.parseDouble(inputs[2].getValue()),Double.parseDouble(inputs[3].getValue()),Integer.parseInt(inputs[4].getValue()));
        if(!s.valid())throw new IllegalArgumentException();DoorNetwork.CHANNEL.sendToServer(new DoorNetwork.DeviceApply(pos,s));onClose();
    }catch(IllegalArgumentException e){error=Component.translatable("message.manydoors.invalid_settings");}}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);g.fill(left,top,left+formWidth,top+225,0xEE17212A);g.drawCenteredString(font,title,left+formWidth/2,top+12,0xFFFFFF);
        String[] fields={"offset_x","offset_y","offset_depth","device_scale","sensor_range"};for(int i=0;i<5;i++)g.drawString(font,Component.translatable("screen.manydoors."+fields[i]),left+20,top+42+i*25,0xEEEEEE,false);
        g.drawString(font,error,left+20,top+170,0xFF8888,false);super.render(g,x,y,partial);}
    @Override public boolean isPauseScreen(){return false;}
}
