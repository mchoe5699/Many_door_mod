package com.mchoe5699.manydoors.control;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.door.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ControlBlockEntity extends BlockEntity {
    private final List<LinkTarget> targets=new ArrayList<>();
    private UUID owner;
    private long poweredUntil;
    private int radius=3;
    public ControlBlockEntity(BlockPos p,BlockState s){super(ManyDoors.CONTROL_ENTITY.get(),p,s);}
    public List<LinkTarget> targets(){return Collections.unmodifiableList(targets);}
    public void setOwner(UUID id){owner=id;setChanged();}
    public boolean canEdit(net.minecraft.world.entity.player.Player p){return !p.isSpectator()&&(owner==null||owner.equals(p.getUUID())||p.hasPermissions(2));}
    public String toggle(LinkTarget target){
        if(targets.remove(target)){sync();return "unlinked";}
        if(targets.size()>=16)return "link_full";
        targets.add(target);sync();return "linked";
    }
    public void clear(){targets.clear();sync();}
    public void radius(int n){radius=Math.max(1,Math.min(8,n));sync();}
    public void press(){
        if(level==null||level.isClientSide)return;
        boolean remote=((ControlBlock)getBlockState().getBlock()).kind==ControlBlock.Kind.EMERGENCY_BUTTON;
        for(LinkTarget target:targets){DoorBlockEntity d=target.resolve(level);if(d!=null)d.activate(remote);}
        poweredUntil=level.getGameTime()+10;power(true);
    }
    private void power(boolean value){
        BlockState s=level.getBlockState(worldPosition);
        if(s.getBlock() instanceof ControlBlock&&s.getValue(ControlBlock.POWERED)!=value){
            level.setBlock(worldPosition,s.setValue(ControlBlock.POWERED,value),3);level.updateNeighborsAt(worldPosition,s.getBlock());
        }
    }
    public void tick(){
        if(level==null||level.isClientSide||level.getGameTime()%5!=0)return;
        var kind=((ControlBlock)getBlockState().getBlock()).kind;
        if(kind==ControlBlock.Kind.MOTION_SENSOR||kind==ControlBlock.Kind.PRESSURE_SENSOR){
            AABB area=kind==ControlBlock.Kind.PRESSURE_SENSOR?new AABB(worldPosition).inflate(0,.15,0):new AABB(worldPosition).inflate(radius,1,radius);
            boolean present=!level.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive()&&!e.isSpectator()).isEmpty();
            if(present)press();else power(false);
        } else if(kind==ControlBlock.Kind.CONTACT_SENSOR||kind==ControlBlock.Kind.STATUS_LAMP){
            boolean open=targets.stream().map(t->t.resolve(level)).anyMatch(d->d!=null&&d.getBlockState().getValue(ConfigurableDoorBlock.OPEN));power(open);
        } else if(level.getGameTime()>=poweredUntil)power(false);
        // Do not prune unloaded targets: they can be loaded again later.
        if(targets.removeIf(t->level.hasChunkAt(t.pos())&&t.resolve(level)==null))sync();
    }
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);ListTag list=new ListTag();targets.forEach(v->list.add(v.save()));t.put("Targets",list);t.putInt("Radius",radius);if(owner!=null)t.putUUID("Owner",owner);}
    @Override public void load(CompoundTag t){super.load(t);targets.clear();for(Tag v:t.getList("Targets",Tag.TAG_COMPOUND)){if(((CompoundTag)v).hasUUID("Identity")&&targets.size()<16)targets.add(LinkTarget.load((CompoundTag)v));}radius=t.contains("Radius")?Math.max(1,Math.min(8,t.getInt("Radius"))):3;owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
