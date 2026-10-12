package com.mchoe5699.manydoors.item;

import com.mchoe5699.manydoors.ManyDoors;
import com.mchoe5699.manydoors.door.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class DoorPlacementItem extends Item {
    public final DoorStyle style;
    public DoorPlacementItem(DoorStyle s,Properties p){super(p);style=s;}
    @Override public InteractionResult useOn(UseOnContext c){
        Level l=c.getLevel();Player player=c.getPlayer();if(player==null)return InteractionResult.FAIL;
        BlockPos pos=c.getClickedPos().relative(c.getClickedFace());Direction facing=player.getDirection();
        DoorSettings settings=DoorSettings.defaults(style);
        for(BlockPos cell:DoorGeometry.footprint(pos,facing,style,settings)){
            if(!l.hasChunkAt(cell)||l.isOutsideBuildHeight(cell)||!l.getBlockState(cell).isAir()||!l.mayInteract(player,cell)||!player.mayUseItemAt(cell,c.getClickedFace(),c.getItemInHand())){
                if(!l.isClientSide)player.displayClientMessage(Component.translatable("message.manydoors.no_space"),true);return InteractionResult.FAIL;
            }
        }
        if(!l.isClientSide){
            l.setBlock(pos,ManyDoors.DOORS.get(style).get().defaultBlockState().setValue(ConfigurableDoorBlock.FACING,facing),3);
            if(!(l.getBlockEntity(pos) instanceof DoorBlockEntity door))return InteractionResult.FAIL;
            door.setOwner(player.getUUID());
            String result=door.configure(settings,player);
            if(!result.equals("ok")){l.removeBlock(pos,false);return InteractionResult.FAIL;}
            if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> text,TooltipFlag flag){text.add(Component.translatable("tooltip.manydoors.door"));}
}
