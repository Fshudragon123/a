package net.aevummc.deathmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public final class AevumDeathMessages extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String FALLBACK_MESSAGE =
            "<#C98B24>✦ <#E0A63A><victim> <#8B8B95>has fallen from the empire.";
    private static final String PREFIX =
            "<#C98B24>✦ <#E0A63A>AevumMC <#8B8B95>» <#F3EBDD>";

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand command = Objects.requireNonNull(
                getCommand("aevumdeath"),
                "aevumdeath command is missing from plugin.yml"
        );
        command.setExecutor(this);
        command.setTabCompleter(this);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Imperium Edition enabled — antique gold, ivory and stone.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("settings.enabled", true)) {
            return;
        }

        Player victim = event.getPlayer();
        DamageSource source = event.getDamageSource();
        DamageCause cause = victim.getLastDamageCause() == null
                ? null
                : victim.getLastDamageCause().getCause();

        Entity attacker = source.getCausingEntity();
        if (attacker == null) {
            attacker = victim.getKiller();
        }

        boolean projectile = cause == DamageCause.PROJECTILE
                || source.getDirectEntity() instanceof Projectile
                || source.isIndirect();

        String key = selectMessageKey(cause, attacker, projectile);
        String killerName = attacker == null ? "" : displayEntityName(attacker);
        Component message = renderMessage(key, victim.getName(), killerName);

        event.deathMessage(message);
        if (getConfig().getBoolean("settings.style-death-screen", true)) {
            event.deathScreenMessageOverride(message);
        }
    }

    private String selectMessageKey(DamageCause cause, Entity attacker, boolean projectile) {
        if (attacker instanceof Player) {
            return projectile ? "player-projectile-kill" : "player-kill";
        }

        if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
            return "explosion";
        }
        // Keep spectacular Warden deaths distinct instead of classifying them as a normal mob hit.
        if (cause == DamageCause.SONIC_BOOM) {
            return "sonic-boom";
        }

        if (attacker != null) {
            if (projectile) {
                return "mob-projectile-kill";
            }
            if (cause == DamageCause.ENTITY_ATTACK
                    || cause == DamageCause.ENTITY_SWEEP_ATTACK
                    || cause == DamageCause.SONIC_BOOM
                    || cause == DamageCause.THORNS) {
                return "mob-kill";
            }
        }

        if (cause == null) {
            return "generic";
        }

        return switch (cause) {
            case FALL, FLY_INTO_WALL -> "fall";
            case LAVA -> "lava";
            case FIRE, FIRE_TICK, HOT_FLOOR, CAMPFIRE -> "fire";
            case DROWNING -> "drowning";
            case VOID -> "void";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            case FREEZE -> "freeze";
            case LIGHTNING -> "lightning";
            case STARVATION -> "starvation";
            case SUFFOCATION -> "suffocation";
            case FALLING_BLOCK -> "falling-block";
            case SONIC_BOOM -> "sonic-boom";
            case DRYOUT -> "dryout";
            case CRAMMING -> "cramming";
            case WORLD_BORDER -> "world-border";
            case KILL, SUICIDE -> "execution";
            case CONTACT -> "contact";
            case POISON, MAGIC -> "magic";
            case WITHER -> "wither";
            case PROJECTILE -> "projectile";
            case THORNS -> "mob-kill";
            default -> "generic";
        };
    }

    private String displayEntityName(Entity entity) {
        if (entity instanceof Player player) {
            return player.getName();
        }

        Component customName = entity.customName();
        if (customName != null) {
            String plain = plainText.serialize(customName).trim();
            if (!plain.isEmpty()) {
                return plain;
            }
        }

        String words = entity.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder result = new StringBuilder(words.length());
        boolean upperNext = true;
        for (char character : words.toCharArray()) {
            result.append(upperNext ? Character.toUpperCase(character) : character);
            upperNext = character == ' ';
        }
        return result.toString();
    }

    private Component renderMessage(String key, String victim, String killer) {
        return miniMessage.deserialize(
                chooseTemplate(key),
                Placeholder.unparsed("victim", victim),
                Placeholder.unparsed("killer", killer)
        );
    }

    private String chooseTemplate(String key) {
        List<String> variants = getConfig().getStringList("messages." + key);
        if (!variants.isEmpty()) {
            if (!getConfig().getBoolean("settings.random-variants", true) || variants.size() == 1) {
                return variants.get(0);
            }
            return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        }

        // Older configs that use a single string per key remain supported.
        String single = getConfig().getString("messages." + key);
        if (single != null && !single.isBlank()) {
            return single;
        }

        List<String> genericVariants = getConfig().getStringList("messages.generic");
        if (!genericVariants.isEmpty()) {
            return genericVariants.get(0);
        }
        String generic = getConfig().getString("messages.generic");
        return generic == null || generic.isBlank() ? FALLBACK_MESSAGE : generic;
    }

    private Set<String> getMessageKeys() {
        if (getConfig().getConfigurationSection("messages") == null) {
            return Set.of("generic", "player-kill", "fall");
        }
        return new TreeSet<>(getConfig().getConfigurationSection("messages").getKeys(false));
    }

    private void sendPrefixed(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(PREFIX + message));
    }

    private String normaliseKey(String input) {
        String key = input.toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (key) {
            case "pvp", "player", "melee" -> "player-kill";
            case "arrow", "bow", "projectile-player" -> "player-projectile-kill";
            case "mob", "monster" -> "mob-kill";
            case "mob-arrow", "mob-projectile" -> "mob-projectile-kill";
            default -> key;
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendPrefixed(sender, "Commands: <#E0A63A>/aevumdeath preview [key] <#8B8B95>• "
                    + "<#E0A63A>/aevumdeath causes <#8B8B95>• <#E0A63A>/aevumdeath reload");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                reloadConfig();
                sendPrefixed(sender, "Death-message settings reloaded.");
                getLogger().info(sender.getName() + " reloaded death-message settings.");
                return true;
            }
            case "causes" -> {
                sendPrefixed(sender, "Message keys: <#E0A63A>"
                        + String.join("<#8B8B95>, <#E0A63A>", getMessageKeys()));
                return true;
            }
            case "preview" -> {
                String key = args.length >= 2 ? normaliseKey(args[1]) : "player-kill";
                if (!getMessageKeys().contains(key)) {
                    sendPrefixed(sender, "Unknown message key. Use <#E0A63A>/aevumdeath causes<#F3EBDD>.");
                    return true;
                }
                String killer = switch (key) {
                    case "mob-kill", "mob-projectile-kill" -> "Zombie";
                    case "player-kill", "player-projectile-kill" -> "Legionnaire";
                    default -> "";
                };
                sender.sendMessage(renderMessage(key, "Imperius", killer));
                return true;
            }
            default -> {
                sendPrefixed(sender, "Unknown command. Use <#E0A63A>/aevumdeath help<#F3EBDD>.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "preview", "causes", "reload").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            List<String> suggestions = new ArrayList<>(getMessageKeys());
            suggestions.addAll(List.of("pvp", "arrow", "mob", "mob-arrow"));
            String input = args[1].toLowerCase(Locale.ROOT);
            return suggestions.stream().distinct().filter(value -> value.startsWith(input)).sorted().toList();
        }
        return List.of();
    }
}
