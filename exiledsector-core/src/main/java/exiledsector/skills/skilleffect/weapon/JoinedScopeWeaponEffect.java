package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class JoinedScopeWeaponEffect implements SkillEffect {

    private final List<ScopedWeaponEffect> parts;
    private final List<WeaponScope> scopes;

    JoinedScopeWeaponEffect(List<ScopedWeaponEffect> parts) {
        this.parts = parts.stream().sorted(Comparator.comparing(ScopedWeaponEffect::scope)).toList();
        this.scopes = this.parts.stream().map(ScopedWeaponEffect::scope).toList();
    }

    public List<ScopedWeaponEffect> parts() {
        return parts;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        parts.forEach(part -> part.apply(stats, modId, magnitude));
    }

    @Override
    public StyledText description(float magnitude) {
        return parts.get(0).family().description(scopes, parts.get(0).mode(), magnitude);
    }

    @Override
    public boolean lowerIsBetter() {
        return parts.get(0).lowerIsBetter();
    }

    @Override
    public StatMode statMode() {
        return parts.get(0).mode();
    }

    @Override
    public String name() {
        return parts.stream().map(ScopedWeaponEffect::name).collect(Collectors.joining("+"));
    }

    @Override
    public String toString() {
        return name();
    }
}
