package exiledsector.charts.phasing.trait;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import exiledsector.charts.StarChartSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhasedTraitConditionTest {

    private static final String MOD_ID = "furnace_mod";

    private final MutableStat hazard = new MutableStat(0f);
    private final Map<String, MutableStat> miningSupply = new HashMap<>();
    private MarketAPI market;
    private Industry mining;
    private FurnaceHeart furnaceHeart;

    @BeforeEach
    void setUp() {
        market = mock(MarketAPI.class);
        when(market.getHazard()).thenReturn(hazard);
        mining = mock(Industry.class);
        when(mining.isFunctional()).thenReturn(true);
        when(mining.getSupply(anyString())).thenAnswer(invocation -> {
            MutableStat quantity = miningSupply.computeIfAbsent(invocation.getArgument(0), commodityId -> new MutableStat(0f));
            MutableCommodityQuantity supply = mock(MutableCommodityQuantity.class);
            when(supply.getQuantity()).thenReturn(quantity);
            return supply;
        });
        when(market.getIndustry(Industries.MINING)).thenReturn(mining);
        MarketConditionAPI condition = mock(MarketConditionAPI.class);
        when(condition.getName()).thenReturn("Furnace Heart");
        furnaceHeart = new FurnaceHeart();
        furnaceHeart.init(market, condition);
    }

    @AfterEach
    void tearDown() {
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
    }

    private float supplied(String commodityId) {
        MutableStat quantity = miningSupply.get(commodityId);
        return quantity == null ? 0f : quantity.getModifiedValue();
    }

    @Test
    void aTraitAddsItsHazardAndMiningOutput() {
        furnaceHeart.apply(MOD_ID);

        assertEquals(0.25f, hazard.getModifiedValue(), 0.0001f);
        assertEquals(2f, supplied(Commodities.ORE));
        assertEquals(1f, supplied(Commodities.RARE_ORE));
    }

    @Test
    void aTraitAppliesNothingWhileStarChartsAreSwitchedOff() {
        furnaceHeart.apply(MOD_ID);
        StarChartSettings.setEnabledForTests(false);

        furnaceHeart.apply(MOD_ID);

        assertEquals(0f, hazard.getModifiedValue());
        assertEquals(0f, supplied(Commodities.ORE));
    }

    @Test
    void aDisruptedIndustryLosesTheOutputUntilItRecovers() {
        furnaceHeart.apply(MOD_ID);
        when(mining.isFunctional()).thenReturn(false);

        furnaceHeart.apply(MOD_ID);

        assertEquals(0f, supplied(Commodities.ORE));
        assertEquals(0.25f, hazard.getModifiedValue(), 0.0001f);
    }

    @Test
    void unapplyingRemovesEveryEffect() {
        furnaceHeart.apply(MOD_ID);

        furnaceHeart.unapply(MOD_ID);

        assertEquals(0f, hazard.getModifiedValue());
        assertEquals(0f, supplied(Commodities.RARE_ORE));
    }
}
