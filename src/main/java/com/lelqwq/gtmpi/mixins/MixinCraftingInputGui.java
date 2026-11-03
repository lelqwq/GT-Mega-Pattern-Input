package com.lelqwq.gtmpi.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.lelqwq.gtmpi.machine.PatternCapacity;

import gregtech.common.gui.modularui.hatch.MTEHatchCraftingInputMEGui;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/** Retain the native MUI2 widgets and actions; expand only their layout. */
@Mixin(value = MTEHatchCraftingInputMEGui.class, remap = false)
public abstract class MixinCraftingInputGui extends MTEHatchBaseGui<MTEHatchCraftingInputME> {

    protected MixinCraftingInputGui(MTEHatchCraftingInputME machine) {
        super(machine);
    }

    @ModifyConstant(
        method = "createSlots",
        constant = { @Constant(intValue = 4), @Constant(intValue = 9) },
        require = 3)
    private int gtmpi$patternGrid(int original) {
        if (!PatternCapacity.isMega(machine.mName)) return original;
        return original == 4 ? 8 : 16;
    }

    @ModifyConstant(method = "createManualSlotUI", constant = @Constant(intValue = 37), require = 1)
    private int gtmpi$manualSlots(int original) {
        return PatternCapacity.expand(machine.mName, original);
    }

    @Inject(method = "getBasePanelHeight", at = @At("RETURN"), cancellable = true, require = 1)
    private void gtmpi$expandedHeight(CallbackInfoReturnable<Integer> cir) {
        if (PatternCapacity.isMega(machine.mName)) cir.setReturnValue(cir.getReturnValueI() + 72);
    }

    @Override
    protected int getBasePanelWidth() {
        return super.getBasePanelWidth() + (PatternCapacity.isMega(machine.mName) ? 126 : 0);
    }
}
