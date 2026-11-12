package com.nuxvisuals.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuxvisuals.NuxVisuals;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** All disk/HTTP work stays off the Minecraft tick and render threads. */
public final class LyricsService {
    public record Track(String artist, String title, int duration) {
        public Track {
            String[] identity=SongIdentity.split(artist,title);
            artist=identity[0];title=identity[1];
        }
    }
    public record Result(Track track, LrcLyrics lyrics, String status) { }
    private record Cached(LrcLyrics lyrics, long expires) { }
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NuxVisuals-Lyrics"); t.setDaemon(true); return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final Map<Track, Cached> CACHE = new LinkedHashMap<>(32, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Track, Cached> entry) { return size() > 32; }
    };
    private static volatile Result result = new Result(null, LrcLyrics.EMPTY, "idle");
    private static long generation;
    private static Track requested;
    private static boolean requestedOnline;
    private static Future<?> pending;
    private static long nextRefresh;

    public static Result current() { return result; }

    public static synchronized void request(Track track, boolean online) {
        if (track.equals(requested) && online == requestedOnline && (pending!=null&&!pending.isDone() || System.currentTimeMillis()<nextRefresh)) return;
        nextRefresh=System.currentTimeMillis()+30_000;
        requested = track;
        requestedOnline = online;
        long ticket = ++generation;
        if (pending != null) pending.cancel(true);
        if(!track.equals(result.track()))result = new Result(track, LrcLyrics.EMPTY, "loading");
        pending = IO.submit(() -> {
            LrcLyrics lyrics = LrcLyrics.EMPTY;
            String status = "not found";
            try {
                // Local lyrics always have priority. Only a sanitized filename is resolved.
                Path dir = FabricLoader.getInstance().getConfigDir().resolve("nuxvisuals/lyrics");
                Files.createDirectories(dir);
                Path local = dir.resolve(fileName(track));
                if (Files.isRegularFile(local) && Files.size(local) <= 1_048_576) {
                    lyrics = LrcLyrics.parse(Files.readString(local, StandardCharsets.UTF_8));
                    status = "local LRC";
                }
                if(lyrics.lines().isEmpty()){
                    Path plain=dir.resolve(fileName(track).replaceFirst("\\.lrc$",".txt"));
                    if(Files.isRegularFile(plain)&&Files.size(plain)<=1_048_576){lyrics=LrcLyrics.fromPlain(Files.readString(plain,StandardCharsets.UTF_8),track.duration());status="local text (estimated)";}
                }
                if (lyrics.lines().isEmpty() && online && !Thread.currentThread().isInterrupted()) {
                    Cached cached = CACHE.get(track);
                    if (cached != null && cached.expires() > System.currentTimeMillis()) {
                        lyrics = cached.lyrics();
                    } else {
                        lyrics = online(track);
                        CACHE.put(track, new Cached(lyrics, System.currentTimeMillis()
                                + (lyrics.lines().isEmpty() ? 300_000 : 86_400_000)));
                    }
                    status = lyrics.lines().isEmpty() ? "not found" : "LRCLIB";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                status = "unavailable";
                NuxVisuals.LOGGER.debug("WorldLyrics: lookup failed for {}", track.title(), e);
            }
            synchronized (LyricsService.class) {
                if (ticket == generation) result = new Result(track, lyrics, status);
            }
        });
    }

    private static LrcLyrics online(Track track) throws Exception {
        if(track.title().isEmpty())return LrcLyrics.EMPTY;
        String query="track_name="+encode(track.title())+(track.artist().isBlank()?"":"&artist_name="+encode(track.artist()));
        if(!track.artist().isBlank()){
            JsonElement exact=fetch("https://lrclib.net/api/get?"+query+(track.duration()>0?"&duration="+track.duration():""));
            if(exact!=null&&exact.isJsonObject()){
                LrcLyrics match=extract(exact.getAsJsonObject(),track.duration());
                if(!match.lines().isEmpty())return match;
            }
        }
        JsonElement search=fetch("https://lrclib.net/api/search?"+query);
        if(search==null||!search.isJsonArray())return LrcLyrics.EMPTY;
        LrcLyrics best=LrcLyrics.EMPTY;double bestScore=-1;int scanned=0;
        for(JsonElement item:search.getAsJsonArray()){
            if(++scanned>40)break;
            if(!item.isJsonObject())continue;
            JsonObject row=item.getAsJsonObject();
            if(!normalize(string(row,"trackName")).equals(normalize(track.title())))continue;
            if(!track.artist().isBlank()&&!normalize(string(row,"artistName")).equals(normalize(track.artist())))continue;
            double d=row.has("duration")&&!row.get("duration").isJsonNull()?row.get("duration").getAsDouble():0;
            if(track.duration()>0&&d>0&&Math.abs(d-track.duration())>8)continue;
            // Without an artist, require a matching known duration instead of guessing another song.
            if(track.artist().isBlank()&&(track.duration()<=0||d<=0))continue;
            LrcLyrics candidate=extract(row,track.duration());
            if(candidate.lines().isEmpty())continue;
            double score=(candidate.estimated()?0:20)+(track.duration()>0&&d>0?8-Math.abs(d-track.duration()):0);
            if(score>bestScore){best=candidate;bestScore=score;}
        }
        return best;
    }

    private static JsonElement fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(6))
                .header("User-Agent", "NuxVisuals/1.0.1 (WorldLyrics)").GET().build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() == 404) return null;
            if (response.statusCode() != 200) throw new IllegalStateException("Lyrics HTTP " + response.statusCode());
            byte[] bytes = body.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) throw new IllegalStateException("Lyrics response too large");
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private static LrcLyrics extract(JsonObject row,int duration) {
        LrcLyrics timed=LrcLyrics.parse(string(row,"syncedLyrics"));
        if(!timed.lines().isEmpty())return timed;
        return LrcLyrics.fromPlain(string(row,"plainLyrics"),duration);
    }
    private static String string(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }
    private static String normalize(String text) { return SongIdentity.normalize(text); }
    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
    public static String fileName(Track track) {
        String name = (track.artist() + " - " + track.title()).replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_").strip();
        if (name.length() > 160) name = name.substring(0, 160);
        // Never leave a trailing dot/space on Windows.
        return name.replaceAll("[. ]+$", "") + ".lrc";
    }
    public static synchronized void reset() {
        ++generation;
        requested = null;
        if (pending != null) pending.cancel(true);
        pending = null;
        result = new Result(null, LrcLyrics.EMPTY, "idle");
    }
    public static synchronized void close() { reset(); IO.shutdownNow(); }
    private LyricsService() { }
}
