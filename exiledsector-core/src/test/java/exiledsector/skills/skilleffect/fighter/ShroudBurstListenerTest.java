package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.ShipExplosionFlareVisual;
import exiledsector.skills.skilleffect.CombatEngineExtension;
import exiledsector.skills.skilleffect.CombatTestShips;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShroudBurstListenerTest {

    private final List<Object> gridShips = new ArrayList<>();
    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private CombatEngineAPI engine;
    private SoundPlayerAPI sounds;

    @BeforeEach
    void setUp() {
        engine = combat.engine();
        combat.shipGridOver(gridShips);
        CombatEntityAPI flare = mock(CombatEntityAPI.class);
        when(flare.getLocation()).thenReturn(new Vector2f());
        when(flare.getVelocity()).thenReturn(new Vector2f());
        when(engine.addLayeredRenderingPlugin(any())).thenReturn(flare);
        sounds = mock(SoundPlayerAPI.class);
        combat.global().when(Global::getSoundPlayer).thenReturn(sounds);
        SettingsAPI settings = mock(SettingsAPI.class);
        SpriteAPI sprite = mock(SpriteAPI.class);
        when(settings.getSprite(anyString())).thenReturn(sprite);
        when(settings.getSprite(anyString(), anyString())).thenReturn(sprite);
        combat.global().when(Global::getSettings).thenReturn(settings);
    }

    private static ShipAPI ship(int owner, float x, float collisionRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.getCollisionRadius()).thenReturn(collisionRadius);
        when(ship.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getHitpoints()).thenReturn(100f);
        return ship;
    }

    @Test
    void aLethalHitDealsEnergyDamageToEnemiesInRangeOnce() {
        ShipAPI fighter = ship(0, 0f, 10f);
        ShipAPI enemy = ship(1, 100f, 20f);
        ShipAPI distantEnemy = ship(1, 400f, 20f);
        ShipAPI ally = ship(0, 50f, 20f);
        gridShips.addAll(List.of(fighter, enemy, distantEnemy, ally));
        ShroudBurstListener listener = new ShroudBurstListener(fighter,
                CombatTestShips.withDynamic(ShroudBurstListener.DAMAGE_KEY, 80f));

        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 50f);
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());

        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);
        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);

        verify(engine, times(1)).applyDamage(eq(enemy), any(), eq(80f), eq(DamageType.ENERGY), eq(0f), eq(false), eq(false), eq(fighter));
        verify(engine, never()).applyDamage(eq(distantEnemy), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
        verify(engine, never()).applyDamage(eq(ally), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void theBurstShowsTheShroudCollapsing() {
        ShipAPI fighter = ship(0, 0f, 10f);
        gridShips.add(fighter);
        ShroudBurstListener listener = new ShroudBurstListener(fighter,
                CombatTestShips.withDynamic(ShroudBurstListener.DAMAGE_KEY, 80f));

        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);

        verify(engine, atLeastOnce()).addSwirlyNebulaParticle(any(), any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any(), anyBoolean());
        verify(engine, atLeastOnce()).addNegativeSwirlyNebulaParticle(any(), any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), any());
        verify(engine).addLayeredRenderingPlugin(any(ShipExplosionFlareVisual.class));
        verify(sounds).playSound(any(), anyFloat(), anyFloat(), any(), any());
    }

    @Test
    void aCarrierWithoutTheEffectLeavesNoBurst() {
        ShipAPI fighter = ship(0, 0f, 10f);
        ShroudBurstListener listener = new ShroudBurstListener(fighter, null);

        listener.notifyAboutToTakeHullDamage(null, fighter, new Vector2f(), 150f);

        verify(engine, never()).addLayeredRenderingPlugin(any());
    }
}
