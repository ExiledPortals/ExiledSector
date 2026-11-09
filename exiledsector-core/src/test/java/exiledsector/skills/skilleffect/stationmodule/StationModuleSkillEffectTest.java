package exiledsector.skills.skilleffect.stationmodule;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class StationModuleSkillEffectTest {

    @Test
    void moduleBonusesChangeNothingOnTheParentItself() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipAPI ship = mock(ShipAPI.class);

        for (StationModuleSkillEffect effect : StationModuleSkillEffect.values()) {
            effect.apply(stats, "exiledSector_skill_node_" + effect.name(), 20f);
            effect.applyAfterShipCreation(ship, "exiledSector_skill_node_" + effect.name(), 20f);
        }

        verifyNoInteractions(stats, ship);
    }
}
