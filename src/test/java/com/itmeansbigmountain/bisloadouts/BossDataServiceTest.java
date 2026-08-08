package com.itmeansbigmountain.bisloadouts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BossDataServiceTest
{
	@Test
	public void hasUsefulFallbackBossSuggestionsBeforeNetworkRefresh()
	{
		BossDataService service = new BossDataService();

		assertTrue(service.getBossNameSuggestions(100).contains("Zulrah"));
		assertTrue(service.getBossNameSuggestions(100).contains("Vorkath"));
		assertTrue(service.getBossNameSuggestions(100).contains("None - best overall for my stats"));
	}

	@Test
	public void newestReleasedBossesAreAvailableWithoutNetworkRefresh()
	{
		BossDataService service = new BossDataService();
		java.util.List<String> bosses = service.getBossNameSuggestions(100);

		for (String boss : java.util.Arrays.asList(
			"Amoxliatl", "The Hueycoatl", "Royal Titans", "Branda the Fire Queen", "Eldric the Ice King",
			"Yama", "Doom of Mokhaiotl", "Gemstone Crab", "Brutus", "Demonic Brutus", "Maggot King", "Mad Angel"))
		{
			assertTrue("missing newest boss: " + boss, bosses.contains(boss));
		}
	}

	@Test
	public void newestBossesUseResearchedLocalProfilesWhenLiveDetailIsUnavailable()
	{
		BossDataService service = new BossDataService();

		BossTarget yama = service.resolveBoss("Yama", BossProfile.GENERAL_PVM);
		assertEquals(1238, yama.getTargetCombat());
		assertEquals(2500, yama.getHitpoints());
		assertEquals(333, yama.getDefCrush());
		assertTrue(yama.getSource().contains("OSRS Wiki local profile"));

		BossTarget doom = service.resolveBoss("Doom of Mokhaiotl", BossProfile.GENERAL_PVM);
		assertEquals(558, doom.getTargetCombat());
		assertEquals(525, doom.getHitpoints());
		assertEquals(60, doom.getDefCrush());

		BossTarget madAngel = service.resolveBoss("Mad Angel", BossProfile.GENERAL_PVM);
		assertEquals(588, madAngel.getTargetCombat());
		assertEquals(755, madAngel.getHitpoints());
		assertEquals(40, madAngel.getDefCrush());
	}

	@Test
	public void noneBossResolvesToBestOverallTarget()
	{
		BossDataService service = new BossDataService();

		BossTarget target = service.resolveBoss("None - best overall for my stats", BossProfile.ZULRAH);

		assertEquals("Best overall", target.getLabel());
		assertTrue(target.getSource().contains("No boss selected"));
	}

	@Test
	public void wikiPageUrlsAlwaysUseOldSchoolRunescapeWiki()
	{
		String wikiUrl = OsrsWikiApiClient.pageUrl("dizana's quiver");

		assertTrue(wikiUrl.startsWith("https://oldschool.runescape.wiki/w/"));
		assertFalse(wikiUrl.contains("runescape.wiki/w/rs3"));
	}
}
