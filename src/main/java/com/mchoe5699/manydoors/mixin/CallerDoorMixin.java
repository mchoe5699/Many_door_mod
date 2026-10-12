package com.mchoe5699.manydoors.mixin;

import com.mchoe5699.manydoors.compat.HomeNetLinks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.qwerty.homenet.blockentity.CallerBlockEntity",remap=false)
public abstract class CallerDoorMixin {
    @Inject(method="openDoors",at=@At("TAIL"),require=1,remap=false)
    private void manydoors$open(CallbackInfo ci){HomeNetLinks.onOpen((BlockEntity)(Object)this);}
}
