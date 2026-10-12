package com.mchoe5699.manydoors.item;

import com.mchoe5699.manydoors.door.DoorBlockEntity;
import com.mchoe5699.manydoors.control.ControlBlockEntity;
import com.mchoe5699.manydoors.network.DoorNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class SettingsItem extends Item {
    public SettingsItem(Properties p){super(p);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){return useOn(c);}
    @Override public InteractionResult useOn(UseOnContext c){
        DoorBlockEntity door=DoorBlockEntity.at(c.getLevel(),c.getClickedPos());
        if(door!=null){
            if(c.getPlayer() instanceof ServerPlayer p){
                if(door.canEdit(p)&&p.level().mayInteract(p,door.getBlockPos()))DoorNetwork.open(p,door);
                else p.displayClientMessage(Component.translatable("message.manydoors.not_owner"),true);
            }return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
        if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof ControlBlockEntity control){
            if(c.getPlayer() instanceof ServerPlayer p&&control.canEdit(p)&&p.level().mayInteract(p,control.getBlockPos())){
                DoorNetwork.openDevice(p,control);
            }return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> text,TooltipFlag flag){text.add(Component.translatable("tooltip.manydoors.settings"));}
}
