package exiledsector.skills.skilleffect.readiness;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCoreTierTest {

    private static PersonAPI core(String coreId, int level) {
        PersonAPI captain = mock(PersonAPI.class);
        MutableCharacterStatsAPI skills = mock(MutableCharacterStatsAPI.class);
        when(captain.getAICoreId()).thenReturn(coreId);
        when(captain.getStats()).thenReturn(skills);
        when(skills.getLevel()).thenReturn(level);
        return captain;
    }

    @Test
    void vanillaCoresRankGammaToOmega() {
        assertEquals(1, AiCoreTier.of(core(Commodities.GAMMA_CORE, 1)));
        assertEquals(2, AiCoreTier.of(core(Commodities.BETA_CORE, 1)));
        assertEquals(3, AiCoreTier.of(core(Commodities.ALPHA_CORE, 1)));
        assertEquals(4, AiCoreTier.of(core(Commodities.OMEGA_CORE, 1)));
    }

    @Test
    void otherCoresRankByLevelAndPeopleNotAtAll() {
        assertEquals(3, AiCoreTier.of(core("modded_core", 7)));
        assertEquals(0, AiCoreTier.of(core("modded_core", 1)));
        assertEquals(0, AiCoreTier.of(core(null, 9)));
        assertEquals(0, AiCoreTier.of(null));
    }

    @Test
    void theBonusScalesWithTheCaptainsTier() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        MutableStat maxCr = mock(MutableStat.class);
        PersonAPI alpha = core(Commodities.ALPHA_CORE, 7);
        when(stats.getFleetMember()).thenReturn(member);
        when(member.getCaptain()).thenReturn(alpha);
        when(stats.getMaxCombatReadiness()).thenReturn(maxCr);

        ReadinessSkillEffect.MAX_COMBAT_READINESS_PER_AI_CORE_TIER_FLAT.apply(stats, "mod_id", 2f);

        verify(maxCr).modifyFlat("mod_id", 0.06f, "Ship skill tree");
        assertEquals("+2/4/6/8% to maximum combat readiness with a gamma/beta/alpha/omega core as captain.",
                ReadinessSkillEffect.MAX_COMBAT_READINESS_PER_AI_CORE_TIER_FLAT.description(2f).plain());
    }
}
