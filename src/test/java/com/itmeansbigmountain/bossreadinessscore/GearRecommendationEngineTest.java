package com.itmeansbigmountain.bossreadinessscore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearRecommendationEngineTest
{
	@Test
	public void recommendsBestAffordableGearForSelectedBossStyleAndStats()
	{
		PlayerStats stats = new PlayerStats(70, 70, 70, 70, 75, 70, 60);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.MIDGAME,
			stats
		);

		assertEquals("Zulrah", recommendation.getBossName());
		assertEquals(CombatStyle.MAGIC, recommendation.getStyle());
		assertEquals("trident of the swamp", recommendation.getItem(GearSlot.WEAPON).getName());
		assertEquals("ahrim's robetop", recommendation.getItem(GearSlot.BODY).getName());
		assertTrue(recommendation.getEstimatedDps() > 0.0D);
		assertTrue(recommendation.getReadinessScore() > 0);
	}

	@Test
	public void blocksItemsWhenPlayerMissingRequirements()
	{
		PlayerStats stats = new PlayerStats(60, 60, 60, 60, 50, 60, 43);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertFalse(recommendation.getItems().values().stream().anyMatch(item -> item.getName().contains("ancestral")));
		assertTrue(recommendation.getWarnings().stream().anyMatch(warning -> warning.contains("Magic level")));
	}

	@Test
	public void autoStyleChoosesHighestScoringSetup()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.AUTO,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertEquals(CombatStyle.MAGIC, recommendation.getStyle());
		assertTrue(recommendation.getAlternatives().stream().anyMatch(alt -> alt.getStyle() == CombatStyle.RANGED));
	}

	@Test
	public void recommendationIncludesSlotAlternativesForPanelCycling()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertTrue(recommendation.getAlternativesForSlot(GearSlot.WEAPON).size() > 1);
		assertEquals(recommendation.getItem(GearSlot.WEAPON), recommendation.getAlternativesForSlot(GearSlot.WEAPON).get(0));
	}

	@Test
	public void meleeOnlyStyleProducesMeleeGear()
	{
		PlayerStats stats = new PlayerStats(80, 80, 80, 80, 70, 70, 70);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.GIANT_MOLE,
			CombatStyle.MELEE,
			BudgetTier.MIDGAME,
			stats
		);

		assertTrue(recommendation.getStyle().isMelee());
		assertTrue(recommendation.getItem(GearSlot.WEAPON).supports(CombatStyle.MELEE));
	}

	@Test
	public void knownItemFallbackIdsAreCaseInsensitiveForIconLookup()
	{
		assertEquals(23971, GearRecommendationEngine.fallbackItemId("Crystal helm"));
		assertEquals(23857, GearRecommendationEngine.fallbackItemId("corrupted bow (perfected)"));
		assertEquals(28951, GearRecommendationEngine.fallbackItemId("dizana's quiver"));
		assertEquals(27275, GearRecommendationEngine.fallbackItemId("Tumeken's shadow"));
		assertEquals(20997, GearRecommendationEngine.fallbackItemId("Twisted bow"));
		assertEquals(29591, GearRecommendationEngine.fallbackItemId("Scorching bow"));
		assertEquals(29594, GearRecommendationEngine.fallbackItemId("Purging staff"));
		assertEquals(29589, GearRecommendationEngine.fallbackItemId("Emberlight"));
		assertEquals(26374, GearRecommendationEngine.fallbackItemId("Zaryte crossbow"));
		assertEquals(28338, GearRecommendationEngine.fallbackItemId("Soulreaper axe"));
		assertEquals(22325, GearRecommendationEngine.fallbackItemId("Scythe of vitur"));
		assertEquals(26219, GearRecommendationEngine.fallbackItemId("Osmumten's fang"));
		assertEquals(10828, GearRecommendationEngine.fallbackItemId("Helm of neitiznot"));
	}

	@Test
	public void currentTwoHandedWeaponNamesSuppressShield()
	{
		java.util.Set<CombatStyle> ranged = java.util.EnumSet.of(CombatStyle.RANGED);
		java.util.Set<CombatStyle> melee = java.util.EnumSet.of(CombatStyle.MELEE, CombatStyle.SLASH);

		assertTrue(new GearItem(GearSlot.WEAPON, 29591, "scorching bow", ranged, 0, 0, 1, 0, 77, 0, 124, 40, 37_000_000, "wiki").isTwoHanded());
		assertTrue(new GearItem(GearSlot.WEAPON, 28338, "soulreaper axe", melee, 80, 80, 1, 0, 0, 0, 134, 121, 286_000_000, "wiki").isTwoHanded());
		assertTrue(new GearItem(GearSlot.WEAPON, 11802, "armadyl godsword", melee, 75, 75, 1, 0, 0, 0, 132, 132, 10_000_000, "wiki").isTwoHanded());
		assertFalse(new GearItem(GearSlot.WEAPON, 26374, "zaryte crossbow", ranged, 0, 0, 1, 0, 80, 0, 110, 80, 367_000_000, "wiki").isTwoHanded());
		assertFalse(new GearItem(GearSlot.WEAPON, 29594, "purging staff", java.util.EnumSet.of(CombatStyle.MAGIC), 50, 0, 0, 77, 0, 0, 37, 25, 37_000_000, "wiki").isTwoHanded());
	}

	@Test
	public void megararesAreRecommendedAndRemoveShieldWhenTwoHanded()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation magic = GearRecommendationEngine.recommend(BossProfile.GENERAL_PVM, CombatStyle.MAGIC, BudgetTier.NO_LIMIT, stats);
		assertEquals("tumeken's shadow", magic.getItem(GearSlot.WEAPON).getName());
		assertTrue(magic.getItem(GearSlot.WEAPON).isTwoHanded());
		assertFalse("2H weapon should not also equip a shield", magic.getItems().containsKey(GearSlot.SHIELD));

		SetupRecommendation ranged = GearRecommendationEngine.recommend(BossProfile.GENERAL_PVM, CombatStyle.RANGED, BudgetTier.NO_LIMIT, stats);
		assertEquals("twisted bow", ranged.getItem(GearSlot.WEAPON).getName());
		assertTrue(ranged.getItem(GearSlot.WEAPON).isTwoHanded());
		assertFalse("2H weapon should not also equip a shield", ranged.getItems().containsKey(GearSlot.SHIELD));
	}

	@Test
	public void localOsrsItemsFillGapsWhenLiveApiIsStale()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);
		java.util.List<GearItem> staleLiveItems = java.util.Arrays.asList(
			new GearItem(GearSlot.WEAPON, 11907, "trident of the seas", java.util.EnumSet.of(CombatStyle.MAGIC),
				0, 0, 0, 75, 0, 0, 25, 10, 45_000, "stale live api"),
			new GearItem(GearSlot.WEAPON, 23857, "corrupted bow (perfected)", java.util.EnumSet.of(CombatStyle.RANGED),
				0, 0, 0, 0, 80, 0, 999, 999, 0, "temporary gauntlet item")
		);

		SetupRecommendation magic = GearRecommendationEngine.recommend(
			BossTarget.fromProfile(BossProfile.GENERAL_PVM), CombatStyle.MAGIC, BudgetTier.NO_LIMIT, stats, staleLiveItems);
		SetupRecommendation ranged = GearRecommendationEngine.recommend(
			BossTarget.fromProfile(BossProfile.GENERAL_PVM), CombatStyle.RANGED, BudgetTier.NO_LIMIT, stats, staleLiveItems);

		assertEquals("tumeken's shadow", magic.getItem(GearSlot.WEAPON).getName());
		assertEquals("twisted bow", ranged.getItem(GearSlot.WEAPON).getName());
		assertFalse(ranged.getItems().values().stream().anyMatch(item -> GearRecommendationEngine.isExcludedMinigameItem(item.getName())));
	}

	@Test
	public void excludesGameModeSpecificTemporaryItems()
	{
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("corrupted bow (perfected)"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("Corrupted staff (attuned)"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("basic halberd"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("Deadman armour"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("Trailblazer reloaded blowpipe ornament kit"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("Leagues trophy"));
		assertTrue(GearRecommendationEngine.isExcludedGameModeItem("Relic hunter body"));
		assertFalse(GearRecommendationEngine.isExcludedGameModeItem("crystal bow"));
		assertFalse(GearRecommendationEngine.isExcludedGameModeItem("bow of faerdhinen"));
	}

	@Test
	public void recommendedItemsUseOldSchoolWikiLinksAndCanonicalItemNames()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.GENERAL_PVM,
			CombatStyle.MELEE,
			BudgetTier.NO_LIMIT,
			stats
		);

		for (GearItem item : recommendation.getItems().values())
		{
			assertTrue(item.getWikiUrl(), item.getWikiUrl().startsWith("https://oldschool.runescape.wiki/w/"));
			assertFalse("placeholder item names should not be recommended", item.getName().contains(" / "));
			assertFalse("placeholder shorthand should not be recommended", item.getName().equals("nezzy helm"));
			assertFalse("placeholder shorthand should not be recommended", item.getName().equals("faceguard"));
		}
	}
}
