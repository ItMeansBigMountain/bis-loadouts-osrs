package com.itmeansbigmountain.bossreadinessscore;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Boss Readiness Score",
	description = "Scores account readiness for bossing from combat, Hitpoints, Prayer, and Defence levels.",
	tags = {"boss", "pvm", "readiness", "combat"}
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
	private ConfigManager configManager;

	private final BossDataService bossDataService = new BossDataService();
	private ExecutorService apiExecutor;
	private BossReadinessScorePanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		log.debug("Boss Readiness Score started");
		apiExecutor = Executors.newSingleThreadExecutor();
		panel = new BossReadinessScorePanel();
		panel.setBossSelectionListener(this::selectBossNameFromPanel);
		panel.showWaitingForLogin();
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
			refreshPanel();
		});
		refreshPanel();
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
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		refreshPanel();
		if (!config.showLoginSummary())
		{
			return;
		}

		int combatLevel = client.getLocalPlayer() == null ? 0 : client.getLocalPlayer().getCombatLevel();
		int hitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
		int prayer = client.getRealSkillLevel(Skill.PRAYER);
		int defence = client.getRealSkillLevel(Skill.DEFENCE);
		int score = calculateReadinessScore(combatLevel, hitpoints, prayer, defence,
			config.targetCombatLevel(), config.targetPrayerLevel());

		String bossName = config.bossName() == null || config.bossName().trim().isEmpty() ? config.bossProfile().getLabel() : config.bossName().trim();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", buildSummaryMessage(bossName, score, config.warningThreshold()), null);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("bossreadinessscore".equals(event.getGroup()))
		{
			refreshPanel();
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

	static String buildSummaryMessage(String bossProfile, int score, int warningThreshold)
	{
		String profile = bossProfile == null || bossProfile.trim().isEmpty() ? "selected boss" : bossProfile.trim();
		String recommendation = score < warningThreshold ? "caution: consider more levels/gear before attempting" : "ready for manual gear checks";
		return String.format("Boss Readiness Score for %s: %d/100 (%s).", profile, clampScore(score), recommendation);
	}

	private static double ratioScore(int actual, int target)
	{
		return Math.min(1.0D, Math.max(0.0D, actual / (double) target)) * MAX_SCORE;
	}

	private static int clampScore(int score)
	{
		return Math.max(0, Math.min(MAX_SCORE, score));
	}

	private void refreshPanel()
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
					panel.showWaitingForLogin();
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
		String requestedBossName = config.bossName();
		BossProfile fallbackProfile = config.bossProfile();
		CombatStyle style = config.combatStyle();
		BudgetTier budget = config.budgetTier();
		apiExecutor.submit(() -> {
			BossTarget target = bossDataService.resolveBoss(requestedBossName, fallbackProfile);
			SetupRecommendation recommendation = GearRecommendationEngine.recommend(target, style, budget, stats, bossDataService.getGearItems());
			String status = bossDataService.getStatus();
			SwingUtilities.invokeLater(() -> {
				if (panel != null)
				{
					panel.updateRecommendation(recommendation, target, status, bossDataService.getBossNameSuggestions(1000));
				}
			});
		});
	}

	private void selectBossNameFromPanel(String bossName)
	{
		if (bossName == null || bossName.trim().isEmpty())
		{
			return;
		}
		String trimmed = bossName.trim();
		String current = config.bossName() == null ? "" : config.bossName().trim();
		if (trimmed.equals(current))
		{
			return;
		}
		configManager.setConfiguration("bossreadinessscore", "bossName", trimmed);
		refreshPanel();
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
