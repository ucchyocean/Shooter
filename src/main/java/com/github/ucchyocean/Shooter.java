/*
 * Copyright ucchy 2013
 */
package com.github.ucchyocean;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author ucchy
 * シューター
 */
public class Shooter extends JavaPlugin implements Listener {

    private static final String NAME = "shooter";
    private static final String DISPLAY_NAME =
            ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + NAME;
    private static final ArrayList<String> LORE;
    static {
        LORE = new ArrayList<String>();
        LORE.add("飛びたい目標をクリックすることで、慣性を利かせながら");
        LORE.add("飛ぶことが出来る。飛べる回数は有限で、EXPバーで");
        LORE.add("残り燃料を確認することが出来る。");
    }
    private static final int DEFAULT_LEVEL = 4;
    private static final int DEFAULT_COST = 10;
    private static final int MAX_LEVEL = 15;
    private static final int RANGE = 50;

    private ItemStack item;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta shooterMeta = item.getItemMeta();
        shooterMeta.setDisplayName(DISPLAY_NAME);
        shooterMeta.setLore(LORE);
        item.setItemMeta(shooterMeta);
    }

    /**
     * コマンドが実行されたときに呼び出されるメソッド
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

            int level = DEFAULT_LEVEL;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[1]);
            }

            giveShooter(player, level);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = DEFAULT_LEVEL;
            if ( args.length >= 3 && args[2].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[2]);
            }

            giveShooter(player, level);

            return true;
        }

        return false;
    }

    /**
     * 指定したプレイヤーに、指定したレベルのShooterを与える
     * @param player プレイヤー
     * @param level レベル
     */
    private void giveShooter(Player player, int level) {

        ItemStack shooter = this.item.clone();

        if ( level < 1 ) {
            level = 1;
        } else if ( level > MAX_LEVEL ) {
            level = MAX_LEVEL;
        }

        shooter.addUnsafeEnchantment(Enchantment.OXYGEN, level);

        ItemStack temp = player.getItemInHand();
        player.setItemInHand(shooter);
        if ( temp != null ) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * クリックされたときのイベント処理
     * @param event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                player.getItemInHand().getItemMeta().getDisplayName() == null ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        if ( event.getAction() == Action.PHYSICAL ) {
            return;
        } else if ( event.getAction() == Action.RIGHT_CLICK_AIR ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
            event.setCancelled(true);
            return;
        }

        Location eLoc = player.getEyeLocation().add(0.5D, 0.0D, 0.5D);

        if ( player.getTargetBlock(null, RANGE).getType() == Material.AIR ) {
            player.sendMessage(ChatColor.RED + "out of range!!");
            player.playEffect(eLoc, Effect.SMOKE, 0);
            player.playEffect(eLoc, Effect.SMOKE, 0);
            player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
            event.setCancelled(true);
            return;
        }

        if ( !hasExperience(player, DEFAULT_COST) ) {
            player.sendMessage(ChatColor.RED + "no fuel!!");
            player.playEffect(eLoc, Effect.SMOKE, 0);
            player.playEffect(eLoc, Effect.SMOKE, 0);
            player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
            event.setCancelled(true);
            return;
        }

        takeExperience(player, DEFAULT_COST);

        ItemStack shooter = player.getItemInHand();
        double level = (double)shooter.getEnchantmentLevel(Enchantment.OXYGEN);

        player.setVelocity(player.getLocation().getDirection().multiply(level));
        player.setFallDistance(-1000F);
        player.playEffect(eLoc, Effect.POTION_BREAK, 21);
        player.playEffect(eLoc, Effect.POTION_BREAK, 21);

        event.setCancelled(true);
    }

    /**
     * プレイヤーから、指定した経験値量を減らす。
     * @param player プレイヤー
     * @param amount 減らす量
     */
    public static void takeExperience(final Player player, int amount) {
        player.giveExp(-amount);
        updateExp(player);
    }

    /**
     * プレイヤーが指定した量の経験値を持っているかどうか判定する。
     * @param player プレイヤー
     * @param amount 判定する量
     * @return もっているかどうか
     */
    public static boolean hasExperience(final Player player, int amount) {
        return (player.getTotalExperience() >= amount);
    }

    /**
     * プレイヤーの経験値量を、指定値に設定する。
     * @param player プレイヤー
     * @param amount 経験値の量
     */
    public static void setExperience(final Player player, int amount) {
        player.setTotalExperience(amount);
        updateExp(player);
    }

    /**
     * 経験値表示を更新する
     * @param player 更新対象のプレイヤー
     */
    private static void updateExp(final Player player) {

        int total = player.getTotalExperience();
        player.setLevel(0);
        player.setExp(0);
        while ( total > player.getExpToLevel() ) {
            total -= player.getExpToLevel();
            player.setLevel(player.getLevel()+1);
        }
        float xp = (float)total / (float)player.getExpToLevel();
        player.setExp(xp);
    }
}
