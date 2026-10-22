package exiledsector.ui.inspect;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.ui.VanillaText;

public class SkillTreeCodexEntry extends BoxedCodexEntry {

    public static final String ID_PREFIX = "exiledSector_skillTree_";

    private final ShipTreeLookup.ShipTree shipTree;

    public SkillTreeCodexEntry(String id, FleetMemberAPI member, ShipTreeLookup.ShipTree shipTree) {
        super(id, Translation.gameText("codex.title"), null, member);
        this.shipTree = shipTree;
    }

    ShipTreeLookup.ShipTree shipTree() {
        return shipTree;
    }

    @Override
    public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
        I18n.forGameText(() -> {
            VanillaText.addPara(info, Translation.styled("codex.title"), 0f, Misc.getBasePlayerColor());
            VanillaText.addPara(info, Translation.msg("summary.level").arg("level", Math.min(shipTree.skillData().getLevel(), ShipLevelConfig.maxLevel())).styled(), 0f,
                    Misc.getGrayColor());
        });
    }

    @Override
    protected void writeDetail(TooltipMakerAPI info, float textWidth) {
        ShipTreeSummaryRenderer.render(info, (FleetMemberAPI) getParam(), shipTree, 0f);
    }
}
