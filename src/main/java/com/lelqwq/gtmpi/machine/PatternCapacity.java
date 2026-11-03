package com.lelqwq.gtmpi.machine;

/** Only the giant machines use expanded pattern storage. Shared slots retain their original semantics. */
public final class PatternCapacity {

    public static final int PATTERNS = 128;
    public static final int MANUAL_START = PATTERNS + 1;
    public static final int INVENTORY_SIZE = MANUAL_START + 9;
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
}
