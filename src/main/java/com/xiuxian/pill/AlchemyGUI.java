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
    private final Map<UUID, Boolean> inDetail = new HashMap<>();

    private static final String T_OVERVIEW = "\u00a76\u00a7l\u70bc\u4e39\u53f0";
    private static final String T_CRAFT = "\u00a76\u00a7l\u70bc\u5236";

    public AlchemyGUI(XiuXianPill plugin) {
        this.plugin = plugin;
        this.craftManager = plugin.getCraftManager();
        this.alchemistManager = plugin.getAlchemistManager();
    }

    public void open(Player p) { showOverview(p); }

    // ==================== OVERVIEW ====================
    // Layout:
    // Row 0: [alchemist info______________]
    // Row 1: [体修 label][p1][p2][p3][p4][p5][p6][p7][p8]
    // Row 2: [         ][p9][p10][p11][p12][p13][___][___][___]
    // Row 3: [法修 label][p1][p2][p3][p4][p5][p6][p7][p8]
    // Row 4: [         ][p9][p10][p11][p12][p13][___][___][___]
    // Row 5: [__________][______][close][________]

    private void showOverview(Player p) {
        inDetail.put(p.getUniqueId(), false);
        Inventory inv = Bukkit.createInventory(new Holder(), 54, T_OVERVIEW);

        // Row 0: Alchemist info
        drawAlchemistRow(inv, p, 0);

        // Row 1-2: 体修 (13 pills -> 9 in row1, 4 in row2)
        List<String> lt = getPillsByType("lt");
        for (int i = 0; i < lt.size(); i++) {
            int slot = (i < 9) ? (9 + i) : (18 + i - 9);
            inv.setItem(slot, makePillIcon(lt.get(i)));
        }
        inv.setItem(18, label("\u00a7a\u00a7l\u25b6 \u4f53\u4fee"));

        // Row 3-4: 法修 (13 pills -> 9 in row3, 4 in row4)
        List<String> xf = getPillsByType("xf");
        for (int i = 0; i < xf.size(); i++) {
            int slot = (i < 9) ? (27 + i) : (36 + i - 9);
            inv.setItem(slot, makePillIcon(xf.get(i)));
        }
        inv.setItem(36, label("\u00a7b\u00a7l\u25b6 \u6cd5\u4fee"));

        // Row 5: close
        inv.setItem(49, createItem(Material.BARRIER, "\u00a7c\u5173\u95ed"));

        p.openInventory(inv);
    }

    // ==================== CRAFT DETAIL ====================
    // Row 0: [alchemist info______________]
    // Row 1: [___][____][pill name________][____][___]
    // Row 2: [凡][灵][宝][圣][仙][天][神][报废][__]
    // Row 3: [___][material cost][___][money cost][___]
    // Row 4: [__________][__craft__][________]
    // Row 5: [__back__][__close__][________]

    private void showCraft(Player p, String id) {
        inDetail.put(p.getUniqueId(), true);
        selectedPill.put(p.getUniqueId(), id);
        Inventory inv = Bukkit.createInventory(new Holder(), 54, T_CRAFT);

        Map<String, Object> pill = plugin.getAllPills().get(id);
        if (pill == null) return;
        int ri = getRealmIndex(id);
        int matCost = craftManager.getMaterialCost(ri);
        int moneyCost = craftManager.getMoneyCost(ri);
        String matId = (String) pill.get("material");

        // Row 0: Alchemist
        drawAlchemistRow(inv, p, 0);

        // Row 1: Pill name + info
        inv.setItem(13, createItem(Material.POTION,
            "\u00a7e\u00a7l" + pill.get("name"),
            "\u00a77\u5883\u754c: \u00a7e" + getRealm(id),
            "\u00a77\u4fee\u4e3a: \u00a7a+" + pill.get("xp-amount"),
            "\u00a77\u8017\u6750: \u00a7e" + matId + " x" + matCost,
            "\u00a77\u7075\u77f3: \u00a7e" + moneyCost));

        // Row 2: Quality table
        String[][] q = {{"\u00a77\u51e1\u54c1","40%","1x"},{"\u00a7a\u7075\u54c1","25%","2x"},
            {"\u00a7b\u5b9d\u54c1","15%","4x"},{"\u00a76\u5723\u54c1","10%","8x"},
            {"\u00a75\u4ed9\u54c1","5%","16x"},{"\u00a7c\u5929\u54c1","3%","32x"},
            {"\u00a74\u795e\u54c1","1%","64x"}};
        for (int i = 0; i < 7; i++) {
            inv.setItem(19 + i, createItem(Material.PAPER,
                q[i][0], "\u00a77\u6982\u7387: \u00a7e" + q[i][1], "\u00a77\u500d\u7387: \u00a7a" + q[i][2]));
        }
        inv.setItem(27, createItem(Material.BARRIER, "\u00a7c\u62a5\u5e9f 1%"));

        // Row 3: Cost summary
        inv.setItem(30, createItem(Material.PAPER, "\u00a77\u6240\u9700\u8017\u6750", "\u00a7e" + matId + " x" + matCost));
        inv.setItem(32, createItem(Material.GOLD_NUGGET, "\u00a77\u6240\u9700\u7075\u77f3", "\u00a7e" + moneyCost));

        // Row 4: Craft
        inv.setItem(40, createItem(Material.FIRE_CHARGE, "\u00a7c\u00a7l\u25b6 \u5f00\u59cb\u70bc\u5236"));

        // Row 5: Back + Close
        inv.setItem(48, createItem(Material.ARROW, "\u00a7e\u8fd4\u56de"));
        inv.setItem(49, createItem(Material.BARRIER, "\u00a7c\u5173\u95ed"));

        p.openInventory(inv);
    }

    // ==================== ALCHEMIST BAR ====================

    private void drawAlchemistRow(Inventory inv, Player p, int startSlot) {
        int lv = alchemistManager.getLevel(p.getUniqueId());
        int xp = alchemistManager.getXp(p.getUniqueId());
        int xpMax = alchemistManager.getXpToNext(p.getUniqueId());
        String name = alchemistManager.getName(p.getUniqueId());
        double bonus = alchemistManager.getBonus(p.getUniqueId());
        String bar = bar(xp, xpMax, 20);

        inv.setItem(startSlot + 4, createItem(Material.BREWING_STAND,
            "\u00a76\u00a7l\u2550\u2550\u2550 \u70bc\u4e39\u5e08 \u2550\u2550\u2550",
            "",
            " \u00a77\u7b49\u7ea7: \u00a7e" + name + "    \u00a77XP: \u00a7e" + xp + "/" + xpMax,
            " \u00a7a" + bar,
            "",
            " \u00a77\u9ad8\u54c1\u8d28\u52a0\u6210: \u00a7a+" + (int)(bonus*100) + "%",
            " \u00a78\u6bcf\u6b21\u70bc\u4e39\u6210\u529f +1~10 XP"));
    }

    private String bar(int cur, int max, int len) {
        if (max <= 0) max = 1;
        int fill = Math.min((int)((double)cur/max*len), len);
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < len; i++) s.append(i < fill ? "\u00a7a\u2588" : "\u00a78\u2591");
        return s.toString() + "\u00a77]";
    }

    // ==================== EVENTS ====================

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int s = e.getRawSlot();
        if (s < 0 || s >= 54) return;

        UUID u = p.getUniqueId();
        if (!inDetail.getOrDefault(u, false)) {
            // Overview: find which pill was clicked
            ItemStack item = e.getInventory().getItem(s);
            if (item == null) return;
            ItemMeta m = item.getItemMeta();
            if (m == null || !m.hasDisplayName()) {
                if (s == 49) p.closeInventory();
                return;
            }
            String name = m.getDisplayName();
            for (Map.Entry<String, Map<String, Object>> en : plugin.getAllPills().entrySet()) {
                if (name.equals(en.getValue().get("name"))) {
                    showCraft(p, en.getKey());
                    return;
                }
            }
        } else {
            // Detail
            if (s == 40) doCraft(p, selectedPill.getOrDefault(u, ""));
            else if (s == 48) showOverview(p);
            else if (s == 49) p.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) {
            UUID u = e.getPlayer().getUniqueId();
            inDetail.remove(u);
            selectedPill.remove(u);
        }
    }

    // ==================== CRAFT ====================

    private void doCraft(Player p, String id) {
        Map<String, Object> pill = plugin.getAllPills().get(id);
        if (pill == null) return;
        int ri = getRealmIndex(id);
        String matId = (String) pill.get("material");
        int matCost = craftManager.getMaterialCost(ri);
        int moneyCost = craftManager.getMoneyCost(ri);

        if (countMat(p, matId) < matCost) {
            p.sendMessage("\u00a7c\u8017\u6750\u4e0d\u8db3\uff01\u9700\u8981 " + matCost + " \u4e2a " + matId);
            return;
        }
        if (!hasMoney(p, moneyCost)) {
            p.sendMessage("\u00a7c\u7075\u77f3\u4e0d\u8db3\uff01\u9700\u8981 " + moneyCost + " \u7075\u77f3");
            return;
        }
        removeMat(p, matId, matCost);
        payMoney(p, moneyCost);

        double bonus = alchemistManager.getBonus(p.getUniqueId());
        PillCraftingManager.CraftResult r = craftManager.craft(ri, bonus);

        if (!r.success) {
            p.sendMessage("\u00a7c\u00a7l\u3010\u7206\u5e9f\u3011\u00a77\u4e39\u836f\u7206\u70b8\uff0c\u6750\u6599\u6d6a\u8d39\uff01");
            alchemistManager.addXp(p, 1 + new Random().nextInt(3));
            return;
        }

        int baseXp = (int) pill.get("xp-amount");
        int finalXp = (int)(baseXp * craftManager.getXpMult(r.qualityIndex));
        String qName = craftManager.getQualityName(r.qualityIndex);
        String qColor = craftManager.getQualityColor(r.qualityIndex);

        giveXp(p, finalXp, id.startsWith("lt"));
        p.sendMessage(qColor + "\u00a7l\u3010\u70bc\u4e39\u6210\u529f\u3011" + qColor + qName + "\u7075\u4e39\uff0c\u83b7\u5f97 \u00a7e" + finalXp + " \u00a77\u4fee\u4e3a");
        alchemistManager.addXp(p, r.alchemistXp);
    }

    // ==================== HELPERS ====================

    private List<String> getPillsByType(String prefix) {
        List<String> list = new ArrayList<>();
        for (String id : plugin.getAllPills().keySet()) {
            if (id.startsWith(prefix)) list.add(id);
        }
        Collections.sort(list);
        return list;
    }

    private ItemStack makePillIcon(String id) {
        Map<String, Object> p = plugin.getAllPills().get(id);
        if (p == null) return new ItemStack(Material.AIR);
        String name = (String) p.get("name");
        int xp = (int) p.get("xp-amount");
        String mat = (String) p.get("material");
        String realm = getRealm(id);

        Material icon;
        if (xp <= 200) icon = Material.COAL;
        else if (xp <= 1500) icon = Material.COAL_BLOCK;
        else if (xp <= 8000) icon = Material.IRON_INGOT;
        else if (xp <= 35000) icon = Material.GOLD_INGOT;
        else icon = Material.DIAMOND;

        return createItem(icon, name,
            "\u00a77\u5883\u754c: \u00a7e" + realm,
            "\u00a77\u4fee\u4e3a: \u00a7a+" + xp,
            "\u00a77\u8017\u6750: \u00a7e" + mat,
            "",
            "\u00a7e\u70b9\u51fb\u70bc\u5236");
    }

    private ItemStack label(String name) {
        return createItem(Material.PAPER, name);
    }

    private String getRealm(String id) {
        XiuXianPill.PillData pd = plugin.getPillData(id);
        return pd != null ? pd.realm : "?";
    }

    private int getRealmIndex(String id) {
        try { return Integer.parseInt(id.substring(2)); } catch (Exception e) { return 0; }
    }

    private int countMat(Player p, String matId) {
        int c = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            String iid = getItemId(it);
            if (matId.equals(iid)) c += it.getAmount();
        }
        return c;
    }

    private void removeMat(Player p, String matId, int amt) {
        int rem = amt;
        for (ItemStack it : p.getInventory().getContents()) {
            if (rem <= 0) break;
            if (it == null) continue;
            if (matId.equals(getItemId(it))) {
                int take = Math.min(rem, it.getAmount());
                it.setAmount(it.getAmount() - take);
                rem -= take;
            }
        }
    }

    private String getItemId(ItemStack it) {
        try {
            org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("XiuXianItems");
            if (pl != null) return (String) pl.getClass().getMethod("getItemId", ItemStack.class).invoke(pl, it);
        } catch (Exception e) {}
        return null;
    }

    private boolean hasMoney(Player p, int amt) {
        try {
            org.bukkit.plugin.Plugin xc = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xc != null) return (boolean) xc.getClass().getMethod("has", Player.class, double.class).invoke(xc, p, (double)amt);
        } catch (Exception e) {}
        return false;
    }

    private void payMoney(Player p, int amt) {
        try {
            org.bukkit.plugin.Plugin xc = Bukkit.getPluginManager().getPlugin("XConomy");
            if (xc != null) xc.getClass().getMethod("pay", Player.class, String.class, double.class).invoke(xc, p, "main", (double)amt);
        } catch (Exception e) {}
    }

    private void giveXp(Player p, int amt, boolean lianti) {
        try {
            org.bukkit.plugin.Plugin c = Bukkit.getPluginManager().getPlugin("XiuXianCore");
            if (c != null) {
                c.getClass().getMethod("addXp", UUID.class, double.class, boolean.class).invoke(c, p.getUniqueId(), (double)amt, lianti);
                c.getClass().getMethod("checkLevelUp", UUID.class).invoke(c, p.getUniqueId());
            }
        } catch (Exception e) {}
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
            m.setLore(l);
            it.setItemMeta(m);
        }
        return it;
    }

    public static class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}