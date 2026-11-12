package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MineHitsTest {

    @Test
    void missilesFlaggedAsMinesCount() {
        MissileAPI mine = mock(MissileAPI.class);
        when(mine.isMine()).thenReturn(true);

        assertTrue(MineHits.isMineHit(mine));
    }

    @Test
    void projectilesNamedAfterMinesCount() {
        DamagingProjectileAPI explosion = mock(DamagingProjectileAPI.class);
        when(explosion.getProjectileSpecId()).thenReturn("minelayer_mine_heavy");
        DamagingProjectileAPI fromMineWeapon = mock(DamagingProjectileAPI.class);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getId()).thenReturn("MineStrike");
        when(fromMineWeapon.getWeapon()).thenReturn(weapon);

        assertTrue(MineHits.isMineHit(explosion));
        assertTrue(MineHits.isMineHit(fromMineWeapon));
    }

    @Test
    void ordinaryShotsBeamsAndCollisionsDoNotCount() {
        DamagingProjectileAPI shell = mock(DamagingProjectileAPI.class);
        when(shell.getProjectileSpecId()).thenReturn("heavymauler_shot");
        MissileAPI torpedo = mock(MissileAPI.class);
        when(torpedo.getProjectileSpecId()).thenReturn("reaper_torp");

        assertFalse(MineHits.isMineHit(shell));
        assertFalse(MineHits.isMineHit(torpedo));
        assertFalse(MineHits.isMineHit(mock(BeamAPI.class)));
        assertFalse(MineHits.isMineHit(null));
    }
}
