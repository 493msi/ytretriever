package cz.tefek.youtubetoolkit;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONTokener;

public class PlayerInfoRetriever
{
    public static PlayerInfo get(String videoID) throws IOException
    {
        URL url = new URL("https://www.youtube.com/watch?v=" + videoID);
        Scanner streamScanner = new Scanner(url.openStream());

        StringBuilder pageStringBuilder = new StringBuilder();

        while (streamScanner.hasNext())
        {
            pageStringBuilder.append(streamScanner.nextLine());
        }

        streamScanner.close();

        Pattern playerConfigPattern = Pattern.compile("<script.*?>(.*?)</script>");
        Matcher playerConfigMatcher = playerConfigPattern.matcher(pageStringBuilder.toString());

        String configScript = null;

        while (playerConfigMatcher.find())
        {
            String script = playerConfigMatcher.group(1);

            if (script.contains("ytplayer.config"))
            {
                configScript = script;
                break;
            }
        }

        if (configScript != null)
        {
            configScript = configScript.substring(configScript.indexOf("ytplayer.config"));
            configScript = configScript.substring(configScript.indexOf("{"));

            int bracketCounter = 0;

            for (int i = 0; i < configScript.length(); i++)
            {
                if (configScript.charAt(i) == '{')
                {
                    bracketCounter++;
                }

                if (configScript.charAt(i) == '}')
                {
                    if (--bracketCounter == 0)
                    {
                        configScript = configScript.substring(0, i + 1);
                    }
                }
            }
        }

        JSONTokener jsonTokener = new JSONTokener(configScript);

        JSONObject mainObj = new JSONObject(jsonTokener);

        JSONObject assets = mainObj.getJSONObject("assets");
        JSONObject args = mainObj.getJSONObject("args");

        String fmts = args.optString("url_encoded_fmt_stream_map", null);
        String adaptiveFmts = args.optString("adaptive_fmts", null);
        String thumbnail = args.getString("thumbnail_url");
        String author = args.getString("author");
        String scriptUrl = "http://s.ytimg.com" + assets.getString("js");
        String title = args.getString("title");

        long views = args.optLong("view_count", Long.MIN_VALUE);
        double loudness = args.optDouble("relative_loudness", 0.0);

        PlayerInfo playerInfo = new PlayerInfo(thumbnail, views, loudness, author, title, adaptiveFmts, fmts, scriptUrl);

        return playerInfo;
    }
}
