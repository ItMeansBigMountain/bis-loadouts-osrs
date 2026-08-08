package com.itmeansbigmountain.bisloadouts;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Shared boss-name ranking for both data resolution and sidebar autocomplete. */
final class BossSearchMatcher
{
	private static final int NO_MATCH = Integer.MAX_VALUE;
	private static final Map<String, String> ALIASES = new LinkedHashMap<>();

	static
	{
		ALIASES.put("kbd", "king black dragon");
		ALIASES.put("jad", "tztok jad");
		ALIASES.put("zuk", "tzkal zuk");
		ALIASES.put("gg", "general graardor");
		ALIASES.put("cerb", "cerberus");
		ALIASES.put("huey", "the hueycoatl");
		ALIASES.put("muspah", "phantom muspah");
	}

	private BossSearchMatcher()
	{
	}

	static List<String> rank(Collection<String> candidates, String query, int limit)
	{
		if (candidates == null || limit <= 0)
		{
			return new ArrayList<>();
		}
		String normalizedQuery = expandAlias(normalize(query));
		if (normalizedQuery.isEmpty())
		{
			return candidates.stream()
				.filter(candidate -> candidate != null && !candidate.trim().isEmpty())
				.distinct()
				.limit(limit)
				.collect(Collectors.toList());
		}
		Map<String, Integer> ranked = new LinkedHashMap<>();
		for (String candidate : candidates)
		{
			if (candidate == null || candidate.trim().isEmpty())
			{
				continue;
			}
			int score = normalizedQuery.isEmpty() ? 0 : scoreNormalized(normalizedQuery, normalize(candidate));
			if (score != NO_MATCH)
			{
				ranked.putIfAbsent(candidate, score);
			}
		}
		return ranked.entrySet().stream()
			.sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue)
				.thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
			.limit(limit)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
	}

	static int score(String query, String candidate)
	{
		return scoreNormalized(expandAlias(normalize(query)), normalize(candidate));
	}

	private static int scoreNormalized(String query, String candidate)
	{
		if (query.isEmpty()) return 0;
		if (candidate.equals(query)) return 0;
		String base = candidate.startsWith("the ") ? candidate.substring(4) : candidate;
		if (base.equals(query)) return 1;
		if (candidate.startsWith(query) || base.startsWith(query)) return 10 + candidate.length() - query.length();
		if (startsAnyWord(candidate, query)) return 30 + candidate.length() - query.length();
		String[] tokens = query.split(" ");
		boolean allTokens = true;
		for (String token : tokens)
		{
			if (!candidate.contains(token))
			{
				allTokens = false;
				break;
			}
		}
		if (allTokens) return 50 + candidate.length() - query.length();
		if (candidate.contains(query)) return 100 + candidate.length() - query.length();
		int distance = levenshtein(query, base);
		int typoAllowance = Math.max(1, Math.min(3, query.length() / 3));
		return distance <= typoAllowance ? 200 + distance : NO_MATCH;
	}

	private static boolean startsAnyWord(String candidate, String query)
	{
		return candidate.startsWith(query) || candidate.contains(" " + query);
	}

	private static String expandAlias(String query)
	{
		return ALIASES.getOrDefault(query, query);
	}

	static String normalize(String value)
	{
		String plain = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", " ")
			.trim();
		return plain.replaceAll("\\s+", " ");
	}

	private static int levenshtein(String left, String right)
	{
		int[] previous = new int[right.length() + 1];
		for (int j = 0; j <= right.length(); j++) previous[j] = j;
		for (int i = 1; i <= left.length(); i++)
		{
			int[] current = new int[right.length() + 1];
			current[0] = i;
			for (int j = 1; j <= right.length(); j++)
			{
				int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
				current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
			}
			previous = current;
		}
		return previous[right.length()];
	}
}
