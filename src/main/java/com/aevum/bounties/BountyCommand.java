package com.aevum.bounties;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class BountyCommand implements CommandExecutor, TabCompleter {
    private final AevumBounties plugin;
    private final BountyManager manager;
    private final BountyGUI gui;

    public BountyCommand(AevumBounties p, BountyManager m, BountyGUI g) {
        plugin = p; manager = m; gui = g;
    }

    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (!p.hasPermission("aevumbounties.use")) {
            p.sendMessage(AevumBounties.color("&cYou don't have permission."));
            return true;
        }

        // Standalone /cancel command.
        if (label.equalsIgnoreCase("cancel")) {
            return cancelCommand(p, a);
        }

        if (a.length == 0) { gui.open(p); return true; }
        if (a[0].equalsIgnoreCase("top")) { gui.openTop(p); return true; }
        if (a[0].equalsIgnoreCase("cancel")) return cancelCommand(p, Arrays.copyOfRange(a, 1, a.length));

        if (a.length == 2) {
            Player target = Bukkit.getPlayerExact(a[0]);
            if (target == null) {
                p.sendMessage(AevumBounties.color("&cThat player is not online."));
                return true;
            }

            double amount;
            try { amount = Double.parseDouble(a[1]); }
            catch (Exception e) {
                p.sendMessage(AevumBounties.color("&cInvalid amount."));
                return true;
            }

            double min = plugin.getConfig().getDouble("settings.min-bounty", 100.0);
            double max = plugin.getConfig().getDouble("settings.max-bounty", 1000000.0);

            if (amount < min || amount > max || !Double.isFinite(amount)) {
                p.sendMessage(AevumBounties.color("&cAmount must be between &6" + plugin.money(min) + " &cand &6" + plugin.money(max) + "&c."));
                return true;
            }

            if (!plugin.getConfig().getBoolean("settings.allow-self", false) && target.equals(p)) {
                p.sendMessage(AevumBounties.color("&cYou cannot bounty yourself."));
                return true;
            }

            Bounty b = manager.create(p, target, amount);
            if (b == null) {
                p.sendMessage(AevumBounties.color("&cNot enough money or the transaction could not be completed."));
                return true;
            }

            p.sendMessage(AevumBounties.color("&a✓ Bounty placed on &e" + target.getName() + " &afor &6" + plugin.money(amount) + "&a."));
            if (plugin.getConfig().getBoolean("settings.announce-new-bounties", true))
                plugin.announce("&6☠ &e" + p.getName() + " &7placed a &6" + plugin.money(amount) + " &7bounty on &c" + target.getName() + "&7.");
            return true;
        }

        p.sendMessage(AevumBounties.color("&6&lBOUNTY &8» &7/bounty &8| &7/bounty top &8| &7/bounty <player> <amount> &8| &7/cancel [player]"));
        return true;
    }

    private boolean cancelCommand(Player p, String[] args) {
        List<Bounty> mine = manager.byCreator(p.getUniqueId());

        if (args.length == 0) {
            if (mine.size() == 1) {
                Bounty b = mine.get(0);
                if (manager.cancel(b.id(), p)) {
                    p.sendMessage(AevumBounties.color("&a✓ Bounty on &e" + b.targetName() + " &ahas been cancelled and fully refunded."));
                } else {
                    p.sendMessage(AevumBounties.color("&cThe bounty could not be cancelled."));
                }
                return true;
            }

            if (mine.isEmpty()) {
                p.sendMessage(AevumBounties.color("&cYou don't have any active bounties."));
            } else {
                p.sendMessage(AevumBounties.color("&eYou have multiple active bounties. Use &6/cancel <player>&e."));
                for (Bounty b : mine)
                    p.sendMessage(AevumBounties.color("&8• &f" + b.targetName() + " &8» &6" + plugin.money(b.amount())));
            }
            return true;
        }

        String targetName = args[0];
        Bounty match = mine.stream().filter(b -> b.targetName().equalsIgnoreCase(targetName)).findFirst().orElse(null);

        if (match == null) {
            try {
                UUID id = UUID.fromString(targetName);
                match = manager.byId(id).filter(b -> b.creator().equals(p.getUniqueId())).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }

        if (match == null) {
            p.sendMessage(AevumBounties.color("&cYou don't have a bounty matching &e" + targetName + "&c."));
            return true;
        }

        if (manager.cancel(match.id(), p)) {
            p.sendMessage(AevumBounties.color("&a✓ Bounty on &e" + match.targetName() + " &ahas been cancelled and fully refunded."));
        } else {
            p.sendMessage(AevumBounties.color("&cThe bounty could not be cancelled."));
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (l.equalsIgnoreCase("cancel")) {
            if (a.length == 1 && s instanceof Player p)
                return manager.byCreator(p.getUniqueId()).stream().map(Bounty::targetName).toList();
            return Collections.emptyList();
        }
        if (a.length == 1) return List.of("top", "cancel");
        return Collections.emptyList();
    }
}
