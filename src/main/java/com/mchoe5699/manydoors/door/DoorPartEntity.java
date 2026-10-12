package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.ManyDoors;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DoorPartEntity extends BlockEntity {
    private BlockPos anchor;
    private UUID identity=new UUID(0,0);
    public DoorPartEntity(BlockPos p,BlockState s) {super(ManyDoors.PART_ENTITY.get(),p,s);anchor=p;}
    public BlockPos anchor(){return anchor;}
    public UUID identity(){return identity;}
    public void bind(BlockPos p,UUID id){anchor=p.immutable();identity=id;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void tick(){if(level!=null&&!level.isClientSide&&level.getGameTime()%20==0&&level.hasChunkAt(anchor)&&(getBlockState().is(ManyDoors.DOOR_PART.get())||DoorBlockEntity.at(level,worldPosition)==null))level.removeBlock(worldPosition,false);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putLong("Anchor",anchor.asLong());t.putUUID("Identity",identity);}
    @Override public void load(CompoundTag t){super.load(t);anchor=BlockPos.of(t.getLong("Anchor"));if(t.hasUUID("Identity"))identity=t.getUUID("Identity");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
