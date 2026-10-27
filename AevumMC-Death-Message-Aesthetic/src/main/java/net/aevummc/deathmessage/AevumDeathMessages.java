package net.aevummc.deathmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DamageCause;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AevumDeathMessages extends JavaPlugin implements Listener {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AevumMC Death Message Aesthetic enabled.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        String key = selectMessageKey(event, killer);
        String template = getConfig().getString("messages." + key,
                getConfig().getString("messages.generic",
                        "<#C98B24>✦ <#E0A63A><victim> <#8B8B95>has fallen from the empire."));

        String rendered = template
                .replace("<victim>", victim.getName())
                .replace("<killer>", killer == null ? "" : killer.getName());

        Component message = miniMessage.deserialize(rendered);
        event.deathMessage(message);
    }

    private String selectMessageKey(PlayerDeathEvent event, Player killer) {
        DamageCause cause = event.getPlayer().getLastDamageCause() == null
                ? null
                : event.getPlayer().getLastDamageCause().getCause();

        if (killer != null) {
            return cause == DamageCause.PROJECTILE ? "projectile-kill" : "player-kill";
        }
        if (cause == null) return "generic";

        return switch (cause) {
            case FALL, FLY_INTO_WALL -> "fall";
            case LAVA -> "lava";
            case FIRE, FIRE_TICK, HOT_FLOOR -> "fire";
            case DROWNING -> "drowning";
            case VOID -> "void";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case FREEZE -> "freeze";
            case LIGHTNING -> "lightning";
            default -> "generic";
        };
    }
}
