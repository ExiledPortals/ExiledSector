package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageType;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.EnumMap;
import java.util.Map;

public class DamageTypeMultsExtension implements BeforeEachCallback, AfterEachCallback {

    private final Map<DamageType, Mults> before = new EnumMap<>(DamageType.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        for (DamageType type : DamageType.values()) {
            before.put(type, DamageTypeMults.current(type));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        before.forEach(DamageTypeMults::set);
    }
}
