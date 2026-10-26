package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.BackedSkillEffect;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SkillEffect;

public record SlotConversionBacking(WeaponSlotConversion.Rule rule) implements EffectBacking {

    SlotConversionBacking(WeaponType fromType, WeaponSize slotSize, WeaponType toType) {
        this(new WeaponSlotConversion.Rule(fromType, slotSize, toType));
    }

    public static WeaponSlotConversion.Rule ruleOf(SkillEffect effect) {
        return effect instanceof BackedSkillEffect backed && backed.backing() instanceof SlotConversionBacking slotConversion
                ? slotConversion.rule() : null;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        if (stats.getFleetMember() != null) {
            WeaponSlotConversion.add(stats.getVariant(), rule);
        }
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.templated(effect, magnitude);
    }
}
