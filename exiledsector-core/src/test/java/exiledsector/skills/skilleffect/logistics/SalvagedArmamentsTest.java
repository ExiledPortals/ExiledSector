package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalvagedArmamentsTest {

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
            when(member.getStats().getDynamic().getValue(SalvagedArmaments.CONTRIBUTION_KEY, 0f)).thenReturn(contribution);
            members.add(member);
        }
        when(fleet.getFleetData().getMembersListCopy()).thenReturn(members);
        return fleet;
    }

    private static CargoStackAPI weaponStack(String weaponId, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getWeaponId()).thenReturn(weaponId);
        when(stack.isWeaponStack()).thenReturn(true);
        when(stack.getWeaponSpecIfWeapon()).thenReturn(spec);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    private static CargoStackAPI wingStack(String wingId, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        FighterWingSpecAPI spec = mock(FighterWingSpecAPI.class);
        when(spec.getId()).thenReturn(wingId);
        when(stack.isFighterWingStack()).thenReturn(true);
        when(stack.getFighterWingSpecIfWing()).thenReturn(spec);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void weaponsAndWingsGainTheFleetShareAndTheFractionIsRolled() {
        CargoAPI loot = mock(CargoAPI.class);
        CargoStackAPI metals = mock(CargoStackAPI.class);
        when(metals.getSize()).thenReturn(500f);
        List<CargoStackAPI> stacks = List.of(weaponStack("lightmg", 12f), wingStack("talon_wing", 3f), metals);
        when(loot.getStacksCopy()).thenReturn(stacks);

        SalvagedArmaments.addExtra(fleet(10f, 15f), loot, new FixedRoll(0.5f));

        verify(loot).addWeapons("lightmg", 3);
        verify(loot).addFighters("talon_wing", 1);
        verify(loot, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void theRolledFractionAddsOneMoreOnlyWhenItLandsUnderTheShare() {
        assertEquals(1, SalvagedArmaments.extraCount(1f, 25f, new FixedRoll(0.2f)));
        assertEquals(0, SalvagedArmaments.extraCount(1f, 25f, new FixedRoll(0.3f)));
        assertEquals(2, SalvagedArmaments.extraCount(10f, 20f, new FixedRoll(0.99f)));
    }

    @Test
    void aFleetWithoutTheEffectLeavesTheSalvageAlone() {
        CargoAPI loot = mock(CargoAPI.class);
        List<CargoStackAPI> stacks = List.of(weaponStack("lightmg", 12f));
        when(loot.getStacksCopy()).thenReturn(stacks);

        SalvagedArmaments.addExtra(fleet(0f), loot, new FixedRoll(0f));

        verify(loot, never()).addWeapons(anyString(), anyInt());
    }
}
