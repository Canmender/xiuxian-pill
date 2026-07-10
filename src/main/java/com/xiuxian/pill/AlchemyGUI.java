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

    private final Map<UUID, String> selectedPill = new HashMap<>();
    private final Map<UUID, Boolean> inDetailView = new HashMap<>();

    // View titles
    private static final String TITLE_OVERVIEW = "\u00a76\u00a7l\u70bc\u4e39\u53f0 \u2500 \u9009\u62e9\u4e39\u836f";
    private static final String TITLE_CRAFT = "\u00a76\u00a7l\u70bc\u4e39\u53f0 \u2500 \u70bc\u5236";

    public AlchemyGUI(XiuXianPill plugin) {
        this.plugin = plugin;
        this.craftManager = plugin.getCraftManager();
        this.alchemistManager = plugin.getAlchemistManager();
    }

    public void open(Player p) {
        openOverview(p);
    }

    // ========== Overview Screen ==========

    private void openOverview(Player p) {
        inDetailView.put(p.getUniqueId(), false);
        Inventory inv = Bukkit.createInventory(new AlchemyHolder(), 54, TITLE_OVERVIEW);

        // Fill background
        fillGlass(inv);

        // Row 0: Alchemist info bar
        drawAlchemistBar(inv, p);

        // Row 1-3: 体修 pills (slots 9-35)
        drawPillSection(inv, p, "lianti", 9);

        // Row 4-5: 法修 pills (slots 36-44)
        drawPillSection(inv, p, "xiufa", 36);

        // Row 5: Close button
        inv.setItem(49, createItem(Material.BARRIER, "\u00a7c\u5173\u95ed"));

        p.openInventory(inv);
    }

    private void drawAlchemistBar(Inventory inv, Player p) {
        int level = alchemistManager.getLevel(p.getUniqueId());
        int xp = alchemistManager.getXp(p.getUniqueId());
        int xpNeed = alchemistManager.getXpToNext(p.getUniqueId());
        String name = alchemistManager.getName(p.getUniqueId());
        double bonus = alchemistManager.getBonus(p.getUniqueId());

        // Progress bar
        String progress = getProgressBar(xp, xpNeed, 20);

        ItemStack alcInfo = createItem(Material.BREWING_STAND,
            "\u00a76\u00a7l\u2550\u2550\u2550 \u70bc\u4e39\u5e08\u4fe1\u606f \u2550\u2550\u2550",
            "",
            "\u00a77\u7b49\u7ea7: \u00a7e" + name,
            "\u00a77\u7ecf\u9a8c: \u00a7e" + xp + " \u00a77/ \u00a7e" + xpNeed,
            " \u00a7a" + progress,
            "",
            "\u00a77\u9ad8\u54c1\u8d28\u52a0\u6210: \u00a7a+" + (int)(bonus * 100) + "%",
            "",
            "\u00a78\u6bcf\u6b21\u70bc\u4e39\u6210\u529f +1~10 \u70bc\u4e39\u5e08XP");
        inv.setItem(4, alcInfo);
    }

    private void drawPillSection(Inventory inv, Player p, String type, int startSlot) {
        Map<String, Map<String, Object>> allPills = plugin.getAllPills();
        List<String> pillIds = new ArrayList<>();
        for (String id : allPills.keySet()) {
            if (type.equals("lianti") && id.startsWith("lt")) pillIds.add(id);
            if (type.equals("xiufa") && id.startsWith("xf")) pillIds.add(id);
        }
        Collections.sort(pillIds);

        String sectionName = type.equals("lianti") ? "\u00a7a\u00a7l\u4f53\u4fee\u4e39\u836f" : "\u00a7b\u00a7l\u6cd5\u4fee\u4e39\u836f";
        inv.setItem(startSlot - 1, createItem(Material.PAPER, sectionName));

        for (int i = 0; i < pillIds.size() && i < 13; i++) {
            String id = pillIds.get(i);
            Map<String, Object> pill = allPills.get(id);
            String pillName = (String) pill.get("name");
            int xpAmount = (int) pill.get("xp-amount");
            String material = (String) pill.get("material");

            // Get realm name from PillData
            XiuXianPill.PillData pd = plugin.getPillData(id);
            String realm = pd != null ? pd.realm : "?";

            Material icon;
            if (xpAmount <= 200) icon = Material.COAL;
            else if (xpAmount <= 1500) icon = Material.COAL_BLOCK;
            else if (xpAmount <= 8000) icon = Material.IRON_INGOT;
            else if (xpAmount <= 35000) icon = Material.GOLD_INGOT;
            else icon = Material.DIAMOND;

            int slot = startSlot + i;
            if (slot >= 54) break;
            inv.setItem(slot, createItem(icon, pillName,
                "\u00a77\u5883\u754c: \u00a7e" + realm,
                "\u00a77\u4fee\u4e3a: \u00a7a+" + xpAmount,
                "\u00a77\u8017\u6750: \u00a7e" + material,
                "",
                "\u00a7e\u70b9\u51fb\u9009\u62e9\u70bc\u5236"));
        }
    }

    // ========== Craft Detail Screen ==========

    private void openCraftDetail(Player p, String pillId) {
        inDetailView.put(p.getUniqueId(), true);
        selectedPill.put(p.getUniqueId(), pillId);

        Inventory inv = Bukkit.createInventory(new AlchemyHolder(), 54, TITLE_CRAFT);
        fillGlass(inv);

        Map<String, Object> pill = plugin.getAllPills().get(pillId);
        if (pill == null) return;

        int realmIndex = getRealmIndex(pillId);
        int matCost = craftManager.getMaterialCost(realmIndex);
        int moneyCost = craftManager.getMoneyCost(realmIndex);
        String materialId = (String) pill.get("material");
        String pillName = (String) pill.get("name");

        // Row 0: Alchemist info bar
        drawAlchemistBar(inv, p);

        // Row 1: Pill info
        inv.setItem(13, createItem(Material.POTION, "\u00a7e\u00a7l" + pillName,
            "\u00a77\u5883\u754c: \u00a7e" + getPillRealm(pillId),
            "\u00a77\u4fee\u4e3a: \u00a7a+" + pill.get("xp-amount"),
            "",
            "\u00a77\u6240\u9700\u8017\u6750: \u00a7e" + matCost + " \u4e2a " + materialId,
            "\u00a77\u6240\u9700\u7075\u77f3: \u00a7e" + moneyCost));

        // Row 2: Quality preview
        drawQualityPreview(inv);

        // Row 3: Cost display
        inv.setItem(31, createItem(Material.GOLD_NUGGET, "\u00a76\u7075\u77f3\u6d88\u8017: \u00a7e" + moneyCost));
        inv.setItem(29, createItem(Material.PAPER, "\u00a77\u6240\u9700\u8017\u6750: \u00a7e" + matCost + " \u4e2a"));

        // Row 4: Craft button
        inv.setItem(40, createItem(Material.FIRE_CHARGE, "\u00a7c\u00a7l\u25b6 \u5f00\u59cb\u70bc\u5236"));

        // Row 5: Back + Close
        inv.setItem(48, createItem(Material.ARROW, "\u00a7e\u8fd4\u56de\u603b\u89c8"));
        inv.setItem(49, createItem(Material.BARRIER, "\u00a7c\u5173\u95ed"));

        p.openInventory(inv);
    }

    private void drawQualityPreview(Inventory inv) {
        String[][] qualities = {
            {"\u00a77\u51e1\u54c1", "40%"},
            {"\u00a7a\u7075\u54c1", "25%"},
            {"\u00a7b\u5b9d\u54c1", "15%"},
            {"\u00a76\u5723\u54c1", "10%"},
            {"\u00a75\u4ed9\u54c1", "5%"},
            {"\u00a7c\u5929\u54c1", "3%"},
            {"\u00a74\u795e\u54c1", "1%"}
        };
        for (int i = 0; i < qualities.length; i++) {
            inv.setItem(20 + i, createItem(Material.PAPER,
                qualities[i][0] + "\u7075\u4e39",
                "\u00a77\u6982\u7387: \u00a7e" + qualities[i][1],
                "\u00a77\u4fee\u4e3a\u500d\u7387: \u00a7a" + getQualityXpMult(i) + "x"));
        }
        inv.setItem(28, createItem(Material.BARRIER, "\u00a7c\u62a5\u5e9f 1%"));
    }

    // ========== Events ==========

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof AlchemyHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        UUID uuid = p.getUniqueId();
        boolean isDetail = inDetailView.getOrDefault(uuid, false);

        if (!isDetail) {
            // Overview mode
            handleOverviewClick(p, slot);
        } else {
            // Detail mode
            handleDetailClick(p, slot);
        }
    }

    private void handleOverviewClick(Player p, int slot) {
        // Check if clicked on a pill slot
        ItemStack item = p.getOpenInventory().getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Find pill by display name
        String clickedName = meta.getDisplayName();
        for (Map.Entry<String, Map<String, Object>> entry : plugin.getAllPills().entrySet()) {
            String pillName = (String) entry.getValue().get("name");
            if (clickedName.equals(pillName)) {
                openCraftDetail(p, entry.getKey());
                return;
            }
        }

        // Close button
        if (slot == 49) {
            p.closeInventory();
        }
    }

    private void handleDetailClick(Player p, int slot) {
        switch (slot) {
            case 40: // Craft
                String pillId = selectedPill.get(p.getUniqueId());
                if (pillId != null) doCraft(p, pillId);
                break;
            case 48: // Back
                openOverview(p);
                break;
            case 49: // Close
                p.closeInventory();
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof AlchemyHolder) {
            UUID uuid = e.getPlayer().getUniqueId();
            inDetailView.remove(uuid);
            selectedPill.remove(uuid);
        }
    }

    // ========== Craft Logic ==========

    private void doCraft(Player p, String pillId) {
        Map<String, Object> pill = plugin.getAllPills().get(pillId);
        if (pill == null) return;

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

        // Check money
        if (!hasMoney(p, moneyCost)) {
            p.sendMessage("\u00a7c\u7075\u77f3\u4e0d\u8db3\uff01\u9700\u8981 \u00a7e" + moneyCost + " \u00a7c\u7075\u77f3");
            return;
        }

        // Consume
        removeMaterial(p, materialId, matCost);
        removeMoney(p, moneyCost);

        // Craft
        double bonus = alchemistManager.getBonus(p.getUniqueId());
        PillCraftingManager.CraftResult result = craftManager.craft(realmIndex, bonus);

        if (!result.success) {
            p.sendMessage("\u00a7c\u00a7l\u3010\u70bc\u4e39\u5931\u8d25\u3011\u00a77\u4e39\u836f\u7206\u5e9f\uff0c\u6750\u6599\u5df2\u6d88\u8017\uff01");
            alchemistManager.addXp(p, 1 + new Random().nextInt(3));
            return;
        }

        // Success
        int xpPill = (int) pill.get("xp-amount");
        int finalXp = (int)(xpPill * getQualityXpMult(result.qualityIndex));
        String qualityName = craftManager.getQualityName(result.qualityIndex);
        String qualityColor = craftManager.getQualityColor(result.qualityIndex);

        boolean ok = giveXp(p, finalXp, pillId.startsWith("lt"));
        if (ok) {
            p.sendMessage(qualityColor + "\u00a7l\u3010\u70bc\u4e39\u6210\u529f\u3011\u00a77\u83b7\u5f97 " + qualityColor + qualityName + "\u7075\u4e39\uff0c\u83b7\u5f97 \u00a7e" + finalXp + " \u00a77\u4fee\u4e3a");
        }

        alchemistManager.addXp(p, result.alchemistXp);
    }

    // ========== Helpers ==========

    private String getProgressBar(int current, int max, int length) {
        if (max <= 0) max = 1;
        int filled = (int) ((double) current / max * length);
        filled = Math.min(filled, length);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? "\u00a7a\u2588" : "\u00a78\u2591");
        }
        bar.append("\u00a77]");
        return bar.toString();
    }

    private double getQualityXpMult(int index) {
        double[] mults = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
        return index >= 0 && index < mults.length ? mults[index] : 1.0;
    }

    private String getPillRealm(String pillId) {
        XiuXianPill.PillData pd = plugin.getPillData(pillId);
        return pd != null ? pd.realm : "?";
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); glass.setItemMeta(meta); }
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private int countMaterial(Player p, String materialId) {
        int count = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            String itemId = getItemId(item);
            if (itemId != null && itemId.equals(materialId)) count += item.getAmount();
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
            org.bukkit.plugin.Plugin xc = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xc != null) {
                java.lang.reflect.Method m = xc.getClass().getMethod("has", Player.class, double.class);
                return (boolean) m.invoke(xc, p, (double) amount);
            }
        } catch (Exception e) {}
        return false;
    }

    private void removeMoney(Player p, int amount) {
        try {
            org.bukkit.plugin.Plugin xc = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xc != null) {
                java.lang.reflect.Method m = xc.getClass().getMethod("pay", Player.class, String.class, double.class);
                m.invoke(xc, p, "main", (double) amount);
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

    private int getRealmIndex(String pillId) {
        try { return Integer.parseInt(pillId.substring(2)); }
        catch (Exception e) { return 0; }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> loreList = new ArrayList<>();
            for (String line : lore) loreList.add(ChatColor.translateAlternateColorCodes('&', line));
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