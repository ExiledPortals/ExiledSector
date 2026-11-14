package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

final class SweptFleetMemberships {

    private final Map<CampaignFleetAPI, Membership> membershipByFleet = new WeakHashMap<>();

    boolean changedSinceRemembered(CampaignFleetAPI fleet) {
        return !Membership.of(fleet).equals(membershipByFleet.get(fleet));
    }

    void remember(CampaignFleetAPI fleet) {
        membershipByFleet.put(fleet, Membership.of(fleet));
    }

    void forget(CampaignFleetAPI fleet) {
        membershipByFleet.remove(fleet);
    }

    private record Membership(String[] memberIds, int[] variantIdentities, boolean[] carriesTreeHullMod) {

        static Membership of(CampaignFleetAPI fleet) {
            List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
            String[] memberIds = new String[members.size()];
            int[] variantIdentities = new int[members.size()];
            boolean[] carriesTreeHullMod = new boolean[members.size()];
            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                FleetMemberAPI member = members.get(memberIndex);
                ShipVariantAPI variant = member.getVariant();
                memberIds[memberIndex] = member.getId();
                variantIdentities[memberIndex] = System.identityHashCode(variant);
                carriesTreeHullMod[memberIndex] = variant != null && variant.hasHullMod(SkillTreeHullMod.ID);
            }
            return new Membership(memberIds, variantIdentities, carriesTreeHullMod);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Membership membership && Arrays.equals(memberIds, membership.memberIds)
                    && Arrays.equals(variantIdentities, membership.variantIdentities)
                    && Arrays.equals(carriesTreeHullMod, membership.carriesTreeHullMod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(memberIds), Arrays.hashCode(variantIdentities), Arrays.hashCode(carriesTreeHullMod));
        }

        @Override
        public String toString() {
            return "Membership[memberIds=" + Arrays.toString(memberIds) + ", variantIdentities=" + Arrays.toString(variantIdentities)
                    + ", carriesTreeHullMod=" + Arrays.toString(carriesTreeHullMod) + "]";
        }
    }
}
