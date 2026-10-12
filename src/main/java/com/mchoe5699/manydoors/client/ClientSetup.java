package com.mchoe5699.manydoors.client;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.network.DoorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ManyDoors.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ManyDoors.DOOR_ENTITY.get(),DoorRenderer::new);e.registerBlockEntityRenderer(ManyDoors.CONTROL_ENTITY.get(),ControlRenderer::new);e.registerEntityRenderer(ManyDoors.COLLIDER.get(),DoorColliderRenderer::new);}
    public static void open(DoorNetwork.Open packet){Minecraft.getInstance().setScreen(new DoorSettingsScreen(packet.pos(),packet.settings()));}
    public static void device(DoorNetwork.DeviceOpen m){Minecraft.getInstance().setScreen(new DeviceSettingsScreen(m.pos(),m.settings()));}
    public static void motion(DoorNetwork.Motion m){var level=Minecraft.getInstance().level;if(level!=null&&level.getBlockEntity(m.pos()) instanceof com.mchoe5699.manydoors.door.DoorBlockEntity d&&d.identity().equals(m.identity()))d.receiveProgress(m.progress());}
}
