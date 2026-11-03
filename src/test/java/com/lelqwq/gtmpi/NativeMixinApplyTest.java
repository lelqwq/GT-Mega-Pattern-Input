package com.lelqwq.gtmpi;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/** Applies the packaged injections to real GT bytecode without starting Minecraft. */
public final class NativeMixinApplyTest {

    public static void main(String[] args) throws Exception {
        String[] entries = System.getProperty("java.class.path")
            .split(File.pathSeparator);
        URL[] urls = new URL[entries.length];
        for (int i = 0; i < entries.length; i++) urls[i] = new File(entries[i]).toURI()
            .toURL();
        String runtimeJar = System.getProperty("gtmpi.runtimeGtJar");
        if (runtimeJar != null) {
            java.util.List<URL> runtimeUrls = new java.util.ArrayList<>();
            runtimeUrls.add(
                new File(System.getProperty("gtmpi.productionJar")).toURI()
                    .toURL());
            runtimeUrls.add(
                new File(runtimeJar).toURI()
                    .toURL());
            runtimeUrls.addAll(java.util.Arrays.asList(urls));
            urls = runtimeUrls.toArray(new URL[0]);
        }
        Launch.classLoader = new LaunchClassLoader(urls);
        Launch.blackboard = new HashMap<>();
        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.");
        // This is a bytecode-only check; do not start FML core plugins or the game launcher.
        var agents = Class.forName("org.spongepowered.asm.launch.platform.MixinContainer")
            .getDeclaredField("agentClasses");
        agents.setAccessible(true);
        ((java.util.List<?>) agents.get(null)).clear();
        var bootstrap = MixinBootstrap.class.getDeclaredMethod("start");
        bootstrap.setAccessible(true);
        bootstrap.invoke(null);
        MixinEnvironment.getDefaultEnvironment()
            .setSide(args.length == 0 ? MixinEnvironment.Side.SERVER : MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("mixins.gtmpi.json");
        var phase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        phase.setAccessible(true);
        phase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        Object transformer = MixinEnvironment.getDefaultEnvironment()
            .getActiveTransformer();
        if (transformer == null) {
            var ctor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer")
                .getDeclaredConstructor();
            ctor.setAccessible(true);
            transformer = ctor.newInstance();
        }
        var transform = transformer.getClass()
            .getMethod("transformClassBytes", String.class, String.class, byte[].class);
        transform.setAccessible(true);
        String[] targets = args.length == 0
            ? new String[] { "gregtech.common.tileentities.machines.MTEHatchCraftingInputME",
                "gregtech.common.tileentities.machines.MTEHatchCraftingInputSlave" }
            : new String[] { "gregtech.common.gui.modularui.hatch.MTEHatchCraftingInputMEGui" };
        for (String target : targets) {
            try (InputStream input = Launch.classLoader.getResourceAsStream(target.replace('.', '/') + ".class")) {
                byte[] before = org.apache.commons.io.IOUtils.toByteArray(input);
                byte[] after = (byte[]) transform.invoke(transformer, target, target, before);
                if (java.util.Arrays.equals(before, after)) throw new AssertionError("Mixin not applied: " + target);
                System.out
                    .println("PASS applied actual Mixin to " + target + ": " + before.length + " -> " + after.length);
            }
        }
    }
}
