package com.aevum.bounties;

import org.bukkit.Bukkit;import org.bukkit.command.*;import org.bukkit.entity.Player;

public final class BountyCommand implements CommandExecutor,TabCompleter{
 private final AevumBounties plugin;private final BountyManager manager;private final BountyGUI gui;
 public BountyCommand(AevumBounties p,BountyManager m,BountyGUI g){plugin=p;manager=m;gui=g;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}
  if(!p.hasPermission("aevumbounties.use")){p.sendMessage(AevumBounties.color("&cYou don't have permission."));return true;}
  if(a.length==0){gui.open(p);return true;}
  if(a[0].equalsIgnoreCase("top")){gui.openTop(p);return true;}
  if(a[0].equalsIgnoreCase("cancel")&&a.length==2){try{if(manager.cancel(java.util.UUID.fromString(a[1]),p))p.sendMessage(AevumBounties.color("&a✓ Bounty cancelled and refunded."));else p.sendMessage(AevumBounties.color("&cThat bounty does not belong to you."));}catch(Exception e){p.sendMessage(AevumBounties.color("&cInvalid bounty ID."));}return true;}
  if(a.length==2){Player target=Bukkit.getPlayerExact(a[0]);if(target==null){p.sendMessage(AevumBounties.color("&cThat player is not online."));return true;}double amount;try{amount=Double.parseDouble(a[1]);}catch(Exception e){p.sendMessage(AevumBounties.color("&cInvalid amount."));return true;}double min=plugin.getConfig().getDouble("settings.min-bounty"),max=plugin.getConfig().getDouble("settings.max-bounty");if(amount<min||amount>max||!Double.isFinite(amount)){p.sendMessage(AevumBounties.color("&cAmount must be between &6"+plugin.money(min)+" &cand &6"+plugin.money(max)+"&c."));return true;}if(!plugin.getConfig().getBoolean("settings.allow-self",false)&&target.equals(p)){p.sendMessage(AevumBounties.color("&cYou cannot bounty yourself."));return true;}Bounty b=manager.create(p,target,amount);if(b==null){p.sendMessage(AevumBounties.color("&cNot enough money or safe save failed."));return true;}p.sendMessage(AevumBounties.color("&a✓ Bounty placed on &e"+target.getName()+" &afor &6"+plugin.money(amount)+"&a."));if(plugin.getConfig().getBoolean("settings.announce-new-bounties",true))plugin.announce("&6☠ &e"+p.getName()+" &7placed a &6"+plugin.money(amount)+" &7bounty on &c"+target.getName()+"&7.");return true;}
  p.sendMessage(AevumBounties.color("&6&lBOUNTY &8» &7/bounty &8| &7/bounty top &8| &7/bounty <player> <amount> &8| &7/bounty cancel <id>"));return true;
 }
 public java.util.List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return java.util.List.of("top","cancel");return java.util.Collections.emptyList();}
}