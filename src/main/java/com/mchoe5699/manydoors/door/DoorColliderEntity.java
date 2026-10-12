package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.item.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Ephemeral collision/picking geometry. Never reserves a block or prevents building. */
public final class DoorColliderEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> ROOT=SynchedEntityData.defineId(DoorColliderEntity.class,EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Optional<UUID>> ID=SynchedEntityData.defineId(DoorColliderEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> INDEX=SynchedEntityData.defineId(DoorColliderEntity.class,EntityDataSerializers.INT);
    private boolean active;
    public DoorColliderEntity(EntityType<? extends DoorColliderEntity> type,Level level){super(type,level);blocksBuilding=false;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(ROOT,BlockPos.ZERO);entityData.define(ID,Optional.empty());entityData.define(INDEX,0);}
    public void bind(DoorBlockEntity root,int index){entityData.set(ROOT,root.getBlockPos());entityData.set(ID,Optional.of(root.identity()));entityData.set(INDEX,index);}
    public DoorBlockEntity root(){DoorBlockEntity d=DoorBlockEntity.at(level(),entityData.get(ROOT));return d!=null&&entityData.get(ID).filter(d.identity()::equals).isPresent()?d:null;}
    public void updateBox(DoorBlockEntity d){
        int i=entityData.get(INDEX);var boxes=d.collisionBoxes();active=i>=0&&i<boxes.size();
        if(active){AABB b=boxes.get(i).move(d.getBlockPos());setPos((b.minX+b.maxX)/2,b.minY,(b.minZ+b.maxZ)/2);setBoundingBox(b);}
        else {setPos(d.getBlockPos().getX()+.5,d.getBlockPos().getY(),d.getBlockPos().getZ()+.5);setBoundingBox(new AABB(position(),position()));}
    }
    @Override public void tick(){DoorBlockEntity d=root();if(d!=null)updateBox(d);else if(!level().isClientSide)discard();}
    @Override public boolean canBeCollidedWith(){return active&&!isRemoved()&&root()!=null;}
    @Override public boolean canCollideWith(Entity other){return canBeCollidedWith()&&other instanceof LivingEntity;}
    @Override public boolean isPickable(){return canBeCollidedWith();}
    @Override public boolean shouldBeSaved(){return false;}
    @Override public float getPickRadius(){return .01f;}
    @Override public ItemStack getPickResult(){var d=root();return d==null?ItemStack.EMPTY:new ItemStack(ManyDoors.DOOR_ITEMS.get(d.style()).get());}
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        DoorBlockEntity d=root();if(d==null)return InteractionResult.PASS;
        ItemStack stack=player.getItemInHand(hand);
        if(stack.getItem() instanceof SettingsItem||stack.getItem() instanceof LinkerItem||stack.getItem().getClass().getName().equals("com.qwerty.homenet.item.DoorLinkerItem")){
            UseOnContext context=new UseOnContext(player,hand,new BlockHitResult(Vec3.atCenterOf(d.getBlockPos()),d.facing(),d.getBlockPos(),false));
            return stack.getItem().onItemUseFirst(stack,context);
        }
        if(!level().isClientSide)d.manualToggle();return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public InteractionResult interactAt(Player p,Vec3 at,InteractionHand hand){return interact(p,hand);}
    @Override public boolean skipAttackInteraction(Entity attacker){
        if(attacker instanceof Player p){var d=root();if(d!=null&&!level().isClientSide&&level().mayInteract(p,d.getBlockPos())){
            if(p.getAbilities().instabuild||p.isShiftKeyDown())level().destroyBlock(d.getBlockPos(),true);
            else p.displayClientMessage(Component.translatable("message.manydoors.shift_remove"),true);
        }}return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){}
    @Override protected void readAdditionalSaveData(CompoundTag t){}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
