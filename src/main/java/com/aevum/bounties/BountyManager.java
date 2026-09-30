package com.aevum.bounties;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BountyManager {
    private final AevumBounties plugin;
    private final Economy economy;
    private final Map<UUID, Bounty> bounties = new LinkedHashMap<>();
    private final File file;
    private FileConfiguration data;

    public BountyManager(AevumBounties plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        data = YamlConfiguration.loadConfiguration(file);
        bounties.clear();
        ConfigurationSection section = data.getConfigurationSection("bounties");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID target = UUID.fromString(key);
                String path = "bounties." + key;
                bounties.put(target, new Bounty(
                        target,
                        data.getString(path + ".target-name", "Unknown"),
                        UUID.fromString(data.getString(path + ".creator")),
                        data.getString(path + ".creator-name", "Unknown"),
                        data.getDouble(path + ".amount"),
                        data.getLong(path + ".created-at")
                ));
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipped malformed bounty: " + key);
            }
        }
    }

    public synchronized boolean create(UUID creator, String creatorName, UUID target, String targetName, double amount) {
        if (creator.equals(target) || bounties.containsKey(target)) return false;
        if (!economy.has(Bukkit.getOfflinePlayer(creator), amount)) return false;
        economy.withdrawPlayer(Bukkit.getOfflinePlayer(creator), amount);
        bounties.put(target, new Bounty(target, targetName, creator, creatorName, amount, System.currentTimeMillis()));
        save();
        return true;
    }

    public synchronized Bounty remove(UUID target) {
        Bounty bounty = bounties.remove(target);
        save();
        return bounty;
    }

    public synchronized boolean cancel(UUID creator, UUID target) {
        Bounty bounty = bounties.get(target);
        if (bounty == null || !bounty.creator().equals(creator)) return false;
        bounties.remove(target);
        economy.depositPlayer(Bukkit.getOfflinePlayer(creator), bounty.amount());
        save();
        return true;
    }

    public synchronized Collection<Bounty> all() {
        return List.copyOf(bounties.values());
    }

    public synchronized List<Bounty> top(int limit) {
        return bounties.values().stream()
                .sorted(Comparator.comparingDouble(Bounty::amount).reversed())
                .limit(limit)
                .toList();
    }

    public synchronized Bounty get(UUID target) {
        return bounties.get(target);
    }

    public Economy economy() {
        return economy;
    }

    public synchronized void save() {
        if (data == null) data = new YamlConfiguration();
        data.set("bounties", null);
        for (Bounty b : bounties.values()) {
            String path = "bounties." + b.target();
            data.set(path + ".target-name", b.targetName());
            data.set(path + ".creator", b.creator().toString());
            data.set(path + ".creator-name", b.creatorName());
            data.set(path + ".amount", b.amount());
            data.set(path + ".created-at", b.createdAt());
        }
        try {
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save bounties.yml: " + ex.getMessage());
        }
    }
}
