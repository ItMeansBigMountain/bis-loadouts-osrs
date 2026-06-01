package com.itmeansbigmountain.bossreadinessscore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public class BossReadinessScorePanel extends PluginPanel
{
	private static final String NONE_BOSS = "None";
	private static final int PANEL_WIDTH = 220;
	private static final int TEXT_WIDTH = 198;

	private final JPanel content = new JPanel();
	private final JComboBox<String> bossSelector = new JComboBox<>();
	private final DefaultComboBoxModel<String> bossModel = new DefaultComboBoxModel<>();
	private final List<String> allBossSuggestions = new ArrayList<>();
	private final Map<GearSlot, Integer> slotIndexes = new EnumMap<>(GearSlot.class);
	private final JRadioButton autoStyle = new JRadioButton("Auto");
	private final JRadioButton magicStyle = new JRadioButton("Mage");
	private final JRadioButton rangedStyle = new JRadioButton("Range");
	private final JRadioButton meleeStyle = new JRadioButton("Melee");
	private BiConsumer<String, CombatStyle> analyzeListener;
	private boolean updatingBossSelector;
	private SetupRecommendation currentRecommendation;
	private BossTarget currentTarget;
	private String currentStatus = "Loading boss/equipment data...";

	public BossReadinessScorePanel()
	{
		super(false);
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		content.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));
		configureBossSelector();
		configureStyleButtons();
		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void setAnalyzeListener(BiConsumer<String, CombatStyle> analyzeListener)
	{
		this.analyzeListener = analyzeListener;
	}

	public void showWaitingForLogin(List<String> suggestions, String status)
	{
		currentRecommendation = null;
		currentTarget = null;
		currentStatus = status == null ? "" : status;
		content.removeAll();
		addControls(suggestions);
		addMuted("Log in, pick boss/style, then Analyze.");
		content.revalidate();
		content.repaint();
	}

	public void updateRecommendation(SetupRecommendation recommendation, BossTarget target, String dataStatus, List<String> suggestions)
	{
		currentRecommendation = recommendation;
		currentTarget = target;
		currentStatus = dataStatus == null ? "" : dataStatus;
		content.removeAll();
		addControls(suggestions);
		if (recommendation == null)
		{
			addMuted("No recommendation yet. Press Analyze.");
		}
		else
		{
			addSummary(recommendation, target);
			addEquipmentGrid(recommendation);
			addWarnings(recommendation);
		}
		content.revalidate();
		content.repaint();
	}

	private void addControls(List<String> suggestions)
	{
		addTitle("Boss selection");
		addBossSelector(suggestions);
		addSpacer();
		addStyleControls();
		addSpacer();
		JButton analyze = new JButton("ANALYZE");
		analyze.setAlignmentX(Component.CENTER_ALIGNMENT);
		analyze.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
		analyze.addActionListener(event -> {
			if (analyzeListener != null)
			{
				analyzeListener.accept(selectedBoss(), selectedStyle());
			}
		});
		content.add(analyze);
		if (currentStatus != null && !currentStatus.isEmpty())
		{
			addMuted(summarizeStatus(currentStatus));
		}
	}

	private void configureBossSelector()
	{
		bossSelector.setEditable(true);
		bossSelector.setModel(bossModel);
		bossSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		bossSelector.setSelectedItem(NONE_BOSS);
		if (bossSelector.getEditor().getEditorComponent() instanceof JTextField)
		{
			JTextField editor = (JTextField) bossSelector.getEditor().getEditorComponent();
			editor.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override public void insertUpdate(DocumentEvent event) { filterLater(editor.getText()); }
				@Override public void removeUpdate(DocumentEvent event) { filterLater(editor.getText()); }
				@Override public void changedUpdate(DocumentEvent event) { filterLater(editor.getText()); }
			});
		}
	}

	private void configureStyleButtons()
	{
		ButtonGroup group = new ButtonGroup();
		group.add(autoStyle);
		group.add(magicStyle);
		group.add(rangedStyle);
		group.add(meleeStyle);
		autoStyle.setSelected(true);
	}

	private void addBossSelector(List<String> suggestions)
	{
		allBossSuggestions.clear();
		allBossSuggestions.add(NONE_BOSS);
		if (suggestions != null)
		{
			for (String suggestion : suggestions)
			{
				if (suggestion != null && !suggestion.trim().isEmpty() && !allBossSuggestions.contains(suggestion))
				{
					allBossSuggestions.add(suggestion);
				}
			}
		}
		filterBossOptions(selectedBoss(), false);
		bossSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(bossSelector);
	}

	private void addStyleControls()
	{
		addTitle("Setup style");
		JPanel styleGrid = new JPanel(new GridBagLayout());
		styleGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
		addStyleButton(styleGrid, autoStyle, 0, 0);
		addStyleButton(styleGrid, magicStyle, 1, 0);
		addStyleButton(styleGrid, rangedStyle, 0, 1);
		addStyleButton(styleGrid, meleeStyle, 1, 1);
		content.add(styleGrid);
		addMuted("Auto = best style. None = best gear for stats.");
	}

	private void addSummary(SetupRecommendation recommendation, BossTarget target)
	{
		addTitle(recommendation.getBossName() + " setup");
		addLine("Chosen style", recommendation.getStyle().toString());
		addLine("Readiness", recommendation.getReadinessScore() + "/100");
		addLine("Est. DPS", String.format("%.2f", recommendation.getEstimatedDps()));
		if (target != null)
		{
			addMuted(summarizeStatus(target.getSource()));
		}
	}

	private void addEquipmentGrid(SetupRecommendation recommendation)
	{
		addTitle("Recommended equipment");
		JPanel grid = new JPanel(new GridBagLayout());
		grid.setAlignmentX(Component.LEFT_ALIGNMENT);
		grid.setBorder(BorderFactory.createLineBorder(new Color(80, 75, 65), 1));
		grid.setBackground(new Color(46, 42, 35));
		addSlot(grid, recommendation, GearSlot.HEAD, 1, 0);
		addSlot(grid, recommendation, GearSlot.CAPE, 0, 1);
		addSlot(grid, recommendation, GearSlot.NECK, 1, 1);
		addSlot(grid, recommendation, GearSlot.AMMUNITION, 2, 1);
		addSlot(grid, recommendation, GearSlot.WEAPON, 0, 2);
		addSlot(grid, recommendation, GearSlot.BODY, 1, 2);
		addSlot(grid, recommendation, GearSlot.SHIELD, 2, 2);
		addSlot(grid, recommendation, GearSlot.LEGS, 1, 3);
		addSlot(grid, recommendation, GearSlot.HANDS, 0, 4);
		addSlot(grid, recommendation, GearSlot.FEET, 1, 4);
		addSlot(grid, recommendation, GearSlot.RING, 2, 4);
		content.add(grid);
		addMuted("< > cycles alternatives.");
	}

	private void addSlot(JPanel grid, SetupRecommendation recommendation, GearSlot slot, int x, int y)
	{
		List<GearItem> alternatives = recommendation.getAlternativesForSlot(slot);
		int index = Math.max(0, Math.min(slotIndexes.getOrDefault(slot, 0), Math.max(0, alternatives.size() - 1)));
		GearItem item = alternatives.isEmpty() ? recommendation.getItem(slot) : alternatives.get(index);
		JPanel cell = new JPanel(new BorderLayout(0, 0));
		cell.setPreferredSize(new Dimension(66, 50));
		cell.setMinimumSize(new Dimension(66, 50));
		cell.setBorder(BorderFactory.createLineBorder(new Color(115, 110, 95), 1));
		cell.setBackground(new Color(62, 58, 49));
		JButton left = tinyButton("<");
		JButton right = tinyButton(">");
		left.setEnabled(alternatives.size() > 1);
		right.setEnabled(alternatives.size() > 1);
		left.addActionListener(event -> cycleSlot(slot, -1));
		right.addActionListener(event -> cycleSlot(slot, 1));
		JLabel label = new JLabel("<html><center><b>" + escape(shortSlot(slot)) + "</b><br>" + escape(item == null ? "—" : compactItemName(item.getName())) + "</center></html>");
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(8.5F));
		cell.add(left, BorderLayout.WEST);
		cell.add(label, BorderLayout.CENTER);
		cell.add(right, BorderLayout.EAST);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.insets = new Insets(1, 1, 1, 1);
		grid.add(cell, constraints);
	}

	private void addWarnings(SetupRecommendation recommendation)
	{
		if (!recommendation.getWarnings().isEmpty())
		{
			addTitle("Notes");
			recommendation.getWarnings().forEach(this::addMuted);
		}
		if (!recommendation.getAlternatives().isEmpty())
		{
			addTitle("Other styles");
			for (SetupRecommendation alt : recommendation.getAlternatives())
			{
				addLine(alt.getStyle().toString(), String.format("%.2f DPS", alt.getEstimatedDps()));
			}
		}
	}

	private JButton tinyButton(String text)
	{
		JButton button = new JButton(text);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(12, 18));
		button.setFont(button.getFont().deriveFont(8.0F));
		return button;
	}

	private void cycleSlot(GearSlot slot, int delta)
	{
		if (currentRecommendation == null)
		{
			return;
		}
		List<GearItem> alternatives = currentRecommendation.getAlternativesForSlot(slot);
		if (alternatives.size() <= 1)
		{
			return;
		}
		int next = Math.floorMod(slotIndexes.getOrDefault(slot, 0) + delta, alternatives.size());
		slotIndexes.put(slot, next);
		updateRecommendation(currentRecommendation, currentTarget, currentStatus, allBossSuggestions);
	}

	private void addStyleButton(JPanel styleGrid, JRadioButton button, int x, int y)
	{
		button.setFont(button.getFont().deriveFont(10.5F));
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 0, 4);
		styleGrid.add(button, constraints);
	}

	private static String shortSlot(GearSlot slot)
	{
		switch (slot)
		{
			case AMMUNITION: return "Ammo";
			case WEAPON: return "Weapon";
			case SHIELD: return "Shield";
			default: return slot.toString();
		}
	}

	private static String compactItemName(String name)
	{
		if (name == null || name.isEmpty())
		{
			return "—";
		}
		String compact = name
			.replace("corrupted ", "corr. ")
			.replace("perfected", "perf.")
			.replace("necklace", "neck")
			.replace("imbued ", "imb. ")
			.replace("dragon ", "d. ")
			.replace("ancestral", "anc.")
			.replace("rune ", "r. ");
		return compact.length() <= 18 ? compact : compact.substring(0, 16) + "…";
	}

	private static String summarizeStatus(String status)
	{
		if (status == null || status.isEmpty())
		{
			return "";
		}
		if (status.contains("No boss selected")) return "No boss: best gear for stats.";
		if (status.contains("OSRS Wiki + GearScape")) return "Live boss data loaded.";
		if (status.contains("Loaded")) return "Boss/equipment data loaded.";
		if (status.contains("fallback")) return "Using fallback data.";
		return status.length() <= 42 ? status : status.substring(0, 39) + "…";
	}

	private CombatStyle selectedStyle()
	{
		if (magicStyle.isSelected()) return CombatStyle.MAGIC;
		if (rangedStyle.isSelected()) return CombatStyle.RANGED;
		if (meleeStyle.isSelected()) return CombatStyle.MELEE;
		return CombatStyle.AUTO;
	}

	private String selectedBoss()
	{
		Object selected = bossSelector.getEditor().getItem();
		String boss = selected == null ? "" : selected.toString().trim();
		return boss.isEmpty() ? NONE_BOSS : boss;
	}

	private void filterLater(String query)
	{
		if (updatingBossSelector)
		{
			return;
		}
		SwingUtilities.invokeLater(() -> filterBossOptions(query, true));
	}

	private void filterBossOptions(String query, boolean showPopup)
	{
		String text = query == null || query.trim().isEmpty() ? NONE_BOSS : query;
		String normalized = text.toLowerCase(Locale.ROOT).trim();
		updatingBossSelector = true;
		bossModel.removeAllElements();
		if (!allBossSuggestions.contains(text))
		{
			bossModel.addElement(text);
		}
		int added = 0;
		for (String suggestion : allBossSuggestions)
		{
			if (normalized.isEmpty() || suggestion.toLowerCase(Locale.ROOT).contains(normalized) || normalized.equals(NONE_BOSS.toLowerCase(Locale.ROOT)))
			{
				bossModel.addElement(suggestion);
				added++;
			}
			if (added >= 60)
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
		label.setFont(label.getFont().deriveFont(Font.BOLD, 13.0F));
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		content.add(label);
	}

	private void addLine(String label, String value)
	{
		JLabel row = new JLabel("<html><div style=\"width:" + TEXT_WIDTH + "px\"><b>" + escape(label) + ":</b> " + escape(value) + "</div></html>");
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		content.add(row);
	}

	private void addMuted(String text)
	{
		JLabel label = new JLabel("<html><div style=\"width:" + TEXT_WIDTH + "px\">" + escape(text) + "</div></html>");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setForeground(Color.GRAY);
		label.setFont(label.getFont().deriveFont(10.0F));
		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 2, 0));
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
