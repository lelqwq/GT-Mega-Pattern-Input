package com.lelqwq.gtmpi;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.lelqwq.gtmpi.machine.PatternCapacity;

/** Offline regression checks against the actual pinned GT dependency; no game or world is started. */
public final class NativeReuseSelfTest {

    private static final String GT = "gregtech/common/tileentities/machines/";

    public static void main(String[] args) throws Exception {
        String mega = "gtmpi.hatch.crafting_input.mega.buffer";
        String proxy = "gtmpi.hatch.crafting_input.mega.proxy";
        String nativeName = "hatch.crafting_input.buffer";
        for (String name : new String[] { mega, "gtmpi.hatch.crafting_input.mega.bus" }) {
            check(PatternCapacity.expand(name, 36) == 128, "128 pattern slots");
            check(PatternCapacity.expand(name, 37) == 129, "manual inventory starts at 129");
            check(PatternCapacity.expand(name, 45) == 137, "manual inventory ends at 137");
            check(PatternCapacity.expand(name, 46) == 138, "138 total slots");
        }
        check(PatternCapacity.expand(nativeName, 36) == 36, "native capacity unchanged");
        check(PatternCapacity.expand(nativeName, 37) == 37, "native shared slots unchanged");
        check(!PatternCapacity.isMega(null), "missing names are not giant machines");
        check(PatternCapacity.canLink(proxy, mega), "giant mirror links to giant bus/buffer");
        check(PatternCapacity.canLink(nativeName, nativeName), "native links remain supported");
        check(!PatternCapacity.canLink(proxy, nativeName), "giant mirror cannot link to native bus");
        check(!PatternCapacity.canLink(nativeName, mega), "native mirror cannot link to giant bus");

        ClassNode nativeBus = read(GT + "MTEHatchCraftingInputME");
        int constructors = 0;
        for (MethodNode method : nativeBus.methods) {
            if (method.name.equals("<init>")) {
                constructors++;
                check(constants(method, set(36, 46)) == 3, "native constructor allocation sites changed");
            }
        }
        check(constructors == 2, "native constructors changed");
        for (String name : new String[] { "getCircuitSlot", "getSharedItems", "getNameSuffix", "getWailaNameLines",
            "updateSlots", "loadNBTData", "createSlotManualWindow", "setInventorySlotContents",
            "isItemValidForSlot" }) {
            MethodNode method = find(nativeBus, name);
            check(constants(method, set(36, 37, 45, 46)) > 0, "missing capacity patch site: " + name);
        }
        ClassNode gui = read("gregtech/common/gui/modularui/hatch/MTEHatchCraftingInputMEGui");
        check(constants(find(gui, "createSlots"), set(4, 9)) == 3, "native grid patch sites changed");
        check(constants(find(gui, "createManualSlotUI"), set(37)) == 1, "manual popup offset changed");

        ClassNode giant = read("com/lelqwq/gtmpi/machine/MTEHatchCraftingInputME");
        check(giant.superName.equals(GT + "MTEHatchCraftingInputME"), "must inherit native implementation");
        Set<String> allowed = set(
            "<init>",
            "newMetaEntity",
            "rows",
            "rowSize",
            "getGUIWidth",
            "getGUIHeight",
            "getCircuitSlotX",
            "addUIWidgets");
        for (MethodNode method : giant.methods) {
            check(
                allowed.contains(method.name) || method.name.startsWith("lambda$"),
                "giant overrides native behavior outside layout: " + method.name);
        }
        ClassNode loader = read("com/lelqwq/gtmpi/loader/MachineLoader");
        boolean nativeMirror = false;
        for (AbstractInsnNode insn : find(loader, "registerMachines").instructions.toArray()) {
            if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                && type.desc.equals(GT + "MTEHatchCraftingInputSlave")) nativeMirror = true;
        }
        check(nativeMirror, "mirror must reuse the original class");

        check(GTInventoryContracts(), "native input quantities must stay long before splitting");
        System.out.println(
            "PASS: native inheritance; mirror reuse/isolation; unchanged native capacity; all constructor, inventory and GUI patch sites; giant NBT slot layout; native long-quantity handling.");
    }

    private static boolean GTInventoryContracts() throws Exception {
        ClassNode slot = read(GT + "MTEHatchCraftingInputME$PatternSlot");
        boolean hasItemLong = false, hasFluidLong = false;
        for (MethodNode method : slot.methods) {
            if (method.name.equals("insertItem")) hasItemLong = method.desc.contains("IAEItemStack;");
            if (method.name.equals("insertFluid")) hasFluidLong = method.desc.contains("IAEFluidStack;");
        }
        return hasItemLong && hasFluidLong;
    }

    private static MethodNode find(ClassNode node, String name) {
        return node.methods.stream()
            .filter(m -> m.name.equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing native method: " + name));
    }

    private static ClassNode read(String name) throws Exception {
        try (InputStream input = NativeReuseSelfTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            check(input != null, "Missing dependency class: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int constants(MethodNode method, Set<Integer> wanted) {
        int count = 0;
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            Integer value = null;
            if (insn instanceof IntInsnNode i && i.getOpcode() != Opcodes.NEWARRAY) value = i.operand;
            else if (insn instanceof LdcInsnNode i && i.cst instanceof Integer integer) value = integer;
            else if (insn.getOpcode() >= Opcodes.ICONST_M1 && insn.getOpcode() <= Opcodes.ICONST_5)
                value = insn.getOpcode() - Opcodes.ICONST_0;
            if (wanted.contains(value)) count++;
        }
        return count;
    }

    @SafeVarargs
    private static <T> Set<T> set(T... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
