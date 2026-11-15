package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForgeSuppliesTest {

    private static final class FixedRoll extends Random {
        private final float roll;

        FixedRoll(float roll) {
            this.roll = roll;
        }

        @Override
        public float nextFloat() {
            return roll;
        }
    }

    private static CampaignFleetAPI fleet(float... contributions) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, Answers.RETURNS_DEEP_STUBS);
        List<FleetMemberAPI> members = new ArrayList<>();
        for (float contribution : contributions) {
            FleetMemberAPI member = mock(FleetMemberAPI.class, Answers.RETURNS_DEEP_STUBS);
            when(member.getStats().getDynamic().getValue(ForgeSupplies.CONTRIBUTION_KEY, 0f)).thenReturn(contribution);
            members.add(member);
        }
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    @Test
    void theFleetSharesOfDefeatedDeploymentPointsAddUpAndTheFractionIsRolled() {
        CargoAPI loot = mock(CargoAPI.class);

        ForgeSupplies.addExtra(fleet(10f, 15f), loot, 90f, new FixedRoll(0.4f));

        verify(loot).addSupplies(23f);
    }

    @Test
    void nothingIsAddedWithoutTheEffectOrWithoutDefeatedShips() {
        CargoAPI loot = mock(CargoAPI.class);

        ForgeSupplies.addExtra(fleet(0f), loot, 90f, new FixedRoll(0f));
        ForgeSupplies.addExtra(fleet(10f), loot, 0f, new FixedRoll(0f));
        ForgeSupplies.addExtra(null, loot, 90f, new FixedRoll(0f));

        verify(loot, never()).addSupplies(anyFloat());
    }
}
