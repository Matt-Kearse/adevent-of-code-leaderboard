import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

public class MakeLeaderTable {

    public static void main(String[] args) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String source = "https://adventofcode.com/2024/leaderboard/private/view/1671463";
        String sourceJson = source+".json";
        String text = getURLContent(sourceJson);
        JsonNode rootNode = objectMapper.readTree(text);


        File file = new File("leaderBoard.html").getCanonicalFile();
        PrintStream out = new PrintStream(new FileOutputStream(file));

        long startTime = Long.parseLong(rootNode.get("day1_ts").asText());
        JsonNode members = rootNode.get("members");
        out.println("<html>");
        out.println("<style>");
        out.println(
                "body {\n" +
                "  background: #0f0f23; /*337 x 5*/\n" +
                "  color: #cccccc;\n" +
                "  font-family: \"Source Code Pro\", monospace;\n" +
                "  font-weight: 200;\n" +
                "  font-size: 14pt;\n" +
                "  min-width: 60em;\n" +
                "}");
        out.println(
                "pre, code, td { font-family: \"Source Code Pro\", monospace; }\n" +
                "a {\n" +
                "  text-decoration: none;\n" +
                "  color: #009900;\n" +
                "}\n" +
                "a:hover, a:focus {\n" +
                "  color: #99ff99;\n" +
                "}\n" +
                "h1, h2 {\n" +
                "  font-size: 1em;\n" +
                "  font-weight: normal;\n" +
                "}");
        out.println("</style>");
        out.println("<body>");


        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm");
        Date date = new Date();
        String now = formatter.format(date);

        out.println("<h1>Alternative Leaderboard derived from <a href=\""+source+"\" target=\"_blank\">Private AOC Leaderboard</a> on "+now+"</h1><br>");

        Map<Integer, List<Map.Entry<String, Long>>> partAs = new LinkedHashMap<>();
        Map<Integer, List<Map.Entry<String, Long>>> partBs = new LinkedHashMap<>();
        Map<Integer, List<Map.Entry<String, Long>>> partABs = new LinkedHashMap<>();

        MedalTables partAMedals = new MedalTables();
        MedalTables partBMedals = new MedalTables();
        MedalTables partABMedals = new MedalTables();
        for(int day=1;day<=31;day++) {
            long dayStartTime = startTime + (day - 1) * 24 * 60 * 60;
            Map<String, Long> star1Times = new LinkedHashMap<>();
            Map<String, Long> bothTimes = new LinkedHashMap<>();
            Map<String, Long> star2Times = new LinkedHashMap<>();
            for (JsonNode member : members) {
                String name = "<code>" + member.get("name").asText() + "&nbsp;".repeat(3) + "</code>";
                JsonNode dayLevels = member.get("completion_day_level");
                JsonNode dayLevel = dayLevels.get("" + day);
                if (dayLevel != null) {
//                    out.println("name=" + name);
                    JsonNode star1 = dayLevel.get("1");
                    JsonNode star2 = dayLevel.get("2");
                    long star1Time = 0;
                    if (star1 != null) {
                        star1Time = Long.parseLong(star1.get("get_star_ts").asText());
                        star1Times.put(name, star1Time - dayStartTime);
                    }
                    if (star2 != null) {
                        long time = Long.parseLong(star2.get("get_star_ts").asText());
                        bothTimes.put(name, time - dayStartTime);
                        star2Times.put(name, time - star1Time);
                    }
                }
            }
            List<Map.Entry<String, Long>> partA = star1Times.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
            List<Map.Entry<String, Long>> partB = star2Times.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
            List<Map.Entry<String, Long>> partAB = bothTimes.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
            partAs.put(day, partA);
            partBs.put(day, partB);
            partABs.put(day, partAB);

            partAMedals.add(partA);
            partBMedals.add(partB);
            partABMedals.add(partAB);
        }
        partABMedals.printTable(out, "Parts A+B");
        partAMedals.printTable(out,"Part A");
        partBMedals.printTable(out, "Part B");



        for(int day=1;day<=31;day++) {
            List<Map.Entry<String, Long>> partA = partAs.get(day);
            List<Map.Entry<String, Long>> partB = partBs.get(day);
            List<Map.Entry<String, Long>> partAB = partABs.get(day);
            long size = Math.max(Math.max(partA.size(),partB.size()), partAB.size());

            if (size>0) {
                DayOfWeek dayOfWeek = LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneId.systemDefault()).getDayOfWeek();
                String niceDay = dayOfWeek.plus(day-1).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                String dayUrl = "https://adventofcode.com/2024/day/"+day;

                String dayUrlContent = getURLContent(dayUrl);
                int titlePosition = dayUrlContent.indexOf("Day "+day+":");
                int titleEnd = dayUrlContent.indexOf("---", titlePosition);
                String title = dayUrlContent.substring(titlePosition, titleEnd).trim();
                out.println("<h1><a href=\""+dayUrl + "\" target=\"_blank\">"+title+"</a> ("+niceDay+")</h1>");
                out.println("<table>");
                out.println("<th></th><th align=left>Part A</th><th></th><th align=left>Part B</th><th></th><th align=left>Parts A + B</th>");
                for (int i = 0; i < size; i++) {
                    String prefix = i==0?"\uD83E\uDD47":i==1?"\uD83E\uDD48":i==2?"\uD83E\uDD49":"";
                    out.print("<tr>");
                    out.println("<td>" + formatTime(partA.get(i).getValue()) + "</td>");
                    out.println("<td>" + prefix+partA.get(i).getKey() + "</td>");
                    if (i < partB.size()) {
                        out.println("<td>" + formatTime(partB.get(i).getValue()) + "</td>");
                        out.println("<td>" + prefix+partB.get(i).getKey() + "</td>");
                        out.println("<td>" + formatTime(partAB.get(i).getValue()) + "</td>");
                        out.println("<td>" + prefix+partAB.get(i).getKey() + "</td>");
                    }
                    out.println("</tr>");
                }
                out.println("</table>");
                out.println();
            }

        }
        out.println("</body>");
        out.println("</html>");
        out.close();
        System.out.println("Saved to "+file);
    }

    private static String formatTime(long _time) {
        int time = (int) _time;
        int minutes = time/60;
        int hours = minutes/60;
        int days = hours/24;

        return formatInterval(days,100)+formatInterval(hours, 24) + formatInterval(minutes, 60) + formatInterval(time, 60)+"&nbsp;";
    }

    private static String formatInterval(int value, int modulo) {
        if (value>=modulo) {
            return String.format(":%02d", value % modulo);
        }
        else if (value>=10) {
            return "&nbsp;"+ (value % modulo);
        }
        else if (value>0) {
            return "&nbsp;&nbsp;"+value;
        }
        else {
            return "&nbsp".repeat(3);

        }
    }

    private static class MedalTables {
        Map<String, Long> golds = new LinkedHashMap<>();
        Map<String, Long> silvers = new LinkedHashMap<>();
        Map<String, Long> bronzes = new LinkedHashMap<>();
        Map<String, Long> medals = new LinkedHashMap<>();

        public void addGold(String name) {
            golds.put(name, golds.getOrDefault(name, 0L)+1);
        }

        public void addSilver(String name) {
            silvers.put(name, silvers.getOrDefault(name, 0L)+1);
        }

        public void addBronze(String name) {
            bronzes.put(name, bronzes.getOrDefault(name, 0L)+1);
        }

        public void addPlacing(String name, int points) {
            medals.put(name, medals.getOrDefault(name, 0L)+points);
        }

        public void printTable(PrintStream out, String label) {
            List<Map.Entry<String, Long>> golds = this.golds.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList().reversed();
            List<Map.Entry<String, Long>> silvers = this.silvers.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList().reversed();
            List<Map.Entry<String, Long>> bronzes = this.bronzes.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList().reversed();
            List<Map.Entry<String, Long>> medals = this.medals.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList().reversed();
            out.println("<h1>--- "+label+" ---</h1>");
            out.println("<table>");
            out.println("<th colspan=2 align=left>Weighted Medals (\uD83E\uDD47=3, \uD83E\uDD48=2, \uD83E\uDD49=1)&nbsp;</th>" +
                    "<th colspan=2 align=left>\uD83E\uDD47 Gold Medals</th>" +
                    "<th colspan=2 align=left>\uD83E\uDD48 Silver Medals</th>" +
                    "<th colspan=2 align=left>\uD83E\uDD49 Bronze Medals</th>");
            int size = Math.max(golds.size(), medals.size());
            for (int i = 0; i < size; i++) {
                out.print("<tr>");
                if (i < medals.size()) {
                    out.println("<td align=right>" + String.format("%d&nbsp;",medals.get(i).getValue()) + "</td>");
                    out.println("<td>" + medals.get(i).getKey() + "</td>");
                }
                else {
                    out.println("<td></td><td></td>");
                }
                if (i<golds.size()) {
                    out.println("<td align=right>" + String.format("%d&nbsp;",golds.get(i).getValue()) + "</td>");
                    out.println("<td>" + golds.get(i).getKey() + "</td>");
                }
                else {
                    out.println("<td></td><td></td>");
                }
                if (i<silvers.size()) {
                    out.println("<td align=right>" + String.format("%d&nbsp;",silvers.get(i).getValue()) + "</td>");
                    out.println("<td>" + silvers.get(i).getKey() + "</td>");
                }
                else {
                    out.println("<td></td><td></td>");
                }
                if (i<bronzes.size()) {
                    out.println("<td align=right>" + String.format("%d&nbsp;",bronzes.get(i).getValue()) + "</td>");
                    out.println("<td>" + bronzes.get(i).getKey() + "</td>");
                }
                else {
                    out.println("<td></td><td></td>");
                }
                out.println("</tr>");
            }
            out.println("</table>");
            out.println("<br>");

        }

        public void add(List<Map.Entry<String, Long>> resultsForOneDay) {
            for(int i=0;i<Math.min(3, resultsForOneDay.size());i++) {
                Map.Entry<String, Long> entry = resultsForOneDay.get(i);
                String name = entry.getKey();
                // todo ideally take ties into account
                addPlacing(name, 3-i);
                if (i==0)
                    addGold(name);
                if (i==1)
                    addSilver(name);
                if (i==2)
                    addBronze(name);
            }

        }
    }

    public static String getURLContent(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String sessionCookie = System.getenv("cookie");
        if (sessionCookie==null) {
            throw new IllegalStateException("must define session cookie in env for runtime environment. e.g. cookie=4534kldfjgl");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "session="+sessionCookie)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {

            System.out.println(response.headers().toString());
            throw new RuntimeException("Failed to fetch URL content. HTTP response code: " + response.statusCode());
        }
    }

}
