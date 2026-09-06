package com.plokie.mixin;

import com.nimbusds.oauth2.sdk.util.OrderedJSONObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//@Mixin(Entity.class)
//public class ShulkerMixin {
//    @Inject(method="isPassenger", at=@At("RETURN"), cancellable = true)
//    void isPassenger(CallbackInfoReturnable<Boolean> cir)
//    {
//        if(((Object)this) instanceof Shulker) {
//            cir.setReturnValue(true);
//        }
//    }
//}

@Mixin(Shulker.class)
public class ShulkerMixin {
//    @Inject(method="findNewAttachment", at=@At("HEAD"), cancellable = true)
//    void findNewAttachment(CallbackInfo ci)
//    {
//        ci.cancel();
//    }
//
////    @Inject(method="tick", at=@At("HEAD"), cancellable = true)
////    void tick(CallbackInfo ci)
////    {
////        ci.cancel();
////    }
//
//    @Inject(method="setPos", at=@At("HEAD"), cancellable = true)
//    void setPos(double d, double e, double f, CallbackInfo ci)
//    {
//        Shulker self = (Shulker)(Object)this;
//        self.setPosRaw(d, e, f);
//        //((Entity)(Object)this).setPos(d, e, f);
//        ci.cancel();
//    }
}
