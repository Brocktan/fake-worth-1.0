package com.fakeworth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Config file: .minecraft/config/fakeworth.json */
public class FakeWorthConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakeworth.json");

    /** Master switch. */
    public boolean enabled = true;

    /**
     * false = the value is shown as-is for whatever stack you hover (7M stays 7M).
     * true  = the value is per item and gets multiplied by the stack size.
     */
    public boolean valuesArePerItem = false;

    /** item id -> fake worth. Accepts numbers like 7M, 30M, 221M, 1.5B, 250K. */
    public Map<String, String> values = new LinkedHashMap<>();

    private transient Map<String, Double> parsed = new HashMap<>();

    public static FakeWorthConfig load() {
        FakeWorthConfig cfg = new FakeWorthConfig();
        try {
            if (Files.exists(FILE)) {
                FakeWorthConfig read = GSON.fromJson(Files.readString(FILE), FakeWorthConfig.class);
                if (read != null) cfg = read;
            } else {
                cfg.values.put("minecraft:purpur_block", "7M");
                cfg.values.put("minecraft:gold_ingot", "30M");
                cfg.values.put("minecraft:gold_block", "221M");
                cfg.save();
            }
        } catch (Exception e) {
            System.err.println("[fakeworth] could not read config, using defaults: " + e);
            cfg.values.put("minecraft:purpur_block", "7M");
            cfg.values.put("minecraft:gold_ingot", "30M");
            cfg.values.put("minecraft:gold_block", "221M");
        }
        cfg.reparse();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[fakeworth] could not save config: " + e);
        }
    }

    private void reparse() {
        parsed = new HashMap<>();
        if (values == null) values = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            Double d = WorthFormat.parse(e.getValue());
            if (d != null) parsed.put(e.getKey(), d);
        }
    }

    public static String id(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    /** The configured fake value for this stack's item, or null if it is not faked. */
    public Double valueFor(ItemStack stack) {
        return parsed.get(id(stack));
    }

    /** Fake value of the whole stack. */
    public double stackValue(double configured, ItemStack stack) {
        return valuesArePerItem ? configured * stack.getCount() : configured;
    }
}
