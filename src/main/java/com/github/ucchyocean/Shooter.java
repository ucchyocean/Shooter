/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

/**
 * シューター
 * @author ucchy
 */
public class Shooter extends JavaPlugin implements Listener {

    private static final String NAME = "shooter";
    private static final String DISPLAY_NAME =
            ChatColor.BLUE.toString() + ChatColor.BOLD.toString() + NAME;
    private static final int DEFAULT_LEVEL = 4;
    private static final int DEFAULT_COST = 10;
    private static final int MAX_LEVEL = 20;
    private static final int REVIVE_SECONDS = 5;
    private static final int REVIVE_AMOUNT = 30;
    private static final int DEFAULT_RANGE = 50;

    private ItemStack item;

    private int configLevel;
    private int configCost;
    private boolean configRevive;
    private int configReviveSeconds;
    private int configReviveAmount;
    private int configRange;

    /**
     * プラグインが有効になったときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    public void onEnable(){

        saveDefaultConfig();
        loadConfigDatas();

        getServer().getPluginManager().registerEvents(this, this);

        item = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta shooterMeta = item.getItemMeta();
        shooterMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(shooterMeta);

        // ColorTeaming のロード
        Plugin colorteaming = null;
        if ( getServer().getPluginManager().isPluginEnabled("ColorTeaming") ) {
            colorteaming = getServer().getPluginManager().getPlugin("ColorTeaming");
            String ctversion = colorteaming.getDescription().getVersion();
            if ( isUpperVersion(ctversion, "2.2.0") ) {
                getLogger().info("ColorTeaming がロードされました。連携機能を有効にします。");
                ColorTeamingBridge bridge = new ColorTeamingBridge(colorteaming);
                bridge.registerItem(item, NAME, DISPLAY_NAME);
            } else {
                getLogger().warning("ColorTeaming のバージョンが古いため、連携機能は無効になりました。");
                getLogger().warning("連携機能を使用するには、ColorTeaming v2.2.0 以上が必要です。");
            }
        }
    }

    /**
     * 設定情報の読み込み処理
     */
    private void loadConfigDatas() {

        FileConfiguration config = getConfig();
        configLevel = config.getInt("defaultLevel", DEFAULT_LEVEL);
        configCost = config.getInt("cost", DEFAULT_COST);
        configRevive = config.getBoolean("revive", true);
        if ( configRevive ) {
            configReviveSeconds = config.getInt("reviveSeconds", REVIVE_SECONDS);
            configReviveAmount = config.getInt("reviveAmount", REVIVE_AMOUNT);
        }
        configRange = config.getInt("range", DEFAULT_RANGE);
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

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("shooter.reload")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"shooter.reload\".");
                return true;
            }

            // コンフィグ再読込
            this.reloadConfig();
            this.loadConfigDatas();
            sender.sendMessage(ChatColor.GREEN + "Shooter configuration was reloaded!");

            return true;

        } else if ( args[0].equalsIgnoreCase("get") ) {

            if (!sender.hasPermission("shooter.get")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"shooter.get\".");
                return true;
            }

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can only use in game.");
                return true;
            }

            Player player = (Player)sender;

            int level = configLevel;
            if ( args.length >= 2 && args[1].matches("^[0-9]+$") ) {
                level = Integer.parseInt(args[1]);
            }

            giveShooter(player, level);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if (!sender.hasPermission("shooter.give")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"shooter.give\".");
                return true;
            }

            Player player = getServer().getPlayerExact(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            int level = configLevel;
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
        final Player player = event.getPlayer();

        // シューターを手に持っているときに発生したイベントでない場合は、無視する
        if ( player.getItemInHand() == null ||
                player.getItemInHand().getType() == Material.AIR ||
                player.getItemInHand().getItemMeta().getDisplayName() == null ||
                !player.getItemInHand().getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
            return;
        }

        // 左クリックでなければ無視する。
        if ( event.getAction() == Action.PHYSICAL ) {
            return;
        } else if ( event.getAction() == Action.RIGHT_CLICK_AIR ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
            event.setCancelled(true);
            return;
        }
        
        // パーミッションがないなら終了する
        if ( !player.hasPermission("shooter.action") ) return;

        Location eLoc = player.getEyeLocation();
        Block clicked = getTargetBlock(player, configRange);

        // クリックしたところが空か、遠すぎる場合
        if ( clicked == null ) {
            player.sendMessage(ChatColor.RED + "out of range!!");
            player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
            player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
            player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
            event.setCancelled(true);
            return;
        }

        // 燃料が無い場合
        if ( !hasExperience(player, configCost) ) {
            player.sendMessage(ChatColor.RED + "no fuel!!");
            player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
            player.getWorld().playEffect(eLoc, Effect.SMOKE, 4);
            player.playSound(eLoc, Sound.IRONGOLEM_THROW, (float)1.0, (float)1.5);
            event.setCancelled(true);
            return;
        }

        // 燃料消費
        takeExperience(player, configCost);

        // 今回の操作で燃料がなくなった場合、数秒後に復活させる
        if ( configRevive && !hasExperience(player, configCost) ) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    takeExperience(player, -configReviveAmount);
                }
            };
            runnable.runTaskLater(this, configReviveSeconds * 20);
            player.sendMessage(ChatColor.GOLD + "your fuel will revive after " + configReviveSeconds + " seconds.");
        }

        // 飛翔
        ItemStack shooter = player.getItemInHand();
        double level = DEFAULT_LEVEL;
        if ( shooter.containsEnchantment(Enchantment.OXYGEN) )
            level = (double)shooter.getEnchantmentLevel(Enchantment.OXYGEN);

        player.setVelocity(player.getLocation().getDirection().multiply(level));
        player.setFallDistance(-1000F);
        player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 21);
        player.getWorld().playEffect(eLoc, Effect.POTION_BREAK, 21);

        event.setCancelled(true);
    }

    
    /**
     * プレイヤーが向いている方向にあるブロックを取得する。
     * @param player プレイヤー
     * @param size 取得する最大距離、140以上を指定しないこと
     * @return プレイヤーが向いている方向にあるブロック、取得できない場合はnullがかえされる
     */
    public static Block getTargetBlock(Player player, int size) {
        
        BlockIterator it = new BlockIterator(player, size);
        while ( it.hasNext() ) {
            Block b = it.next();
            if ( b != null && b.getType() != Material.AIR ) {
                return b;
            }
        }
        return null;
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

    /**
     * 指定されたバージョンが、基準より新しいバージョンかどうかを確認する<br>
     * 完全一致した場合もtrueになることに注意。
     * @param version 確認するバージョン
     * @param border 基準のバージョン
     * @return 基準より確認対象の方が新しいバージョンかどうか
     */
    private boolean isUpperVersion(String version, String border) {

        String[] versionArray = version.split("\\.");
        int[] versionNumbers = new int[versionArray.length];
        for ( int i=0; i<versionArray.length; i++ ) {
            if ( !versionArray[i].matches("[0-9]+") )
                return false;
            versionNumbers[i] = Integer.parseInt(versionArray[i]);
        }

        String[] borderArray = border.split("\\.");
        int[] borderNumbers = new int[borderArray.length];
        for ( int i=0; i<borderArray.length; i++ ) {
            if ( !borderArray[i].matches("[0-9]+") )
                return false;
            borderNumbers[i] = Integer.parseInt(borderArray[i]);
        }

        int index = 0;
        while ( (versionNumbers.length > index) && (borderNumbers.length > index) ) {
            if ( versionNumbers[index] > borderNumbers[index] ) {
                return true;
            } else if ( versionNumbers[index] < borderNumbers[index] ) {
                return false;
            }
            index++;
        }
        if ( borderNumbers.length == index ) {
            return true;
        } else {
            return false;
        }
    }
}
