package com.mchoe5699.manydoors.mixin;

import com.mchoe5699.manydoors.compat.HomeNetLinks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.qwerty.homenet.blockentity.ReceiverBlockEntity",remap=false)
public abstract class ReceiverDoorMixin {
    // This site is AFTER HomeNet's idle/outgoing guards and BEFORE it clears the call.
    @Inject(method="openDoor",at=@At(value="INVOKE",target="Lcom/qwerty/homenet/blockentity/ReceiverBlockEntity;resetCall()V"),require=1,remap=false)
    private void manydoors$open(CallbackInfo ci){HomeNetLinks.onOpen((BlockEntity)(Object)this);}
}
