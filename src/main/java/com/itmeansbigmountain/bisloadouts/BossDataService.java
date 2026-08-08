package com.itmeansbigmountain.bisloadouts;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BossDataService
{
	private static final String USER_AGENT = "BisLoadouts/1.0 (RuneLite plugin; github.com/ItMeansBigMountain/bis-loadouts-osrs)";
	private static final String GEARSCAPE_MONSTERS = "https://api.gearscape.net/api/monster";
	private static final String GEARSCAPE_MONSTER_ID = "https://api.gearscape.net/api/monster/id/";
	private static final String GEARSCAPE_EQUIPMENT = "https://api.gearscape.net/api/equipment/all";
	private static final String GEARSCAPE_WEAPONS = "https://api.gearscape.net/api/weapon/all";
	private static final String WIKI_PRICE_MAPPING = "https://prices.runescape.wiki/api/v1/osrs/mapping";
	private static final String WIKI_BOSSES_CATEGORY = "https://oldschool.runescape.wiki/api.php?action=query&format=json&list=categorymembers&cmtitle=Category:Bosses&cmnamespace=0&cmlimit=500";
	private static final List<String> FALLBACK_BOSSES = Arrays.asList(
		"None - best overall for my stats", "General PvM", "Scurrius", "Giant Mole", "Barrows", "Fight Caves", "Fight Kiln", "Inferno",
		"Vorkath", "Zulrah", "Phantom Muspah", "The Gauntlet", "Corrupted Gauntlet", "Tombs of Amascut", "Chambers of Xeric",
		"Theatre of Blood", "Nex", "Nightmare", "Phosani's Nightmare", "Duke Sucellus", "The Leviathan", "Vardorvis", "The Whisperer",
		"Abyssal Sire", "Alchemical Hydra", "Araxxor", "Amoxliatl", "The Hueycoatl", "Royal Titans", "Branda the Fire Queen",
		"Eldric the Ice King", "Yama", "Doom of Mokhaiotl", "Gemstone Crab", "Brutus", "Demonic Brutus", "Maggot King", "Mad Angel",
		"Artio", "Callisto", "Calvar'ion", "Vet'ion", "Venenatis", "Spindel",
		"Cerberus", "Commander Zilyana", "General Graardor", "K'ril Tsutsaroth", "Kree'arra", "Dagannoth Prime", "Dagannoth Rex",
		"Dagannoth Supreme", "Kalphite Queen", "King Black Dragon", "Kraken", "Thermonuclear smoke devil", "Corporeal Beast",
		"Sarachnis", "Skotizo", "Tempoross", "Wintertodt", "Zalcano", "Hespori", "Obor", "Bryophyta", "Grotesque Guardians"
	);
	private static final Map<String, BossTarget> LOCAL_BOSS_TARGETS = buildLocalBossTargets();

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
		status = gearStatus + "Loaded " + bosses.size() + " boss autocomplete entries (" + gearscapeBosses + " GearScape + " + wikiOnlyBosses + " Wiki-only) and " + items.size() + " Wiki-validated live equipment records.";
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
			BossTarget localTarget = LOCAL_BOSS_TARGETS.get(normalize(entry.getName()));
			if (localTarget != null)
			{
				return localTarget;
			}
			String wikiUrl = OsrsWikiApiClient.pageUrl(entry.getName());
			try
			{
				wikiUrl = wikiClient.findPage(entry.getName()).getUrl();
			}
			catch (IOException | InterruptedException | RuntimeException ignored)
			{
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
			loadoutTarget(intValue(monster, "level_attack", combat / 3)),
			loadoutTarget(intValue(monster, "level_strength", combat / 3)),
			loadoutTarget(intValue(monster, "level_defence", combat / 3)),
			loadoutTarget(intValue(monster, "level_ranged", combat / 3)),
			loadoutTarget(intValue(monster, "level_magic", combat / 3)),
			intValue(monster, "level_magic", combat / 3),
			intValue(monster, "level_hp", 100),
			intValue(monster, "def_stab", 0),
			intValue(monster, "def_slash", 0),
			intValue(monster, "def_crush", 0),
			intValue(monster, "def_magic", 0),
			intValue(monster, "def_ranged", 0),
			ElementalType.fromApi(stringValue(monster, "weakness_type", "")),
			intValue(monster, "weakness", 0),
			entry.getAttributes(),
			wikiUrl,
			"OSRS Wiki + GearScape monster detail API"
		);
	}

	private static int loadoutTarget(int monsterLevel)
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

	private static Map<String, BossTarget> buildLocalBossTargets()
	{
		Map<String, BossTarget> targets = new HashMap<>();
		addLocalBoss(targets, "Amoxliatl", 263, 520, 0, 0, 80, 170, 0, 100, 100, 40, 100, 200,
			ElementalType.FIRE, 30, Arrays.asList("boss", "spectral"), "25 September 2024");
		addLocalBoss(targets, "The Hueycoatl", 642, 2500, 150, 50, 125, 50, 50, 100, 100, 0, 200, 350,
			ElementalType.EARTH, 60, Arrays.asList("boss", "draconic"), "25 September 2024");
		addLocalBoss(targets, "Royal Titans", 350, 600, 300, 250, 100, 100, 150, 12, 12, 0, 700, 700,
			ElementalType.NONE, 0, Arrays.asList("boss", "giant", "Branda: 50% Water; Eldric: 50% Fire"), "5 February 2025");
		addLocalBoss(targets, "Branda the Fire Queen", 350, 600, 300, 250, 100, 100, 150, 12, 12, 0, 700, 700,
			ElementalType.WATER, 50, Arrays.asList("boss", "giant", "fiery"), "5 February 2025");
		addLocalBoss(targets, "Eldric the Ice King", 350, 600, 300, 250, 100, 100, 150, 12, 12, 0, 700, 700,
			ElementalType.FIRE, 50, Arrays.asList("boss", "giant", "icy"), "5 February 2025");
		addLocalBoss(targets, "Yama", 1238, 2500, 320, 350, 225, 250, 210, 100, 80, 333, 60, 220,
			ElementalType.WATER, 50, Arrays.asList("boss", "demon", "120% demonbane vulnerability"), "14 May 2025");
		addLocalBoss(targets, "Doom of Mokhaiotl", 558, 525, 300, 190, 90, 275, 110, 300, 300, 60, 160, 160,
			ElementalType.NONE, 0, Arrays.asList("boss", "demon", "demonic shield requires demonbane at delve 3+"), "23 July 2025");
		addLocalBoss(targets, "Gemstone Crab", 160, 300, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,
			ElementalType.NONE, 0, Arrays.asList("boss", "training", "effectively infinite HP; 300 effective HP for ruby bolts"), "23 July 2025");
		addLocalBoss(targets, "Brutus", 30, 58, 12, 25, 10, 8, 1, -7, -7, -7, -3, -7,
			ElementalType.EARTH, 25, Arrays.asList("boss", "free-to-play"), "25 February 2026");
		addLocalBoss(targets, "Demonic Brutus", 1224, 750, 380, 418, 200, 272, 1, 182, 65, 216, 520, 418,
			ElementalType.EARTH, 25, Arrays.asList("boss", "hard mode"), "25 February 2026");
		addLocalBoss(targets, "Maggot King", 741, 1500, 200, 250, 200, 200, 300, 172, 100, 45, 150, 158,
			ElementalType.FIRE, 80, Arrays.asList("boss"), "30 June 2026");
		addLocalBoss(targets, "Mad Angel", 588, 755, 150, 230, 175, 150, 250, 60, 80, 40, 185, 185,
			ElementalType.EARTH, 15, Arrays.asList("boss", "golem"), "29 July 2026");
		return Collections.unmodifiableMap(targets);
	}

	private static void addLocalBoss(Map<String, BossTarget> targets, String name, int combat, int hitpoints,
		int attack, int strength, int defence, int magic, int ranged, int defStab, int defSlash, int defCrush,
		int defMagic, int defRanged, ElementalType elementalWeakness, int elementalWeaknessPercent,
		List<String> attributes, String releaseDate)
	{
		targets.put(normalize(name), new BossTarget(name, -1, combat, loadoutTarget(attack), loadoutTarget(strength),
			loadoutTarget(defence), loadoutTarget(ranged), loadoutTarget(magic), magic, hitpoints, defStab, defSlash,
			defCrush, defMagic, defRanged, elementalWeakness, elementalWeaknessPercent, attributes,
			OsrsWikiApiClient.pageUrl(name),
			"OSRS Wiki local profile; released " + releaseDate + "; used when GearScape detail is unavailable"));
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
		WikiItemMapping wikiItems = fetchWikiItemMapping();
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
				toGearItem(slot, item.getAsJsonObject(), wikiItems).ifPresent(items::add);
			}
		}
		JsonArray weapons = getJson(GEARSCAPE_WEAPONS).getAsJsonArray("weapons");
		for (JsonElement item : weapons)
		{
			toGearItem(GearSlot.WEAPON, item.getAsJsonObject(), wikiItems).ifPresent(items::add);
		}
		return items;
	}

	private WikiItemMapping fetchWikiItemMapping() throws IOException, InterruptedException
	{
		Set<Integer> ids = new java.util.HashSet<>();
		Set<String> names = new java.util.HashSet<>();
		JsonArray mapping = getJsonArray(WIKI_PRICE_MAPPING);
		for (JsonElement element : mapping)
		{
			JsonObject obj = element.getAsJsonObject();
			int id = intValue(obj, "id", -1);
			String name = normalize(stringValue(obj, "name", ""));
			if (id > 0)
			{
				ids.add(id);
			}
			if (!name.isEmpty())
			{
				names.add(name);
			}
		}
		return new WikiItemMapping(ids, names);
	}

	private Optional<GearItem> toGearItem(GearSlot slot, JsonObject obj, WikiItemMapping wikiItems)
	{
		String name = stringValue(obj, "name", "");
		if (name.isEmpty() || name.toLowerCase(Locale.ROOT).contains("null") || GearRecommendationEngine.isExcludedGameModeItem(name))
		{
			return Optional.empty();
		}
		int itemId = itemIdFor(obj);
		if (!wikiItems.accepts(itemId, name))
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
		String wikiUrl = OsrsWikiApiClient.pageUrl(name);
		String sourceNote = "Live GearScape stats; verified against OSRS Wiki price mapping; OSRS Wiki item page: " + wikiUrl;
		return Optional.of(new GearItem(slot, itemId, name, styles,
			intValue(obj, "attack_req", 1), intValue(obj, "strength_req", 1), intValue(obj, "defence_req", 1),
			intValue(obj, "magic_req", 1), intValue(obj, "ranged_req", 1), intValue(obj, "prayer_req", 1),
			attackBonus, strengthBonus, price, sourceNote, stringValue(obj, "icon", null), wikiUrl,
			booleanValue(obj, "two_handed", GearItem.isKnownTwoHanded(slot, name)), ammoIdsFor(obj)));
	}

	private static Set<CombatStyle> stylesFor(JsonObject obj)
	{
		Set<CombatStyle> styles = EnumSet.noneOf(CombatStyle.class);
		String style = stringValue(obj, "combat_style", "").toLowerCase(Locale.ROOT);
		if (style.contains("magic"))
		{
			styles.add(CombatStyle.MAGIC);
		}
		if (style.contains("ranged"))
		{
			styles.add(CombatStyle.RANGED);
		}
		if (style.contains("melee") || style.contains("stab") || style.contains("slash") || style.contains("crush"))
		{
			styles.add(CombatStyle.STAB);
			styles.add(CombatStyle.SLASH);
			styles.add(CombatStyle.CRUSH);
		}

		if (intValue(obj, "magic_bonus", 0) + intValue(obj, "magic_str", 0) > 0)
		{
			styles.add(CombatStyle.MAGIC);
		}
		if (intValue(obj, "ranged_bonus", 0) + intValue(obj, "ranged_str", 0) > 0)
		{
			styles.add(CombatStyle.RANGED);
		}
		if (Math.max(Math.max(intValue(obj, "stab_bonus", 0), intValue(obj, "slash_bonus", 0)), intValue(obj, "crush_bonus", 0)) + intValue(obj, "melee_str", 0) > 0)
		{
			styles.add(CombatStyle.STAB);
			styles.add(CombatStyle.SLASH);
			styles.add(CombatStyle.CRUSH);
		}
		return styles;
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

	private static Set<Integer> ammoIdsFor(JsonObject obj)
	{
		JsonElement value = obj.get("ammunition");
		if (value == null || value.isJsonNull() || !value.isJsonArray())
		{
			return Collections.emptySet();
		}
		Set<Integer> ids = new HashSet<>();
		for (JsonElement element : value.getAsJsonArray())
		{
			if (!element.isJsonNull())
			{
				ids.add(element.getAsInt());
			}
		}
		return ids;
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
		return new JsonParser().parse(response.body()).getAsJsonObject();
	}

	private JsonArray getJsonArray(String url) throws IOException, InterruptedException
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
		return new JsonParser().parse(response.body()).getAsJsonArray();
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
		itemId = intValue(obj, "id", -1);
		if (itemId > 0)
		{
			return itemId;
		}
		return GearRecommendationEngine.fallbackItemId(stringValue(obj, "name", ""));
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
	private static final class WikiItemMapping
	{
		private final Set<Integer> ids;
		private final Set<String> names;

		private WikiItemMapping(Set<Integer> ids, Set<String> names)
		{
			this.ids = ids;
			this.names = names;
		}

		private boolean accepts(int itemId, String name)
		{
			return (itemId > 0 && ids.contains(itemId)) || names.contains(normalize(name));
		}
	}

}
