package com.mchoe5699.manydoors.network;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.door.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DoorNetwork {
    private static final String VERSION="1";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ManyDoors.MOD_ID,"doors"),()->VERSION,VERSION::equals,VERSION::equals);
    public record Open(BlockPos pos,DoorSettings settings){
        static void encode(Open m,FriendlyByteBuf b){b.writeBlockPos(m.pos);write(b,m.settings);}
        static Open decode(FriendlyByteBuf b){return new Open(b.readBlockPos(),read(b));}
        static void handle(Open m,Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.mchoe5699.manydoors.client.ClientSetup.open(m)));
            ctx.get().setPacketHandled(true);
        }
    }
    public record Apply(BlockPos pos,DoorSettings settings){
        static void encode(Apply m,FriendlyByteBuf b){b.writeBlockPos(m.pos);write(b,m.settings);}
        static Apply decode(FriendlyByteBuf b){return new Apply(b.readBlockPos(),read(b));}
        static void handle(Apply m,Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer p=ctx.get().getSender();if(p==null||p.isSpectator()||!p.level().hasChunkAt(m.pos)||p.distanceToSqr(m.pos.getX()+.5,m.pos.getY()+.5,m.pos.getZ()+.5)>144||!p.level().mayInteract(p,m.pos))return;
                var be=p.level().getBlockEntity(m.pos);if(!(be instanceof DoorBlockEntity door))return;
                String result=door.configure(m.settings,p);p.displayClientMessage(Component.translatable("message.manydoors."+result),true);
            });ctx.get().setPacketHandled(true);
        }
    }
    static void write(FriendlyByteBuf b,DoorSettings s){b.writeVarInt(s.width());b.writeVarInt(s.height());b.writeDouble(s.thickness());b.writeDouble(s.angle());b.writeEnum(s.mode());b.writeVarInt(s.closeTicks());b.writeBoolean(s.rightHinge());}
    static DoorSettings read(FriendlyByteBuf b){return new DoorSettings(b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readEnum(DoorSettings.Mode.class),b.readVarInt(),b.readBoolean());}
    public static void register(){
        CHANNEL.registerMessage(0,Open.class,Open::encode,Open::decode,Open::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Apply.class,Apply::encode,Apply::decode,Apply::handle,java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(ServerPlayer p,DoorBlockEntity door){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Open(door.getBlockPos(),door.settings()));}
}
