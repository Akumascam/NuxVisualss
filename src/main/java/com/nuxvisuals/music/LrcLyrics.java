package com.nuxvisuals.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable timed lyrics; supports repeated timestamps and millisecond [offset:] metadata. */
public final class LrcLyrics {
    public record Line(double seconds, String text) { }
    private static final Pattern STAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d+)]", Pattern.CASE_INSENSITIVE);
    public static final LrcLyrics EMPTY = new LrcLyrics(List.of());
    private final List<Line> lines;
    private boolean estimated;
    public boolean estimated(){return estimated;}

    private LrcLyrics(List<Line> lines) { this.lines = List.copyOf(lines); }
    public List<Line> lines() { return lines; }

    public static LrcLyrics parse(String lrc) {
        if (lrc == null || lrc.isBlank()) return EMPTY;
        double offset = 0;
        Matcher metadata = OFFSET.matcher(lrc);
        while (metadata.find()) {
            try { offset = Long.parseLong(metadata.group(1)) / 1000.0; }
            catch (NumberFormatException ignored) { }
        }
        List<Line> parsed = new ArrayList<>();
        for (String raw : lrc.replace("\uFEFF", "").split("\\R")) {
            Matcher times = STAMP.matcher(raw);
            List<Double> stamps = new ArrayList<>();
            int end = 0;
            while (times.find()) {
                int seconds = Integer.parseInt(times.group(2));
                if (seconds >= 60) continue;
                String fraction = times.group(3);
                double sub = fraction == null ? 0 : Integer.parseInt(fraction) / Math.pow(10, fraction.length());
                stamps.add(Integer.parseInt(times.group(1)) * 60.0 + seconds + sub + offset);
                end = times.end();
            }
            if (stamps.isEmpty()) continue;
            String text = clean(raw.substring(end));
            // Empty timed lines are retained: they clear lyrics during instrumental breaks.
            for (double time : stamps) parsed.add(new Line(Math.max(0, time), text));
        }
        parsed.sort(Comparator.comparingDouble(Line::seconds));
        List<Line> unique = new ArrayList<>();
        for (Line line : parsed) {
            if (!unique.isEmpty() && unique.getLast().seconds() == line.seconds()) {
                unique.set(unique.size() - 1, line);
            } else unique.add(line);
        }
        return unique.isEmpty() ? EMPTY : new LrcLyrics(unique);
    }

    /** Plain text fallback, explicitly estimated rather than pretending to have karaoke timestamps. */
    public static LrcLyrics fromPlain(String text,double duration){
        if(text==null||text.isBlank())return EMPTY;
        List<String> cleaned=new ArrayList<>();
        for(String raw:text.split("\\R")){
            String line=clean(raw);
            if(!line.isBlank()&&!line.matches("\\[.*]"))cleaned.add(line);
            if(cleaned.size()>=1024)break;
        }
        if(cleaned.isEmpty())return EMPTY;
        double total=0;for(String line:cleaned)total+=Math.max(2,line.split("\\s+").length);
        double end=Double.isFinite(duration)&&duration>1?duration:Math.max(30,cleaned.size()*4.0);
        double cursor=Math.min(1,end*.02);List<Line> result=new ArrayList<>();
        for(String line:cleaned){result.add(new Line(cursor,line));cursor+=(end-1)*Math.max(2,line.split("\\s+").length)/total;}
        LrcLyrics lyrics=new LrcLyrics(result);lyrics.estimated=true;return lyrics;
    }

    public int indexAt(double seconds) {
        int lo = 0, hi = lines.size() - 1, result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lines.get(mid).seconds() <= seconds) { result = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        return result;
    }

    public static String clean(String text) {
        String result = text.replaceAll("[\\p{Cntrl}§]", "").strip();
        int count = result.codePointCount(0, result.length());
        return count > 180 ? result.substring(0, result.offsetByCodePoints(0, 180)) + "…" : result;
    }
}
