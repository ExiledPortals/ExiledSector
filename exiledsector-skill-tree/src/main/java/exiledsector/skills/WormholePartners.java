package exiledsector.skills;

@FunctionalInterface
public interface WormholePartners {

    String wormholePartner(SkillNode node);

    static String pairedWith(SkillNode node) {
        return node.getType().getTier() == SkillTier.WORMHOLE ? node.getPairedNodeId() : null;
    }
}
