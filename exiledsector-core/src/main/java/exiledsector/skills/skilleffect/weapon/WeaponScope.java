package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.i18n.Translation;

import java.util.Arrays;
import java.util.List;

public enum WeaponScope {

    ALL(null, ""),
    BALLISTIC(ALL, "BALLISTIC_"),
    MISSILE(ALL, "MISSILE_"),
    ENERGY(ALL, "ENERGY_"),
    NON_BEAM_ENERGY(ENERGY, "NON_BEAM_ENERGY_"),
    BEAM(ENERGY, "BEAM_");

    private final WeaponScope parentScope;
    private final String namePrefix;

    WeaponScope(WeaponScope parentScope, String namePrefix) {
        this.parentScope = parentScope;
        this.namePrefix = namePrefix;
    }

    public WeaponScope parent() {
        return parentScope;
    }

    public List<WeaponScope> children() {
        return Arrays.stream(values()).filter(scope -> scope.parentScope == this).toList();
    }

    String namePrefix() {
        return namePrefix;
    }

    public String qualify(String statKey) {
        return qualify(List.of(this), statKey);
    }

    public static String qualify(List<WeaponScope> scopes, String statKey) {
        if (scopes.size() == 1 && scopes.get(0) == ALL) {
            return Translation.text(statKey);
        }
        return Translation.msg("weapon.qualified").arg("scope", scopeNames(scopes)).arg("stat", Translation.text(statKey)).text();
    }

    private static String scopeNames(List<WeaponScope> scopes) {
        List<String> names = scopes.stream().map(scope -> Translation.text("weapon.scope." + scope.name())).toList();
        if (names.size() == 1) {
            return names.get(0);
        }
        String rest = String.join(Translation.text("format.list.sep"), names.subList(0, names.size() - 1));
        return Translation.msg("weapon.scopes.joined").arg("rest", rest).arg("last", names.get(names.size() - 1)).text();
    }

    boolean isAncestorOf(WeaponScope other) {
        for (WeaponScope scope = other.parentScope; scope != null; scope = scope.parentScope) {
            if (scope == this) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(WeaponAPI weapon) {
        return switch (this) {
            case ALL -> true;
            case BALLISTIC -> weapon.getType() == WeaponAPI.WeaponType.BALLISTIC;
            case MISSILE -> weapon.getType() == WeaponAPI.WeaponType.MISSILE;
            case ENERGY -> weapon.getType() == WeaponAPI.WeaponType.ENERGY;
            case NON_BEAM_ENERGY -> weapon.getType() == WeaponAPI.WeaponType.ENERGY && !weapon.isBeam();
            case BEAM -> weapon.isBeam();
        };
    }
}
