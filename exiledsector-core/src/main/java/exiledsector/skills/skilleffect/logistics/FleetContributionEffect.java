package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;

record FleetContributionEffect(String contributionKey, StatMode reportedMode, boolean appliesToNpcShips) implements EffectBacking {

    static FleetContributionEffect playerFleetContribution(String contributionKey) {
        return new FleetContributionEffect(contributionKey, null, false);
    }

    static FleetContributionEffect anyFleetContribution(String contributionKey) {
        return new FleetContributionEffect(contributionKey, null, true);
    }

    FleetContributionEffect reportedAs(StatMode mode) {
        return new FleetContributionEffect(contributionKey, mode, appliesToNpcShips);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        stats.getDynamic().getMod(contributionKey).modifyFlat(modId, magnitude);
    }

    @Override
    public StatMode mode() {
        return reportedMode;
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.templated(effect, magnitude);
    }
}
