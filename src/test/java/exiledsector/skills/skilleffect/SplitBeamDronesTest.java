package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SplitBeamDronesTest {

    private static final class FakeSplitter implements DamageDealtModifier, SplitBeamSource {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            return null;
        }
    }

    private static ShipAPI shieldedShipAtOrigin(float shieldRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        ShieldAPI shield = mock(ShieldAPI.class);
        when(ship.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getShield()).thenReturn(shield);
        when(shield.isOn()).thenReturn(true);
        when(shield.getRadius()).thenReturn(shieldRadius);
        return ship;
    }

    @Test
    void refractionStartsAtTheImpactPointWhenTheSplitTargetIsAwayFromThePrimary() {
        ShipAPI primary = shieldedShipAtOrigin(100f);

        Vector2f origin = SplitBeamDrones.refractionOrigin(primary, new Vector2f(100f, 0f), new Vector2f(500f, 0f));

        assertEquals(new Vector2f(100f, 0f), origin);
    }

    @Test
    void refractionStartsBeyondThePrimarysFarSideWhenTheSplitTargetIsBehindIt() {
        ShipAPI primary = shieldedShipAtOrigin(100f);

        Vector2f origin = SplitBeamDrones.refractionOrigin(primary, new Vector2f(100f, 0f), new Vector2f(-500f, 0f));

        assertEquals(-110f, origin.x, 0.01f);
        assertEquals(0f, origin.y, 0.01f);
    }

    @Test
    void drawsTheVisibleBeamFromThePrimaryImpactPointWhileTheDroneFiresFromBeyondThePrimary() {
        ShipAPI firingShip = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(firingShip.isAlive()).thenReturn(true);
        ShipAPI primary = shieldedShipAtOrigin(100f);
        ShipAPI splitTarget = mock(ShipAPI.class);
        when(splitTarget.isAlive()).thenReturn(true);
        when(splitTarget.getLocation()).thenReturn(new Vector2f(-500f, 0f));
        ShipAPI drone = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        Vector2f droneLocation = new Vector2f();
        when(drone.getLocation()).thenReturn(droneLocation);
        com.fs.starfarer.api.combat.WeaponAPI droneWeapon = mock(com.fs.starfarer.api.combat.WeaponAPI.class);
        com.fs.starfarer.api.combat.BeamAPI droneBeam = mock(com.fs.starfarer.api.combat.BeamAPI.class);
        when(drone.getAllWeapons()).thenReturn(List.of(droneWeapon));
        when(droneWeapon.getBeams()).thenReturn(List.of(droneBeam));
        when(droneBeam.getBrightness()).thenReturn(1f);
        when(droneBeam.getWidth()).thenReturn(12f);
        when(droneBeam.getCoreColor()).thenReturn(java.awt.Color.WHITE);
        when(droneBeam.getFringeColor()).thenReturn(java.awt.Color.CYAN);
        com.fs.starfarer.api.combat.WeaponAPI primaryWeapon = mock(com.fs.starfarer.api.combat.WeaponAPI.class);
        Vector2f impactPoint = new Vector2f(100f, 0f);

        try (MockedStatic<SplitBeamDroneFactory> factory = Mockito.mockStatic(SplitBeamDroneFactory.class);
             MockedStatic<org.magiclib.plugins.MagicFakeBeamPlugin> fakeBeams = Mockito.mockStatic(org.magiclib.plugins.MagicFakeBeamPlugin.class)) {
            factory.when(() -> SplitBeamDroneFactory.create(firingShip, primaryWeapon)).thenReturn(drone);
            SplitBeamDrones drones = new SplitBeamDrones(firingShip);

            drones.refresh(primaryWeapon, primary, splitTarget, impactPoint, 0.5f);
            drones.advance(0.016f);

            assertEquals(-110f, droneLocation.x, 0.01f);
            fakeBeams.verify(() -> org.magiclib.plugins.MagicFakeBeamPlugin.addBeam(
                    Mockito.eq(0f), Mockito.eq(0f), Mockito.eq(12f), Mockito.eq(new Vector2f(100f, 0f)),
                    Mockito.anyFloat(), Mockito.anyFloat(), any(), any()));
        }
    }

    @Test
    void keepsFiringForAtLeastOneSecondAfterTheSplitStartsEvenWithoutFurtherHits() {
        ShipAPI firingShip = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(firingShip.isAlive()).thenReturn(true);
        ShipAPI primary = shieldedShipAtOrigin(100f);
        ShipAPI splitTarget = mock(ShipAPI.class);
        when(splitTarget.isAlive()).thenReturn(true);
        when(splitTarget.getLocation()).thenReturn(new Vector2f(500f, 0f));
        ShipAPI drone = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(drone.getLocation()).thenReturn(new Vector2f());
        com.fs.starfarer.api.combat.WeaponAPI droneWeapon = mock(com.fs.starfarer.api.combat.WeaponAPI.class);
        when(drone.getAllWeapons()).thenReturn(List.of(droneWeapon));
        com.fs.starfarer.api.combat.WeaponAPI primaryWeapon = mock(com.fs.starfarer.api.combat.WeaponAPI.class);
        com.fs.starfarer.api.combat.CombatEngineAPI engine = mock(com.fs.starfarer.api.combat.CombatEngineAPI.class);

        try (MockedStatic<SplitBeamDroneFactory> factory = Mockito.mockStatic(SplitBeamDroneFactory.class);
             MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            factory.when(() -> SplitBeamDroneFactory.create(firingShip, primaryWeapon)).thenReturn(drone);
            global.when(Global::getCombatEngine).thenReturn(engine);
            SplitBeamDrones drones = new SplitBeamDrones(firingShip);

            drones.refresh(primaryWeapon, primary, splitTarget, new Vector2f(100f, 0f), 0.5f);
            drones.advance(0.8f);
            verify(droneWeapon, Mockito.atLeastOnce()).setForceFireOneFrame(true);
            verify(engine, never()).removeEntity(drone);

            drones.advance(0.3f);
            verify(engine).removeEntity(drone);
        }
    }

    @Test
    void officerCopyCarriesTheCaptainsIdentityPersonalityAndEverySkillLevel() {
        PersonAPI captain = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
        FullName name = mock(FullName.class);
        PersonalityAPI personality = mock(PersonalityAPI.class);
        MutableCharacterStatsAPI.SkillLevelAPI skill = mock(MutableCharacterStatsAPI.SkillLevelAPI.class);
        SkillSpecAPI skillSpec = mock(SkillSpecAPI.class);
        when(captain.getName()).thenReturn(name);
        when(captain.getPortraitSprite()).thenReturn("portrait.png");
        when(captain.getPersonalityAPI()).thenReturn(personality);
        when(personality.getId()).thenReturn("aggressive");
        when(captain.getStats().getLevel()).thenReturn(5);
        when(captain.getStats().getSkillsCopy()).thenReturn(List.of(skill));
        when(skill.getSkill()).thenReturn(skillSpec);
        when(skillSpec.getId()).thenReturn("energy_weapon_mastery");
        when(skill.getLevel()).thenReturn(2f);
        FactoryAPI factory = mock(FactoryAPI.class);
        PersonAPI copy = mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS);
        when(factory.createPerson()).thenReturn(copy);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getFactory).thenReturn(factory);

            SplitBeamDroneFactory.officerCopy(captain);
        }

        verify(copy).setName(name);
        verify(copy).setPortraitSprite("portrait.png");
        verify(copy).setPersonality("aggressive");
        verify(copy.getStats()).setLevel(5);
        verify(copy.getStats()).setSkillLevel("energy_weapon_mastery", 2f);
    }

    @Test
    void sharesOnlyDamageListenersThatDoNotTickEveryFrameAndAreNotAlreadyOnTheDrone() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier plain = mock(DamageDealtModifier.class);
        DamageDealtModifier ticking = mock(DamageDealtModifier.class, Mockito.withSettings().extraInterfaces(AdvanceableListener.class));
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(plain, ticking));

        SplitBeamDroneFactory.shareDamageListeners(firingShip, drone);

        verify(drone).addListener(plain);
        verify(drone, never()).addListener(ticking);
    }

    @Test
    void replacesTheOfficerCopysOwnInstanceOfAListenerWithTheFiringShipsSoItReadsTheRealShip() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier listener = mock(DamageDealtModifier.class);
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(listener));

        SplitBeamDroneFactory.shareDamageListeners(firingShip, drone);

        org.mockito.InOrder order = Mockito.inOrder(drone);
        order.verify(drone).removeListenerOfClass(listener.getClass());
        order.verify(drone).addListener(listener);
    }

    @Test
    void neverSharesABeamSplitterSoDronesCannotSplitAgain() {
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipAPI drone = mock(ShipAPI.class);
        DamageDealtModifier splitter = new FakeSplitter();
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(splitter));

        SplitBeamDroneFactory.shareDamageListeners(firingShip, drone);

        verify(drone, never()).addListener(any());
    }

    @Test
    void mirroringReplacesTheDronesModifiersWithTheFiringShipsCurrentOnes() {
        MutableShipStatsAPI source = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableShipStatsAPI drone = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableStat droneEnergyDamage = new MutableStat(1f);
        MutableStat sourceEnergyDamage = new MutableStat(1f);
        droneEnergyDamage.modifyMult("expired_system", 2f);
        sourceEnergyDamage.modifyMult("high_energy_focus", 1.5f);
        when(drone.getEnergyWeaponDamageMult()).thenReturn(droneEnergyDamage);
        when(source.getEnergyWeaponDamageMult()).thenReturn(sourceEnergyDamage);
        StatBonus droneBeamRange = new StatBonus();
        StatBonus sourceBeamRange = new StatBonus();
        sourceBeamRange.modifyPercent("advanced_optics", 20f);
        when(drone.getBeamWeaponRangeBonus()).thenReturn(droneBeamRange);
        when(source.getBeamWeaponRangeBonus()).thenReturn(sourceBeamRange);

        SplitBeamDroneStats.mirror(source, drone);

        assertEquals(1.5f, droneEnergyDamage.getModifiedValue(), 0.0001f);
        assertEquals(120f, droneBeamRange.computeEffective(100f), 0.0001f);
    }

    @Test
    void mirroringCarriesTheBeamHardFluxBonusSoSplitBeamsFromDronesKeepIt() {
        MutableShipStatsAPI source = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        MutableShipStatsAPI drone = mock(MutableShipStatsAPI.class, Answers.RETURNS_DEEP_STUBS);
        String key = ShieldSkillEffect.BeamHardFluxListener.HARD_FLUX_PERCENT_KEY;
        StatBonus sourceHardFlux = new StatBonus();
        StatBonus droneHardFlux = new StatBonus();
        sourceHardFlux.modifyFlat("exiledSector_skill_beam_1", 50f);
        droneHardFlux.modifyFlat("stale", 10f);
        when(source.getDynamic().getMod(key)).thenReturn(sourceHardFlux);
        when(drone.getDynamic().getMod(key)).thenReturn(droneHardFlux);

        SplitBeamDroneStats.mirror(source, drone);

        assertEquals(50f, droneHardFlux.getFlatBonus(), 0.0001f);
    }

    @Test
    void droneShareListenerScalesOnlyBeamHitsAndHandsItsModifierBackToTheEngine() {
        DamageDealtModifier shareListener = new SplitBeamDrones.ShareListener();
        DamageAPI beamHit = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);
        DamageAPI projectileHit = mock(DamageAPI.class, Answers.RETURNS_DEEP_STUBS);

        String beamResult = shareListener.modifyDamageDealt(mock(com.fs.starfarer.api.combat.BeamAPI.class),
                mock(CombatEntityAPI.class), beamHit, new Vector2f(), true);
        String projectileResult = shareListener.modifyDamageDealt(new Object(), mock(CombatEntityAPI.class),
                projectileHit, new Vector2f(), true);

        assertEquals("exiledSector_splitBeamDroneShare", beamResult);
        verify(beamHit.getModifier()).modifyMult("exiledSector_splitBeamDroneShare", 1f);
        assertEquals(null, projectileResult);
    }
}
