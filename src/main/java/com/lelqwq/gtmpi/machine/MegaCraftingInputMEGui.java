package com.lelqwq.gtmpi.machine;

import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

import appeng.api.implementations.ICraftingPatternItem;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.ProxiedMteGui.ProxiedMteGuiData;
import gregtech.common.gui.modularui.hatch.MTEHatchCraftingInputSlaveGui;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.PatternSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/**
 * MUI2 GUI for the giant crafting input hatch.
 *
 * <p>
 * The pattern inventory is split into pages of 9 columns × 4 rows. Only one page is visible at a time; the
 * prev/next buttons and the page indicator switch pages. This keeps the panel close to the native size instead
 * of expanding it to fit all 144 slots at once.
 */
public class MegaCraftingInputMEGui extends MTEHatchBaseGui<MTEHatchCraftingInputME> {

    private static final String PATTERN_INV_NAME = "gtmpi_pattern_inv";
    private static final String MANUAL_ITEM_INV_NAME = "gtmpi_manual_item_inv";
    private static final String MANUAL_PANEL_KEY = "gtmpi_manual_slots_panel";

    private static final int PATTERN_COLUMNS = 9;
    private static final int PATTERN_ROWS_PER_PAGE = 4;
    private static final int SLOTS_PER_PAGE = PATTERN_COLUMNS * PATTERN_ROWS_PER_PAGE;
    private static final int PAGE_COUNT = (PatternCapacity.PATTERNS + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;

    private static final int PANEL_WIDTH = 192;
    private static final int PANEL_HEIGHT = 184;

    private static final int MANUAL_SLOT_ROW = 3;
    private static final int MANUAL_SLOT_PER_ROW = 3;

    private final PagedWidget.Controller pageController = new PagedWidget.Controller();
    private final int[] currentPage = { 0 };
    private final ProxiedMteGuiData proxyData;

    public MegaCraftingInputMEGui(MTEHatchCraftingInputME machine) {
        this(machine, null);
    }

    public MegaCraftingInputMEGui(MTEHatchCraftingInputME machine, ProxiedMteGuiData proxyData) {
        super(machine);
        this.proxyData = proxyData;
    }

    @Override
    protected int getBasePanelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int getBasePanelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected boolean supportsMuffler() {
        return false;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        syncManager.registerSlotGroup(PATTERN_INV_NAME, PATTERN_ROWS_PER_PAGE);

        final PagedWidget<?> pages = new PagedWidget<>().controller(this.pageController)
            .onPageChange(index -> this.currentPage[0] = index)
            .size(SLOT_SIZE * PATTERN_COLUMNS + 4, SLOT_SIZE * PATTERN_ROWS_PER_PAGE)
            .horizontalCenter();

        for (int page = 0; page < PAGE_COUNT; page++) {
            pages.addPage(createPage(page, syncManager));
        }

        final ParentWidget<?> content = super.createContentSection(panel, syncManager);
        content.child(pages);
        return content;
    }

    private Grid createPage(int page, PanelSyncManager syncManager) {
        final int start = page * SLOTS_PER_PAGE;
        return new Grid().gridOfWidthHeight(PATTERN_COLUMNS, PATTERN_ROWS_PER_PAGE, ($x, $y, index) -> {
            final int slotIndex = start + index;
            if (slotIndex >= PatternCapacity.PATTERNS) {
                return new TextWidget<>(IKey.EMPTY).size(SLOT_SIZE);
            }
            return new PatternSlot().slot(
                new ModularSlot(machine.inventoryHandler, slotIndex)
                    .filter(itemStack -> itemStack != null && itemStack.getItem() instanceof ICraftingPatternItem)
                    .changeListener((itemStack, onlyAmount, client, init) -> {
                        if (!client && !init) {
                            machine.onPatternChange(slotIndex, itemStack);
                        }
                    })
                    .slotGroup(PATTERN_INV_NAME));
        })
            .size(SLOT_SIZE * PATTERN_COLUMNS + 4, SLOT_SIZE * PATTERN_ROWS_PER_PAGE);
    }

    private Flow createPageControls() {
        return Flow.row()
            .coverChildrenWidth()
            .height(18)
            .child(createPageButton("<", this.pageController::previousPage))
            .child(new TextWidget<>(IKey.dynamic(() -> (this.currentPage[0] + 1) + "/" + PAGE_COUNT)).width(20))
            .child(createPageButton(">", this.pageController::nextPage));
    }

    private ButtonWidget<?> createPageButton(String label, Runnable action) {
        return new ButtonWidget<>().size(14, 18)
            .overlay(IKey.str(label))
            .onMousePressed(mouseButton -> {
                action.run();
                return true;
            });
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager).child(createPageControls())
            .child(createOptimizerButton())
            .child(createShowPatternButton())
            .child(createExportButton())
            .child(createDoublePatternButton())
            .child(createManualItemsButton(syncManager));
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomRightCornerFlow(panel, syncManager).childIf(
            proxyData != null && proxyData.hasOrigin(),
            () -> MTEHatchCraftingInputSlaveGui.createRecipeOrderButton(proxyData.getOriginMTE())
                .size(SLOT_SIZE));
    }

    private ToggleButton createOptimizerButton() {
        BooleanSyncValue optimizerSync = new BooleanSyncValue(
            () -> !machine.disablePatternOptimization,
            val -> machine.disablePatternOptimization = !val).allowC2S();

        return new ToggleButton().value(optimizerSync)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_PATTERN_OPTIMIZE)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern"))
            .addTooltip(
                true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.enable"))
            .addTooltip(
                false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.disabled"));
    }

    private ToggleButton createShowPatternButton() {
        BooleanSyncValue showPatternSync = new BooleanSyncValue(
            () -> machine.showPattern,
            val -> machine.showPattern = val).allowC2S();

        return new ToggleButton().value(showPatternSync)
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_BLACKLIST)
            .addTooltip(
                true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.enable"))
            .addTooltip(
                false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.disabled"));
    }

    private ButtonWidget<?> createExportButton() {
        InteractionSyncHandler exportSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient() && mouseDelta.mouseButton == 0) {
                machine.refundAll(false);
            }
        });

        return new ButtonWidget<>().syncHandler(exportSyncHandler)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_EXPORT)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.export"));
    }

    private ButtonWidget<?> createDoublePatternButton() {
        InteractionSyncHandler doubleSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient()) {
                int val = mouseDelta.shift ? 1 : 0;
                if (mouseDelta.mouseButton == 1) val |= 0b10;
                machine.doublePatterns(val);
            }
        });

        return new ButtonWidget<>().syncHandler(doubleSyncHandler)
            .overlay(GTGuiTextures.OVERLAY_BUTTON_X2)
            .addTooltipLine(StatCollector.translateToLocal("gui.tooltips.appliedenergistics2.DoublePatterns"));
    }

    private ButtonWidget<?> createManualItemsButton(PanelSyncManager syncManager) {
        IPanelHandler popupPanel = syncManager
            .syncedPanel(MANUAL_PANEL_KEY, true, (manager, handler) -> createManualSlotUI(manager));

        return new ButtonWidget<>().overlay(GTGuiTextures.OVERLAY_BUTTON_PLUS_LARGE)
            .addTooltipLine(
                StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.place_manual_items"))
            .onMousePressed(mouseButton -> {
                popupPanel.openPanel();
                return popupPanel.isPanelOpen();
            });
    }

    private ModularPanel createManualSlotUI(PanelSyncManager syncManager) {
        return createPopUpPanel(MANUAL_PANEL_KEY).size(68, 76)
            .bottomRelOffset(0.5f, 52)
            .child(
                new ItemSlotGridBuilder(machine.inventoryHandler, syncManager)
                    .size(MANUAL_SLOT_PER_ROW, MANUAL_SLOT_ROW)
                    .slotGroupKey(MANUAL_ITEM_INV_NAME)
                    .indexOffset(PatternCapacity.MANUAL_START)
                    .build()
                    .marginTop(16)
                    .horizontalCenter());
    }
}
