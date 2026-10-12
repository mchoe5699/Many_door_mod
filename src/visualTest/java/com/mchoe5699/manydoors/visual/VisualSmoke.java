package com.mchoe5699.manydoors.visual;

import com.mchoe5699.manydoors.*;
import com.mchoe5699.manydoors.client.*;
import com.mchoe5699.manydoors.control.*;
import com.mchoe5699.manydoors.door.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Opt-in development-only client smoke test, excluded from the release JAR. */
@Mod.EventBusSubscriber(modid="manydoors",value=Dist.CLIENT)
public final class VisualSmoke {
    private static final Logger LOG=LogUtils.getLogger();
    private static final AtomicBoolean ready=new AtomicBoolean();
    private static int ticks,stage;
    private static boolean scheduled;
    private static DoorSettingsScreen settingsScreen;
    private static final BlockPos DOOR=new BlockPos(0,65,2),BUTTON=new BlockPos(4,66,2),SENSOR=new BlockPos(4,68,2),LOBBY=new BlockPos(7,65,2);
    private static void screenshot(String name){var mc=Minecraft.getInstance();new File(mc.gameDirectory,"visual-test/screenshots").mkdirs();Screenshot.grab(new File(mc.gameDirectory,"visual-test"),name+".png",mc.getMainRenderTarget(),c->LOG.info("VISUAL_SCREENSHOT {} {}",name,c.getString()));}
    private static void camera(double x,double y,double z,float pitch){var mc=Minecraft.getInstance();mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.getAbilities().flying=true;p.onUpdateAbilities();p.setNoGravity(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.teleportTo(p.serverLevel(),x,y,z,0,pitch);});}
    private static void scene(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();ServerLevel l=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
        for(int x=-4;x<=15;x++)for(int z=-6;z<=7;z++){l.getChunkAt(new BlockPos(x,64,z));l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);}
        for(int x=3;x<=5;x++)for(int y=65;y<=69;y++)l.setBlock(new BlockPos(x,y,3),Blocks.POLISHED_DIORITE.defaultBlockState(),3);
        BlockPos[] roots={DOOR,LOBBY,new BlockPos(11,65,2)};DoorStyle[] styles={DoorStyle.APARTMENT_ENTRANCE,DoorStyle.LOBBY_AUTOMATIC,DoorStyle.GLASS_ENTRANCE};
        for(int i=0;i<roots.length;i++){
            l.setBlock(roots[i],ManyDoors.DOORS.get(styles[i]).get().defaultBlockState(),3);var d=(DoorBlockEntity)l.getBlockEntity(roots[i]);d.setOwner(player.getUUID());var defaults=DoorSettings.defaults(styles[i]);d.configure(new DoorSettings(defaults.width(),defaults.height(),defaults.thickness(),defaults.angle(),defaults.mode(),styles[i]==DoorStyle.LOBBY_AUTOMATIC?0:defaults.closeTicks(),defaults.rightHinge()),player);d.setOpen(false);d.applyDesign(new int[0],player);
        }
        l.setBlock(BUTTON,ManyDoors.CONTROLS.get(ControlBlock.Kind.BUTTON).get().defaultBlockState(),3);l.setBlock(SENSOR,ManyDoors.CONTROLS.get(ControlBlock.Kind.MOTION_SENSOR).get().defaultBlockState(),3);
        ((ControlBlockEntity)l.getBlockEntity(BUTTON)).setOwner(player.getUUID());((ControlBlockEntity)l.getBlockEntity(SENSOR)).setOwner(player.getUUID());
        ((ControlBlockEntity)l.getBlockEntity(BUTTON)).clear();((ControlBlockEntity)l.getBlockEntity(BUTTON)).toggle(LinkTarget.of((DoorBlockEntity)l.getBlockEntity(LOBBY)));
        var phone=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("qwertys_homenet","lobby_phone"));if(phone!=null)l.setBlock(LOBBY.above().north(),phone.defaultBlockState(),3);
        l.setDayTime(6000);player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.setNoGravity(true);player.teleportTo(l,4,66,-6,0,0);ready.set(true);LOG.info("VISUAL_SCENE_READY");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("manydoors.visualTest"))return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        if(!mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("manydoors_visual_test"))return;
        if(!scheduled){scheduled=true;mc.getSingleplayerServer().execute(VisualSmoke::scene);return;}
        if(!ready.get())return;mc.options.pauseOnLostFocus=false;
        ticks++;
        if(ticks<90)return;ticks=0;
        try{
            switch(stage++){
                case 0->{mc.options.hideGui=true;screenshot("01-showroom");mc.options.fov().set(40);camera(4.5,65.15,.8f,0);}
                case 1->{screenshot("02-wall-button");camera(4.5,67,.9f,0);}
                case 2->{screenshot("03-wall-sensor");mc.options.fov().set(70);camera(4,66,-6,0);mc.options.hideGui=false;var d=(DoorBlockEntity)mc.level.getBlockEntity(DOOR);settingsScreen=new DoorSettingsScreen(DOOR,d.settings());mc.setScreen(settingsScreen);}
                case 3->{screenshot("04-door-settings");var d=(ControlBlockEntity)mc.level.getBlockEntity(BUTTON);mc.setScreen(new DeviceSettingsScreen(BUTTON,d.mount()));}
                case 4->{screenshot("05-device-position");mc.setScreen(new DesignScreen(settingsScreen,(DoorBlockEntity)mc.level.getBlockEntity(DOOR)));}
                case 5->{var screen=(DesignScreen)mc.screen;if(!screen.importPhoto(Path.of("/workspace/.tools/test-photo.jpg")))throw new IllegalStateException("JPEG import failed");
                    if(!screen.importPhoto(Path.of(System.getProperty("manydoors.testPhoto","/workspace/.tools/test-photo.png"))))throw new IllegalStateException("PNG import failed");LOG.info("VISUAL_PNG_JPEG_IMPORT_OK");}
                case 6->{screenshot("06-photo-editor");var s=(DesignScreen)mc.screen;int zoom=Math.max(1,Math.min(3,Math.min((s.height-75)/DoorDesign.HEIGHT,(s.width-197)/DoorDesign.WIDTH)));int left=(s.width-(DoorDesign.WIDTH*zoom+185))/2,top=Math.max(28,(s.height-Math.max(DoorDesign.HEIGHT*zoom,204)-32)/2+24);int side=left+DoorDesign.WIDTH*zoom+12;s.mouseClicked(left+5*zoom,top+5*zoom,0);s.mouseReleased(left+5*zoom,top+5*zoom,0);s.mouseClicked(side+80,top+163,0);s.mouseReleased(side+80,top+163,0);mc.setScreen(null);}
                case 7->{var d=(DoorBlockEntity)mc.level.getBlockEntity(DOOR);if(!d.hasDesign())throw new IllegalStateException("Photo/drawing upload did not arrive on client");int[] data=d.designPixels();if((data[48*DoorDesign.WIDTH+32]&0xFFFFFF)!=0x2350DC||(data[5*DoorDesign.WIDTH+5]&0xFFFFFF)!=0x795438)throw new IllegalStateException("Imported/painted pixels changed in network round trip");LOG.info("VISUAL_DESIGN_ROUND_TRIP_OK {}",d.designHash());mc.options.hideGui=true;screenshot("07-custom-door");camera(8.5,66,-2,0);}
                case 8->{screenshot("08-lobby-mount");mc.getSingleplayerServer().execute(()->((ControlBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(BUTTON)).press());}
                case 9->{if(!mc.level.getBlockState(LOBBY).getValue(ConfigurableDoorBlock.OPEN)||((DoorBlockEntity)mc.level.getBlockEntity(LOBBY)).progress(1)<.99)throw new IllegalStateException("Button did not open/render the lobby automatic door");screenshot("09-lobby-open");LOG.info("VISUAL_SMOKE_COMPLETE");mc.stop();}
                default->{}
            }
        }catch(Throwable t){LOG.error("VISUAL_SMOKE_FAILED",t);mc.stop();}
    }
}
