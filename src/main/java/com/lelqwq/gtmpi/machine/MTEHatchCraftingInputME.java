package com.lelqwq.gtmpi.machine;

import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

/** All processing, inventories, persistence and AE integration are inherited from GTNH 2.9.0. */
@IMetaTileEntity.SkipGenerateDescription
public class MTEHatchCraftingInputME extends gregtech.common.tileentities.machines.MTEHatchCraftingInputME {

    public MTEHatchCraftingInputME(int id, String name, String regionalName, boolean fluids) {
        super(id, name, regionalName, fluids);
    }

    public MTEHatchCraftingInputME(String name, int tier, String[] description, ITexture[][][] textures,
        boolean fluids) {
        super(name, tier, description, textures, fluids);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new MTEHatchCraftingInputME(mName, mTier, mDescriptionArray, mTextures, supportsFluids());
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new MegaCraftingInputMEGui(this).build(data, syncManager, uiSettings);
    }

    @Override
    public int rows() {
        return 16;
    }

    @Override
    public int rowSize() {
        return 9;
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        PatternCapacity.migrateLegacySlots(mInventory, aNBT);
    }
}
