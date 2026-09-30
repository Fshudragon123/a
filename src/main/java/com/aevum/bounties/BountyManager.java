package com.aevum.bounties;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class BountyManager {
    private final AevumBounties plugin;
    private final Economy economy;
    private final Map<UUID, Bounty> bounties = new LinkedHashMap<>();
    private final File file;

    public BountyManager(AevumBounties plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public Economy economy() { return economy; }
    public Collection<Bounty> all() { return Collections.unmodifiableCollection(bounties.values()); }
    public Optional<Bounty> byId(UUID id) { return Optional.ofNullable(bounties.get(id)); }
    public List<Bounty> byTarget(UUID target) {
        return bounties.values().stream().filter(b -> b.target().equals(target)).toList();
    }
    public List<Bounty> byCreator(UUID creator) {
        return bounties.values().stream().filter(b -> b.creator().equals(creator)).toList();
    }
    public double total() { return bounties.values().stream().mapToDouble(Bounty::amount).sum(); }

    public Bounty create(Player creator, Player target, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !economy.has(creator, amount)) return null;
        EconomyResponse withdrawal = economy.withdrawPlayer(creator, amount);
        if (!withdrawal.transactionSuccess()) return null;

        Bounty bounty = new Bounty(UUID.randomUUID(), target.getUniqueId(), target.getName(),
                creator.getUniqueId(), creator.getName(), amount, System.currentTimeMillis());
        bounties.put(bounty.id(), bounty);

        if (!save()) {
            bounties.remove(bounty.id());
            economy.depositPlayer(creator, amount);
            return null;
        }
        return bounty;
    }

    public boolean cancel(UUID id, Player requester) {
        Bounty bounty = bounties.get(id);
        if (bounty == null || !bounty.creator().equals(requester.getUniqueId())) return false;

        bounties.remove(id);
        if (!save()) {
            bounties.put(id, bounty);
            return false;
        }

        EconomyResponse refund = economy.depositPlayer(requester, bounty.amount());
        if (!refund.transactionSuccess()) {
            plugin.getLogger().severe("Bounty refund failed for " + requester.getName() + " (" + id + "): " + refund.errorMessage);
            return false;
        }
        return true;
    }

    public Bounty claim(Player hunter, Player target) {
        List<Bounty> hits = byTarget(target.getUniqueId()).stream()
                .filter(b -> !b.creator().equals(hunter.getUniqueId()))
                .toList();
        if (hits.isEmpty()) return null;

        double total = hits.stream().mapToDouble(Bounty::amount).sum();
        Map<UUID, Bounty> removed = new LinkedHashMap<>();
        for (Bounty bounty : hits) {
            removed.put(bounty.id(), bounty);
            bounties.remove(bounty.id());
        }

        if (!save()) {
            bounties.putAll(removed);
            return null;
        }

        EconomyResponse reward = economy.depositPlayer(hunter, total);
        if (!reward.transactionSuccess()) {
            bounties.putAll(removed);
            save();
            plugin.getLogger().severe("Bounty reward payment failed for " + hunter.getName() + ": " + reward.errorMessage);
            return null;
        }

        return new Bounty(UUID.randomUUID(), target.getUniqueId(), target.getName(),
                hunter.getUniqueId(), hunter.getName(), total, System.currentTimeMillis());
    }

    public List<Bounty> top() {
        return bounties.values().stream()
                .sorted(Comparator.comparingDouble(Bounty::amount).reversed()
                        .thenComparingLong(Bounty::createdAt))
                .limit(45)
                .collect(Collectors.toList());
    }

    private void load() {
        if (!file.exists()) { save(); return; }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("bounties");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection x = section.getConfigurationSection(key);
                if (x == null) continue;
                UUID id = UUID.fromString(key);
                bounties.put(id, new Bounty(id,
                        UUID.fromString(Objects.requireNonNull(x.getString("target"))),
                        x.getString("targetName", "Unknown"),
                        UUID.fromString(Objects.requireNonNull(x.getString("creator"))),
                        x.getString("creatorName", "Unknown"),
                        x.getDouble("amount"), x.getLong("createdAt")));
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipped malformed bounty " + key + ": " + ex.getMessage());
            }
        }
    }

    public boolean save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Bounty b : bounties.values()) {
            String key = "bounties." + b.id();
            config.set(key + ".target", b.target().toString());
            config.set(key + ".targetName", b.targetName());
            config.set(key + ".creator", b.creator().toString());
            config.set(key + ".creatorName", b.creatorName());
            config.set(key + ".amount", b.amount());
            config.set(key + ".createdAt", b.createdAt());
        }
        try {
            file.getParentFile().mkdirs();
            config.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save bounty data: " + ex.getMessage());
            return false;
        }
    }
}