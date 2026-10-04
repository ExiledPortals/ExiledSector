# Simplified Chinese glossary

The zh_CN catalogue was drafted by Claude and still needs review by a native speaker. It follows the terms the Chinese Starsector community uses for vanilla concepts where they are well established. Change a term here first, then apply it across `data/strings/exiledSector/zh_CN.json` so the whole catalogue stays consistent.

## Core game terms

| English | 简体中文 | Notes |
|---|---|---|
| Flux | 幅能 | |
| Hard flux / soft flux | 硬幅能 / 软幅能 | |
| Flux capacity | 幅能容量 | |
| Flux dissipation | 幅能耗散 | |
| Flux capacitors / flux vents | 电容 / 耗散器 | Refit-screen terms, as in "Caps, Vents or Hull" |
| Active venting | 主动排散 | "venting speed" is 主动排散速度 |
| Overloaded | 过载 | |
| Ordnance points (OP) | 装配点 | |
| Combat readiness (CR) | 战备值 | Peak CR duration is 峰值战备时间 |
| Deployment points (DP) | 部署点 | |
| Hull mod | 船插 | Community shorthand; 舰船插件 is the formal alternative |
| D-mod / S-mod | D插 / S插 | |
| Hull (points) | 结构（值） | |
| Armor | 装甲 | |
| Shield | 护盾 | Front 前向, omni 全向 |
| Shield arc / upkeep / efficiency | 护盾角度 / 护盾维持 / 护盾效率 | |
| Phase / phase cloak | 相位 / 相位斗篷 | Check against the official name of the Phase Cloak system |
| Ballistic / energy / missile | 实弹 / 能量 / 导弹 | |
| Beam / non-beam energy / burst beam | 光束 / 非光束能量 / 脉冲光束 | |
| Hybrid weapon | 混合武器 | |
| Point defense | 点防御 | |
| Fighter (craft in general) | 战机 | |
| Fighter / interceptor / bomber / support (wing roles) | 战斗机 / 截击机 / 轰炸机 / 支援机 | |
| Fighter bay | 机库 | |
| Frigate / destroyer / cruiser / capital | 护卫舰 / 驱逐舰 / 巡洋舰 / 主力舰 | |
| Sensor profile / sensor strength | 传感器特征 / 传感器强度 | |
| Burn level | 航速 | Max burn level is 最大航速 |
| Transponder | 应答器 | |
| Supplies / fuel / cargo / crew / marines | 补给 / 燃料 / 货舱 / 船员 / 陆战队 | |
| Heavy machinery | 重型机械 | |
| Salvage | 打捞 | |
| ECM / ECCM | ECM / ECCM | Left as acronyms, as in vanilla |
| EMP arc | EMP电弧 | |
| Nav rating / command points | 导航评级 / 指挥点 | |
| Combat objectives | 战场目标 | |
| Refit screen | 改装界面 | |
| Codex | 百科 | |
| su (space units) | su | Left untranslated |
| Hegemony / Tri-Tachyon / Luddic / Sindrian Diktat / Persean League / pirates | 霸主 / 速子科技 / 卢德 / 辛达强权 / 英仙座联盟 / 海盗 | Faction names; check against the official localisation |
| [REDACTED] | [数据删除] | |

## Mod terms

| English | 简体中文 | Notes |
|---|---|---|
| Exiled Sector | Exiled Sector | The mod name is kept in English |
| Skill tree | 技能树 | |
| Node | 节点 | |
| Notable / keystone | 重要节点 / 基石节点 | |
| Starting location (root) | 起点 | |
| Allocate / deallocate | 分配 / 取消分配 | |
| Free allocation | 免费分配 | |
| Level / XP | 等级 / 经验 | |
| Build (NPC layout) | 构筑 | |
| Template / auto-allocate | 模板 / 自动分配 | A saved set of nodes a ship can follow |
| Wormhole | 虫洞 | |
| Escort | 护航 | |
| Refraction / refract | 折射 | Refracting Projectiles' shots, matching its name 折射弹体 |
| Volume (a region on the hyperspace view) | 星域 | "Sindrian volume" is 辛达强权星域 |

## Left in English on purpose

- **Second-in-Command skill names** (Redistribution, Enhanced Overrides, Reconfiguration). They come from another mod, and a guessed translation could disagree with that mod's own Chinese text.
- **The language option labels** (Auto, English, Simplified Chinese). LunaLib saves the chosen label, so they must stay the same in every language.

## Style

- Use full-width punctuation: ，。：；！？（）“”.
- Don't add spaces around numbers or highlight tags. The mod adds any spacing Starsector needs to colour game-drawn text.
- Keep every `{placeholder}` and every `<good>`/`<bad>`/`<hl>`/`<hullmod>`/`<node>` tag. Move them to wherever the matching words go in the Chinese sentence.
- `desc.mult.*` describes a multiplicative change. It is written as 提高/降低 …（乘算） to tell it apart from the additive `desc.pct.*` wording.
