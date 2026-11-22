package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DealtHullDamageTest {

    private static ApplyDamageResultAPI hullDamage(float damage) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToHull()).thenReturn(damage);
        return result;
    }

    @Test
    void everyReceiverOfTheDealerHearsItsHullDamage() {
        ShipAPI dealer = mock(ShipAPI.class);
        ShipAPI target = mock(ShipAPI.class);
        List<Float> heard = new ArrayList<>();
        DealtHullDamage router = new DealtHullDamage();
        router.add(dealer, (hitShip, damage) -> heard.add(damage));
        router.add(dealer, (hitShip, damage) -> heard.add(damage * 2f));

        router.reportDamageApplied(dealer, target, hullDamage(100f));

        assertEquals(List.of(100f, 200f), heard);
    }

    @Test
    void otherDealersAndHitsThatMissTheHullAreIgnored() {
        ShipAPI dealer = mock(ShipAPI.class);
        ShipAPI target = mock(ShipAPI.class);
        List<Float> heard = new ArrayList<>();
        DealtHullDamage router = new DealtHullDamage();
        router.add(dealer, (hitShip, damage) -> heard.add(damage));

        router.reportDamageApplied(mock(ShipAPI.class), target, hullDamage(100f));
        router.reportDamageApplied(dealer, target, hullDamage(0f));

        assertEquals(List.of(), heard);
    }
}
