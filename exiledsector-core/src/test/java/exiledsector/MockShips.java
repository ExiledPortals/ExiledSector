package exiledsector;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockShips {

    private MockShips() {
    }

    public static MemberBuilder member() {
        return new MemberBuilder();
    }

    public static MemberBuilder member(String id) {
        MemberBuilder builder = new MemberBuilder();
        when(builder.member.getId()).thenReturn(id);
        return builder;
    }

    public static final class MemberBuilder {

        private final FleetMemberAPI member = mock(FleetMemberAPI.class);
        private ShipHullSpecAPI hullSpec;

        private MemberBuilder() {
        }

        public MemberBuilder shipName(String shipName) {
            when(member.getShipName()).thenReturn(shipName);
            return this;
        }

        public MemberBuilder hullSize(HullSize hullSize) {
            when(hullSpec().getHullSize()).thenReturn(hullSize);
            return this;
        }

        public MemberBuilder hullName(String hullNameWithDashClass) {
            when(hullSpec().getHullNameWithDashClass()).thenReturn(hullNameWithDashClass);
            return this;
        }

        public MemberBuilder variant(ShipVariantAPI variant) {
            when(member.getVariant()).thenReturn(variant);
            return this;
        }

        public ShipHullSpecAPI hullSpec() {
            if (hullSpec == null) {
                hullSpec = mock(ShipHullSpecAPI.class);
                when(member.getHullSpec()).thenReturn(hullSpec);
            }
            return hullSpec;
        }

        public FleetMemberAPI build() {
            return member;
        }
    }
}
