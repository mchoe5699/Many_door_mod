package com.mchoe5699.manydoors.door;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** A real fixed mounting column, distinct from retired moving-area placeholders. */
public final class LobbyMountBlock extends DoorPartBlock {
    public LobbyMountBlock(Properties properties){super(properties);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&!l.isClientSide){var d=DoorBlockEntity.at(l,p);if(d!=null&&!d.restructuring)l.destroyBlock(d.getBlockPos(),true);}
        super.onRemove(old,l,p,next,moving);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.block();}
}
