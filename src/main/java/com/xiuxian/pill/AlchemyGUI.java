package com.xiuxian.pill;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AlchemyGUI implements Listener {

    private final XiuXianPill plugin;
    private final PillCraftingManager craftManager;
    private final AlchemistManager alchemistManager;

    // GUI state per player: selected pill type
    private final Map<UUID, String> selectedPill = new HashMap<>();

    // Slot definitions
    private static final int SLOT_PILL_TYPE = 4;      // row1 col5 - pill type display
    private static final int SLOT_OUTPUT = 22;         // row3 col4 - output preview
    private static final int SLOT_MATERIAL = 29;       // row4 col2 - material input
    private static final int SLOT_COST_DISPLAY = 31;   // row4 col4 - cost display
    private static final int SLOT_CRAFT = 40;          // row5 col4 - craft button
    private static final int SLOT_CLOSE = 43;          // row5 col7 - close button
    private static final int SLOT_ALCHEMIST = 4;       // reuse for alchemist info

    public AlchemyGUI(XiuXianPill plugin) {
        this.plugin = plugin;
        this.craftManager = plugin.getCraftManager();
        this.alchemistManager = plugin.getAlchemistManager();
    }

    public void open(Player p) {
        open(p, null);
    }

    public void open(Player p, String pillId) {
        if (pillId != null) {
            selectedPill.put(p.getUniqueId(), pillId);
        }
        if (!selectedPill.containsKey(p.getUniqueId())) {
            // Default to first available pill
            selectedPill.put(p.getUniqueId(), "lt0");
        }

        Inventory inv = Bukkit.createInventory(new AlchemyHolder(), 54, "\u00a76\u00a7l\u70bc\u4e39\u53f0");
        fillBackground(inv);
        updateDisplay(inv, p);
        p.openInventory(inv);
    }

    private void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }
    }

    private void updateDisplay(Inventory inv, Player p) {
        String pillId = selectedPill.getOrDefault(p.getUniqueId(), "lt0");
        Map<String, Map<String, Object>> pills = plugin.getAllPills();
        if (!pills.containsKey(pillId)) return;

        Map<String, Object> pill = pills.get(pillId);
        int realmIndex = getRealmIndex(pillId);

        // Alchemist info (slot 4)
        int alcLevel = alchemistManager.getLevel(p.getUniqueId());
        int alcXp = alchemistManager.getXp(p.getUniqueId());
        int alcXpNeed = alchemistManager.getXpToNext(p.getUniqueId());
        double alcBonus = alchemistManager.getBonus(p.getUniqueId());
        String alcName = alchemistManager.getName(p.getUniqueId());

        ItemStack alcItem = createItem(Material.BREWING_STAND, "\u00a76\u00a7l\u70bc\u4e39\u5e08\u7b49\u7ea7",
            "\u00a77\u7b49\u7ea7: \u00a7e" + alcName,
            "\u00a77XP: \u00a7e" + alcXp + " \u00a77/ \u00a7e" + alcXpNeed,
            "\u00a77\u9ad8\u54c1\u8d28\u52a0\u6210: \u00a7a+" + (int)(alcBonus * 100) + "%");
        inv.setItem(SLOT_ALCHEMIST, alcItem);

        // Pill type display (slot 13)
        String pillName = (String) pill.get("name");
        ItemStack pillDisplay = createItem(Material.POTION, "\u00a7e\u5f53\u524d\u4e39\u836f: " + pillName,
            "\u00a77\u70b9\u51fb\u5de6\u4fa7\u5207\u6362\u4e39\u836f\u7c7b\u578b",
            "\u00a77\u5f53\u524d: \u00a7e" + pillId);
        inv.setItem(SLOT_PILL_TYPE, pillDisplay);

        // Left/Right arrows for pill selection (slots 9 and 17)
        inv.setItem(9, createItem(Material.ARROW, "\u00a7a\u4e0a\u4e00\u4e2a\u4e39\u836f"));
        inv.setItem(17, createItem(Material.ARROW, "\u00a7a\u4e0b\u4e00\u4e2a\u4e39\u836f"));

        // Material cost display (slot 29)
        String materialId = (String) pill.get("material");
        int matCost = craftManager.getMaterialCost(realmIndex);
        int moneyCost = craftManager.getMoneyCost(realmIndex);
        ItemStack matItem = createItem(Material.PAPER, "\u00a77\u6240\u9700\u8017\u6750",
            "\u00a77\u7c7b\u578b: \u00a7e" + materialId,
            "\u00a77\u6570\u91cf: \u00a7e" + matCost + " \u4e2a",
            "\u00a77\u7075\u77f3\u6d88\u8017: \u00a7e" + moneyCost);
        inv.setItem(SLOT_MATERIAL, matItem);

        // Cost display (slot 31)
        ItemStack costItem = createItem(Material.GOLD_NUGGET, "\u00a76\u7075\u77f3\u6d88\u8017: \u00a7e" + moneyCost,
            "\u00a77\u70bc\u5236\u4e00\u6b21\u9700\u8981\u4ee5\u4e0a\u7075\u77f3");
        inv.setItem(SLOT_COST_DISPLAY, costItem);

        // Output preview (slot 22) - show all possible qualities
        ItemStack output = createItem(Material.GHAST_TEAR, "\u00a76\u70bc\u5236\u4ea7\u51fa",
            "\u00a77\u53ef\u80fd\u4ea7\u51fa\u4ee5\u4e0b\u54c1\u8d28:",
            "\u00a77\u51e1\u54c1\u7075\u4e39 (40%)",
            "\u00a7a\u7075\u54c1\u7075\u4e39 (25%)",
            "\u00a7b\u5b9d\u54c1\u7075\u4e39 (15%)",
            "\u00a76\u5723\u54c1\u7075\u4e39 (10%)",
            "\u00a75\u4ed9\u54c1\u7075\u4e39 (5%)",
            "\u00a7c\u5929\u54c1\u7075\u4e39 (3%)",
            "\u00a74\u795e\u54c1\u7075\u4e39 (1%)",
            "",
            "\u00a7c\u62a5\u5e9f\u6982\u7387: 1%");
        inv.setItem(SLOT_OUTPUT, output);

        // Craft button (slot 40)
        ItemStack craftBtn = createItem(Material.FIRE_CHARGE, "\u00a7c\u00a7l\u5f00\u59cb\u70bc\u5236",
            "\u00a77\u70b9\u51fb\u5f00\u59cb\u70bc\u5236\u4e39\u836f");
        inv.setItem(SLOT_CRAFT, craftBtn);

        // Close button (slot 43)
        inv.setItem(SLOT_CLOSE, createItem(Material.BARRIER, "\u00a7c\u5173\u95ed"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof AlchemyHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        String pillId = selectedPill.getOrDefault(p.getUniqueId(), "lt0");

        switch (slot) {
            case 9: // Previous pill
                cyclePill(p, -1);
                break;
            case 17: // Next pill
                cyclePill(p, 1);
                break;
            case 40: // Craft
                doCraft(p, pillId);
                break;
            case 43: // Close
                p.closeInventory();
                break;
        }
        // Refresh display
        if (slot == 9 || slot == 17 || slot == 40) {
            updateDisplay(e.getInventory(), p);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof AlchemyHolder) {
            // Cleanup if needed
        }
    }

    private void cyclePill(Player p, int direction) {
        List<String> pillIds = new ArrayList<>(plugin.getAllPills().keySet());
        Collections.sort(pillIds);
        String current = selectedPill.getOrDefault(p.getUniqueId(), pillIds.get(0));
        int idx = pillIds.indexOf(current);
        idx = (idx + direction + pillIds.size()) % pillIds.size();
        selectedPill.put(p.getUniqueId(), pillIds.get(idx));
    }

    private void doCraft(Player p, String pillId) {
        Map<String, Object> pill = plugin.getAllPills().get(pillId);
        if (pill == null) {
            p.sendMessage("\u00a7c\u672a\u627e\u5230\u4e39\u836f\u914d\u65b9");
            return;
        }

        int realmIndex = getRealmIndex(pillId);
        String materialId = (String) pill.get("material");
        int matCost = craftManager.getMaterialCost(realmIndex);
        int moneyCost = craftManager.getMoneyCost(realmIndex);

        // Check materials
        int playerMats = countMaterial(p, materialId);
        if (playerMats < matCost) {
            p.sendMessage("\u00a7c\u8017\u6750\u4e0d\u8db3\uff01\u9700\u8981 \u00a7e" + matCost + " \u00a7c\u4e2a " + materialId + "\uff0c\u5f53\u524d\u6709 \u00a7e" + playerMats);
            return;
        }

        // Check money (XConomy)
        if (!hasMoney(p, moneyCost)) {
            p.sendMessage("\u00a7c\u7075\u77f3\u4e0d\u8db3\uff01\u9700\u8981 \u00a7e" + moneyCost + " \u00a7c\u7075\u77f3");
            return;
        }

        // Consume materials
        removeMaterial(p, materialId, matCost);
        removeMoney(p, moneyCost);

        // Craft
        double bonus = alchemistManager.getBonus(p.getUniqueId());
        PillCraftingManager.CraftResult result = craftManager.craft(realmIndex, bonus);

        if (!result.success) {
            // Scrap
            p.sendMessage("\u00a7c\u00a7l\u3010\u70bc\u4e39\u5931\u8d25\u3011\u00a77\u4e39\u836f\u7206\u5e9f\uff0c\u6750\u6599\u5df2\u6d88\u8017\uff01");
            // Still give alchemist XP on failure (1-3)
            alchemistManager.addXp(p, 1 + new Random().nextInt(3));
            return;
        }

        // Success - give pill
        int xpPill = (int) pill.get("xp-amount");
        int finalXp = (int)(xpPill * getQualityMultiplier(result.qualityIndex));
        String qualityName = craftManager.getQualityName(result.qualityIndex);
        String qualityColor = craftManager.getQualityColor(result.qualityIndex);

        // Give pill via XiuXianCore API
        boolean ok = giveXp(p, finalXp, isLiantiPill(pillId));

        if (ok) {
            p.sendMessage(qualityColor + "\u00a7l\u3010\u70bc\u4e39\u6210\u529f\u3011\u00a77\u83b7\u5f97 " + qualityColor + qualityName + "\u7075\u4e39\uff0c\u83b7\u5f97 \u00a7e" + finalXp + " \u00a77\u4fee\u4e3a");
        }

        // Alchemist XP
        alchemistManager.addXp(p, result.alchemistXp);
    }

    private double getQualityMultiplier(int qualityIndex) {
        double[] mults = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
        return qualityIndex >= 0 && qualityIndex < mults.length ? mults[qualityIndex] : 1.0;
    }

    private int countMaterial(Player p, String materialId) {
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            // Check if this is the right material by custom-model-data and name
            String itemId = getItemId(item);
            if (itemId != null && itemId.equals(materialId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player p, String materialId, int amount) {
        int remaining = amount;
        for (ItemStack item : p.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item == null) continue;
            String itemId = getItemId(item);
            if (itemId != null && itemId.equals(materialId)) {
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
    }

    private String getItemId(ItemStack item) {
        // Try to get item ID from XiuXianItems
        try {
            org.bukkit.plugin.Plugin ia = Bukkit.getPluginManager().getPlugin("XiuXianItems");
            if (ia != null) {
                java.lang.reflect.Method m = ia.getClass().getMethod("getItemId", ItemStack.class);
                Object result = m.invoke(ia, item);
                return result != null ? result.toString() : null;
            }
        } catch (Exception e) {}
        return null;
    }

    private boolean hasMoney(Player p, int amount) {
        try {
            org.bukkit.plugin.Plugin xconomy = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xconomy != null) {
                java.lang.reflect.Method m = xconomy.getClass().getMethod("has", Player.class, double.class);
                return (boolean) m.invoke(xconomy, p, (double) amount);
            }
        } catch (Exception e) {}
        return false;
    }

    private void removeMoney(Player p, int amount) {
        try {
            org.bukkit.plugin.Plugin xconomy = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xconomy != null) {
                java.lang.reflect.Method m = xconomy.getClass().getMethod("pay", Player.class, String.class, double.class);
                m.invoke(xconomy, p, "main", (double) amount);
            }
        } catch (Exception e) {}
    }

    private boolean giveXp(Player p, int amount, boolean isLianti) {
        try {
            org.bukkit.plugin.Plugin core = Bukkit.getPluginManager().getPlugin("XiuXianCore");
            if (core != null) {
                java.lang.reflect.Method m = core.getClass().getMethod("addXp", UUID.class, double.class, boolean.class);
                m.invoke(core, p.getUniqueId(), (double) amount, isLianti);
                java.lang.reflect.Method check = core.getClass().getMethod("checkLevelUp", UUID.class);
                check.invoke(core, p.getUniqueId());
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    private boolean isLiantiPill(String pillId) {
        return pillId.startsWith("lt");
    }

    private int getRealmIndex(String pillId) {
        try {
            String num = pillId.substring(2);
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class AlchemyHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}