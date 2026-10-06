package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefractionDronesTest {

    private MockedStatic<WeaponDroneFactory> factory;
    private final Deque<RefractionDrone> created = new ArrayDeque<>();
    private RefractionDrones pool;
    private EnergyChainListener chain;
    private final ShipAPI hitShip = mock(ShipAPI.class);
    private final ShipAPI target = mock(ShipAPI.class);
    private final ChainLink link = new ChainLink(List.of(), 1, 0.8f);

    @BeforeEach
    void setUp() {
        factory = Mockito.mockStatic(WeaponDroneFactory.class);
        factory.when(() -> WeaponDroneFactory.createSingleShot(any(), any(), any())).thenAnswer(invocation -> created.poll());
        pool = new RefractionDrones(mock(ShipAPI.class));
        chain = mock(EnergyChainListener.class);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    private static WeaponAPI weapon(String id) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(weapon.getSpec()).thenReturn(spec);
        when(spec.getWeaponId()).thenReturn(id);
        when(weapon.getRange()).thenReturn(700f);
        return weapon;
    }

    private RefractionDrone queueDrone(String weaponId) {
        RefractionDrone drone = mock(RefractionDrone.class);
        when(drone.weaponId()).thenReturn(weaponId);
        when(drone.isInPlay()).thenReturn(true);
        created.add(drone);
        return drone;
    }

    private void launch(WeaponAPI weapon) {
        pool.launch(chain, weapon, hitShip, new Vector2f(), target, link);
    }

    @Test
    void aReadyDroneWithTheSameWeaponIsReusedInsteadOfBuildingANewOne() {
        WeaponAPI weapon = weapon("energy_gun");
        RefractionDrone drone = queueDrone("energy_gun");
        launch(weapon);
        when(drone.isReady()).thenReturn(true);

        launch(weapon);

        verify(drone, times(2)).launch(eq(chain), eq(hitShip), any(), eq(target), eq(link), eq(700f));
        factory.verify(() -> WeaponDroneFactory.createSingleShot(any(), any(), any()), times(1));
    }

    @Test
    void aBusyDroneMeansANewOneIsBuilt() {
        WeaponAPI weapon = weapon("energy_gun");
        RefractionDrone busy = queueDrone("energy_gun");
        RefractionDrone second = queueDrone("energy_gun");
        launch(weapon);

        launch(weapon);

        verify(busy).launch(any(), any(), any(), any(), any(), anyFloat());
        verify(second).launch(any(), any(), any(), any(), any(), anyFloat());
    }

    @Test
    void atTheCapTheLongestIdleDroneOfAnotherWeaponIsRecycled() {
        RefractionDrone longestIdle = null;
        for (int i = 0; i < SingleShotDrones.MAX_DRONES; i++) {
            RefractionDrone drone = queueDrone("other_gun_" + i);
            launch(weapon("other_gun_" + i));
            when(drone.isReady()).thenReturn(true);
            when(drone.idleSeconds()).thenReturn((float) i);
            longestIdle = drone;
        }
        RefractionDrone fresh = queueDrone("energy_gun");

        launch(weapon("energy_gun"));

        verify(longestIdle).remove();
        verify(fresh).launch(any(), any(), any(), any(), any(), anyFloat());
    }

    @Test
    void atTheCapWithEveryDroneBusyTheRefractionIsSkipped() {
        for (int i = 0; i < SingleShotDrones.MAX_DRONES; i++) {
            queueDrone("energy_gun");
            launch(weapon("energy_gun"));
        }
        RefractionDrone extra = queueDrone("energy_gun");

        launch(weapon("energy_gun"));

        verify(extra, never()).launch(any(), any(), any(), any(), any(), anyFloat());
    }

    @Test
    void aWeaponWhoseDroneCannotBeBuiltIsMarkedUnsupported() {
        WeaponAPI weapon = weapon("broken_gun");
        factory.when(() -> WeaponDroneFactory.createSingleShot(any(), any(), any())).thenThrow(new IllegalStateException("no hull"));

        launch(weapon);

        factory.verify(() -> WeaponDroneFactory.markProjectileUnsupported(weapon));
    }

    @Test
    void dronesThatLeftTheBattleAreDroppedAndReplaced() {
        WeaponAPI weapon = weapon("energy_gun");
        RefractionDrone gone = queueDrone("energy_gun");
        launch(weapon);
        when(gone.isReady()).thenReturn(true);
        when(gone.isInPlay()).thenReturn(false);
        RefractionDrone replacement = queueDrone("energy_gun");

        launch(weapon);

        verify(gone, times(1)).launch(any(), any(), any(), any(), any(), anyFloat());
        verify(replacement).launch(any(), any(), any(), any(), any(), anyFloat());
    }
}
