package com.lelqwq.gtmpi.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.lelqwq.gtmpi.machine.PatternCapacity;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputSlave;

/** Preserve the existing separation of native and giant proxy links. */
@Mixin(value = MTEHatchCraftingInputSlave.class, remap = false)
public abstract class MixinCraftingInputProxy {

    @Inject(method = "trySetMasterFromCoord", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtmpi$separateProxyFamilies(int x, int y, int z, CallbackInfoReturnable<MTEHatchCraftingInputME> cir) {
        MTEHatchCraftingInputSlave self = (MTEHatchCraftingInputSlave) (Object) this;
        var tile = self.getBaseMetaTileEntity()
            .getWorld()
            .getTileEntity(x, y, z);
        if (tile instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() instanceof MTEHatchCraftingInputME master
            && !PatternCapacity.canLink(self.mName, master.mName)) {
            cir.setReturnValue(null);
        }
    }
}
