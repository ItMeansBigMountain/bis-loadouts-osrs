package com.itmeansbigmountain.bisloadouts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OsrsWikiApiClient
{
	private static final String API = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT = "BisLoadouts/1.0 (RuneLite plugin; github.com/ItMeansBigMountain/bis-loadouts-osrs)";
	private final HttpClient httpClient;

	public OsrsWikiApiClient()
	{
		this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
	}

	OsrsWikiApiClient(HttpClient httpClient)
	{
		this.httpClient = httpClient;
	}

	public WikiPage findPage(String query) throws IOException, InterruptedException
	{
		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
		String url = API + "?action=opensearch&format=json&limit=1&namespace=0&search=" + encoded;
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(10))
			.header("User-Agent", USER_AGENT)
			.GET()
			.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 400)
		{
			throw new IOException("OSRS Wiki API returned HTTP " + response.statusCode());
		}
		JsonArray root = new JsonParser().parse(response.body()).getAsJsonArray();
		JsonArray titles = root.get(1).getAsJsonArray();
		JsonArray descriptions = root.get(2).getAsJsonArray();
		JsonArray urls = root.get(3).getAsJsonArray();
		if (titles.size() == 0)
		{
			return new WikiPage(query, pageUrl(query), "No exact wiki page match returned.");
		}
		return new WikiPage(titles.get(0).getAsString(), urls.get(0).getAsString(), descriptions.size() > 0 ? descriptions.get(0).getAsString() : "");
	}

	public static String pageUrl(String title)
	{
		return "https://oldschool.runescape.wiki/w/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
	}

	public static final class WikiPage
	{
		private final String title;
		private final String url;
		private final String description;
		WikiPage(String title, String url, String description)
		{
			this.title = title;
			this.url = url;
			this.description = description;
		}
		public String getTitle() { return title; }
		public String getUrl() { return url; }
		public String getDescription() { return description; }
	}
}
