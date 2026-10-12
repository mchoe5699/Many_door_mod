package com.mchoe5699.manydoors.door;

import com.mchoe5699.manydoors.item.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public class ConfigurableDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN;
    public final DoorStyle style;
    public ConfigurableDoorBlock(DoorStyle style,Properties props){super(props);this.style=style;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(OPEN,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,OPEN);}
    @Override public net.minecraft.world.item.Item asItem(){return com.mchoe5699.manydoors.ManyDoors.DOOR_ITEMS.get(style).get();}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new DoorBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,com.mchoe5699.manydoors.ManyDoors.DOOR_ENTITY.get(),(level,pos,state,be)->be.tick());}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return l.getBlockEntity(p) instanceof DoorBlockEntity d?d.shapeAt(p):Shapes.empty();}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof LinkerItem || player.getItemInHand(hand).getItem() instanceof SettingsItem)return InteractionResult.PASS;
        if(!l.isClientSide && l.getBlockEntity(p) instanceof DoorBlockEntity d)d.manualToggle();
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){
        if(!l.isClientSide && l.hasNeighborSignal(p) && l.getBlockEntity(p) instanceof DoorBlockEntity d)d.activate(false);
    }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!old.is(next.getBlock()) && l.getBlockEntity(p) instanceof DoorBlockEntity d)d.removeParts();
        super.onRemove(old,l,p,next,moving);
    }
}
