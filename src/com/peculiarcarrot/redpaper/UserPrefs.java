package com.peculiarcarrot.redpaper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * The user's customizable preferences
 * @author Austin Harman
 *
 */
public class UserPrefs {
	
	public RedPaper redPaper;
	
	public static UserPrefs Default;
	
	public UserPrefs(RedPaper redPaper)
	{
		this.redPaper = redPaper;
	}
	
	public void changeToDefaults()
	{
		numTop = Default.numTop;
		subreddits = Default.subreddits;
		changeMins = Default.changeMins;
		updateHours = Default.updateHours;
		minWidth = Default.minWidth;
		minHeight = Default.minHeight;
		maxStoredImages = Default.maxStoredImages;
		allowNSFW = Default.allowNSFW;
	}
	
	/**
	 * The subreddits to look through
	 */
	public String[] subreddits = new String[]{
			"breathless",
			"imaginarywildlands",
			"imaginarycityscapes",
			"imaginarylandscapes",
			"earthporn",
			"skyporn",
			"eyecandy",
			"jungleporn",
			"nature",
			"spaceporn"};
	/**
	 * How many top posts to look at from each subreddit
	 */
	public int numTop = 15;
	/**
	 * How many minutes between each wallpaper change
	 */
	public int changeMins = 5;
	/**
	 * How many hours between every update
	 */
	public int updateHours = 6;
	/**
	 * The minimum size an image must be to be eligible for use as a wallpaper
	 */
	public int minWidth = 1280, minHeight = 720;
	/**
	 * The maximum number of wallpapers that should be stored in the folder.
	 * Once this limit is reached, the oldest images will start being deleted.
	 */
	public int maxStoredImages = 100;
	/**
	 * Whether or not NSFW images will be allowed as a wallpaper
	 */
	public boolean allowNSFW = false;

	/**
	 * This device's ID
	 */
	public String deviceID;
	
	/**
	 * @return True if there is a file with the preferences saved.
	 */
	public boolean preferencesExist()
	{
		return new File(redPaper.preferencesSavePath).exists();
	}

	/**
	 * Saves the user's preferences to a file
	 */
	public void save()
	{
		File f = new File(redPaper.preferencesSavePath);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
	              new FileOutputStream(f), "utf-8"))) {
			
			JSONObject obj = new JSONObject();

			obj.put("numTop", numTop);
			obj.put("changeMins", changeMins);
			obj.put("updateHours", updateHours);
			obj.put("minWidth", minWidth);
			obj.put("minHeight", minHeight);
			obj.put("maxStoredImages", maxStoredImages);
			obj.put("allowNSFW", allowNSFW);
			obj.put("deviceID", deviceID);
			
			JSONArray srs = new JSONArray();
			for(String s: subreddits)
				srs.put(s);
			obj.put("subreddits", srs);
			writer.write(obj.toString(2));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the user's preferences from a file
	 */
	public void load()
	{
		String line;
		String all = "";
		try (
		    InputStream fis = new FileInputStream(new File(redPaper.preferencesSavePath));
		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		    BufferedReader br = new BufferedReader(isr);
		)
		{
			while ((line = br.readLine()) != null) {
		        all += line;
		    }
		 JSONParser parser = new JSONParser();
		 org.json.simple.JSONObject obj=(org.json.simple.JSONObject)parser.parse(all);
		 
		 for(Object o: obj.keySet())
		 {
			 switch((String)o)
			 {
			 case "numTop":
				 numTop = (int) ((long)obj.get("numTop"));
				 break;
			 case "changeMins":
				 changeMins = (int) ((long)obj.get("changeMins"));
				 break;
			 case "minWidth":
				 minWidth = (int) ((long)obj.get("minWidth"));
				 break;
			 case "minHeight":
				 minHeight = (int) ((long)obj.get("minHeight"));
				 break;
			 case "maxStoredImages":
				 maxStoredImages = (int) ((long)obj.get("maxStoredImages"));
				 break;
			 case "updateHours":
				 updateHours = (int) ((long)obj.get("updateHours"));
				 break;
			 case "allowNSFW":
				 allowNSFW = (boolean)obj.get("allowNSFW");
				 break;
			 case "deviceID":
				 deviceID = (String)obj.get("deviceID");
				 break;
			 }
		 }
		 
		 org.json.simple.JSONArray srs = (org.json.simple.JSONArray) obj.get("subreddits");
		 subreddits = new String[srs.size()];
		 for(int i = 0; i < subreddits.length; i++)
			 subreddits[i] = (String)srs.get(i);
		 
		} catch (Exception e) {
			System.err.println("There was an error loading the file.");
			e.printStackTrace();
		}
	}

}
