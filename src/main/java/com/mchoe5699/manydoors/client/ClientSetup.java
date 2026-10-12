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
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ManyDoors.DOOR_ENTITY.get(),DoorRenderer::new);}
    public static void open(DoorNetwork.Open packet){Minecraft.getInstance().setScreen(new DoorSettingsScreen(packet.pos(),packet.settings()));}
}
