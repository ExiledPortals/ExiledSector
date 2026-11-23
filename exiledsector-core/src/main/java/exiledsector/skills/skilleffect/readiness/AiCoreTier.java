package exiledsector.skills.skilleffect.readiness;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import java.util.Map;

final class AiCoreTier {

    private static final Map<String, Integer> VANILLA_TIERS = Map.of(
            Commodities.GAMMA_CORE, 1,
            Commodities.BETA_CORE, 2,
            Commodities.ALPHA_CORE, 3,
            Commodities.OMEGA_CORE, 4);

    private AiCoreTier() {
    }

    static int of(PersonAPI captain) {
        if (captain == null || captain.isDefault() || captain.getAICoreId() == null) {
            return 0;
        }
        Integer vanillaTier = VANILLA_TIERS.get(captain.getAICoreId());
        if (vanillaTier != null) {
            return vanillaTier;
        }
        return captain.getStats() == null ? 0 : byLevel(captain.getStats().getLevel());
    }

    static int byLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level <= 4) {
            return 1;
        }
        if (level <= 6) {
            return 2;
        }
        return level <= 8 ? 3 : 4;
    }
}
