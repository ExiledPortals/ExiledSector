package exiledsector.charts;

import com.fs.starfarer.api.campaign.ControlGroupsAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.LinkedHashSet;

public class FakeCampaignUiData implements PersistentUIDataAPI {

    private LocationAPI campaignMapLocation;
    private SectorEntityToken courseTarget;

    public LocationAPI getCampaignMapLocation() {
        return campaignMapLocation;
    }

    public void setCampaignMapLocation(LocationAPI campaignMapLocation) {
        this.campaignMapLocation = campaignMapLocation;
    }

    public void setCourseTarget(SectorEntityToken courseTarget) {
        this.courseTarget = courseTarget;
    }

    @Override
    public SectorEntityToken getCourseTarget() {
        return courseTarget;
    }

    @Override
    public AbilitySlotsAPI getAbilitySlotsAPI() {
        return null;
    }

    @Override
    public LinkedHashSet<String> getCheckedRefitTags() {
        return new LinkedHashSet<>();
    }

    @Override
    public LinkedHashSet<String> getAllRefitTags() {
        return new LinkedHashSet<>();
    }

    @Override
    public ControlGroupsAPI getControlGroups() {
        return null;
    }
}
