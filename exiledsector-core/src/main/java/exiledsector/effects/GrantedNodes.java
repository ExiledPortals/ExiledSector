package exiledsector.effects;

import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.i18n.StyledText;
import exiledsector.skills.EffectContribution;

import java.util.List;

public final class GrantedNodes {

    private static final DomainContributions<GrantedNodeEffects> LOOKUPS = DomainContributions.fromEveryDomain(ExiledSectorDomain::grantedNodeEffects);

    private GrantedNodes() {
    }

    public static List<EffectContribution> withoutTree(List<String> nodeIds, ShipEffectContext context, String modIdPrefix) {
        if (nodeIds.isEmpty()) {
            return List.of();
        }
        List<GrantedNodeEffects> lookups = LOOKUPS.all();
        return lookups.isEmpty() ? List.of() : lookups.get(0).withoutTree(nodeIds, context, modIdPrefix);
    }

    public static List<StyledText> descriptionLines(String nodeId) {
        List<GrantedNodeEffects> lookups = LOOKUPS.all();
        return lookups.isEmpty() ? List.of() : lookups.get(0).descriptionLines(nodeId);
    }
}
