# 💊 XiuXianPill

> MC 修仙服丹药插件 — 经验丹服用系统

[![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 简介

XiuXianPill 提供完整的丹药服用系统，包含 **26 种经验丹**（体修 13 种 + 法修 13 种）。每种丹药对应一个境界，只能服用 <= 当前境界的丹药，手持右键即可使用。

## 功能特性

| 系统 | 说明 |
|------|------|
| 26 种经验丹 | 体修 13 种 + 法修 13 种 |
| 境界限制 | 只能服用 <= 当前境界的丹药 |
| 右键服用 | 手持丹药右键即可使用 |
| 自动检测 | 自动识别物品是否为丹药 |
| 管理命令 | /pill give, /pill list, /pill use |

## 架构

```
XiuXianPill (v1.0)
|-- 丹药数据加载 (config.yml)
|-- 右键服用事件监听
|-- 境界检查
|-- 调用 XiuXianCore API (反射)
+-- 命令系统 (/pill)
```

## 丹药列表

### 体修丹药

| 境界 | 丹药 | 修为 | 材质 |
|------|------|------|------|
| 通脉 | 开脉丹 | 100 | 煤炭 |
| 锻骨 | 铸骨丹 | 200 | 煤炭 |
| 练腑 | 洗腑丹 | 400 | 煤炭 |
| 元武 | 凝元丹 | 800 | 煤炭块 |
| 神力 | 神力丹 | 1,500 | 煤炭块 |
| 破虚 | 破虚丹 | 3,000 | 煤炭块 |
| 混元 | 混元丹 | 5,000 | 铁锭 |
| 大成 | 大成丹 | 8,000 | 铁锭 |
| 涅槃 | 涅槃丹 | 12,000 | 铁锭 |
| 真武 | 真武丹 | 20,000 | 金锭 |
| 金相 | 金相丹 | 35,000 | 金锭 |
| 太上 | 太上丹 | 60,000 | 钻石 |
| 罗天 | 罗天丹 | 100,000 | 钻石 |

### 法修丹药

| 境界 | 丹药 | 修为 | 材质 |
|------|------|------|------|
| 练气 | 聚气丹 | 100 | 煤炭 |
| 筑基 | 固基丹 | 200 | 煤炭 |
| 结丹 | 凝丹丹 | 400 | 煤炭 |
| 元婴 | 化婴丹 | 800 | 煤炭块 |
| 化神 | 通神丹 | 1,500 | 煤炭块 |
| 返虚 | 归虚丹 | 3,000 | 煤炭块 |
| 合体 | 融合丹 | 5,000 | 铁锭 |
| 大乘 | 大乘丹 | 8,000 | 铁锭 |
| 渡劫 | 渡劫丹 | 12,000 | 铁锭 |
| 真仙 | 真仙丹 | 20,000 | 金锭 |
| 金仙 | 金仙丹 | 35,000 | 金锭 |
| 太乙 | 太乙丹 | 60,000 | 钻石 |
| 大罗 | 大罗丹 | 100,000 | 钻石 |

## 依赖

| 插件 | 状态 | 说明 |
|------|------|------|
| XiuXianCore | 必需 | 通过反射调用 addXp() 和 checkLevelUp() API |

## 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| /pill list | 基础 | 查看所有丹药列表 |
| /pill use | 基础 | 手持丹药服用 |
| /pill give <玩家> <类型> [数量] | xipill.admin | 给予玩家丹药 |
| /pill reload | xipill.admin | 重载丹药配置 |

### 丹药类型标识

体修丹药: lt0 ~ lt12 (对应 13 个境界)
法修丹药: xf0 ~ xf12 (对应 13 个境界)

```bash
# 给玩家 5 颗开脉丹 (体修第 1 境界)
/pill give PlayerName lt0 5

# 给玩家 3 颗大罗丹 (法修最高境界)
/pill give PlayerName xf12 3
```

## 配置

配置文件位于 `plugins/XiuXianPill/config.yml`，格式如下:

```yaml
lianti-pills:
  0:
    name: "&a开脉丹"
    description: "&7服用后获得100修为"
    realm: 通脉
    xp-amount: 100
    material: COAL
  1:
    name: "&a铸骨丹"
    description: "&7服用后获得200修为"
    realm: 锻骨
    xp-amount: 200
    material: COAL

xiufa-pills:
  0:
    name: "&a聚气丹"
    description: "&7服用后获得100修为"
    realm: 练气
    xp-amount: 100
    material: COAL

use-message: "&a&l[服用] &7服用 &f{pill_name} &7获得 &c{xp_amount} &7修为"
realm-too-high-message: "&c&l[境界不足] &7需要 &f{required_realm} &7境界才能服用此丹药"
```

## 编译

```bash
javac -cp paper-api-1.21.4.jar:gson-2.11.0.jar \
      -d build \
      -sourcepath src/main/java \
      src/main/java/com/xiuxian/pill/XiuXianPill.java
```

## 部署

1. 确保 XiuXianCore 已安装并运行
2. 编译 JAR 文件
3. 放入 server/plugins/ 目录
4. 重启服务器
5. 首次启动会生成默认 config.yml

## 注意事项

- 丹药通过反射调用 XiuXianCore 的 addXp() 和 checkLevelUp() 方法
- 不要直接修改 XiuXianCore 的 JSON 存档文件，会被定时器覆盖
- 修改 config.yml 后需要重启服务器或执行 /pill reload

## 更新日志

### v1.0 (2026-07-08)
- 26 种经验丹 (体修 13 + 法修 13)
- 境界服用限制
- 右键服用
- 通过反射调用 XiuXianCore API

---

相关项目:
- XiuXianCore - 核心等级系统
- XiuXianCombat - 战斗属性系统
- XiuXianItems - 自定义物品系统

## License

MIT
