package exiledsector.domain;

import exiledlib.ModSettingsListener;
import exiledsector.ExiledSector;

public final class DomainSettingsListener extends ModSettingsListener {

    @Override
    protected void reload() {
        ExiledSector.domains().forEach(ExiledSectorDomain::reloadSettings);
    }
}
