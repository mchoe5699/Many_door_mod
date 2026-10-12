package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.ManyDoors;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.sounds.*;

public class DoorBlockEntity extends BlockEntity {
    private DoorSettings settings;
    private UUID identity=UUID.randomUUID(), owner;
    private Set<BlockPos> occupied=new HashSet<>();
    public boolean restructuring;
    private float progress, previousProgress;
    private long closeAt=-1, shapeTick=-1;
    private List<AABB> cachedBoxes=List.of();
    private final Map<BlockPos,VoxelShape> shapes=new HashMap<>();

    public DoorBlockEntity(BlockPos pos,BlockState state) {
        super(ManyDoors.DOOR_ENTITY.get(),pos,state);
        settings=DoorSettings.defaults(style()); occupied.add(pos);
    }
    public DoorStyle style() { return ((ConfigurableDoorBlock)getBlockState().getBlock()).style; }
    public DoorSettings settings() { return settings; }
    public UUID identity() { return identity; }
    public void setOwner(UUID id) { owner=id; setChanged(); }
    public boolean canEdit(Player player) { return !player.isSpectator() && (owner==null || owner.equals(player.getUUID()) || player.hasPermissions(2)); }
    public Direction facing() { return getBlockState().getValue(ConfigurableDoorBlock.FACING); }
    public float progress(float partial) { return previousProgress+(progress-previousProgress)*partial; }
    public static DoorBlockEntity at(Level level,BlockPos pos) {
        if(!level.hasChunkAt(pos)) return null;
        BlockEntity b=level.getBlockEntity(pos);
        if(b instanceof DoorBlockEntity d) return d;
        if(b instanceof DoorPartEntity part && level.hasChunkAt(part.anchor())) {
            BlockEntity root=level.getBlockEntity(part.anchor());
            if(root instanceof DoorBlockEntity d && d.identity.equals(part.identity())) return d;
        }
        return null;
    }
    public String configure(DoorSettings next, Player player) {
        if(level==null || level.isClientSide) return "server_only";
        if(player!=null && !canEdit(player)) return "not_owner";
        if(!next.valid()) return "invalid_settings";
        Set<BlockPos> proposed=DoorGeometry.footprint(worldPosition,facing(),style(),next);
        for(BlockPos p:proposed) {
            if(!level.hasChunkAt(p) || level.isOutsideBuildHeight(p)) return "outside_world";
            if(player!=null && !level.mayInteract(player,p)) return "protected";
            if(p.equals(worldPosition)) continue;
            BlockEntity b=level.getBlockEntity(p);
            boolean own=b instanceof DoorPartEntity part && part.anchor().equals(worldPosition) && part.identity().equals(identity);
            if(!own && !level.getBlockState(p).isAir()) return "no_space";
        }
        if(player!=null) for(BlockPos p:occupied) if(!level.mayInteract(player,p)) return "protected";
        restructuring=true;
        try {
            for(BlockPos p:occupied) if(!p.equals(worldPosition) && !proposed.contains(p)) {
                if(isOwnPart(p)) level.removeBlock(p,false);
            }
            for(BlockPos p:proposed) if(!p.equals(worldPosition) && !isOwnPart(p)) {
                level.setBlock(p,ManyDoors.DOOR_PART.get().defaultBlockState(),3);
                if(level.getBlockEntity(p) instanceof DoorPartEntity part) part.bind(worldPosition,identity);
            }
            occupied=proposed; settings=next; shapes.clear(); shapeTick=-1;
            sync(); return "ok";
        } finally { restructuring=false; }
    }
    private boolean isOwnPart(BlockPos p) {
        return level.getBlockEntity(p) instanceof DoorPartEntity part && part.anchor().equals(worldPosition) && identity.equals(part.identity());
    }
    public void removeParts() {
        if(level==null || level.isClientSide || restructuring) return;
        restructuring=true;
        for(BlockPos p:occupied) if(!p.equals(worldPosition) && level.hasChunkAt(p) && isOwnPart(p)) level.removeBlock(p,false);
        restructuring=false;
    }
    public boolean activate(boolean remote) {
        if(level==null || level.isClientSide || (!remote && !settings.mode().allowsSensor())) return false;
        setOpen(true);
        closeAt=settings.closeTicks()==0 ? -1 : level.getGameTime()+settings.closeTicks();
        setChanged(); return true;
    }
    public boolean manualToggle() {
        if(!settings.mode().allowsClick()) return false;
        setOpen(!getBlockState().getValue(ConfigurableDoorBlock.OPEN)); closeAt=-1; setChanged(); return true;
    }
    public void setOpen(boolean open) {
        if(level==null || level.isClientSide) return;
        BlockState state=level.getBlockState(worldPosition);
        if(!(state.getBlock() instanceof ConfigurableDoorBlock) || state.getValue(ConfigurableDoorBlock.OPEN)==open) return;
        level.setBlock(worldPosition,state.setValue(ConfigurableDoorBlock.OPEN,open),3);
        level.playSound(null,worldPosition,open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE,SoundSource.BLOCKS,.6f,1);
    }
    public void tick() {
        if(level==null) return;
        previousProgress=progress;
        if(!level.isClientSide && closeAt>=0 && level.getGameTime()>=closeAt) { setOpen(false);closeAt=-1;setChanged(); }
        boolean open=getBlockState().getValue(ConfigurableDoorBlock.OPEN);
        float next=style().motion==DoorStyle.Motion.REVOLVE?(open?progress+.02f:progress):(open?Math.min(1,progress+.08f):Math.max(0,progress-.08f));
        if(!level.isClientSide && next!=progress && obstructed(next)) {
            if(!open) { setOpen(true);closeAt=level.getGameTime()+Math.max(20,settings.closeTicks());setChanged(); }
            if(level.getGameTime()%4==0)sync();
            return;
        }
        progress=next;
        if(!level.isClientSide&&next!=previousProgress&&level.getGameTime()%4==0)sync();
    }
    private boolean obstructed(float next) {
        for(AABB b:DoorGeometry.boxes(style(),settings,next,facing())) {
            if(!level.getEntitiesOfClass(LivingEntity.class,b.move(worldPosition).deflate(.015),e->e.isAlive()&&!e.isSpectator()).isEmpty()) return true;
        }
        return false;
    }
    public VoxelShape shapeAt(BlockPos cell) {
        if(level==null) return Shapes.empty();
        if(shapeTick!=Float.floatToIntBits(progress)) {
            shapeTick=Float.floatToIntBits(progress);shapes.clear();cachedBoxes=DoorGeometry.boxes(style(),settings,progress,facing());
        }
        return shapes.computeIfAbsent(cell,p->{
            int dx=p.getX()-worldPosition.getX(),dy=p.getY()-worldPosition.getY(),dz=p.getZ()-worldPosition.getZ();
            AABB clip=new AABB(dx,dy,dz,dx+1,dy+1,dz+1);
            VoxelShape shape=Shapes.empty();
            for(AABB b:cachedBoxes) if(b.intersects(clip)) shape=Shapes.or(shape,Shapes.create(b.intersect(clip).move(-dx,-dy,-dz)));
            return shape;
        });
    }
    public void sync() { setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);t.put("Settings",settings.save());t.putUUID("Identity",identity);
        if(owner!=null)t.putUUID("Owner",owner);
        t.putLongArray("Occupied",occupied.stream().mapToLong(BlockPos::asLong).toArray());
        t.putLong("CloseAt",closeAt);t.putFloat("Progress",progress);
    }
    @Override public void load(CompoundTag t) {
        super.load(t);settings=DoorSettings.load(t.getCompound("Settings"),style());
        if(t.hasUUID("Identity"))identity=t.getUUID("Identity");owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;
        occupied=new HashSet<>();for(long v:t.getLongArray("Occupied"))occupied.add(BlockPos.of(v));occupied.add(worldPosition);
        closeAt=t.contains("CloseAt")?t.getLong("CloseAt"):-1;progress=t.getFloat("Progress");previousProgress=progress;shapes.clear();shapeTick=-1;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(16); }
}
