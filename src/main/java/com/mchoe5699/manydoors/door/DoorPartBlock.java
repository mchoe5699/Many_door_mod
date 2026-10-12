package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.item.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class DoorPartBlock extends BaseEntityBlock {
    public DoorPartBlock(Properties p){super(p);}
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter l,BlockPos p,BlockState s){DoorBlockEntity d=l instanceof Level level?DoorBlockEntity.at(level,p):null;return d==null?net.minecraft.world.item.ItemStack.EMPTY:new net.minecraft.world.item.ItemStack(com.mchoe5699.manydoors.ManyDoors.DOOR_ITEMS.get(d.style()).get());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DoorPartEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,com.mchoe5699.manydoors.ManyDoors.PART_ENTITY.get(),(level,pos,state,be)->be.tick());}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){DoorBlockEntity d=l instanceof Level level?DoorBlockEntity.at(level,p):null;return d!=null?d.shapeAt(p):Shapes.empty();}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof LinkerItem || player.getItemInHand(hand).getItem() instanceof SettingsItem)return InteractionResult.PASS;
        DoorBlockEntity d=DoorBlockEntity.at(l,p);if(!l.isClientSide&&d!=null)d.manualToggle();return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&!l.isClientSide){DoorBlockEntity d=DoorBlockEntity.at(l,p);if(d!=null&&!d.restructuring)l.destroyBlock(d.getBlockPos(),true);}
        super.onRemove(old,l,p,next,moving);
    }
}
