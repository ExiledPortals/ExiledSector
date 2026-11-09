package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

final class FleetContributions {

    private FleetContributions() {
    }

    static float sum(List<FleetMemberAPI> members, String contributionKey) {
        float total = 0f;
        for (FleetMemberAPI member : members) {
            if (!member.isMothballed()) {
                total += member.getStats().getDynamic().getValue(contributionKey, 0f);
            }
        }
        return total;
    }

    static float largestPositive(List<FleetMemberAPI> members, String contributionKey) {
        float largest = 0f;
        for (FleetMemberAPI member : members) {
            if (!member.isMothballed()) {
                largest = Math.max(largest, member.getStats().getDynamic().getValue(contributionKey, 0f));
            }
        }
        return largest;
    }

    static float largestMagnitude(List<FleetMemberAPI> members, String contributionKey) {
        float strongest = 0f;
        for (FleetMemberAPI member : members) {
            float value = member.isMothballed() ? 0f : member.getStats().getDynamic().getValue(contributionKey, 0f);
            if (Math.abs(value) > Math.abs(strongest)) {
                strongest = value;
            }
        }
        return strongest;
    }
}
