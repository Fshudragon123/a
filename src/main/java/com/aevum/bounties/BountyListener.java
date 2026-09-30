package com.aevum.bounties;
import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.entity.PlayerDeathEvent;import org.bukkit.event.inventory.InventoryClickEvent;
public final class BountyListener implements Listener{
 private final AevumBounties p;private final BountyManager m;private final BountyGUI g;public BountyListener(AevumBounties p,BountyManager m,BountyGUI g){this.p=p;this.m=m;this.g=g;}
 @EventHandler public void click(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player pl)g.click(pl,e);}
 @EventHandler public void death(PlayerDeathEvent e){Player target=e.getEntity(),hunter=target.getKiller();if(hunter==null||hunter.equals(target))return;Bounty b=m.claim(hunter,target);if(b==null)return;if(p.getConfig().getBoolean("settings.announce-claims",true))p.announce("&6☠ &e"+hunter.getName()+" &7claimed the bounty on &c"+target.getName()+" &7for &6"+p.money(b.amount())+"&7!");hunter.sendMessage(AevumBounties.color("&6&lBOUNTY CLAIMED &8» &aYou received &6"+p.money(b.amount())+"&a."));}
}