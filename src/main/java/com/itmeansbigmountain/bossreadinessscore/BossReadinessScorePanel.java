package com.itmeansbigmountain.bossreadinessscore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public class BossReadinessScorePanel extends PluginPanel
{
	private final JPanel content = new JPanel();
	private final JComboBox<String> bossSelector = new JComboBox<>();
	private final DefaultComboBoxModel<String> bossModel = new DefaultComboBoxModel<>();
	private final List<String> allBossSuggestions = new ArrayList<>();
	private Consumer<String> bossSelectionListener;
	private boolean updatingBossSelector;

	public BossReadinessScorePanel()
	{
		super(false);
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		configureBossSelector();
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
		addBossSelector(recommendation.getBossName(), suggestions);
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
		addBossSelector("", allBossSuggestions);
		addTitle("Boss Readiness Score");
		addMuted("Log in, then type/select a boss in the live autocomplete, then pick style/budget in plugin config.");
		content.revalidate();
		content.repaint();
	}


	public void setBossSelectionListener(Consumer<String> bossSelectionListener)
	{
		this.bossSelectionListener = bossSelectionListener;
	}

	private void configureBossSelector()
	{
		bossSelector.setEditable(true);
		bossSelector.setModel(bossModel);
		bossSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		bossSelector.addActionListener(event -> {
			if (updatingBossSelector || bossSelectionListener == null)
			{
				return;
			}
			Object selected = bossSelector.getEditor().getItem();
			String bossName = selected == null ? "" : selected.toString().trim();
			if (!bossName.isEmpty())
			{
				bossSelectionListener.accept(bossName);
			}
		});

		if (bossSelector.getEditor().getEditorComponent() instanceof JTextField)
		{
			JTextField editor = (JTextField) bossSelector.getEditor().getEditorComponent();
			editor.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent event)
				{
					filterLater(editor.getText());
				}

				@Override
				public void removeUpdate(DocumentEvent event)
				{
					filterLater(editor.getText());
				}

				@Override
				public void changedUpdate(DocumentEvent event)
				{
					filterLater(editor.getText());
				}
			});
		}
	}

	private void filterLater(String query)
	{
		if (updatingBossSelector)
		{
			return;
		}
		SwingUtilities.invokeLater(() -> filterBossOptions(query, true));
	}

	private void addBossSelector(String selectedBoss, java.util.List<String> suggestions)
	{
		allBossSuggestions.clear();
		if (suggestions != null)
		{
			allBossSuggestions.addAll(suggestions);
		}
		addTitle("Boss search");
		filterBossOptions(selectedBoss == null ? "" : selectedBoss, false);
		bossSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(bossSelector);
		addMuted("Type to filter live OSRS Wiki + GearScape boss names; press Enter or pick a dropdown result to load it.");
	}

	private void filterBossOptions(String query, boolean showPopup)
	{
		String text = query == null ? "" : query;
		String normalized = text.toLowerCase(Locale.ROOT).trim();
		updatingBossSelector = true;
		bossModel.removeAllElements();
		if (!text.trim().isEmpty())
		{
			bossModel.addElement(text);
		}
		int added = 0;
		for (String suggestion : allBossSuggestions)
		{
			if (suggestion == null || suggestion.equals(text))
			{
				continue;
			}
			if (normalized.isEmpty() || suggestion.toLowerCase(Locale.ROOT).contains(normalized))
			{
				bossModel.addElement(suggestion);
				added++;
			}
			if (added >= 30)
			{
				break;
			}
		}
		bossSelector.getEditor().setItem(text);
		updatingBossSelector = false;
		if (showPopup && bossSelector.isShowing() && bossModel.getSize() > 0)
		{
			bossSelector.showPopup();
		}
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
