package com.lelqwq.gtmpi.machine;

import java.util.Collections;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.CycleButtonWidget;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotGroup;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.items.misc.ItemEncodedPattern;
import gregtech.api.enums.SoundResource;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

/** All processing, inventories, persistence and AE integration are inherited from GTNH 2.9.0. */
@IMetaTileEntity.SkipGenerateDescription
public class MTEHatchCraftingInputME extends gregtech.common.tileentities.machines.MTEHatchCraftingInputME {

    private static final int MAX_PATTERN_COUNT = PatternCapacity.PATTERNS;
    private static final int MANUAL_SLOT_WINDOW = 10;

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
    public int rows() {
        return 16;
    }

    @Override
    public int rowSize() {
        return 8;
    }

    @Override
    public int getGUIWidth() {
        return super.getGUIWidth() + 126;
    }

    @Override
    public int getGUIHeight() {
        return super.getGUIHeight() + 72;
    }

    @Override
    public int getCircuitSlotX() {
        return super.getCircuitSlotX() + 126;
    }

    @Override
    public void addUIWidgets(ModularWindow.@NotNull Builder builder, UIBuildContext buildContext) {
        buildContext.addSyncedWindow(MANUAL_SLOT_WINDOW, this::createSlotManualWindow);
        builder.widget(
            SlotGroup.ofItemHandler(inventoryHandler, 16)
                .startFromSlot(0)
                .endAtSlot(MAX_PATTERN_COUNT - 1)
                .phantom(false)
                .background(getGUITextureSet().getItemSlot(), GTUITextures.OVERLAY_SLOT_PATTERN_ME)
                .widgetCreator(slot -> new SlotWidget(slot) {

                    @Override
                    protected ItemStack getItemStackForRendering(Slot slotIn) {
                        ItemStack stack = slot.getStack();
                        if (stack == null || !(stack.getItem() instanceof ItemEncodedPattern patternItem)) {
                            return stack;
                        }
                        ItemStack output = patternItem.getOutput(stack);
                        return output != null ? output : stack;
                    }
                }.setFilter(itemStack -> itemStack.getItem() instanceof ICraftingPatternItem)
                    .setChangeListener(() -> onPatternChange(slot.getSlotIndex(), slot.getStack())))
                .build()
                .setPos(7, 9))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    widget.getContext()
                        .openSyncedWindow(MANUAL_SLOT_WINDOW);
                }
            })
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_PLUS_LARGE)
                .addTooltip(
                    StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.place_manual_items"))
                .setSize(16, 16)
                .setPos(296, 46))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> {
                if (clickData.mouseButton == 0) {
                    refundAll(false);
                }
            })
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_EXPORT)
                .addTooltip(StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.export"))
                .setSize(16, 16)
                .setPos(296, 28))
            .widget(
                new CycleButtonWidget()
                    .setToggle(() -> disablePatternOptimization, val -> disablePatternOptimization = val)
                    .setStaticTexture(GTUITextures.OVERLAY_BUTTON_PATTERN_OPTIMIZE)
                    .setVariableBackground(GTUITextures.BUTTON_STANDARD_TOGGLE)
                    .addTooltip(0, "Pattern Optimization:\n§7Allowed")
                    .addTooltip(1, "Pattern Optimization:\n§7Disabled")
                    .setPos(296, 10)
                    .setSize(16, 16))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> {
                int val = clickData.shift ? 1 : 0;
                if (clickData.mouseButton == 1) val |= 0b10;
                doublePatterns(val);
            })
                .setPlayClickSound(true)
                .setBackground(GTUITextures.BUTTON_STANDARD, GTUITextures.OVERLAY_BUTTON_X2)
                .addTooltip(StatCollector.translateToLocal("gui.tooltips.appliedenergistics2.DoublePatterns"))
                .setSize(16, 16)
                .setPos(320, 10))
            .widget(
                new ButtonWidget().setOnClick((clickData, widget) -> showPattern = !showPattern)
                    .setPlayClickSoundResource(
                        () -> showPattern ? SoundResource.GUI_BUTTON_UP.resourceLocation
                            : SoundResource.GUI_BUTTON_DOWN.resourceLocation)
                    .setBackground(() -> {
                        if (showPattern) {
                            return new IDrawable[] { GTUITextures.BUTTON_STANDARD_PRESSED,
                                GTUITextures.OVERLAY_BUTTON_WHITELIST };
                        } else {
                            return new IDrawable[] { GTUITextures.BUTTON_STANDARD,
                                GTUITextures.OVERLAY_BUTTON_BLACKLIST };
                        }
                    })
                    .attachSyncer(
                        new FakeSyncWidget.BooleanSyncer(() -> showPattern, val -> showPattern = val),
                        builder)
                    .dynamicTooltip(
                        () -> Collections.singletonList(
                            StatCollector.translateToLocal(
                                "GT5U.infodata.hatch.crafting_input_me.show_pattern."
                                    + (showPattern ? "enable" : "disabled"))))
                    .setTooltipShowUpDelay(gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY)
                    .setUpdateTooltipEveryTick(true)
                    .setPos(320, 28)
                    .setSize(16, 16));
    }

}
