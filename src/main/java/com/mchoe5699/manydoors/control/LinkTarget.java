package com.mchoe5699.manydoors.control;

import com.mchoe5699.manydoors.door.DoorBlockEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public record LinkTarget(BlockPos pos, UUID identity) {
    public static LinkTarget of(DoorBlockEntity door){return new LinkTarget(door.getBlockPos().immutable(),door.identity());}
    public DoorBlockEntity resolve(Level level){DoorBlockEntity d=DoorBlockEntity.at(level,pos);return d!=null&&d.getBlockPos().equals(pos)&&d.identity().equals(identity)?d:null;}
    public CompoundTag save(){CompoundTag t=new CompoundTag();t.putLong("Pos",pos.asLong());t.putUUID("Identity",identity);return t;}
    public static LinkTarget load(CompoundTag t){return new LinkTarget(BlockPos.of(t.getLong("Pos")),t.getUUID("Identity"));}
}
