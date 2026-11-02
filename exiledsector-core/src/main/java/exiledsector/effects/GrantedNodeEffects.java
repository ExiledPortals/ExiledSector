package exiledsector.effects;

import exiledsector.i18n.StyledText;
import exiledsector.skills.EffectContribution;

import java.util.List;

public interface GrantedNodeEffects {

    List<EffectContribution> withoutTree(List<String> nodeIds, ShipEffectContext context, String modIdPrefix);

    String displayName(String nodeId);

    List<StyledText> descriptionLines(String nodeId);
}
