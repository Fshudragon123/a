package com.aevum.bounties;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;

public final class BountyGUI {
    public static final String BOARD_TITLE="§0✦ §6§lBOUNTY BOARD §0✦";
    public static final String TOP_TITLE="§0✦ §e§lBOUNTY HALL §0✦";
    public static final String MINE_TITLE="§0✦ §b§lMY BOUNTIES §0✦";
    private final AevumBounties p; private final BountyManager m;

    public BountyGUI(AevumBounties p,BountyManager m){this.p=p;this.m=m;}

    public void open(Player pl){
        Inventory i=Bukkit.createInventory(null,54,BOARD_TITLE);
        fill(i,Material.BLACK_STAINED_GLASS_PANE," ");
        button(i,4,Material.NETHER_STAR,"§6§l✦ BOUNTY BOARD ✦","§7Aevum's most wanted.","","§fActive §8» §6"+m.all().size(),"§fTotal §8» §6"+p.money(m.total()));
        button(i,48,Material.WRITABLE_BOOK,"§a§l＋ PLACE BOUNTY","§7Put a price on someone's head.","","§e▶ Click to start");
        button(i,49,Material.GOLD_BLOCK,"§e§l★ BOUNTY HALL","§7View the highest-value targets.","","§e▶ Click to open");
        button(i,50,Material.BARRIER,"§c§l✕ CLOSE","§7Close the bounty board.");
        button(i,51,Material.PAPER,"§b§l✦ MY BOUNTIES","§7Manage bounties you posted.","","§e▶ Click to manage");
        int s=9; for(Bounty b:m.top()){if(s>=45)break;i.setItem(s++,card(b));}
        pl.openInventory(i);
    }

    private ItemStack card(Bounty b){
        ItemStack x=new ItemStack(Material.PLAYER_HEAD);
        SkullMeta z=(SkullMeta)x.getItemMeta();
        z.setOwningPlayer(Bukkit.getOfflinePlayer(b.target()));
        z.setDisplayName("§c☠ §f§l"+b.targetName()+" §c☠");
        z.setLore(List.of("§8━━━━━━━━━━━━━━━━","§6§lREWARD","§f✦ "+p.money(b.amount()),"","§7Posted by §f"+b.creatorName(),"§7Age §f"+age(b.createdAt()),"","§c⚔ §e§lWANTED","§7Find them. Kill them. Get paid.","§8━━━━━━━━━━━━━━━━"));
        x.setItemMeta(z); return x;
    }

    public void openTop(Player pl){
        Inventory i=Bukkit.createInventory(null,54,TOP_TITLE);
        fill(i,Material.GRAY_STAINED_GLASS_PANE," ");
        button(i,4,Material.GOLDEN_HELMET,"§e§l★ BOUNTY HALL ★","§7The richest targets currently wanted.");
        int s=10,r=1; for(Bounty b:m.top()){if(s>=44)break;Material mat=r==1?Material.DIAMOND_BLOCK:r==2?Material.GOLD_BLOCK:r==3?Material.IRON_BLOCK:Material.PAPER;ItemStack x=new ItemStack(mat);ItemMeta z=x.getItemMeta();z.setDisplayName("§e#"+r+" §f§l"+b.targetName());z.setLore(List.of("§6Reward §8» §f"+p.money(b.amount()),"§7Posted by §f"+b.creatorName()));x.setItemMeta(z);i.setItem(s++,x);r++;}
        button(i,49,Material.ARROW,"§7← BACK","§7Return to the bounty board."); pl.openInventory(i);
    }

    public void openMine(Player pl){
        Inventory i=Bukkit.createInventory(null,54,MINE_TITLE);
        fill(i,Material.BLACK_STAINED_GLASS_PANE," ");
        button(i,4,Material.NAME_TAG,"§b§l✦ MY BOUNTIES ✦","§7Bounties you have posted.","","§7Click one to cancel and refund it.");
        List<Bounty> mine=m.byCreator(pl.getUniqueId());
        int s=10;
        for(Bounty b:mine){
            if(s>=44)break;
            ItemStack x=new ItemStack(Material.PLAYER_HEAD);
            SkullMeta z=(SkullMeta)x.getItemMeta();
            z.setOwningPlayer(Bukkit.getOfflinePlayer(b.target()));
            z.setDisplayName("§c☠ §f§l"+b.targetName());
            z.setLore(List.of("§8━━━━━━━━━━━━━━━━","§6Reward §8» §f"+p.money(b.amount()),"§7Posted "+age(b.createdAt())+" ago","","§c✕ CLICK TO CANCEL","§7You will receive a full refund."));
            z.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(p,"bounty-id"),org.bukkit.persistence.PersistentDataType.STRING,b.id().toString());
            x.setItemMeta(z); i.setItem(s++,x);
        }
        if(mine.isEmpty()) button(i,22,Material.BARRIER,"§7§lNO ACTIVE BOUNTIES","§8You haven't posted any bounties.");
        button(i,49,Material.ARROW,"§7← BACK","§7Return to the bounty board.");
        pl.openInventory(i);
    }

    public void click(Player pl,InventoryClickEvent e){
        String t=e.getView().getTitle();
        if(!t.equals(BOARD_TITLE)&&!t.equals(TOP_TITLE)&&!t.equals(MINE_TITLE))return;
        e.setCancelled(true);
        if(e.getRawSlot()<0||e.getRawSlot()>=e.getView().getTopInventory().getSize())return;

        if(t.equals(TOP_TITLE)&&e.getRawSlot()==49){open(pl);return;}
        if(t.equals(MINE_TITLE)){
            if(e.getRawSlot()==49){open(pl);return;}
            ItemStack item=e.getCurrentItem();
            if(item!=null&&item.hasItemMeta()){
                String raw=item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(p,"bounty-id"),org.bukkit.persistence.PersistentDataType.STRING);
                if(raw!=null)try{
                    UUID id=UUID.fromString(raw);
                    if(m.cancel(id,pl))pl.sendMessage(AevumBounties.color("&a✓ Bounty cancelled and &6refunded&a."));
                    else pl.sendMessage(AevumBounties.color("&cThat bounty could not be cancelled."));
                    openMine(pl);
                }catch(IllegalArgumentException ex){pl.sendMessage(AevumBounties.color("&cInvalid bounty."));}
            }
            return;
        }
        if(t.equals(BOARD_TITLE)){
            if(e.getRawSlot()==50)pl.closeInventory();
            else if(e.getRawSlot()==49)openTop(pl);
            else if(e.getRawSlot()==48){pl.closeInventory();new ChatFlow(p,m,this,pl).start();}
            else if(e.getRawSlot()==51)openMine(pl);
        }
    }

    private void fill(Inventory i,Material mat,String n){ItemStack x=new ItemStack(mat);ItemMeta z=x.getItemMeta();z.setDisplayName(n);x.setItemMeta(z);for(int s=0;s<i.getSize();s++)i.setItem(s,x.clone());}
    private void button(Inventory i,int s,Material mat,String n,String...l){ItemStack x=new ItemStack(mat);ItemMeta z=x.getItemMeta();z.setDisplayName(n);z.setLore(List.of(l));x.setItemMeta(z);i.setItem(s,x);}
    private String age(long t){long s=Math.max(0,(System.currentTimeMillis()-t)/1000);return s<60?s+"s":s<3600?s/60+"m":s<86400?s/3600+"h":s/86400+"d";}
}