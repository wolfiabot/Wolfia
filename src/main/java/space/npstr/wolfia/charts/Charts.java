/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.charts;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.stats.CommandStats;
import space.npstr.wolfia.db.entity.stats.GeneralBotStats;
import spark.ModelAndView;
import spark.Request;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 10.07.17.
 * <p>
 * Shows some charts on a website. Considered as internal usage currently.
 */
public class Charts {

    private static final Logger log = LoggerFactory.getLogger(Charts.class);

    private static final int SINCE_FOREVER = 0;
    private static final int RANGE_ALL = -1;

    @SuppressWarnings("UnusedReturnValue")
    public static Charts spark() {
        return ChartsHolder.INSTANCE;
    }

    //holder singleton pattern
    private static class ChartsHolder {
        private static final Charts INSTANCE = new Charts();
    }


    private Charts() {
        Spark.port(4567);

        if (Config.C.isDebug) {
            final String projectDir = System.getProperty("user.dir");
            final String staticDir = "/src/main/resources/spark";
            Spark.staticFiles.externalLocation(projectDir + staticDir);
        } else {
            Spark.staticFiles.location("/spark");
            Spark.ipAddress("127.0.0.1"); //only listen to loopback that will be provided by an nginx reverse proxy
        }

        Spark.before(
                (rq, rs) -> log.debug("Request received from {} for {}{}", rq.ip(), rq.url(), paramsToString(rq.queryString())),
                //redirect root requests to the charts view
                (rq, rs) -> {
                    if ("/".equals(rq.pathInfo())) {
                        rs.redirect("/charts");
                    }
                },
                //no trailing slashes
                (rq, rs) -> {
                    final String path = rq.pathInfo();
                    if (path.length() > 1 && path.endsWith("/")) {
                        rs.redirect(path.substring(0, path.length() - 1));
                    }
                }
        );

        // @formatter:off
        // this is a non-thymeleaf way to do things which we might use in the future:
        // Spark.get("/charts", (a, q) -> IOUtils.toString(Spark.class.getResourceAsStream("/templates/charts.html")));
        Spark.get("/charts",                                             (rq, rs) -> Charts.charts(), new ThymeleafTemplateEngine());

        //serves random data for debugging purposes
        Spark.get("/random",                                             (rq, rs) -> Charts.generateRandomData(rq));
        Spark.get("/random/latest",                                      (rq, rs) -> Charts.getLatestRandomDataPoint());

        //all /generalbotstats/[stats] endpoints (not the /latest ones) support the following parameters:
        //  int  range; eg. range=10            -> will return up to 10 results
        //  long since; eg. since=1500000000000 -> will return results that are dated after the specified unix epoch

        //these will return a JSON object with named fields for each of the data
        Spark.get("/generalbotstats",                                    (rq, rs) -> Charts.getGeneralBotStats(rq));
        Spark.get("/generalbotstats/latest",                             (rq, rs) -> Charts.getLatestGeneralBotStats());

        //these will return a JSON array for contains either all data points or just the most recent one
        Spark.get("/generalbotstats/usercount",                          (rq, rs) -> Charts.getUserCount(rq));
        Spark.get("/generalbotstats/usercount/latest",                   (rq, rs) -> Charts.getLatestUserCount());
        Spark.get("/generalbotstats/guildcount",                         (rq, rs) -> Charts.getGuildCount(rq));
        Spark.get("/generalbotstats/guildcount/latest",                  (rq, rs) -> Charts.getLatestGuildCount());
        Spark.get("/generalbotstats/gamesbeingplayedcount",              (rq, rs) -> Charts.getGamesBeingPlayedCount(rq));
        Spark.get("/generalbotstats/gamesbeingplayedcount/latest",       (rq, rs) -> Charts.getLatestGamesBeingPlayedCount());
        Spark.get("/generalbotstats/availableprivateguildscount",        (rq, rs) -> Charts.getAvailablePrivateGuildsCount(rq));
        Spark.get("/generalbotstats/availableprivateguildscount/latest", (rq, rs) -> Charts.getLatestAvailablePrivateGuildsCount());
        Spark.get("/generalbotstats/freememory",                         (rq, rs) -> Charts.getFreeMemory(rq));
        Spark.get("/generalbotstats/freememory/latest",                  (rq, rs) -> Charts.getLatestFreeMemory());
        Spark.get("/generalbotstats/maxmemory",                          (rq, rs) -> Charts.getMaxMemory(rq));
        Spark.get("/generalbotstats/maxmemory/latest",                   (rq, rs) -> Charts.getLatestMaxMemory());
        Spark.get("/generalbotstats/totalmemory",                        (rq, rs) -> Charts.getTotalMemory(rq));
        Spark.get("/generalbotstats/totalmemory/latest",                 (rq, rs) -> Charts.getLatestTotalMemory());
        Spark.get("/generalbotstats/averageload",                        (rq, rs) -> Charts.getAverageLoad(rq));
        Spark.get("/generalbotstats/averageload/latest",                 (rq, rs) -> Charts.getLatestAverageLoad());

        //NOTE: the command stats api should likely never be publicly exposed due to angleshooting potential
        Spark.get("/commandstats/executionduration",                     (rq, rs) -> Charts.getCommandExecutionDuration(rq));

        Spark.get("/commandstats/averageexecutionduration/latest",       (rq, rs) -> Charts.getAverageCommandExecutionDuration(rq));
        // @formatter:on

        Wolfia.scheduledExecutor.scheduleAtFixedRate(Charts::updateRand, 0, 1, TimeUnit.SECONDS);
    }

    //########## views

    /**
     * Loads and show all the charts
     */
    private static ModelAndView charts() {
        log.info("serving charts");

        final Map<String, Object> model = new HashMap<>();

        final JSONArray randomData = new JSONArray();
        final long now = System.currentTimeMillis();
        for (int i = 20; i > 10; i--) {
            final JSONArray point = new JSONArray();
            point.put(now - (i * 1000));
            point.put(ThreadLocalRandom.current().nextInt(100));
            randomData.put(point);
        }
        model.put("randomData", randomData);

        final JSONObject allGeneralBotStats = getGeneralBotStats(RANGE_ALL, SINCE_FOREVER);
        model.putAll(allGeneralBotStats.toMap());

        return new ModelAndView(model, "charts"); // located in resources/templates
    }


    //########## random api

    private static JSONArray generateRandomData(final Request request) {
        final int range = extractRange(request, 10);
        final JSONArray data = new JSONArray();
        for (int i = range; i > 0; i--) {
            final Object[] arr = {System.currentTimeMillis() - i * 1000, ThreadLocalRandom.current().nextInt(100)};
            data.put(new JSONArray(arr));
        }
        return data;
    }

    private static long ts = 0;
    private static int rand = 0;

    private static void updateRand() {
        ts = System.currentTimeMillis();
        rand = ThreadLocalRandom.current().nextInt(100);
    }

    private static JSONArray getLatestRandomDataPoint() {
        final Object[] arr = {ts, rand};
        return new JSONArray(arr);
    }


    //########## general bot stats api

    private static JSONObject getGeneralBotStats(final Request request) {
        final int range = request != null ? extractRange(request, RANGE_ALL) : RANGE_ALL;
        final long since = request != null ? extractSince(request, SINCE_FOREVER) : SINCE_FOREVER;
        return getGeneralBotStats(range, since);
    }

    private static JSONObject getLatestGeneralBotStats() {
        return getGeneralBotStats(1, SINCE_FOREVER);
    }

    private static JSONArray getUserCount(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("userCountData");
    }

    private static JSONArray getGuildCount(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("guildCountData");
    }

    private static JSONArray getGamesBeingPlayedCount(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("gamesBeingPlayedCountData");
    }

    private static JSONArray getAvailablePrivateGuildsCount(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("availablePrivateGuildsCountData");
    }

    private static JSONArray getFreeMemory(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("freeMemoryData");
    }

    private static JSONArray getMaxMemory(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("maxMemoryData");
    }

    private static JSONArray getTotalMemory(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("totalMemoryData");
    }

    private static JSONArray getAverageLoad(final Request request) {
        return getGeneralBotStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("averageLoadData");
    }

    private static JSONArray getLatestUserCount() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("userCountData").getJSONArray(0);
    }

    private static JSONArray getLatestGuildCount() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("guildCountData").getJSONArray(0);
    }

    private static JSONArray getLatestGamesBeingPlayedCount() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("gamesBeingPlayedCountData").getJSONArray(0);
    }

    private static JSONArray getLatestAvailablePrivateGuildsCount() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("availablePrivateGuildsCountData").getJSONArray(0);
    }

    private static JSONArray getLatestFreeMemory() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("freeMemoryData").getJSONArray(0);
    }

    private static JSONArray getLatestMaxMemory() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("maxMemoryData").getJSONArray(0);
    }

    private static JSONArray getLatestTotalMemory() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("totalMemoryData").getJSONArray(0);
    }

    private static JSONArray getLatestAverageLoad() {
        return getGeneralBotStats(1, SINCE_FOREVER).getJSONArray("averageLoadData").getJSONArray(0);
    }

    private static JSONObject getGeneralBotStats(final int range, final long since) {
        final JSONObject result = new JSONObject();

        final JSONArray userCountData = new JSONArray();
        final JSONArray guildCountData = new JSONArray();
        final JSONArray gamesBeingPlayedCountData = new JSONArray();
        final JSONArray availablePrivateGuildsCountData = new JSONArray();
        final JSONArray freeMemoryData = new JSONArray();
        final JSONArray maxMemoryData = new JSONArray();
        final JSONArray totalMemoryData = new JSONArray();
        final JSONArray averageLoadData = new JSONArray();

        for (final GeneralBotStats gbs : loadGeneralBotStats(range, since)) {
            final long timeStamp = gbs.getTimeStamp();
            userCountData.put(jsonArrayFrom(timeStamp, gbs.getUserCount()));
            guildCountData.put(jsonArrayFrom(timeStamp, gbs.getGuildCount()));
            gamesBeingPlayedCountData.put(jsonArrayFrom(timeStamp, gbs.getGamesBeingPlayed()));
            availablePrivateGuildsCountData.put(jsonArrayFrom(timeStamp, gbs.getAvailablePrivateGuildsCount()));
            freeMemoryData.put(jsonArrayFrom(timeStamp, gbs.getFreeMemory()));
            maxMemoryData.put(jsonArrayFrom(timeStamp, gbs.getMaxMemory()));
            totalMemoryData.put(jsonArrayFrom(timeStamp, gbs.getTotalMemory()));
            averageLoadData.put(jsonArrayFrom(timeStamp, gbs.getAverageLoad()));
        }

        result.put("userCountData", userCountData);
        result.put("guildCountData", guildCountData);
        result.put("gamesBeingPlayedCountData", gamesBeingPlayedCountData);
        result.put("availablePrivateGuildsCountData", availablePrivateGuildsCountData);
        result.put("freeMemoryData", freeMemoryData);
        result.put("maxMemoryData", maxMemoryData);
        result.put("totalMemoryData", totalMemoryData);
        result.put("averageLoadData", averageLoadData);
        return result;
    }

    //the returned list needs to be sorted by ascending timestamps
    //however we cant easily use the limit together with ORDER BY ASC as it will return the oldest entries while we are
    //interested in the most recent ones
    //this means, if there is a limit, we need to select with ORDER BY DESC, and then reverse the resulting list
    //we could always select with ORDER BY DESC and reverse, but that would mean we have to reverse the look ups without
    //a limit (the biggest ones), and this sounds like bad idea longterm
    //NOTE: we can have the database do this for us if we use plain SQL queries instead of JPQL
    private static List<GeneralBotStats> loadGeneralBotStats(final int limit, final long since) {
        String query = "SELECT * FROM (%s) AS foo ORDER BY foo.time_stamp ASC";
        String subquery = "SELECT * FROM stats_general_bot WHERE stats_general_bot.time_stamp > :since ORDER BY stats_general_bot.time_stamp DESC";
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("since", since);

        if (limit > 0) {
            subquery += " LIMIT :limit";
            parameters.put("limit", limit);
        }

        query = String.format(query, subquery);
        return DbWrapper.selectPlainSqlQueryList(query, parameters, GeneralBotStats.class);
    }


    //########## command stats api


    private static JSONArray getCommandExecutionDuration(final Request request) {
        return getCommandStats(extractRange(request, RANGE_ALL), extractSince(request, SINCE_FOREVER))
                .getJSONArray("executionDuration");
    }


    /**
     * @return average command execution duration
     */
    private static JSONArray getAverageCommandExecutionDuration(final Request request) {
        final int range = request != null ? extractRange(request, 100) : 100; //get the average over the last 100 commands
        final long since = request != null ? extractSince(request, SINCE_FOREVER) : SINCE_FOREVER;
        final long averageCommandExecutionDuration = loadAverageCommandExecutionDuration(range, since);
        return jsonArrayFrom(System.currentTimeMillis(), averageCommandExecutionDuration);
    }

    private static long loadAverageCommandExecutionDuration(final int limit, final long since) {
        String query = "SELECT AVG(foo.execution_duration) FROM (%s) AS foo";
        String subquery = "SELECT * FROM stats_commands WHERE stats_commands.executed_time > :since ORDER BY stats_commands.executed_time DESC";
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("since", since);

        if (limit > 0) {
            subquery += " LIMIT :limit";
            parameters.put("limit", limit);
        }

        query = String.format(query, subquery);
        return DbWrapper.selectPlainSqlQuerySingleResult(query, parameters, BigDecimal.class).longValue();

    }

    private static JSONObject getCommandStats(final int range, final long since) {
        final JSONObject result = new JSONObject();

        final JSONArray executionDuration = new JSONArray();

        for (final CommandStats commandStats : loadCommandStats(range, since)) {
            final long timeStamp = commandStats.getReceived();
            executionDuration.put(jsonArrayFrom(timeStamp, commandStats.getDuration()));
        }


        result.put("executionDuration", executionDuration);
        return result;
    }

    private static List<CommandStats> loadCommandStats(final int limit, final long since) {
        String query = "SELECT * FROM (%s) AS foo ORDER BY foo.executed_time ASC";
        String subquery = "SELECT * FROM stats_commands WHERE stats_commands.executed_time > :since ORDER BY stats_commands.executed_time DESC";
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("since", since);

        if (limit > 0) {
            subquery += " LIMIT :limit";
            parameters.put("limit", limit);
        }

        query = String.format(query, subquery);
        return DbWrapper.selectPlainSqlQueryList(query, parameters, CommandStats.class);
    }


    //########## helper methods

    private static JSONArray jsonArrayFrom(final Object... objects) {
        return new JSONArray(objects);
    }

    private static int extractRange(final Request request, final int defaultValue) {
        final String range = request.queryParams("range");
        return range == null ? defaultValue : Integer.parseInt(range);
    }

    private static long extractSince(final Request request, final long defaultValue) {
        final String since = request.queryParams("since");
        return since == null ? defaultValue : Long.parseLong(since);
    }

    private static String paramsToString(final String query) {
        return query == null ? "" : "?" + query;
    }
}
