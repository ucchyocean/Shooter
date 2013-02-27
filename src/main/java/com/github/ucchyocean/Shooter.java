/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author ucchy
 *
 */
public class Shooter extends JavaPlugin implements Listener {

    private static final String SHOOTER_NAME =
            ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + "Shooter";
    private static final int DEFAULT_LEVEL = 10;
    private static final int DEFAULT_COST = 20;

    private ItemStack shooter;

    public void onEnable(){

        getServer().getPluginManager().registerEvents(this, this);

        shooter = new ItemStack(Material.BOW, 1);
        ItemMeta shooterMeta = shooter.getItemMeta();
        shooterMeta.setDisplayName(SHOOTER_NAME);
        shooter.setItemMeta(shooterMeta);
        ShapedRecipe shooterRecipe =
                new ShapedRecipe(new ItemStack(shooter)).
                shape(new String[] { "dg ", "d g", "dg " }).
                setIngredient('d', Material.DIAMOND).
                setIngredient('g', Material.GOLD_NUGGET);
        getServer().addRecipe(shooterRecipe);

    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("get") ) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            Player player = (Player)sender;

            ItemStack shooter = this.shooter.clone();
            int level = DEFAULT_LEVEL;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                int temp = Integer.parseInt(args[1]);
                if ( 1 <= temp && temp <= 10 ) {
                    level = temp;
                }
            }
            shooter.addUnsafeEnchantment(Enchantment.OXYGEN, level);

            ItemStack temp = player.getItemInHand();
            player.setItemInHand(shooter);
            if ( temp != null ) {
                player.getInventory().addItem(temp);
            }

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            ItemStack shooter = this.shooter.clone();
            int level = DEFAULT_LEVEL;
            if ( args.length >= 3 && args[2].matches("^[0-9]+$") ) {
                int temp = Integer.parseInt(args[2]);
                if ( 1 <= temp && temp <= 10 ) {
                    level = temp;
                }
            }
            shooter.addUnsafeEnchantment(Enchantment.OXYGEN, level);

            ItemStack temp = player.getItemInHand();
            player.setItemInHand(shooter);
            if ( temp != null ) {
                player.getInventory().addItem(temp);
            }

            return true;
        }

        return false;
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if ( player.getItemInHand() != null &&
                player.getItemInHand().getType() != Material.AIR &&
                player.getItemInHand().getItemMeta().getDisplayName() != null &&
                player.getItemInHand().getItemMeta().getDisplayName().equals(SHOOTER_NAME) ) {

            if ( event.getAction() == Action.LEFT_CLICK_AIR ||
                    event.getAction() == Action.LEFT_CLICK_BLOCK) {

                if ( player.getTargetBlock(null, 100).getType() != Material.AIR ) {

                    if ( !hasExperience(player, DEFAULT_COST) ) {
                        player.sendMessage(ChatColor.RED + "no fuel!!");

                    } else {

                        takeExperience(player, DEFAULT_COST);

                        ItemStack shooter = player.getItemInHand();
                        double level = (double)shooter.getEnchantmentLevel(Enchantment.OXYGEN);

                        player.setVelocity(player.getLocation().getDirection().multiply(level));
                        player.setFallDistance(-1000F);
                        player.playEffect(player.getEyeLocation().add(0.5D, 0.0D, 0.5D), Effect.POTION_BREAK, 21);
                        player.playEffect(player.getEyeLocation().add(0.5D, 0.0D, 0.5D), Effect.POTION_BREAK, 21);
                    }

                } else {

                    player.sendMessage(ChatColor.RED + "out of range!!");
                }
            }

            event.setCancelled(true);
        }
    }

    private static int getExpAtLevel(final Player player) {
        return getExpAtLevel(player.getLevel());
    }

    private static int getExpAtLevel(final int level) {
        if (level > 29) {
            return 62 + (level - 30) * 7;
        }
        if (level > 15) {
            return 17 + (level - 15) * 3;
        }
        return 17;
    }

    private static int getTotalExperience(final Player player) {
        int exp = (int)Math.round(getExpAtLevel(player) * player.getExp());
        int currentLevel = player.getLevel();

        while (currentLevel > 0)
        {
            currentLevel--;
            exp += getExpAtLevel(currentLevel);
        }
        return exp;
    }

    private static void takeExperience(final Player player, int amount) {
        int remain = amount;
        int exp = (int)Math.round(getExpAtLevel(player) * player.getExp());
        if ( exp >= remain ) {
            player.giveExp(-remain);
            return;
        } else {
            player.giveExp(-exp);
            remain -= exp;
        }
        while ( remain > 0 && player.getLevel() > 0 ) {
            player.setLevel(player.getLevel()-1);
            player.setExp(1.0f);
            exp = getExpAtLevel(player);
            if ( exp >= remain ) {
                player.giveExp(-remain);
                return;
            } else {
                player.giveExp(-exp);
                remain -= exp;
            }
        }
        return;
    }

    private static boolean hasExperience(final Player player, int amount) {
        int exp = getTotalExperience(player);
        return (exp >= amount);
    }
}
