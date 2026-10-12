package com.mchoe5699.manydoors.item;

import com.mchoe5699.manydoors.compat.HomeNetLinks;
import com.mchoe5699.manydoors.control.*;
import com.mchoe5699.manydoors.door.DoorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LinkerItem extends Item {
    public LinkerItem(Properties p){super(p);}
    private void tell(Player p,String key){p.displayClientMessage(Component.translatable("message.manydoors."+key),true);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return link(c);}
    @Override public InteractionResult useOn(UseOnContext c){return link(c);}
    private InteractionResult link(UseOnContext c){
        Player p=c.getPlayer();if(p==null)return InteractionResult.FAIL;
        Level l=c.getLevel();BlockPos pos=c.getClickedPos();
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(p.isSpectator()||!l.mayInteract(p,pos))return InteractionResult.FAIL;
        DoorBlockEntity door=DoorBlockEntity.at(l,pos);
        if(door!=null){
            if(!door.canEdit(p)){tell(p,"not_owner");return InteractionResult.FAIL;}
            if(p.isShiftKeyDown()){c.getItemInHand().removeTagKey("Selection");tell(p,"selection_cleared");}
            else {
                CompoundTag selection=LinkTarget.of(door).save();selection.putString("Dimension",l.dimension().location().toString());
                c.getItemInHand().getOrCreateTag().put("Selection",selection);tell(p,"door_selected");
            }
            return InteractionResult.CONSUME;
        }
        BlockEntity be=l.getBlockEntity(pos);
        if(!(be instanceof ControlBlockEntity)&&!HomeNetLinks.supported(be))return InteractionResult.PASS;
        if(be instanceof ControlBlockEntity control&&!control.canEdit(p)){tell(p,"not_owner");return InteractionResult.FAIL;}
        if(p.isShiftKeyDown()){
            if(be instanceof ControlBlockEntity control)control.clear();else HomeNetLinks.get((ServerLevel)l).clear(pos);
            tell(p,"links_cleared");return InteractionResult.CONSUME;
        }
        CompoundTag selection=c.getItemInHand().getOrCreateTag().getCompound("Selection");
        if(!selection.hasUUID("Identity")||!selection.getString("Dimension").equals(l.dimension().location().toString())){tell(p,"select_door");return InteractionResult.FAIL;}
        LinkTarget target=LinkTarget.load(selection);door=target.resolve(l);
        if(door==null){tell(p,"missing_door");return InteractionResult.FAIL;}
        if(!door.canEdit(p)||!l.mayInteract(p,door.getBlockPos())){tell(p,"not_owner");return InteractionResult.FAIL;}
        if(door.getBlockPos().distSqr(pos)>64*64){tell(p,"link_range");return InteractionResult.FAIL;}
        String result=be instanceof ControlBlockEntity control?control.toggle(target):HomeNetLinks.get((ServerLevel)l).toggle(be,target);
        tell(p,result);return InteractionResult.CONSUME;
    }
    @Override public boolean isFoil(ItemStack s){return s.hasTag()&&s.getTag().contains("Selection");}
    @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> text,TooltipFlag flag){text.add(Component.translatable("tooltip.manydoors.linker"));}
}
