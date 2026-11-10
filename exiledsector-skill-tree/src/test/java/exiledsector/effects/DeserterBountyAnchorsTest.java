package exiledsector.effects;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import exiledsector.items.AnchorRows;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.skills.SkillTree;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeserterBountyAnchorsTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String PIRATE_ANCHOR = "wormhole_anchor_pirate";

    private final List<IntelInfoPlugin> bounties = new ArrayList<>();
    private SectorAPI sector;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(AnchorRows.row(HEGEMONY_ANCHOR, "hegemony", "hegemony_2").put("drop_factions", "hegemony"))
                .put(AnchorRows.row(PIRATE_ANCHOR, "pirate", "pirate_wormhole_2").put("drop_factions", "pirates")));
        IntelManagerAPI intelManager = mock(IntelManagerAPI.class);
        when(intelManager.getIntel(PersonBountyIntel.class)).thenReturn(bounties);
        sector = mock(SectorAPI.class);
        when(sector.getIntelManager()).thenReturn(intelManager);
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
    }

    @Test
    void beatingADeserterFlagshipDropsItsFactionsAnchor() {
        FleetMemberAPI flagship = member("deserter_flagship");
        bounties.add(bounty(PersonBountyIntel.BountyType.DESERTER, "hegemony", flagship));

        List<ItemData> rewards = fight(won(), List.of(member("escort"), flagship), List.of());

        assertEquals(List.of(HEGEMONY_ANCHOR), rewards.stream().map(ItemData::definitionId).toList());
    }

    @Test
    void sparingTheFlagshipDropsNothing() {
        bounties.add(bounty(PersonBountyIntel.BountyType.DESERTER, "hegemony", member("deserter_flagship")));

        assertTrue(fight(won(), List.of(member("escort")), List.of()).isEmpty());
    }

    @Test
    void pirateBountiesDropNothingExtra() {
        FleetMemberAPI flagship = member("pirate_flagship");
        bounties.add(bounty(PersonBountyIntel.BountyType.PIRATE, "hegemony", flagship));

        assertTrue(fight(won(), List.of(flagship), List.of()).isEmpty());
    }

    @Test
    void aLostBattleDropsNothing() {
        FleetMemberAPI flagship = member("deserter_flagship");
        bounties.add(bounty(PersonBountyIntel.BountyType.DESERTER, "hegemony", flagship));
        BattleLoot lost = new BattleLoot(new Random(3L), new Random(4L));
        lost.setPlayerWon(false);

        assertTrue(fight(lost, List.of(flagship), List.of()).isEmpty());
    }

    @Test
    void anAnchorTheBattleAlreadyRolledIsNotDoubled() {
        FleetMemberAPI flagship = member("deserter_flagship");
        bounties.add(bounty(PersonBountyIntel.BountyType.DESERTER, "hegemony", flagship));
        List<ItemData> rolled = List.of(ItemData.rolled(ItemDefinitions.get(HEGEMONY_ANCHOR), 1L));

        assertTrue(fight(won(), List.of(flagship), rolled).isEmpty());
    }

    @Test
    void aFactionWithoutAnAnchorDropsNothing() {
        FleetMemberAPI flagship = member("deserter_flagship");
        bounties.add(bounty(PersonBountyIntel.BountyType.DESERTER, "independent", flagship));

        assertTrue(fight(won(), List.of(flagship), List.of()).isEmpty());
    }

    private List<ItemData> fight(BattleLoot battle, List<FleetMemberAPI> lostEnemies, List<ItemData> rolledDrops) {
        DeserterBountyAnchors.recordEngagement(battle, new BattleEngagement(member -> null, 1f, 0f, lostEnemies), sector);
        return DeserterBountyAnchors.claim(battle, rolledDrops, definition -> true);
    }

    private static BattleLoot won() {
        BattleLoot battle = new BattleLoot(new Random(3L), new Random(4L));
        battle.setPlayerWon(true);
        return battle;
    }

    private static FleetMemberAPI member(String id) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    private static PersonBountyIntel bounty(PersonBountyIntel.BountyType type, String factionId, FleetMemberAPI flagship) {
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        PersonBountyIntel bounty = mock(PersonBountyIntel.class);
        when(bounty.getBountyType()).thenReturn(type);
        when(bounty.getFaction()).thenReturn(faction);
        when(bounty.getFlagship()).thenReturn(flagship);
        return bounty;
    }
}
