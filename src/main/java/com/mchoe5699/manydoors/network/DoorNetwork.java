package com.mchoe5699.manydoors.network;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.door.*;
import com.mchoe5699.manydoors.control.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DoorNetwork {
    private static final String VERSION="3";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ManyDoors.MOD_ID,"doors"),()->VERSION,VERSION::equals,VERSION::equals);
    public record Open(BlockPos pos,DoorSettings settings){
        static void encode(Open m,FriendlyByteBuf b){b.writeBlockPos(m.pos);write(b,m.settings);}
        static Open decode(FriendlyByteBuf b){return new Open(b.readBlockPos(),read(b));}
        static void handle(Open m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mchoe5699.manydoors.client.ClientSetup.open(m)));c.get().setPacketHandled(true);}
    }
    private static boolean editable(ServerPlayer p,BlockPos pos){return p!=null&&!p.isSpectator()&&p.level().hasChunkAt(pos)&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=144&&p.level().mayInteract(p,pos);}
    private static void tell(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("message.manydoors."+key),true);}
    public record Apply(BlockPos pos,DoorSettings settings){
        static void encode(Apply m,FriendlyByteBuf b){b.writeBlockPos(m.pos);write(b,m.settings);}
        static Apply decode(FriendlyByteBuf b){return new Apply(b.readBlockPos(),read(b));}
        static void handle(Apply m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(editable(p,m.pos)&&p.level().getBlockEntity(m.pos) instanceof DoorBlockEntity d)tell(p,d.configure(m.settings,p));});c.get().setPacketHandled(true);}
    }
    /** Small motion update: custom pixels and legacy coordinates are never resent every animation tick. */
    public record Motion(BlockPos pos,UUID identity,float progress){
        static void encode(Motion m,FriendlyByteBuf b){b.writeBlockPos(m.pos);b.writeUUID(m.identity);b.writeFloat(m.progress);}
        static Motion decode(FriendlyByteBuf b){return new Motion(b.readBlockPos(),b.readUUID(),b.readFloat());}
        static void handle(Motion m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mchoe5699.manydoors.client.ClientSetup.motion(m)));c.get().setPacketHandled(true);}
    }
    public record DeviceOpen(BlockPos pos,MountSettings settings){
        static void encode(DeviceOpen m,FriendlyByteBuf b){b.writeBlockPos(m.pos);writeMount(b,m.settings);}
        static DeviceOpen decode(FriendlyByteBuf b){return new DeviceOpen(b.readBlockPos(),readMount(b));}
        static void handle(DeviceOpen m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mchoe5699.manydoors.client.ClientSetup.device(m)));c.get().setPacketHandled(true);}
    }
    public record DeviceApply(BlockPos pos,MountSettings settings){
        static void encode(DeviceApply m,FriendlyByteBuf b){b.writeBlockPos(m.pos);writeMount(b,m.settings);}
        static DeviceApply decode(FriendlyByteBuf b){return new DeviceApply(b.readBlockPos(),readMount(b));}
        static void handle(DeviceApply m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(editable(p,m.pos)&&p.level().getBlockEntity(m.pos) instanceof ControlBlockEntity be)tell(p,be.configure(m.settings,p));});c.get().setPacketHandled(true);}
    }
    public record DesignApply(BlockPos pos,UUID identity,int[] pixels){
        public static void encode(DesignApply m,FriendlyByteBuf b){b.writeBlockPos(m.pos);b.writeUUID(m.identity);b.writeVarIntArray(m.pixels);}
        public static DesignApply decode(FriendlyByteBuf b){return new DesignApply(b.readBlockPos(),b.readUUID(),b.readVarIntArray(DoorDesign.PIXELS));}
        static void handle(DesignApply m,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(editable(p,m.pos)&&p.level().getBlockEntity(m.pos) instanceof DoorBlockEntity d&&d.identity().equals(m.identity))tell(p,d.applyDesign(m.pixels,p));});c.get().setPacketHandled(true);}
    }
    static void write(FriendlyByteBuf b,DoorSettings s){b.writeVarInt(s.width());b.writeVarInt(s.height());b.writeDouble(s.thickness());b.writeDouble(s.angle());b.writeEnum(s.mode());b.writeVarInt(s.closeTicks());b.writeBoolean(s.rightHinge());}
    static DoorSettings read(FriendlyByteBuf b){return new DoorSettings(b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readEnum(DoorSettings.Mode.class),b.readVarInt(),b.readBoolean());}
    static void writeMount(FriendlyByteBuf b,MountSettings s){b.writeDouble(s.x());b.writeDouble(s.y());b.writeDouble(s.depth());b.writeDouble(s.scale());b.writeVarInt(s.range());}
    static MountSettings readMount(FriendlyByteBuf b){return new MountSettings(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt());}
    public static void register(){
        CHANNEL.registerMessage(0,Open.class,Open::encode,Open::decode,Open::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Apply.class,Apply::encode,Apply::decode,Apply::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2,Motion.class,Motion::encode,Motion::decode,Motion::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3,DeviceOpen.class,DeviceOpen::encode,DeviceOpen::decode,DeviceOpen::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4,DeviceApply.class,DeviceApply::encode,DeviceApply::decode,DeviceApply::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5,DesignApply.class,DesignApply::encode,DesignApply::decode,DesignApply::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(ServerPlayer p,DoorBlockEntity d){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Open(d.getBlockPos(),d.settings()));}
    public static void openDevice(ServerPlayer p,ControlBlockEntity be){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new DeviceOpen(be.getBlockPos(),be.mount()));}
    public static void motion(DoorBlockEntity d){if(d.getLevel() instanceof ServerLevel l)CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(()->l.getChunkAt(d.getBlockPos())),new Motion(d.getBlockPos(),d.identity(),d.progress(1)));}
}
