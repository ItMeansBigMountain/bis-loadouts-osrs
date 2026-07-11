package com.itmeansbigmountain.bossreadinessscore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
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
	private static final int PANEL_WIDTH = 210;
	private static final int TEXT_WIDTH = 184;
	private static final int CONTROL_WIDTH = 176;
	private static final int EQUIPMENT_CELL_WIDTH = 52;
	private static final int EQUIPMENT_CELL_HEIGHT = 64;
	private static final int ICON_SIZE = 32;

	private final JPanel content = new JPanel();
	private final JComboBox<String> bossSelector = new JComboBox<>();
	private final DefaultComboBoxModel<String> bossModel = new DefaultComboBoxModel<>();
	private final List<String> allBossSuggestions = new ArrayList<>();
	private final Map<GearSlot, Integer> slotIndexes = new EnumMap<>(GearSlot.class);
	private final JRadioButton autoStyle = new JRadioButton("Auto");
	private final JRadioButton magicStyle = new JRadioButton("Mag");
	private final JRadioButton rangedStyle = new JRadioButton("Rng");
	private final JRadioButton meleeStyle = new JRadioButton("Mel");
	private BiConsumer<String, CombatStyle> analyzeListener;
	private BiConsumer<GearItem, JLabel> itemIconProvider = (item, label) -> { };
	private Consumer<GearItem> itemWikiOpener = item -> { };
	private boolean updatingBossSelector;
	private SetupRecommendation currentRecommendation;
	private BossTarget currentTarget;
	private String currentStatus = "Loading boss/equipment data...";
	private Boolean weaponTwoHandedMode;

	public BossReadinessScorePanel()
	{
		super(false);
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		content.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
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

	public void setItemIconProvider(BiConsumer<GearItem, JLabel> itemIconProvider)
	{
		this.itemIconProvider = itemIconProvider == null ? (item, label) -> { } : itemIconProvider;
	}

	public void setItemWikiOpener(Consumer<GearItem> itemWikiOpener)
	{
		this.itemWikiOpener = itemWikiOpener == null ? item -> { } : itemWikiOpener;
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
		JButton analyze = new JButton("Analyze");
		analyze.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension analyzeSize = new Dimension(CONTROL_WIDTH, 32);
		analyze.setPreferredSize(analyzeSize);
		analyze.setMinimumSize(new Dimension(140, 30));
		analyze.setMaximumSize(analyzeSize);
		analyze.addActionListener(event -> {
			if (analyzeListener != null)
			{
				analyzeListener.accept(selectedBoss(), selectedStyle());
			}
		});
		addCentered(analyze, CONTROL_WIDTH, 32);
		if (currentStatus != null && !currentStatus.isEmpty())
		{
			addMuted(summarizeStatus(currentStatus));
		}
	}

	private void configureBossSelector()
	{
		bossSelector.setEditable(true);
		bossSelector.setModel(bossModel);
		Dimension bossSelectorSize = new Dimension(CONTROL_WIDTH, 28);
		bossSelector.setPreferredSize(bossSelectorSize);
		bossSelector.setMinimumSize(bossSelectorSize);
		bossSelector.setMaximumSize(bossSelectorSize);
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
		bossSelector.setAlignmentX(Component.CENTER_ALIGNMENT);
		addCentered(bossSelector, CONTROL_WIDTH, 30);
	}

	private void addStyleControls()
	{
		addTitle("Setup style");
		JPanel styleGrid = new JPanel(new GridBagLayout());
		styleGrid.setOpaque(false);
		styleGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
		styleGrid.setMaximumSize(new Dimension(CONTROL_WIDTH, 42));
		addStyleButton(styleGrid, autoStyle, 0, 0);
		addStyleButton(styleGrid, magicStyle, 1, 0);
		addStyleButton(styleGrid, rangedStyle, 0, 1);
		addStyleButton(styleGrid, meleeStyle, 1, 1);
		addCentered(styleGrid, CONTROL_WIDTH, 42);
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

		JPanel gridWrapper = new JPanel(new GridBagLayout());
		gridWrapper.setOpaque(false);
		gridWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
		gridWrapper.setMaximumSize(new Dimension(PANEL_WIDTH, grid.getPreferredSize().height));
		gridWrapper.add(grid, new GridBagConstraints());
		content.add(gridWrapper);
		addWeaponSetToggle(recommendation);
		addMuted("< > cycles the selected 1H/2H set: strongest on the left, weaker to the right.");
		if (currentTarget != null)
		{
			addMuted("Boss weakness: " + bossWeaknessText(currentTarget));
		}
	}

	private void addSlot(JPanel grid, SetupRecommendation recommendation, GearSlot slot, int x, int y)
	{
		GearItem selectedWeapon = displayedWeapon(recommendation);
		boolean disabledByTwoHander = slot == GearSlot.SHIELD && selectedWeapon != null && selectedWeapon.isTwoHanded();
		List<GearItem> alternatives = disabledByTwoHander ? java.util.Collections.emptyList() : displayedAlternatives(recommendation, slot, selectedWeapon);
		int index = Math.max(0, Math.min(slotIndexes.getOrDefault(slot, 0), Math.max(0, alternatives.size() - 1)));
		GearItem item = disabledByTwoHander ? null : alternatives.isEmpty() ? recommendation.getItem(slot) : alternatives.get(index);
		JPanel cell = new JPanel(new BorderLayout(0, 0));
		Dimension cellSize = new Dimension(EQUIPMENT_CELL_WIDTH, EQUIPMENT_CELL_HEIGHT);
		cell.setPreferredSize(cellSize);
		cell.setMinimumSize(cellSize);
		cell.setBorder(BorderFactory.createLineBorder(new Color(115, 110, 95), 1));
		cell.setBackground(new Color(62, 58, 49));
		JButton left = tinyButton("<");
		JButton right = tinyButton(">");
		left.setEnabled(alternatives.size() > 1);
		right.setEnabled(alternatives.size() > 1);
		left.addActionListener(event -> cycleSlot(slot, -1));
		right.addActionListener(event -> cycleSlot(slot, 1));
		JPanel center = new JPanel();
		center.setOpaque(false);
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		JLabel icon = createIconLabel(item);
		icon.setAlignmentX(Component.CENTER_ALIGNMENT);
		String itemText = disabledByTwoHander ? "2H weapon" : item == null ? "—" : compactItemName(item.getName());
		if (slot == GearSlot.WEAPON && item != null)
		{
			itemText = (item.isTwoHanded() ? "2H " : "1H ") + itemText;
		}
		JLabel label = new JLabel("<html><center><b>" + escape(shortSlot(slot)) + "</b><br>" + escape(itemText) + "</center></html>");
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(7.0F));
		makeItemWikiClickable(center, item);
		makeItemWikiClickable(icon, item);
		makeItemWikiClickable(label, item);
		center.add(icon);
		center.add(label);
		cell.add(left, BorderLayout.WEST);
		cell.add(center, BorderLayout.CENTER);
		cell.add(right, BorderLayout.EAST);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.insets = new Insets(1, 1, 1, 1);
		grid.add(cell, constraints);
	}

	private JLabel createIconLabel(GearItem item)
	{
		JLabel label = new JLabel();
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
		label.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));
		if (item == null)
		{
			label.setText("—");
			label.setForeground(Color.LIGHT_GRAY);
			return label;
		}
		itemIconProvider.accept(item, label);
		if (label.getIcon() == null && (item.getItemId() <= 0 && (item.getIconBase64() == null || item.getIconBase64().isEmpty())))
		{
			label.setText("?");
			label.setToolTipText(item.getName() + " (missing item image) - click for OSRS Wiki");
			label.setForeground(Color.LIGHT_GRAY);
		}
		else
		{
			label.setText("");
			label.setToolTipText(item.getName() + " - click for OSRS Wiki");
		}
		return label;
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

	private void makeItemWikiClickable(Component component, GearItem item)
	{
		if (item == null)
		{
			return;
		}
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				itemWikiOpener.accept(item);
			}
		});
	}

	private JButton tinyButton(String text)
	{
		JButton button = new JButton(text);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(8, 16));
		button.setFont(button.getFont().deriveFont(6.5F));
		return button;
	}

	private GearItem displayedWeapon(SetupRecommendation recommendation)
	{
		List<GearItem> weaponAlternatives = displayedWeaponAlternatives(recommendation);
		if (weaponAlternatives.isEmpty())
		{
			return recommendation.getItem(GearSlot.WEAPON);
		}
		int index = Math.max(0, Math.min(slotIndexes.getOrDefault(GearSlot.WEAPON, 0), weaponAlternatives.size() - 1));
		return weaponAlternatives.get(index);
	}

	private List<GearItem> displayedAlternatives(SetupRecommendation recommendation, GearSlot slot, GearItem selectedWeapon)
	{
		List<GearItem> alternatives = slot == GearSlot.WEAPON
			? displayedWeaponAlternatives(recommendation)
			: recommendation.getAlternativesForSlot(slot);
		if (slot == GearSlot.AMMUNITION && selectedWeapon != null && !selectedWeapon.getCompatibleAmmoIds().isEmpty())
		{
			List<GearItem> compatible = new ArrayList<>();
			for (GearItem item : alternatives)
			{
				if (selectedWeapon.acceptsAmmo(item))
				{
					compatible.add(item);
				}
			}
			return compatible;
		}
		return alternatives;
	}

	private List<GearItem> displayedWeaponAlternatives(SetupRecommendation recommendation)
	{
		List<GearItem> all = recommendation.getAlternativesForSlot(GearSlot.WEAPON);
		if (all.isEmpty())
		{
			return all;
		}
		if (weaponTwoHandedMode == null || all.stream().noneMatch(item -> item.isTwoHanded() == weaponTwoHandedMode))
		{
			GearItem selected = recommendation.getItem(GearSlot.WEAPON);
			weaponTwoHandedMode = selected == null || selected.isTwoHanded();
		}
		List<GearItem> filtered = new ArrayList<>();
		for (GearItem item : all)
		{
			if (item.isTwoHanded() == weaponTwoHandedMode)
			{
				filtered.add(item);
			}
		}
		return filtered.isEmpty() ? all : filtered;
	}

	private void addWeaponSetToggle(SetupRecommendation recommendation)
	{
		List<GearItem> all = recommendation.getAlternativesForSlot(GearSlot.WEAPON);
		boolean hasOneHanded = all.stream().anyMatch(item -> !item.isTwoHanded());
		boolean hasTwoHanded = all.stream().anyMatch(GearItem::isTwoHanded);
		if (!hasOneHanded || !hasTwoHanded)
		{
			return;
		}
		JPanel toggle = new JPanel(new GridBagLayout());
		toggle.setOpaque(false);
		JButton oneHanded = tinyButton("1H");
		JButton twoHanded = tinyButton("2H");
		oneHanded.setPreferredSize(new Dimension(34, 18));
		twoHanded.setPreferredSize(new Dimension(34, 18));
		oneHanded.setEnabled(Boolean.TRUE.equals(weaponTwoHandedMode));
		twoHanded.setEnabled(!Boolean.TRUE.equals(weaponTwoHandedMode));
		oneHanded.addActionListener(event -> switchWeaponSet(false));
		twoHanded.addActionListener(event -> switchWeaponSet(true));
		toggle.add(oneHanded);
		toggle.add(twoHanded);
		addCentered(toggle, CONTROL_WIDTH, 20);
	}

	private void switchWeaponSet(boolean twoHanded)
	{
		weaponTwoHandedMode = twoHanded;
		slotIndexes.put(GearSlot.WEAPON, 0);
		slotIndexes.put(GearSlot.SHIELD, 0);
		slotIndexes.put(GearSlot.AMMUNITION, 0);
		updateRecommendation(currentRecommendation, currentTarget, currentStatus, allBossSuggestions);
	}

	private void cycleSlot(GearSlot slot, int delta)
	{
		if (currentRecommendation == null)
		{
			return;
		}
		List<GearItem> alternatives = displayedAlternatives(currentRecommendation, slot, displayedWeapon(currentRecommendation));
		if (alternatives.size() <= 1)
		{
			return;
		}
		int next = Math.floorMod(slotIndexes.getOrDefault(slot, 0) + delta, alternatives.size());
		slotIndexes.put(slot, next);
		updateRecommendation(currentRecommendation, currentTarget, currentStatus, allBossSuggestions);
	}

	private static String bossWeaknessText(BossTarget target)
	{
		List<DefenceValue> values = new ArrayList<>();
		values.add(new DefenceValue("Stab", target.getDefStab()));
		values.add(new DefenceValue("Slash", target.getDefSlash()));
		values.add(new DefenceValue("Crush", target.getDefCrush()));
		values.add(new DefenceValue("Magic", target.getDefMagic()));
		values.add(new DefenceValue("Ranged", target.getDefRanged()));
		if (values.stream().allMatch(value -> value.value == 0))
		{
			return "unknown from this data source";
		}
		values.sort(Comparator.comparingInt(value -> value.value));
		StringBuilder builder = new StringBuilder();
		for (DefenceValue value : values)
		{
			if (builder.length() > 0)
			{
				builder.append(" > ");
			}
			builder.append(value.label).append(" ").append(value.value);
		}
		return builder.toString();
	}

	private static final class DefenceValue
	{
		private final String label;
		private final int value;

		private DefenceValue(String label, int value)
		{
			this.label = label;
			this.value = value;
		}
	}

	private void addStyleButton(JPanel styleGrid, JRadioButton button, int x, int y)
	{
		button.setFont(button.getFont().deriveFont(10.0F));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setOpaque(false);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 0, 0, 2);
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
		JLabel label = new JLabel("<html><div style=\"width:" + TEXT_WIDTH + "px; text-align:center\">" + escape(text) + "</div></html>");
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12.0F));
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		addCentered(label, TEXT_WIDTH, label.getPreferredSize().height);
	}

	private void addLine(String label, String value)
	{
		JLabel row = new JLabel("<html><div style=\"width:" + TEXT_WIDTH + "px; text-align:center\"><b>" + escape(label) + ":</b> " + escape(value) + "</div></html>");
		row.setAlignmentX(Component.CENTER_ALIGNMENT);
		row.setHorizontalAlignment(JLabel.CENTER);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		addCentered(row, TEXT_WIDTH, row.getPreferredSize().height);
	}

	private void addMuted(String text)
	{
		JLabel label = new JLabel("<html><div style=\"width:" + TEXT_WIDTH + "px; text-align:center\">" + escape(text) + "</div></html>");
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setForeground(Color.GRAY);
		label.setFont(label.getFont().deriveFont(10.0F));
		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 2, 0));
		addCentered(label, TEXT_WIDTH, label.getPreferredSize().height);
	}

	private void addCentered(Component component, int width, int height)
	{
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setOpaque(false);
		wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
		wrapper.setPreferredSize(new Dimension(PANEL_WIDTH, height));
		wrapper.setMaximumSize(new Dimension(PANEL_WIDTH, height));
		wrapper.add(component, new GridBagConstraints());
		component.setPreferredSize(new Dimension(width, height));
		component.setMaximumSize(new Dimension(width, height));
		content.add(wrapper);
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
