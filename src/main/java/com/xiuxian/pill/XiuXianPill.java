package com.xiuxian.pill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class XiuXianPill extends JavaPlugin implements CommandExecutor, Listener {
    
    private final Map<String, PillData> liantiPills = new HashMap<>();
    private final Map<String, PillData> xiufaPills = new HashMap<>();
    private File xiuxianDataDir;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadPills();
        
        xiuxianDataDir = new File("D:/MineCraft/server/plugins/XiuXianCore/data");
        
        getCommand("pill").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getLogger().info("XiuXianPill enabled: " + liantiPills.size() + " lianti pills, " + xiufaPills.size() + " xiufa pills");
    }
    
    private void loadPills() {
        liantiPills.clear();
        xiufaPills.clear();
        FileConfiguration cfg = getConfig();
        
        ConfigurationSection ltSection = cfg.getConfigurationSection("lianti-pills");
        if (ltSection != null) {
            for (String key : ltSection.getKeys(false)) {
                ConfigurationSection s = ltSection.getConfigurationSection(key);
                if (s == null) continue;
                liantiPills.put("lt" + key, new PillData(
                    Integer.parseInt(key),
                    colorize(s.getString("name", "")),
                    s.getString("description", ""),
                    s.getString("realm", ""),
                    s.getInt("xp-amount", 100),
                    Material.matchMaterial(s.getString("material", "PAPER"))
                ));
            }
        }
        
        ConfigurationSection xfSection = cfg.getConfigurationSection("xiufa-pills");
        if (xfSection != null) {
            for (String key : xfSection.getKeys(false)) {
                ConfigurationSection s = xfSection.getConfigurationSection(key);
                if (s == null) continue;
                xiufaPills.put("xf" + key, new PillData(
                    Integer.parseInt(key),
                    colorize(s.getString("name", "")),
                    s.getString("description", ""),
                    s.getString("realm", ""),
                    s.getInt("xp-amount", 100),
                    Material.matchMaterial(s.getString("material", "PAPER"))
                ));
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                showPillList(sender);
                break;
            case "give":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "用法: /pill give <玩家> <类型> [数量]");
                    return true;
                }
                givePill(sender, args);
                break;
            case "reload":
                if (!sender.hasPermission("xipill.admin")) {
                    sender.sendMessage(ChatColor.RED + "没有权限");
                    return true;
                }
                reloadConfig();
                loadPills();
                sender.sendMessage(ChatColor.GREEN + "丹药配置已重载");
                break;
            case "use":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("只有玩家可以使用");
                    return true;
                }
                usePillFromHand((Player) sender);
                break;
            default:
                showHelp(sender);
        }
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&6&l      丹药系统"));
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize(" &e/pill list &7- 查看丹药列表"));
        sender.sendMessage(colorize(" &e/pill give <玩家> <类型> [数量] &7- 给予丹药"));
        sender.sendMessage(colorize(" &e/pill use &7- 手持服用"));
        sender.sendMessage(colorize(" &e/pill reload &7- 重载配置"));
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void showPillList(CommandSender sender) {
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&6&l      体修经验丹"));
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Map.Entry<String, PillData> entry : liantiPills.entrySet()) {
            PillData pill = entry.getValue();
            sender.sendMessage(colorize(" &f" + entry.getKey() + " &7- " + pill.name + " &7(" + pill.realm + ") " + pill.xpAmount + "修为"));
        }
        
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(colorize("&6&l      法修经验丹"));
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Map.Entry<String, PillData> entry : xiufaPills.entrySet()) {
            PillData pill = entry.getValue();
            sender.sendMessage(colorize(" &f" + entry.getKey() + " &7- " + pill.name + " &7(" + pill.realm + ") " + pill.xpAmount + "修为"));
        }
        
        sender.sendMessage(colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void givePill(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xipill.admin")) {
            sender.sendMessage(ChatColor.RED + "没有权限");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家不在线: " + args[1]);
            return;
        }
        
        String pillType = args[2].toLowerCase();
        int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        
        PillData pill = liantiPills.get(pillType);
        if (pill == null) pill = xiufaPills.get(pillType);
        
        if (pill == null) {
            sender.sendMessage(ChatColor.RED + "无效的丹药类型: " + pillType);
            return;
        }
        
        ItemStack item = createPillItem(pill);
        item.setAmount(amount);
        target.getInventory().addItem(item);
        
        sender.sendMessage(ChatColor.GREEN + "给予 " + target.getName() + " " + pill.name + " x" + amount);
    }
    
    private void usePillFromHand(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            p.sendMessage(ChatColor.YELLOW + "请手持丹药");
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            p.sendMessage(ChatColor.YELLOW + "这不是丹药");
            return;
        }
        
        getLogger().info("Looking for pill: " + meta.getDisplayName() + " type=" + item.getType());
        PillData pill = findPill(meta.getDisplayName(), item.getType());
        if (pill == null) {
            p.sendMessage(ChatColor.YELLOW + "这不是丹药");
            getLogger().info("Pill not found! name=" + meta.getDisplayName());
            return;
        }
        getLogger().info("Found pill: " + pill.name + " xp=" + pill.xpAmount);
        usePill(p, pill, item);
    }
    
    private void usePill(Player p, PillData pill, ItemStack item) {
        getLogger().info("usePill called: " + pill.name);
        // 检查境界
        boolean isLianti = liantiPills.containsValue(pill);
        int playerRealmIdx = getPlayerRealmIndex(p, isLianti);
        
        if (playerRealmIdx < pill.realmIndex) {
            String requiredRealm = isLianti ? 
                getLantiRealmName(pill.realmIndex) : 
                getXufaRealmName(pill.realmIndex);
            p.sendMessage(colorize(getConfig().getString("realm-too-high-message", "境界不足")
                .replace("{required_realm}", requiredRealm)));
            return;
        }
        
        // 给予修为
        boolean success = addXpToPlayer(p, pill.xpAmount, isLianti);
        if (!success) {
            p.sendMessage(ChatColor.RED + "给予修为失败");
            return;
        }
        
        // 消耗物品
        item.setAmount(item.getAmount() - 1);
        
        String msg = getConfig().getString("use-message", "服用 {pill_name} 获得 {xp_amount} 修为");
        p.sendMessage(colorize(msg
            .replace("{pill_name}", pill.name)
            .replace("{xp_amount}", String.valueOf(pill.xpAmount))));
    }
    
    // ========== 修为系统 ==========
    
    private boolean addXpToPlayer(Player p, int xpAmount, boolean isLianti) {
        // 通过XiuXianCore的API添加修为
        Plugin plugin = Bukkit.getPluginManager().getPlugin("XiuXianCore");
        if (plugin == null) {
            getLogger().warning("XiuXianCore not found!");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = plugin.getClass().getMethod("addXp", java.util.UUID.class, double.class, boolean.class);
            method.invoke(plugin, p.getUniqueId(), (double) xpAmount, isLianti);
            // 立即检查升级
            java.lang.reflect.Method levelUpMethod = plugin.getClass().getMethod("checkLevelUp", java.util.UUID.class);
            levelUpMethod.invoke(plugin, p.getUniqueId());
            getLogger().info("Added " + xpAmount + " XP to " + p.getName() + " via XiuXianCore API");
            return true;
        } catch (Exception e) {
            getLogger().warning("Failed to call XiuXianCore API: " + e.getMessage());
            return false;
        }
    }
    
    private int getPlayerRealmIndex(Player p, boolean isLianti) {
        if (xiuxianDataDir == null || !xiuxianDataDir.exists()) return 12;
        
        File file = new File(xiuxianDataDir, p.getUniqueId().toString() + ".json");
        if (!file.exists()) return 12;
        
        try {
            JsonObject json = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            int level = isLianti ? 
                (json.has("liantiLevel") ? json.get("liantiLevel").getAsInt() : 0) :
                (json.has("xiufaLevel") ? json.get("xiufaLevel").getAsInt() : 0);
            return level / 100;
        } catch (Exception e) {
            return 12;
        }
    }
    
    private PillData findPill(String displayName, Material material) {
        for (PillData pill : liantiPills.values()) {
            if (pill.material == material && pill.name.equals(displayName)) return pill;
        }
        for (PillData pill : xiufaPills.values()) {
            if (pill.material == material && pill.name.equals(displayName)) return pill;
        }
        return null;
    }
    
    // ========== 右键服用 ==========
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        
        getLogger().info("Looking for pill: " + meta.getDisplayName() + " type=" + item.getType());
        PillData pill = findPill(meta.getDisplayName(), item.getType());
        if (pill != null) {
            event.setCancelled(true);
            usePill(p, pill, item);
        }
    }
    
    // ========== 工具方法 ==========
    
    private ItemStack createPillItem(PillData pill) {
        ItemStack item = new ItemStack(pill.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(pill.name);
            List<String> lore = new ArrayList<>();
            lore.add(colorize(pill.description));
            lore.add(colorize("&7境界: " + pill.realm));
            lore.add(colorize("&7修为: +" + pill.xpAmount));
            lore.add("");
            lore.add(colorize("&7右键服用"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private String getLantiRealmName(int idx) {
        String[] realms = {"通脉", "锻骨", "练腑", "元武", "神力", "破虚", "混元", "大成", "涅槃", "真武", "金相", "太上", "罗天"};
        return idx >= 0 && idx < realms.length ? realms[idx] : "未知";
    }
    
    private String getXufaRealmName(int idx) {
        String[] realms = {"练气", "筑基", "结丹", "元婴", "化神", "返虚", "合体", "大乘", "渡劫", "真仙", "金仙", "太乙", "大罗"};
        return idx >= 0 && idx < realms.length ? realms[idx] : "未知";
    }
    
    private String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    // ========== 内部类 ==========
    
    static class PillData {
        int realmIndex;
        String name, description, realm;
        int xpAmount;
        Material material;
        
        PillData(int realmIndex, String name, String description, String realm, 
                 int xpAmount, Material material) {
            this.realmIndex = realmIndex;
            this.name = name;
            this.description = description;
            this.realm = realm;
            this.xpAmount = xpAmount;
            this.material = material;
        }
    }
}
