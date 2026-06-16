package org.exmple.newcustommusicclientsideplayer.client.update;

import com.google.gson.Gson;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CModrinthUpdateClient {
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;

    public CModrinthUpdateClient() {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CUpdatePolicy.REQUEST_TIMEOUT_SECONDS))
            .build());
    }

    CModrinthUpdateClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Optional<List<CModrinthVersion>> fetchProjectVersions(CUpdateEnvironment environment) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(projectVersionsUri(environment))
                .timeout(Duration.ofSeconds(CUpdatePolicy.REQUEST_TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", environment.userAgent())
                .GET()
                .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonArray()) {
                return Optional.empty();
            }

            return Optional.of(parseVersions(root.getAsJsonArray(), environment.projectUrl()));
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private static URI projectVersionsUri(CUpdateEnvironment environment) {
        String loaders = encodeJsonArray(environment.loader());
        String gameVersions = encodeJsonArray(environment.minecraftVersion());
        return URI.create(API_BASE_URL
            + "/project/"
            + environment.projectSlug()
            + "/version?loaders="
            + loaders
            + "&game_versions="
            + gameVersions
            + "&include_changelog=true");
    }

    private static String encodeJsonArray(String value) {
        String json = GSON.toJson(List.of(value));
        return URLEncoder.encode(json, StandardCharsets.UTF_8);
    }

    private static List<CModrinthVersion> parseVersions(JsonArray versions, String projectUrl) {
        List<CModrinthVersion> parsed = new ArrayList<>();
        for (JsonElement versionElement : versions) {
            if (!versionElement.isJsonObject()) {
                continue;
            }

            JsonObject version = versionElement.getAsJsonObject();
            String id = getString(version, "id");
            parsed.add(new CModrinthVersion(
                id,
                getString(version, "version_number"),
                getString(version, "version_type"),
                getString(version, "status"),
                getStringList(version, "loaders"),
                getStringList(version, "game_versions"),
                getString(version, "date_published"),
                getString(version, "changelog"),
                hasFiles(version),
                versionPageUrl(projectUrl, id)
            ));
        }

        return List.copyOf(parsed);
    }

    private static boolean hasFiles(JsonObject version) {
        JsonElement files = version.get("files");
        return files != null && files.isJsonArray() && !files.getAsJsonArray().isEmpty();
    }

    private static String versionPageUrl(String projectUrl, String versionId) {
        if (versionId == null || versionId.isBlank()) {
            return projectUrl;
        }

        return projectUrl + "/version/" + versionId;
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return "";
        }

        try {
            return element.getAsString();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static List<String> getStringList(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item == null || item.isJsonNull() || !item.isJsonPrimitive()) {
                continue;
            }

            try {
                values.add(item.getAsString());
            } catch (RuntimeException ignored) {
            }
        }

        return List.copyOf(values);
    }
}
