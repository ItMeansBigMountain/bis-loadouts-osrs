package com.itmeansbigmountain.bossreadinessscore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
	public void noneBossResolvesToBestOverallTarget()
	{
		BossDataService service = new BossDataService();

		BossTarget target = service.resolveBoss("None - best overall for my stats", BossProfile.ZULRAH);

		assertEquals("Best overall", target.getLabel());
		assertTrue(target.getSource().contains("No boss selected"));
	}
}
