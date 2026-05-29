package com.itmeansbigmountain.bossreadinessscore;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BossReadinessScorePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BossReadinessScorePlugin.class);
		RuneLite.main(args);
	}
}