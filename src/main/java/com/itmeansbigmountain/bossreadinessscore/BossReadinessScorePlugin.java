package com.itmeansbigmountain.bossreadinessscore;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Boss Readiness Score",
	description = "Side-panel boss gear analyzer that recommends best wearable OSRS gear by stats, boss, and combat style.",
	tags = {"boss", "pvm", "readiness", "gear", "bis"}
)
@Slf4j
public class BossReadinessScorePlugin extends Plugin
{
	private static final int MAX_SCORE = 100;

	@Inject
	private Client client;

	@Inject
	private BossReadinessScoreConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	private final BossDataService bossDataService = new BossDataService();
	private ExecutorService apiExecutor;
	private BossReadinessScorePanel panel;
	private NavigationButton navButton;
	private volatile String selectedBossName = "None";
	private volatile CombatStyle selectedStyle = CombatStyle.AUTO;

	@Override
	protected void startUp()
	{
		log.debug("Boss Readiness Score started");
		apiExecutor = Executors.newSingleThreadExecutor();
		panel = new BossReadinessScorePanel();
		panel.setItemImageProvider(this::loadItemImage);
		panel.setAnalyzeListener((bossName, style) -> {
			selectedBossName = bossName;
			selectedStyle = style == null ? CombatStyle.AUTO : style;
			refreshPanel(true);
		});
		panel.showWaitingForLogin(bossDataService.getBossNameSuggestions(1000), bossDataService.getStatus());
		navButton = NavigationButton.builder()
			.tooltip("Boss Readiness Score")
			.icon(createIcon())
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		apiExecutor.submit(() -> {
			try
			{
				bossDataService.refresh();
			}
			catch (Exception ex)
			{
				log.warn("Unable to refresh OSRS Wiki/GearScape data", ex);
			}
			refreshPanel(false);
		});
	}

	@Override
	protected void shutDown()
	{
		log.debug("Boss Readiness Score stopped");
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		if (apiExecutor != null)
		{
			apiExecutor.shutdownNow();
		}
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			refreshPanel(false);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("bossreadinessscore".equals(event.getGroup()))
		{
			refreshPanel(false);
		}
	}

	static int calculateReadinessScore(int combatLevel, int hitpointsLevel, int prayerLevel, int defenceLevel,
		int targetCombatLevel, int targetPrayerLevel)
	{
		int safeTargetCombat = Math.max(1, targetCombatLevel);
		int safeTargetPrayer = Math.max(1, targetPrayerLevel);

		double combatScore = ratioScore(combatLevel, safeTargetCombat) * 0.55D;
		double hitpointsScore = ratioScore(hitpointsLevel, 99) * 0.20D;
		double prayerScore = ratioScore(prayerLevel, safeTargetPrayer) * 0.15D;
		double defenceScore = ratioScore(defenceLevel, 99) * 0.10D;

		return clampScore((int) Math.round(combatScore + hitpointsScore + prayerScore + defenceScore));
	}

	private static double ratioScore(int actual, int target)
	{
		return Math.min(1.0D, Math.max(0.0D, actual / (double) target)) * MAX_SCORE;
	}

	private static int clampScore(int score)
	{
		return Math.max(0, Math.min(MAX_SCORE, score));
	}

	private void refreshPanel(boolean forceAnalyze)
	{
		if (panel == null)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			SwingUtilities.invokeLater(() -> {
				if (panel != null)
				{
					panel.showWaitingForLogin(bossDataService.getBossNameSuggestions(1000), bossDataService.getStatus());
				}
			});
			return;
		}
		PlayerStats stats = new PlayerStats(
			client.getRealSkillLevel(Skill.ATTACK),
			client.getRealSkillLevel(Skill.STRENGTH),
			client.getRealSkillLevel(Skill.DEFENCE),
			client.getRealSkillLevel(Skill.HITPOINTS),
			client.getRealSkillLevel(Skill.MAGIC),
			client.getRealSkillLevel(Skill.RANGED),
			client.getRealSkillLevel(Skill.PRAYER)
		);
		String requestedBossName = selectedBossName;
		CombatStyle style = selectedStyle;
		BudgetTier budget = config.budgetTier();
		apiExecutor.submit(() -> {
			BossTarget target = bossDataService.resolveBoss(requestedBossName, BossProfile.GENERAL_PVM);
			SetupRecommendation recommendation = forceAnalyze || panel != null
				? GearRecommendationEngine.recommend(target, style, budget, stats, bossDataService.getGearItems())
				: null;
			String status = bossDataService.getStatus();
			SwingUtilities.invokeLater(() -> {
				if (panel != null)
				{
					panel.updateRecommendation(recommendation, target, status, bossDataService.getBossNameSuggestions(1000));
				}
			});
		});
	}

	private BufferedImage loadItemImage(GearItem item)
	{
		if (item == null || item.getItemId() <= 0)
		{
			return null;
		}
		try
		{
			return itemManager.getImage(item.getItemId());
		}
		catch (Exception ex)
		{
			log.debug("Unable to load item image for {} ({})", item.getName(), item.getItemId(), ex);
			return null;
		}
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(33, 150, 243));
		graphics.fillOval(1, 1, 14, 14);
		graphics.setColor(Color.WHITE);
		graphics.drawString("B", 4, 12);
		graphics.dispose();
		return image;
	}

	@Provides
	BossReadinessScoreConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossReadinessScoreConfig.class);
	}
}
