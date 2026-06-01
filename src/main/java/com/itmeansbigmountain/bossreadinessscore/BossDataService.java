package com.itmeansbigmountain.bossreadinessscore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BossDataService
{
	private static final String USER_AGENT = "BossReadinessScore/1.0 (RuneLite plugin; github.com/ItMeansBigMountain/boss-readiness-score-osrs)";
	private static final String GEARSCAPE_MONSTERS = "https://api.gearscape.net/api/monster";
	private static final String GEARSCAPE_MONSTER_ID = "https://api.gearscape.net/api/monster/id/";
	private static final String GEARSCAPE_EQUIPMENT = "https://api.gearscape.net/api/equipment/all";
	private static final String GEARSCAPE_WEAPONS = "https://api.gearscape.net/api/weapon/all";
	private static final String WIKI_BOSSES_CATEGORY = "https://oldschool.runescape.wiki/api.php?action=query&format=json&list=categorymembers&cmtitle=Category:Bosses&cmnamespace=0&cmlimit=500";
	private static final List<String> FALLBACK_BOSSES = Arrays.asList(
		"None - best overall for my stats", "General PvM", "Scurrius", "Giant Mole", "Barrows", "Fight Caves", "Fight Kiln", "Inferno",
		"Vorkath", "Zulrah", "Phantom Muspah", "The Gauntlet", "Corrupted Gauntlet", "Tombs of Amascut", "Chambers of Xeric",
		"Theatre of Blood", "Nex", "Nightmare", "Phosani's Nightmare", "Duke Sucellus", "The Leviathan", "Vardorvis", "The Whisperer",
		"Abyssal Sire", "Alchemical Hydra", "Araxxor", "Artio", "Callisto", "Calvar'ion", "Vet'ion", "Venenatis", "Spindel",
		"Cerberus", "Commander Zilyana", "General Graardor", "K'ril Tsutsaroth", "Kree'arra", "Dagannoth Prime", "Dagannoth Rex",
		"Dagannoth Supreme", "Kalphite Queen", "King Black Dragon", "Kraken", "Thermonuclear smoke devil", "Corporeal Beast",
		"Sarachnis", "Skotizo", "Tempoross", "Wintertodt", "Zalcano", "Hespori", "Obor", "Bryophyta", "Grotesque Guardians"
	);

	private final HttpClient httpClient;
	private final OsrsWikiApiClient wikiClient;
	private final AtomicReference<List<BossIndexEntry>> bossIndex = new AtomicReference<>(fallbackBossEntries());
	private final AtomicReference<List<GearItem>> gearItems = new AtomicReference<>(Collections.emptyList());
	private volatile Instant loadedAt;
	private volatile String status = "Wiki/GearScape data has not loaded yet.";

	public BossDataService()
	{
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), new OsrsWikiApiClient());
	}

	BossDataService(HttpClient httpClient, OsrsWikiApiClient wikiClient)
	{
		this.httpClient = httpClient;
		this.wikiClient = wikiClient;
	}

	public void refresh() throws IOException, InterruptedException
	{
		List<BossIndexEntry> bosses = fallbackBossEntries();
		List<GearItem> items = Collections.emptyList();
		String gearStatus;
		try
		{
			bosses = fetchBossIndex();
		}
		catch (IOException | RuntimeException ex)
		{
			gearStatus = "Boss API unavailable; using local fallback boss list. ";
			status = gearStatus + ex.getMessage();
		}
		try
		{
			items = fetchGearItems();
			gearStatus = "";
		}
		catch (IOException | RuntimeException ex)
		{
			gearStatus = "Live equipment API unavailable; using checked-in gear fallback. ";
		}
		bossIndex.set(bosses);
		gearItems.set(items);
		loadedAt = Instant.now();
		long gearscapeBosses = bosses.stream().filter(BossIndexEntry::hasGearscapeId).count();
		long wikiOnlyBosses = bosses.size() - gearscapeBosses;
		status = gearStatus + "Loaded " + bosses.size() + " boss autocomplete entries (" + gearscapeBosses + " GearScape + " + wikiOnlyBosses + " Wiki-only) and " + items.size() + " live equipment records.";
	}

	public List<GearItem> getGearItems()
	{
		return gearItems.get();
	}

	public String getStatus()
	{
		return status;
	}

	public List<String> getBossNameSuggestions(int limit)
	{
		return bossIndex.get().stream().map(BossIndexEntry::getName).distinct().limit(limit).collect(Collectors.toList());
	}

	public List<String> searchBossNameSuggestions(String query, int limit)
	{
		String normalized = normalize(query);
		return bossIndex.get().stream()
			.sorted(Comparator.comparingInt(entry -> matchScore(normalized, normalize(entry.getName()))))
			.map(BossIndexEntry::getName)
			.distinct()
			.limit(limit)
			.collect(Collectors.toList());
	}

	public BossTarget resolveBoss(String requestedBossName, BossProfile fallbackProfile)
	{
		String requested = requestedBossName == null ? "" : requestedBossName.trim();
		if (requested.isEmpty() || requested.equalsIgnoreCase("none") || requested.toLowerCase(Locale.ROOT).startsWith("none -"))
		{
			BossTarget general = BossTarget.fromProfile(BossProfile.GENERAL_PVM);
			return new BossTarget("Best overall", -1, general.getTargetCombat(), general.getTargetAttack(), general.getTargetStrength(),
				general.getTargetDefence(), general.getTargetRanged(), general.getTargetMagic(), general.getHitpoints(), 0, 0, 0, 0, 0,
				Collections.emptyList(), "", "No boss selected; ranking strongest legal gear for your stats");
		}
		Optional<BossIndexEntry> match = findBestBossMatch(requested);
		if (!match.isPresent())
		{
			BossTarget fallback = BossTarget.fromProfile(fallbackProfile);
			return new BossTarget(requested, -1, fallback.getTargetCombat(), fallback.getTargetAttack(), fallback.getTargetStrength(),
				fallback.getTargetDefence(), fallback.getTargetRanged(), fallback.getTargetMagic(), fallback.getHitpoints(), 0, 0, 0, 0, 0,
				Collections.emptyList(), OsrsWikiApiClient.pageUrl(requested), "OSRS Wiki fallback; GearScape boss index not loaded/matched");
		}
		BossIndexEntry entry = match.get();
		if (!entry.hasGearscapeId())
		{
			String wikiUrl = OsrsWikiApiClient.pageUrl(entry.getName());
			try
			{
				wikiUrl = wikiClient.findPage(entry.getName()).getUrl();
			}
			catch (IOException | InterruptedException | RuntimeException ignored)
			{
				if (ignored instanceof InterruptedException)
				{
					Thread.currentThread().interrupt();
				}
			}
			return new BossTarget(entry.getName(), -1, entry.getCombatLevel(), 60, 60, 60, 60, 60, 100,
				0, 0, 0, 0, 0, entry.getAttributes(), wikiUrl, "OSRS Wiki Category:Bosses autocomplete entry; GearScape stats unavailable");
		}
		try
		{
			return fetchBossDetails(entry);
		}
		catch (IOException | InterruptedException ex)
		{
			if (ex instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			return new BossTarget(entry.getName(), entry.getId(), entry.getCombatLevel(), 60, 60, 60, 60, 60, 100,
				0, 0, 0, 0, 0, entry.getAttributes(), OsrsWikiApiClient.pageUrl(cleanWikiTitle(entry.getName())),
				"GearScape boss index fallback; detail fetch failed: " + ex.getMessage());
		}
	}

	private Optional<BossIndexEntry> findBestBossMatch(String query)
	{
		String normalized = normalize(query);
		return bossIndex.get().stream()
			.min(Comparator.comparingInt(entry -> weightedMatchScore(normalized, entry)));
	}

	private static int weightedMatchScore(String query, BossIndexEntry entry)
	{
		int baseScore = matchScore(query, normalize(entry.getName()));
		return entry.hasGearscapeId() ? baseScore : baseScore + 50;
	}

	private static int matchScore(String query, String candidate)
	{
		if (candidate.equals(query))
		{
			return 0;
		}
		String candidateBase = candidate.replaceAll(" \\(.*?\\)", "").trim();
		if (candidateBase.equals(query))
		{
			return 1;
		}
		if (candidate.contains(query))
		{
			return 10 + candidate.length() - query.length();
		}
		if (query.contains(candidateBase))
		{
			return 20 + query.length() - candidateBase.length();
		}
		return 1000 + levenshtein(query, candidateBase);
	}

	private BossTarget fetchBossDetails(BossIndexEntry entry) throws IOException, InterruptedException
	{
		JsonObject monster = getJson(GEARSCAPE_MONSTER_ID + entry.getId()).getAsJsonObject("monster");
		String wikiTitle = cleanWikiTitle(stringValue(monster, "name", entry.getName()));
		String wikiUrl = OsrsWikiApiClient.pageUrl(wikiTitle);
		try
		{
			wikiUrl = wikiClient.findPage(wikiTitle).getUrl();
		}
		catch (IOException | RuntimeException ignored)
		{
			// Keep generated wiki URL if live wiki search fails.
		}
		int combat = intValue(monster, "level_cb", entry.getCombatLevel());
		return new BossTarget(
			stringValue(monster, "name", entry.getName()),
			entry.getId(),
			combat,
			readinessTarget(intValue(monster, "level_attack", combat / 3)),
			readinessTarget(intValue(monster, "level_strength", combat / 3)),
			readinessTarget(intValue(monster, "level_defence", combat / 3)),
			readinessTarget(intValue(monster, "level_ranged", combat / 3)),
			readinessTarget(intValue(monster, "level_magic", combat / 3)),
			intValue(monster, "level_hp", 100),
			intValue(monster, "def_stab", 0),
			intValue(monster, "def_slash", 0),
			intValue(monster, "def_crush", 0),
			intValue(monster, "def_magic", 0),
			intValue(monster, "def_ranged", 0),
			entry.getAttributes(),
			wikiUrl,
			"OSRS Wiki + GearScape monster detail API"
		);
	}

	private static int readinessTarget(int monsterLevel)
	{
		return Math.max(35, Math.min(99, (int) Math.round(monsterLevel / 4.0D + 35.0D)));
	}

	private List<BossIndexEntry> fetchBossIndex() throws IOException, InterruptedException
	{
		Map<String, BossIndexEntry> merged = fallbackBossEntries().stream()
			.collect(Collectors.toMap(entry -> normalize(entry.getName()), entry -> entry, (left, right) -> left, HashMap::new));
		JsonArray monsters = getJson(GEARSCAPE_MONSTERS).getAsJsonArray("monsters");
		for (JsonElement element : monsters)
		{
			JsonObject obj = element.getAsJsonObject();
			if (!booleanValue(obj, "boss", false))
			{
				continue;
			}
			BossIndexEntry entry = new BossIndexEntry(intValue(obj, "id", -1), stringValue(obj, "name", "Unknown boss"),
				intValue(obj, "level_cb", 1), stringList(obj, "attributes"));
			merged.put(normalize(entry.getName()), entry);
		}

		for (String wikiBossName : fetchWikiBossNames())
		{
			String normalized = normalize(wikiBossName);
			merged.putIfAbsent(normalized, new BossIndexEntry(-1, wikiBossName, 85, Arrays.asList("boss", "wiki")));
		}

		return merged.values().stream().sorted(Comparator.comparing(BossIndexEntry::getName)).collect(Collectors.toList());
	}

	private static List<BossIndexEntry> fallbackBossEntries()
	{
		List<BossIndexEntry> entries = new ArrayList<>();
		for (String name : FALLBACK_BOSSES)
		{
			entries.add(new BossIndexEntry(-1, name, "None - best overall for my stats".equals(name) ? 1 : 85, Arrays.asList("fallback", "boss")));
		}
		return entries;
	}

	private List<String> fetchWikiBossNames() throws IOException, InterruptedException
	{
		List<String> names = new ArrayList<>();
		String next = WIKI_BOSSES_CATEGORY;
		while (next != null)
		{
			JsonObject root = getJson(next);
			JsonObject query = root.getAsJsonObject("query");
			if (query != null && query.has("categorymembers"))
			{
				for (JsonElement element : query.getAsJsonArray("categorymembers"))
				{
					String title = stringValue(element.getAsJsonObject(), "title", "");
					if (!title.isEmpty() && !"Boss".equalsIgnoreCase(title))
					{
						names.add(title);
					}
				}
			}
			JsonObject cont = root.getAsJsonObject("continue");
			if (cont != null && cont.has("cmcontinue"))
			{
				next = WIKI_BOSSES_CATEGORY + "&cmcontinue=" + URLEncoder.encode(cont.get("cmcontinue").getAsString(), java.nio.charset.StandardCharsets.UTF_8);
			}
			else
			{
				next = null;
			}
		}
		return names;
	}

	private List<GearItem> fetchGearItems() throws IOException, InterruptedException
	{
		List<GearItem> items = new ArrayList<>();
		JsonObject equipmentRoot = getJson(GEARSCAPE_EQUIPMENT).getAsJsonObject("equipment");
		for (Map.Entry<String, JsonElement> entry : equipmentRoot.entrySet())
		{
			GearSlot slot = gearSlot(entry.getKey());
			if (slot == null || !entry.getValue().isJsonArray())
			{
				continue;
			}
			for (JsonElement item : entry.getValue().getAsJsonArray())
			{
				toGearItem(slot, item.getAsJsonObject()).ifPresent(items::add);
			}
		}
		JsonArray weapons = getJson(GEARSCAPE_WEAPONS).getAsJsonArray("weapons");
		for (JsonElement item : weapons)
		{
			toGearItem(GearSlot.WEAPON, item.getAsJsonObject()).ifPresent(items::add);
		}
		return items;
	}

	private Optional<GearItem> toGearItem(GearSlot slot, JsonObject obj)
	{
		String name = stringValue(obj, "name", "");
		if (name.isEmpty() || name.toLowerCase(Locale.ROOT).contains("null"))
		{
			return Optional.empty();
		}
		Set<CombatStyle> styles = stylesFor(obj);
		if (styles.isEmpty())
		{
			return Optional.empty();
		}
		int attackBonus = attackBonusFor(styles, obj);
		int strengthBonus = strengthBonusFor(styles, obj);
		if (attackBonus <= 0 && strengthBonus <= 0 && slot != GearSlot.SHIELD)
		{
			return Optional.empty();
		}
		long price = Math.max(0L, longValue(obj, "price", 0L));
		String sourceNote = "Live GearScape stats; wiki: " + OsrsWikiApiClient.pageUrl(name);
		return Optional.of(new GearItem(slot, itemIdFor(obj), name, styles,
			intValue(obj, "attack_req", 1), intValue(obj, "strength_req", 1), intValue(obj, "defence_req", 1),
			intValue(obj, "magic_req", 1), intValue(obj, "ranged_req", 1), intValue(obj, "prayer_req", 1),
			attackBonus, strengthBonus, price, sourceNote));
	}

	private static Set<CombatStyle> stylesFor(JsonObject obj)
	{
		String style = stringValue(obj, "combat_style", "").toLowerCase(Locale.ROOT);
		if (style.contains("magic"))
		{
			return EnumSet.of(CombatStyle.MAGIC);
		}
		if (style.contains("ranged"))
		{
			return EnumSet.of(CombatStyle.RANGED);
		}
		if (style.contains("melee"))
		{
			return EnumSet.of(CombatStyle.STAB, CombatStyle.SLASH, CombatStyle.CRUSH);
		}
		int magic = intValue(obj, "magic_bonus", 0) + intValue(obj, "magic_str", 0);
		int ranged = intValue(obj, "ranged_bonus", 0) + intValue(obj, "ranged_str", 0);
		int melee = Math.max(Math.max(intValue(obj, "stab_bonus", 0), intValue(obj, "slash_bonus", 0)), intValue(obj, "crush_bonus", 0)) + intValue(obj, "melee_str", 0);
		int max = Math.max(melee, Math.max(magic, ranged));
		if (max == magic && max > 0) return EnumSet.of(CombatStyle.MAGIC);
		if (max == ranged && max > 0) return EnumSet.of(CombatStyle.RANGED);
		if (max > 0) return EnumSet.of(CombatStyle.STAB, CombatStyle.SLASH, CombatStyle.CRUSH);
		return EnumSet.noneOf(CombatStyle.class);
	}

	private static int attackBonusFor(Set<CombatStyle> styles, JsonObject obj)
	{
		if (styles.contains(CombatStyle.MAGIC) && styles.size() == 1) return intValue(obj, "magic_bonus", 0);
		if (styles.contains(CombatStyle.RANGED) && styles.size() == 1) return intValue(obj, "ranged_bonus", 0);
		return Math.max(Math.max(intValue(obj, "stab_bonus", 0), intValue(obj, "slash_bonus", 0)), intValue(obj, "crush_bonus", 0));
	}

	private static int strengthBonusFor(Set<CombatStyle> styles, JsonObject obj)
	{
		if (styles.contains(CombatStyle.MAGIC) && styles.size() == 1) return intValue(obj, "magic_str", 0);
		if (styles.contains(CombatStyle.RANGED) && styles.size() == 1) return Math.max(intValue(obj, "ranged_str", 0), intValue(obj, "ranged_bonus", 0) / 2);
		return intValue(obj, "melee_str", 0);
	}

	private JsonObject getJson(String url) throws IOException, InterruptedException
	{
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(20))
			.header("User-Agent", USER_AGENT)
			.GET()
			.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 400)
		{
			throw new IOException(url + " returned HTTP " + response.statusCode());
		}
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	private static GearSlot gearSlot(String key)
	{
		switch (key.toLowerCase(Locale.ROOT))
		{
			case "head": return GearSlot.HEAD;
			case "cape": return GearSlot.CAPE;
			case "neck": return GearSlot.NECK;
			case "weapon": return GearSlot.WEAPON;
			case "shield": return GearSlot.SHIELD;
			case "body": return GearSlot.BODY;
			case "legs": return GearSlot.LEGS;
			case "hands": return GearSlot.HANDS;
			case "feet": return GearSlot.FEET;
			case "ring": return GearSlot.RING;
			case "ammunition": return GearSlot.AMMUNITION;
			default: return null;
		}
	}

	private static String cleanWikiTitle(String name)
	{
		return name.replaceAll(" \\(.*?\\)", "").trim();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	private static int levenshtein(String a, String b)
	{
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j < costs.length; j++) costs[j] = j;
		for (int i = 1; i <= a.length(); i++)
		{
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++)
			{
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];
	}

	private static int intValue(JsonObject obj, String key, int fallback)
	{
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? fallback : value.getAsInt();
	}

	private static int itemIdFor(JsonObject obj)
	{
		int itemId = intValue(obj, "item_id", -1);
		if (itemId > 0)
		{
			return itemId;
		}
		itemId = intValue(obj, "osrs_id", -1);
		if (itemId > 0)
		{
			return itemId;
		}
		return intValue(obj, "id", -1);
	}

	private static long longValue(JsonObject obj, String key, long fallback)
	{
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? fallback : value.getAsLong();
	}

	private static boolean booleanValue(JsonObject obj, String key, boolean fallback)
	{
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
	}

	private static String stringValue(JsonObject obj, String key, String fallback)
	{
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? fallback : value.getAsString();
	}

	private static List<String> stringList(JsonObject obj, String key)
	{
		JsonElement value = obj.get(key);
		if (value == null || !value.isJsonArray()) return Collections.emptyList();
		List<String> out = new ArrayList<>();
		for (JsonElement element : value.getAsJsonArray()) out.add(element.getAsString());
		return out;
	}

	private static final class BossIndexEntry
	{
		private final int id;
		private final String name;
		private final int combatLevel;
		private final List<String> attributes;
		BossIndexEntry(int id, String name, int combatLevel, List<String> attributes)
		{
			this.id = id;
			this.name = name;
			this.combatLevel = combatLevel;
			this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
		}
		int getId() { return id; }
		boolean hasGearscapeId() { return id >= 0; }
		String getName() { return name; }
		int getCombatLevel() { return combatLevel; }
		List<String> getAttributes() { return attributes; }
	}
}
