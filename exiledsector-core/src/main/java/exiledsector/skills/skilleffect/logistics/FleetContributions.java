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
}
