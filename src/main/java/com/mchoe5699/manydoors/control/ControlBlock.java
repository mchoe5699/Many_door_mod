package com.mchoe5699.manydoors.control;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.item.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ControlBlock extends BaseEntityBlock {
    public enum Kind { BUTTON, MOTION_SENSOR, PRESSURE_SENSOR, CONTACT_SENSOR, STATUS_LAMP, EMERGENCY_BUTTON }
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public final Kind kind;
    public ControlBlock(Kind kind,Properties props){super(props);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(POWERED,false).setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(POWERED,FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(kind==Kind.PRESSURE_SENSOR)return c.getClickedFace()==Direction.UP?defaultBlockState():null;
        if(c.getClickedFace().getAxis()==Direction.Axis.Y)return null;
        BlockState s=defaultBlockState().setValue(FACING,c.getClickedFace());return canSurvive(s,c.getLevel(),c.getClickedPos())?s:null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack){if(!l.isClientSide&&entity instanceof Player player&&l.getBlockEntity(p) instanceof ControlBlockEntity be)be.setOwner(player.getUUID());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){MountSettings mount=l.getBlockEntity(p) instanceof ControlBlockEntity be?be.mount():MountSettings.defaults();return Shapes.create(mount.bounds(s.getValue(FACING),kind));}
    @Override public boolean canSurvive(BlockState s,LevelReader l,BlockPos p){Direction direction=kind==Kind.PRESSURE_SENSOR?Direction.DOWN:s.getValue(FACING).getOpposite();BlockPos backing=p.relative(direction);return l.getBlockState(backing).isFaceSturdy(l,backing,direction.getOpposite());}
    @Override public BlockState updateShape(BlockState s,Direction direction,BlockState other,LevelAccessor l,BlockPos p,BlockPos neighbor){Direction support=kind==Kind.PRESSURE_SENSOR?Direction.DOWN:s.getValue(FACING).getOpposite();return direction==support&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,direction,other,l,p,neighbor);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ControlBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ManyDoors.CONTROL_ENTITY.get(),(level,pos,state,be)->be.tick());}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof LinkerItem || player.getItemInHand(hand).getItem() instanceof SettingsItem)return InteractionResult.PASS;
        if(!l.isClientSide && l.getBlockEntity(p) instanceof ControlBlockEntity be){
            if(kind==Kind.BUTTON||kind==Kind.EMERGENCY_BUTTON)be.press();
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.manydoors.links",be.targets().size()),true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public boolean isSignalSource(BlockState s){return true;}
    @Override public int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return s.getValue(POWERED)?15:0;}
}
