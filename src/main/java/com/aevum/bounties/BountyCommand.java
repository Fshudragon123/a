package com.aevum.bounties;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class BountyCommand implements CommandExecutor, TabCompleter {
    private final AevumBounties plugin;
    private final BountyManager manager;
    private final BountyGUI gui;

    public BountyCommand(AevumBounties plugin, BountyManager manager, BountyGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("aevumbounties.use")) {
            player.sendMessage(AevumBounties.color("&cYou don't have permission to use bounties."));
            return true;
        }

        // /bounty
        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        // /bounty top
        if (args[0].equalsIgnoreCase("top")) {
            if (args.length != 1) {
                usage(player);
                return true;
            }
            gui.openTop(player);
            return true;
        }

        // /bounty set <player> <amount>
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length != 3) {
                player.sendMessage(AevumBounties.color("&cUsage: &e/bounty set <player> <amount>"));
                return true;
            }
            return setBounty(player, args[1], args[2]);
        }

        // /bounty cancel [player]
        if (args[0].equalsIgnoreCase("cancel")) {
            if (args.length > 2) {
                player.sendMessage(AevumBounties.color("&cUsage: &e/bounty cancel [player]"));
                return true;
            }
            return cancelBounty(player, args.length == 2 ? args[1] : null);
        }

        usage(player);
        return true;
    }

    private boolean setBounty(Player player, String targetName, String amountText) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(AevumBounties.color("&cThat player is not online."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("settings.allow-self", false) && target.equals(player)) {
            player.sendMessage(AevumBounties.color("&cYou cannot place a bounty on yourself."));
            return true;
        }

        final double amount;
        try {
            amount = Double.parseDouble(amountText.replace(",", ""));
        } catch (NumberFormatException ex) {
            player.sendMessage(AevumBounties.color("&cInvalid amount. Example: &e/bounty set Steve 1000"));
            return true;
        }

        double min = minBounty();
        double max = maxBounty();

        if (!Double.isFinite(amount)) {
            player.sendMessage(AevumBounties.color("&cInvalid amount."));
            return true;
        }

        if (amount < min) {
            player.sendMessage(AevumBounties.color("&cThe minimum bounty is &6" + plugin.money(min) + "&c."));
            return true;
        }

        if (amount > max) {
            player.sendMessage(AevumBounties.color("&cThe maximum bounty is &6" + plugin.money(max) + "&c."));
            return true;
        }

        Bounty bounty = manager.create(player, target, amount);
        if (bounty == null) {
            player.sendMessage(AevumBounties.color("&cThe bounty could not be created. Make sure you have enough money."));
            return true;
        }

        player.sendMessage(AevumBounties.color(
                "&6&lBOUNTY &8» &a✓ Bounty placed on &e" + target.getName() +
                " &afor &6" + plugin.money(amount) + "&a."
        ));

        if (plugin.getConfig().getBoolean("settings.announce-new-bounties", true)) {
            plugin.announce("&6☠ &e" + player.getName() + " &7placed a &6" +
                    plugin.money(amount) + " &7bounty on &c" + target.getName() + "&7.");
        }
        return true;
    }

    private boolean cancelBounty(Player player, String targetName) {
        List<Bounty> mine = manager.byCreator(player.getUniqueId());

        if (mine.isEmpty()) {
            player.sendMessage(AevumBounties.color("&cYou don't have any active bounties."));
            return true;
        }

        Bounty match;

        if (targetName == null) {
            if (mine.size() > 1) {
                player.sendMessage(AevumBounties.color("&eYou have multiple bounties. Use &6/bounty cancel <player>&e."));
                for (Bounty b : mine) {
                    player.sendMessage(AevumBounties.color("&8• &f" + b.targetName() + " &8» &6" + plugin.money(b.amount())));
                }
                return true;
            }
            match = mine.get(0);
        } else {
            match = mine.stream()
                    .filter(b -> b.targetName().equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                player.sendMessage(AevumBounties.color("&cYou don't have a bounty on &e" + targetName + "&c."));
                return true;
            }
        }

        if (!manager.cancel(match.id(), player)) {
            player.sendMessage(AevumBounties.color("&cThe bounty could not be cancelled. No money was removed."));
            return true;
        }

        player.sendMessage(AevumBounties.color(
                "&a✓ Bounty on &e" + match.targetName() +
                " &ahas been cancelled and &6" + plugin.money(match.amount()) + " &ahas been refunded."
        ));
        return true;
    }

    private double minBounty() {
        if (plugin.getConfig().contains("settings.min-bounty"))
            return plugin.getConfig().getDouble("settings.min-bounty");
        return plugin.getConfig().getDouble("settings.minimum-bounty", 100.0);
    }

    private double maxBounty() {
        if (plugin.getConfig().contains("settings.max-bounty"))
            return plugin.getConfig().getDouble("settings.max-bounty");
        return plugin.getConfig().getDouble("settings.maximum-bounty", 1000000.0);
    }

    private void usage(Player player) {
        player.sendMessage(AevumBounties.color("&6&l✦ AEVUM BOUNTIES"));
        player.sendMessage(AevumBounties.color("&7/bounty &8» &fOpen the bounty board"));
        player.sendMessage(AevumBounties.color("&7/bounty top &8» &fView the bounty hall"));
        player.sendMessage(AevumBounties.color("&7/bounty set <player> <amount> &8» &fPost a bounty"));
        player.sendMessage(AevumBounties.color("&7/bounty cancel &8» &fCancel your only active bounty"));
        player.sendMessage(AevumBounties.color("&7/bounty cancel <player> &8» &fCancel a specific bounty"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return List.of("top", "set", "cancel").stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            return manager.byCreator(player.getUniqueId()).stream()
                    .map(Bounty::targetName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> !n.equalsIgnoreCase(player.getName()))
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .limit(50)
                    .toList();
        }

        return Collections.emptyList();
    }
}
