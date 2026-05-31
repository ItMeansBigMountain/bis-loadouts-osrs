package com.itmeansbigmountain.bossreadinessscore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.PluginPanel;

public class BossReadinessScorePanel extends PluginPanel
{
	private final JPanel content = new JPanel();

	public BossReadinessScorePanel()
	{
		super(false);
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
	}

	public void updateRecommendation(SetupRecommendation recommendation)
	{
		updateRecommendation(recommendation, null, "Using local fallback data.", java.util.Collections.emptyList());
	}

	public void updateRecommendation(SetupRecommendation recommendation, BossTarget target, String dataStatus, java.util.List<String> suggestions)
	{
		content.removeAll();
		addTitle(recommendation.getBossName() + " readiness");
		if (target != null)
		{
			addLine("Data source", target.getSource());
			if (target.getWikiUrl() != null && !target.getWikiUrl().isEmpty())
			{
				addLine("Wiki", target.getWikiUrl());
			}
		}
		if (dataStatus != null && !dataStatus.isEmpty())
		{
			addMuted(dataStatus);
		}
		addLine("Style", recommendation.getStyle().toString());
		addLine("Readiness", recommendation.getReadinessScore() + "/100");
		addLine("Est. DPS", String.format("%.2f", recommendation.getEstimatedDps()));
		addLine("Hit chance", Math.round(recommendation.getHitChance() * 100.0D) + "%");
		addLine("Max hit", String.valueOf(recommendation.getMaxHit()));
		addSpacer();
		addTitle("Recommended gear");
		for (Map.Entry<GearSlot, GearItem> entry : recommendation.getItems().entrySet())
		{
			GearItem item = entry.getValue();
			addLine(entry.getKey().toString(), item.getName());
			if (item.getNote() != null && !item.getNote().isEmpty())
			{
				addMuted("  " + item.getNote());
			}
		}
		if (!recommendation.getWarnings().isEmpty())
		{
			addSpacer();
			addTitle("Warnings");
			recommendation.getWarnings().forEach(this::addMuted);
		}
		if (!recommendation.getAlternatives().isEmpty())
		{
			addSpacer();
			addTitle("Other styles");
			for (SetupRecommendation alt : recommendation.getAlternatives())
			{
				addLine(alt.getStyle().toString(), String.format("%.2f DPS", alt.getEstimatedDps()));
			}
		}
		if (suggestions != null && !suggestions.isEmpty())
		{
			addSpacer();
			addTitle("Live boss search examples");
			addMuted(String.join(", ", suggestions));
		}
		content.revalidate();
		content.repaint();
	}

	public void showWaitingForLogin()
	{
		content.removeAll();
		addTitle("Boss Readiness Score");
		addMuted("Log in, then pick a boss/style/budget in plugin config. This panel will show recommended gear by slot.");
		content.revalidate();
		content.repaint();
	}

	private void addTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 15.0F));
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		content.add(label);
	}

	private void addLine(String label, String value)
	{
		JLabel row = new JLabel("<html><b>" + escape(label) + ":</b> " + escape(value) + "</html>");
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		content.add(row);
	}

	private void addMuted(String text)
	{
		JLabel label = new JLabel("<html>" + escape(text) + "</html>");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setForeground(Color.GRAY);
		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 3, 0));
		content.add(label);
	}

	private void addSpacer()
	{
		JPanel spacer = new JPanel();
		spacer.setMaximumSize(new Dimension(1, 8));
		spacer.setOpaque(false);
		content.add(spacer);
	}

	private static String escape(String value)
	{
		return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
