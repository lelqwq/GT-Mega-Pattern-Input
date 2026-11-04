package com.lelqwq.gtmpi.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/** Only the giant machines use expanded pattern storage. Shared slots retain their original semantics. */
public final class PatternCapacity {

    /** 4 pages of 9 columns × 4 rows. */
    public static final int PATTERNS = 144;

    /** Circuit slot index; directly follows the pattern slots. */
    public static final int CIRCUIT = PATTERNS;

    /** Number of shared/manual slots. */
    public static final int MANUAL_SLOTS = 9;

    /** First shared/manual slot index. */
    public static final int MANUAL_START = PATTERNS + 1;

    /** Pattern slots + circuit slot + shared slots. */
    public static final int INVENTORY_SIZE = MANUAL_START + MANUAL_SLOTS;

    /** Old 128-slot layout (from 1.1.1 and earlier). */
    public static final int LEGACY_CIRCUIT = 128;
    public static final int LEGACY_MANUAL_START = LEGACY_CIRCUIT + 1;

    /** Even older 4x8 layout, migrated by GT5U itself before this class sees it. */
    public static final int LEGACY_4X8_CIRCUIT = 4 * 8;
    public static final int LEGACY_4X8_MANUAL_START = LEGACY_4X8_CIRCUIT + 1;

    private static final int EXTRA_PATTERNS = PATTERNS - 36;

    private PatternCapacity() {}

    public static boolean isMega(String name) {
        return name != null && name.startsWith("gtmpi.hatch.crafting_input.mega.");
    }

    public static int expand(String name, int original) {
        return isMega(name) ? original + EXTRA_PATTERNS : original;
    }

    public static boolean canLink(String proxyName, String masterName) {
        return isMega(proxyName) == isMega(masterName);
    }

    /**
     * Moves circuit/shared items from the old slot indices to the new ones.
     *
     * <p>
     * GT5U already migrates the 4x8 -> 4x9 layout itself. This method covers both that leftover and the
     * 128 -> 144 layout used by earlier releases of this mod. Pattern slots are never moved; a legacy slot is
     * only treated as a circuit/shared slot when the NBT data contains no pattern at that index.
     */
    public static void migrateLegacySlots(ItemStack[] inventory, NBTTagCompound nbt) {
        if (inventory == null || nbt == null) {
            return;
        }

        final boolean[] hasPattern = new boolean[PATTERNS];
        final NBTTagList patterns = nbt.getTagList("internalInventory", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < patterns.tagCount(); i++) {
            final int slot = patterns.getCompoundTagAt(i)
                .getInteger("patternSlot");
            if (slot >= 0 && slot < hasPattern.length) {
                hasPattern[slot] = true;
            }
        }

        migrateSlotRange(
            inventory,
            hasPattern,
            LEGACY_4X8_CIRCUIT,
            CIRCUIT,
            LEGACY_4X8_MANUAL_START,
            MANUAL_START,
            MANUAL_SLOTS);
        migrateSlotRange(
            inventory,
            hasPattern,
            LEGACY_CIRCUIT,
            CIRCUIT,
            LEGACY_MANUAL_START,
            MANUAL_START,
            MANUAL_SLOTS);
    }

    private static void migrateSlotRange(ItemStack[] inventory, boolean[] hasPattern, int oldCircuit, int newCircuit,
        int oldManualStart, int newManualStart, int manualSlots) {
        if (oldCircuit >= 0 && oldCircuit < hasPattern.length
            && !hasPattern[oldCircuit]
            && inventory[oldCircuit] != null
            && inventory[newCircuit] == null) {
            inventory[newCircuit] = inventory[oldCircuit];
            inventory[oldCircuit] = null;
        }

        for (int i = 0; i < manualSlots; i++) {
            final int oldIndex = oldManualStart + i;
            final int newIndex = newManualStart + i;
            if (oldIndex < hasPattern.length && !hasPattern[oldIndex]
                && inventory[oldIndex] != null
                && inventory[newIndex] == null) {
                inventory[newIndex] = inventory[oldIndex];
                inventory[oldIndex] = null;
            }
        }
    }
}
