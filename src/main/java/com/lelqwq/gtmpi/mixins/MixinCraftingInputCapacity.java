package com.lelqwq.gtmpi.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.lelqwq.gtmpi.machine.PatternCapacity;

import gregtech.api.interfaces.ITexture;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/** Adjust only storage dimensions and indices; keep every native processing method intact. */
@Mixin(value = MTEHatchCraftingInputME.class, remap = false)
public abstract class MixinCraftingInputCapacity {

    // Static handlers are safe for constants passed to super() before this is initialized.
    @ModifyConstant(
        method = "<init>(ILjava/lang/String;Ljava/lang/String;Z)V",
        constant = { @Constant(intValue = 36), @Constant(intValue = 46) },
        require = 3)
    private static int gtmpi$registeredCapacity(int original, int id, String name, String regional, boolean fluids) {
        return PatternCapacity.expand(name, original);
    }

    @ModifyConstant(
        method = "<init>(Ljava/lang/String;I[Ljava/lang/String;[[[Lgregtech/api/interfaces/ITexture;Z)V",
        constant = { @Constant(intValue = 36), @Constant(intValue = 46) },
        require = 3)
    private static int gtmpi$instanceCapacity(int original, String name, int tier, String[] description,
        ITexture[][][] textures, boolean fluids) {
        return PatternCapacity.expand(name, original);
    }

    @ModifyConstant(
        method = { "getCircuitSlot", "getSharedItems", "getNameSuffix", "getWailaNameLines", "updateSlots",
            "loadNBTData", "createSlotManualWindow" },
        constant = { @Constant(intValue = 36), @Constant(intValue = 37), @Constant(intValue = 45),
            @Constant(intValue = 46) },
        require = 1)
    private int gtmpi$sharedSlotIndices(int original) {
        return PatternCapacity.expand(((MTEHatchCraftingInputME) (Object) this).mName, original);
    }

    @ModifyConstant(
        method = { "setInventorySlotContents", "func_70299_a" },
        constant = @Constant(intValue = 36),
        remap = false,
        require = 1)
    private int gtmpi$patternInventoryBoundary(int original) {
        return PatternCapacity.expand(((MTEHatchCraftingInputME) (Object) this).mName, original);
    }

    @ModifyConstant(
        method = { "isItemValidForSlot", "func_94041_b" },
        constant = @Constant(intValue = 36),
        remap = false,
        require = 1)
    private int gtmpi$patternValidationBoundary(int original) {
        return PatternCapacity.expand(((MTEHatchCraftingInputME) (Object) this).mName, original);
    }
}
