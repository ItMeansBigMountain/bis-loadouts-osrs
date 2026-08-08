package com.itmeansbigmountain.bisloadouts;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BisLoadoutsPanelTest
{
	@Test
	public void handednessUsesOneToggleBesideAnalyze() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			BisLoadoutsPanel panel = new BisLoadoutsPanel();
			GearItem twoHanded = weapon("Test 2H", true);
			GearItem oneHanded = weapon("Test 1H", false);
			Map<GearSlot, GearItem> items = new EnumMap<>(GearSlot.class);
			items.put(GearSlot.WEAPON, twoHanded);
			Map<GearSlot, List<GearItem>> alternatives = new EnumMap<>(GearSlot.class);
			alternatives.put(GearSlot.WEAPON, Arrays.asList(twoHanded, oneHanded));
			SetupRecommendation recommendation = new SetupRecommendation("Test boss", CombatStyle.MAGIC,
				items, alternatives, 1.0D, 1.0D, 1, 100, Collections.emptyList(), Collections.emptyList());

			panel.updateRecommendation(recommendation, null, "", Collections.emptyList());

			JButton analyze = onlyButton(panel, "Analyze");
			JButton mode = onlyHandednessButton(panel);
			assertEquals("2H", mode.getText());
			assertSame("mode toggle should share the Analyze row", analyze.getParent(), mode.getParent());

			mode.doClick();

			assertEquals("1H", onlyHandednessButton(panel).getText());
		});
	}

	@Test
	public void bossAutocompleteUsesShorthandAndStaysCompact() throws Exception
	{
		final BisLoadoutsPanel[] panel = new BisLoadoutsPanel[1];
		SwingUtilities.invokeAndWait(() -> {
			panel[0] = new BisLoadoutsPanel();
			panel[0].showWaitingForLogin(Arrays.asList("Zulrah", "Vorkath", "King Black Dragon", "TzTok-Jad",
				"General Graardor", "Cerberus", "The Hueycoatl", "Phantom Muspah", "TzKal-Zuk",
				"Demonic Brutus", "Brutus", "Yama", "Amoxliatl", "Scurrius"), "");
			JComboBox<?> selector = comboBoxes(panel[0]).get(0);
			((JTextField) selector.getEditor().getEditorComponent()).setText("kbd");
		});
		SwingUtilities.invokeAndWait(() -> {
			JComboBox<?> selector = comboBoxes(panel[0]).get(0);
			List<String> values = new java.util.ArrayList<>();
			for (int i = 0; i < selector.getModel().getSize(); i++)
			{
				values.add(String.valueOf(selector.getModel().getElementAt(i)));
			}
			assertTrue(values.contains("King Black Dragon"));
			assertTrue("autocomplete should stay compact", values.size() <= 13);
			assertTrue("search purpose should be visible", labels(panel[0]).stream()
				.anyMatch(label -> label.getText().contains("Search boss")));
		});
	}

	private static GearItem weapon(String name, boolean twoHanded)
	{
		return new GearItem(GearSlot.WEAPON, -1, name, EnumSet.of(CombatStyle.MAGIC),
			0, 0, 0, 0, 0, 0, 1, 1, 0, "test", null, "https://example.com", twoHanded);
	}

	private static JButton onlyButton(Container root, String text)
	{
		List<JButton> matches = buttons(root).stream().filter(button -> text.equals(button.getText())).collect(Collectors.toList());
		assertEquals("button count for " + text, 1, matches.size());
		return matches.get(0);
	}

	private static JButton onlyHandednessButton(Container root)
	{
		List<JButton> matches = buttons(root).stream()
			.filter(button -> "1H".equals(button.getText()) || "2H".equals(button.getText()))
			.collect(Collectors.toList());
		assertEquals("exactly one handedness toggle", 1, matches.size());
		return matches.get(0);
	}

	private static List<JButton> buttons(Container root)
	{
		java.util.ArrayList<JButton> result = new java.util.ArrayList<>();
		for (Component component : root.getComponents())
		{
			if (component instanceof JButton)
			{
				result.add((JButton) component);
			}
			if (component instanceof Container)
			{
				result.addAll(buttons((Container) component));
			}
		}
		return result;
	}

	private static List<JLabel> labels(Container root)
	{
		java.util.ArrayList<JLabel> result = new java.util.ArrayList<>();
		for (Component component : root.getComponents())
		{
			if (component instanceof JLabel)
			{
				result.add((JLabel) component);
			}
			if (component instanceof Container)
			{
				result.addAll(labels((Container) component));
			}
		}
		return result;
	}

	private static List<JComboBox<?>> comboBoxes(Container root)
	{
		java.util.ArrayList<JComboBox<?>> result = new java.util.ArrayList<>();
		for (Component component : root.getComponents())
		{
			if (component instanceof JComboBox)
			{
				result.add((JComboBox<?>) component);
			}
			if (component instanceof Container)
			{
				result.addAll(comboBoxes((Container) component));
			}
		}
		return result;
	}
}
