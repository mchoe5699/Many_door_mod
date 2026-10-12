package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.network.DoorNetwork;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.sounds.*;

public class DoorBlockEntity extends BlockEntity {
    private final DoorStyle registeredStyle;
    private DoorSettings settings;
    private UUID identity=UUID.randomUUID(), owner;
    private Set<BlockPos> occupied=new HashSet<>();
    public boolean restructuring;
    private boolean migrateLegacy;
    private float progress, previousProgress;
    private long closeAt=-1;
    private int geometryFrame=-1;
    private List<AABB> cachedBoxes=List.of();
    private final List<DoorColliderEntity> colliders=new ArrayList<>();
    private int[] customPixels=new int[0];
    private int designHash;
    private int colliderCapacity=-1;
    public boolean hasDesign(){return customPixels.length==DoorDesign.PIXELS;}

    public DoorBlockEntity(BlockPos pos,BlockState state){
        super(ManyDoors.DOOR_ENTITY.get(),pos,state);
        registeredStyle=state.getBlock() instanceof ConfigurableDoorBlock b?b.style:DoorStyle.WOODEN;
        settings=DoorSettings.defaults(registeredStyle);occupied.add(pos);
    }
    public DoorStyle style(){return registeredStyle;}
    public DoorSettings settings(){return settings;}
    public UUID identity(){return identity;}
    public void setOwner(UUID id){owner=id;setChanged();}
    public boolean canEdit(Player p){return !p.isSpectator()&&(owner==null||owner.equals(p.getUUID())||p.hasPermissions(2));}
    public Direction facing(){return getBlockState().hasProperty(ConfigurableDoorBlock.FACING)?getBlockState().getValue(ConfigurableDoorBlock.FACING):Direction.NORTH;}
    public float progress(float partial){return previousProgress+(progress-previousProgress)*partial;}
    public int[] designPixels(){return customPixels.clone();}
    public int designHash(){return designHash;}
    public String applyDesign(int[] pixels,Player player){
        if(level==null||level.isClientSide)return "server_only";
        if(player!=null&&!canEdit(player))return "not_owner";
        if(!DoorDesign.valid(pixels))return "invalid_design";
        customPixels=pixels.clone();designHash=Arrays.hashCode(customPixels);sync();return "ok";
    }
    public static DoorBlockEntity at(Level l,BlockPos p){
        if(!l.hasChunkAt(p))return null;
        BlockEntity b=l.getBlockEntity(p);
        if(b instanceof DoorBlockEntity d&&!d.isRemoved()&&l.getBlockState(p).getBlock() instanceof ConfigurableDoorBlock)return d;
        if(b instanceof DoorPartEntity part&&l.hasChunkAt(part.anchor())){
            BlockEntity root=l.getBlockEntity(part.anchor());
            if(root instanceof DoorBlockEntity d&&!d.isRemoved()&&d.identity.equals(part.identity()))return d;
        }return null;
    }
    /** Only the anchor and the real lobby mounting column occupy blocks. Moving leaves do not. */
    public static Set<BlockPos> mountingCells(BlockPos anchor,DoorStyle style,DoorSettings s){
        Set<BlockPos> cells=new HashSet<>();cells.add(anchor);
        if(style==DoorStyle.LOBBY_AUTOMATIC)for(int y=1;y<s.height();y++)cells.add(anchor.above(y));
        return cells;
    }
    private boolean ownPart(BlockPos p){return level.getBlockEntity(p) instanceof DoorPartEntity part&&part.anchor().equals(worldPosition)&&identity.equals(part.identity());}
    private void cleanupOldParts(){
        if(level==null||level.isClientSide)return;
        restructuring=true;
        try{
            for(BlockPos p:occupied)if(!p.equals(worldPosition)&&level.hasChunkAt(p)&&ownPart(p)&&level.getBlockState(p).is(ManyDoors.DOOR_PART.get()))level.removeBlock(p,false);
        }finally{restructuring=false;}
        migrateLegacy=false;occupied=mountingCells(worldPosition,style(),settings);setChanged();
    }
    public String configure(DoorSettings next,Player player){
        if(level==null||level.isClientSide)return "server_only";
        if(player!=null&&!canEdit(player))return "not_owner";
        if(!next.valid()||(style()==DoorStyle.LOBBY_AUTOMATIC&&next.width()<2))return "invalid_settings";
        Set<BlockPos> proposed=mountingCells(worldPosition,style(),next);
        if(worldPosition.getY()+next.height()>level.getMaxBuildHeight())return "outside_world";
        for(BlockPos p:proposed){
            if(!level.hasChunkAt(p)||level.isOutsideBuildHeight(p))return "outside_world";
            if(player!=null&&!level.mayInteract(player,p))return "protected";
            if(!p.equals(worldPosition)&&!ownPart(p)&&!level.getBlockState(p).isAir())return "no_space";
        }
        if(migrateLegacy)cleanupOldParts();
        restructuring=true;
        try{
            for(BlockPos p:occupied)if(!p.equals(worldPosition)&&!proposed.contains(p)&&level.hasChunkAt(p)&&ownPart(p))level.removeBlock(p,false);
            for(BlockPos p:proposed)if(!p.equals(worldPosition)&&!ownPart(p)){
                level.setBlock(p,ManyDoors.LOBBY_MOUNT.get().defaultBlockState(),3);
                if(level.getBlockEntity(p) instanceof DoorPartEntity part)part.bind(worldPosition,identity);
            }
            occupied=proposed;settings=next;geometryFrame=-1;colliderCapacity=-1;removeColliders();ensureColliders();sync();return "ok";
        }finally{restructuring=false;}
    }
    public void removeParts(){
        if(level==null||level.isClientSide||restructuring)return;
        removeColliders();restructuring=true;
        try{for(BlockPos p:occupied)if(!p.equals(worldPosition)&&level.hasChunkAt(p)&&ownPart(p))level.removeBlock(p,false);}
        finally{restructuring=false;}
    }
    public boolean activate(boolean remote){
        if(level==null||level.isClientSide||(!remote&&!settings.mode().allowsSensor()))return false;
        setOpen(true);closeAt=settings.closeTicks()==0?-1:level.getGameTime()+settings.closeTicks();setChanged();return true;
    }
    public boolean manualToggle(){
        if(!settings.mode().allowsClick())return false;
        setOpen(!getBlockState().getValue(ConfigurableDoorBlock.OPEN));closeAt=-1;setChanged();return true;
    }
    public void setOpen(boolean open){
        if(level==null||level.isClientSide)return;
        BlockState s=level.getBlockState(worldPosition);
        if(!(s.getBlock() instanceof ConfigurableDoorBlock)||s.getValue(ConfigurableDoorBlock.OPEN)==open)return;
        level.setBlock(worldPosition,s.setValue(ConfigurableDoorBlock.OPEN,open),3);
        level.playSound(null,worldPosition,open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE,SoundSource.BLOCKS,.6f,1);
    }
    public List<AABB> collisionBoxes(){
        if(geometryFrame!=(Float.floatToIntBits(progress)^(facing().ordinal()<<24))){
            geometryFrame=Float.floatToIntBits(progress)^(facing().ordinal()<<24);cachedBoxes=DoorGeometry.boxes(style(),settings,progress,facing());
        }return cachedBoxes;
    }
    private void ensureColliders(){
        if(level==null||level.isClientSide||isRemoved())return;
        if(colliderCapacity<0)colliderCapacity=Math.max(DoorGeometry.boxes(style(),settings,0,facing()).size(),DoorGeometry.boxes(style(),settings,1,facing()).size());
        int count=colliderCapacity;
        // Removing before replacement prevents duplicate saved/helper entities after chunk reloads.
        if(colliders.size()!=count||colliders.stream().anyMatch(e->e.isRemoved())){
            removeColliders();
            for(int i=0;i<count;i++){
                DoorColliderEntity e=ManyDoors.COLLIDER.get().create(level);
                if(e!=null){e.bind(this,i);e.updateBox(this);level.addFreshEntity(e);colliders.add(e);}
            }
        }
    }
    private void removeColliders(){for(DoorColliderEntity e:colliders)e.discard();colliders.clear();}
    @Override public void setRemoved(){removeColliders();super.setRemoved();}
    public void receiveProgress(float p){
        if(!Float.isFinite(p))return;
        progress=style().motion==DoorStyle.Motion.REVOLVE?Math.max(0,p)%4:Math.max(0,Math.min(1,p));previousProgress=progress;geometryFrame=-1;
    }
    public void tick(){
        if(level==null||isRemoved()||!(getBlockState().getBlock() instanceof ConfigurableDoorBlock))return;
        if(!level.isClientSide&&migrateLegacy)cleanupOldParts();
        previousProgress=progress;
        if(!level.isClientSide&&closeAt>=0&&level.getGameTime()>=closeAt){setOpen(false);closeAt=-1;setChanged();}
        boolean open=getBlockState().getValue(ConfigurableDoorBlock.OPEN);
        float next=style().motion==DoorStyle.Motion.REVOLVE?(open?progress+.02f:progress):(open?Math.min(1,progress+.08f):Math.max(0,progress-.08f));
        if(!level.isClientSide&&next<progress&&style().motion!=DoorStyle.Motion.REVOLVE&&obstructed(next)){
            setOpen(true);closeAt=level.getGameTime()+Math.max(20,settings.closeTicks());next=progress;
        }
        progress=next;
        if(style().motion==DoorStyle.Motion.REVOLVE&&progress>=4){progress-=4;previousProgress-=4;}
        if(!level.isClientSide){
            ensureColliders();for(DoorColliderEntity e:colliders)e.updateBox(this);
            if(next!=previousProgress&&level.getGameTime()%4==0)DoorNetwork.motion(this);
        }
    }
    private boolean obstructed(float next){
        for(AABB b:DoorGeometry.boxes(style(),settings,next,facing()))
            if(!level.getEntitiesOfClass(LivingEntity.class,b.move(worldPosition).deflate(.015),e->e.isAlive()&&!e.isSpectator()).isEmpty())return true;
        return false;
    }
    public VoxelShape shapeAt(BlockPos cell){
        if(style()==DoorStyle.LOBBY_AUTOMATIC)return Shapes.block();
        int dx=cell.getX()-worldPosition.getX(),dy=cell.getY()-worldPosition.getY(),dz=cell.getZ()-worldPosition.getZ();
        AABB clip=new AABB(dx,dy,dz,dx+1,dy+1,dz+1);VoxelShape shape=Shapes.empty();
        for(AABB b:collisionBoxes())if(b.intersects(clip))shape=Shapes.or(shape,Shapes.create(b.intersect(clip).move(-dx,-dy,-dz)));
        return shape;
    }
    public void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag t){
        super.saveAdditional(t);t.putInt("Schema",3);t.put("Settings",settings.save());t.putUUID("Identity",identity);
        if(owner!=null)t.putUUID("Owner",owner);
        t.putLongArray("Occupied",occupied.stream().mapToLong(BlockPos::asLong).toArray());t.putLong("CloseAt",closeAt);t.putFloat("Progress",progress);t.putIntArray("Design",customPixels);
    }
    @Override public void load(CompoundTag t){
        super.load(t);settings=DoorSettings.load(t.getCompound("Settings"),registeredStyle);
        if(t.hasUUID("Identity"))identity=t.getUUID("Identity");owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;
        occupied=new HashSet<>();long[] stored=t.getLongArray("Occupied");for(int i=0;i<Math.min(stored.length,4096);i++)occupied.add(BlockPos.of(stored[i]));occupied.add(worldPosition);
        migrateLegacy=t.getInt("Schema")<3;closeAt=t.contains("CloseAt")?t.getLong("CloseAt"):-1;receiveProgress(t.getFloat("Progress"));
        int[] pixels=t.getIntArray("Design");customPixels=DoorDesign.valid(pixels)?pixels:new int[0];designHash=Arrays.hashCode(customPixels);
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(16);}
}
