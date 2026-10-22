package exiledsector.items;

import exiledlib.ModCsv;
import exiledlib.items.ItemType;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static exiledsector.items.ItemDataIntegrity.FIRST_DATA_LINE;

public final class WormholeAnchorDataRules implements ItemDataRules {

    public static final WormholeAnchorDataRules INSTANCE = new WormholeAnchorDataRules();

    private WormholeAnchorDataRules() {
    }

    @Override
    public ItemType type() {
        return ItemType.WORMHOLE_ANCHOR;
    }

    @Override
    public List<ItemDataIntegrity.Violation> checkSeparately(Collection<String> definitionIds) throws IOException, JSONException {
        JSONArray anchorRows = ItemDataIntegrity.orEmpty(ModCsv.rows("id", WormholeAnchorDefinitions.DATA_PATH));
        return checkWormholeAnchors(anchorRows, SkillTree.getDeclaredNodes(), definitionIds);
    }

    public static List<ItemDataIntegrity.Violation> checkWormholeAnchors(JSONArray anchorRows, Map<String, SkillNode> treeNodes,
                                                                         Collection<String> otherDefinitionIds) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        checkAnchorRows(integrity, ItemDataIntegrity.orEmpty(anchorRows), treeNodes, otherDefinitionIds);
        return integrity.violations();
    }

    private static void checkAnchorRows(ItemDataIntegrity integrity, JSONArray rows, Map<String, SkillNode> treeNodes,
                                        Collection<String> otherDefinitionIds) throws JSONException {
        String file = WormholeAnchorDefinitions.DATA_PATH;
        Map<String, Integer> linesById = new HashMap<>();
        Map<String, Integer> linesByVolume = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, "id");
            if (definitionId.isEmpty()) {
                integrity.error(file, line, "the row has no id");
                return;
            }
            Integer earlierLine = linesById.putIfAbsent(definitionId, line);
            if (earlierLine != null) {
                integrity.error(file, line, "id " + definitionId + " is already used on line " + earlierLine);
            } else if (otherDefinitionIds.contains(definitionId)) {
                integrity.error(file, line, "id " + definitionId + " is already used by a subroutine, hull item or star chart");
            }
            String volume = ModCsv.text(row, WormholeAnchorDefinitions.VOLUME_COLUMN);
            if (!WormholeAnchorDefinitions.isVolume(volume)) {
                integrity.error(file, line, definitionId + " has volume \"" + volume + "\"; expected a tree region other than "
                        + SkillTags.CORE_REGION);
            } else {
                Integer volumeLine = linesByVolume.putIfAbsent(volume, line);
                if (volumeLine != null) {
                    integrity.error(file, line, definitionId + " leads to " + volume + ", which already has an anchor on line " + volumeLine);
                }
            }
            checkAnchorExit(integrity, file, line, definitionId, volume, ModCsv.text(row, WormholeAnchorDefinitions.EXIT_NODE_COLUMN), treeNodes);
            String unlock = ModCsv.text(row, "unlock");
            if (!WormholeAnchorDefinitions.isKnownUnlock(unlock)) {
                integrity.error(file, line, definitionId + " has the unknown unlock \"" + unlock + "\"; use an item unlock id, "
                        + ItemUnlock.CLEARED_CHART_TIER_PREFIX + "N or leave it blank");
            }
            String dropChance = ModCsv.text(row, WormholeAnchorDefinitions.DROP_CHANCE_COLUMN);
            if (!WormholeAnchorDefinitions.isValidDropChance(dropChance)) {
                integrity.error(file, line, definitionId + " has the drop chance \"" + dropChance + "\"; expected a number of 0 or more");
            }
            String color = ModCsv.text(row, WormholeAnchorDefinitions.COLOR_COLUMN);
            if (!WormholeAnchorDefinitions.isValidColor(color)) {
                integrity.error(file, line, definitionId + " has the color \"" + color + "\"; expected #RRGGBB or blank");
            }
        });
    }

    private static void checkAnchorExit(ItemDataIntegrity integrity, String file, int line, String definitionId, String volume, String exitNodeId,
                                        Map<String, SkillNode> treeNodes) {
        if (exitNodeId.isEmpty()) {
            integrity.error(file, line, definitionId + " has no exit node");
            return;
        }
        SkillNode exitNode = treeNodes.get(exitNodeId);
        if (exitNode == null) {
            integrity.error(file, line, definitionId + " exits at " + exitNodeId + ", which is not in the skill tree");
            return;
        }
        if (!volume.equals(exitNode.getRegion())) {
            integrity.error(file, line, definitionId + " exits at " + exitNodeId + ", which is in " + exitNode.getRegion() + " rather than " + volume);
        }
        SkillTier exitTier = exitNode.getType().getTier();
        if (exitTier != SkillTier.WORMHOLE_EXIT) {
            integrity.error(file, line, definitionId + " exits at " + exitNodeId + ", which is a " + exitTier + " node rather than a wormhole exit");
        }
    }
}
