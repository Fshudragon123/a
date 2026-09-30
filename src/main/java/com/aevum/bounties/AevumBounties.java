package com.aevum.bounties;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AevumBounties extends JavaPlugin {
    private Economy economy;
    private BountyManager bountyManager;
    private BountyGUI bountyGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("data.yml", false);

        if (!setupEconomy()) {
            getLogger().severe("Vault economy provider not found. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bountyManager = new BountyManager(this, economy);
        bountyManager.load();

        bountyGUI = new BountyGUI(this, bountyManager);
        BountyCommand command = new BountyCommand(this, bountyManager, bountyGUI);

        getCommand("bounty").setExecutor(command);
        getCommand("bounty").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(
                new BountyListener(this, bountyManager, bountyGUI), this
        );

        long ticks = Math.max(20L, getConfig().getLong("settings.autosave-seconds", 30) * 20L);
        Bukkit.getScheduler().runTaskTimer(this, bountyManager::save, ticks, ticks);

        getLogger().info("AevumBounties enabled — the hunt is open.");
    }

    @Override
    public void onDisable() {
        if (bountyManager != null) bountyManager.save();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
}
