package exiledsector.domain;

import exiledsector.ModSettingsListener;
import exiledsector.ExiledSector;

public final class DomainSettingsListener extends ModSettingsListener {

    @Override
    protected void reload() {
        ExiledSector.domains().forEach(ExiledSectorDomain::reloadSettings);
    }
}
