package com.peculiarcarrot.redpaper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import ga.dryco.redditjerk.exceptions.OAuthClientException;
import ga.dryco.redditjerk.implementation.RedditApi;
import ga.dryco.redditjerk.wrappers.Link;
import ga.dryco.redditjerk.wrappers.User;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class RedPaper{
	
	/**
	 * Where we're going to save all of our data
	 */
	public final String savePath = System.getenv("APPDATA")+"\\RedPaper\\";
	public final String imageSavePath = savePath + "images\\";
	public final String preferencesSavePath = savePath + "preferences.json";
	
	/**
	 * All the customizable user preferences
	 */
	public UserPrefs userPrefs;
	
	/**
	 * The exact time the program started
	 */
	public long startTime;
	/**
	 * How many times we've updated the queue (used for timekeeping)
	 */
	public int updates = 0;
	/**
	 * You know, the Reddit instance.
	 */
	RedditApi red;
	/**
	 * The images in queue for us to use
	 */
	ArrayList<QueuedImage> queuedImages = new ArrayList<QueuedImage>();
	/**
	 * Whether or not everything has been initialized
	 */
	boolean initialized;
	
	public boolean allowDownloading = true;
	public boolean paused;
	
	public static String programName="RedPaper";
	
	public String userAgent = "windows:com.peculiarcarrot.redpaper." + programName.toLowerCase() + ":v1.0.0 (by /u/peculiarcarrot)";
	public boolean shouldTryGettingImages = true;
	public boolean isConnected = true;
	
	/**
	 * How long in ms between pings while we're disconnected
	 */
	private int timePerPing = 10 * 1000;
	private Timer connectTimer;
	
	public boolean queueImageUpdate;
	
	/**
	 * Loads the preferences from disk, or saves default prefs if none exist
	 */
	public void initializePrefs()
	{
		red = RedditApi.getRedditInstance(userAgent);
		if(UserPrefs.Default == null)
			UserPrefs.Default = new UserPrefs(this);
		startTime = System.currentTimeMillis();
		{
			//Create the data directories if they don't exist
			File folder = new File(imageSavePath);
			userPrefs = new UserPrefs(this);
			if(!folder.exists())
				folder.mkdirs();
			
			//Load preferences if we can find them
			if(userPrefs.preferencesExist())
				userPrefs.load();
		}
		if(userPrefs.deviceID == null)
			userPrefs.deviceID = new RandomString(25).nextString();
		userPrefs.save();
		red.loginApp("MEj48WB7OhI7DQ", userPrefs.deviceID);
	}
	
	/**
	 * Start the process of setting wallpapers
	 */
	public void begin() {
		updateImages();
		iterateThroughImages();
	}
	
	public static String getStartupLocation()
	{
		return System.getProperty("java.io.tmpdir").replace("Local\\Temp\\", "Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup")+"\\RedPaper.lnk";
	}
	
	/**
	 * This loops through the queued images over a specified interval
	 */
	public void iterateThroughImages()
	{
		if(queuedImages.size() == 0 && isConnected)
		{
			error("I couldn't find any images that fit your preferences. Sorry!");
			shouldTryGettingImages = false;
		}
		//System.out.println("--------------------------"+queuedImages.size());
		if(shouldTryGettingImages && queuedImages.size() > 0)
		{
			for(int i = 0; i < queuedImages.size(); i++)
			 {
				//Load the image. If it loads correctly and fits the size requirements in userprefs, we change the wallpaper
				 String path = saveImage(queuedImages.get(i).link,queuedImages.get(i).redditID);
				 if(i == queuedImages.size() - 1)
					 i =- 1;
				 //System.out.println(path != null);
				 if(path != null)
				 {
					 WallpaperChanger.change(path);
				 }
				 else
					 continue;
				 
				 //Sleep for however many minutes the user wants
				 try {
					Thread.sleep(1000 * 60 * userPrefs.changeMins);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				 if(paused)
				 {
					 i--;
					 continue;
				 }
				 //If it's time to update the queue from Reddit, do it
				 if(System.currentTimeMillis() > startTime + 1000 * 60 * 60 * userPrefs.updateHours * updates || queueImageUpdate)
				 {
					 queueImageUpdate = true;
					 if(allowDownloading)
						 updateImages();
					 break;
				 }
				 //While the number of files in the path is greater than the number set in userprefs, delete the oldest one.
				 File dir = new File(imageSavePath);
				 while(dir.listFiles().length > userPrefs.maxStoredImages)
				 {
					 long oldest=0;
					 File old = null;
					 for(File file: dir.listFiles()) 
					 {
						 if(old == null || file.lastModified() < oldest)
						 {
							 oldest = file.lastModified();
							 old = file;
						 }
					 }
					 old.delete();
				 }
			 }
			iterateThroughImages();
		}
		else
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Checks Reddit for the hottest pics on the given subreddits. Refreshes the image queue with those.
	 */
	public void updateImages()
	{
		queueImageUpdate = false;
		try
		{
			//trayIcon.setToolTip(programName + " - Updating images...");
			//frame.setTitle(trayIcon.getToolTip());
			updates++;
			queuedImages.clear();
			
			//Get the top images from the given subreddits. We determine if the given post is an image by looking for ".jpg" or ".png" in the link URL.
			//Won't use NSFW images unless the user preferences are changed.
			for(String s: userPrefs.subreddits)
			 {
				 for(Link sr: red.getSubreddit(s).getHot(userPrefs.numTop))
				 {
					 String o = sr.getUrl();
					 if((!sr.getOver18() || userPrefs.allowNSFW) && sr.getUrl() != null)
						 if(o.toLowerCase().contains(".jpg") || o.toLowerCase().contains(".png"))
						 {
							 queuedImages.add(new QueuedImage(o, sr.getId()));
						 }
				 }
			 }
			//trayIcon.setToolTip(programName);
			//frame.setTitle(programName+" Settings");
			isConnected = true;
			Collections.shuffle(queuedImages);
			if(connectTimer != null)
				connectTimer.cancel();
		}
		catch(OAuthClientException e)
		{
			//frame.setTitle("RedPaper - Can't connect");
			if(isConnected)
			{
				error("Hmm. RedPaper couldn't connect to Reddit. Check your internet connection.");
				isConnected = false;
				connectTimer = new Timer();
				connectTimer.schedule(new TimerTask()
				{
					public void run() {
						updateImages();
					}
					
				}, timePerPing, timePerPing);
			}
		}
	}
	
	public void kill()
	{
		if(connectTimer != null)
			connectTimer.cancel();
	}
	
	public void error(String message)
	{
    	Platform.runLater(new Runnable()
    	{
        	public void run()
        	{
				Alert error = new Alert(AlertType.ERROR, message, ButtonType.OK);
				error.setTitle("Whoops");
				error.showAndWait();
        	}
    	});
	}
	
	/**
	 * Saves the given image to disk
	 * @param imageUrl
	 * @param id
	 * @return The file path of the saved image
	 */
	public String saveImage(String imageUrl,String id) {
		String filename = imageSavePath+"\\img_" + id + ".jpg";
		File imgFile = new File(filename);
		if(imgFile.exists())
		{
			BufferedImage img = getImage(filename);
			if(img == null || img.getWidth() < userPrefs.minWidth || img.getHeight() < userPrefs.minHeight)
			{
				imgFile.delete();
				return null;
			}
			return filename;
		}
		if(!allowDownloading)
			return null;
		try {
			URL url = new URL(imageUrl);
			URLConnection uc = url.openConnection();
			uc.addRequestProperty("User-Agent", 
			"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
			InputStream is = uc.getInputStream();
			OutputStream os = new FileOutputStream(filename);
			byte[] b = new byte[2048];
			int length;
			while ((length = is.read(b)) != -1)
			{
				os.write(b, 0, length);
			}
	
			BufferedImage img = getImage(filename);
			is.close();
			os.close();
			if(img.getWidth() < userPrefs.minWidth || img.getHeight() < userPrefs.minHeight)
				return null;
			return filename;
		}
		catch(IOException e) {
			e.printStackTrace();
			//The site doesn't want us to look at the image, so we'll just move on
			return null;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Loads the image from the file
	 * @param filename
	 */
	public BufferedImage getImage(String filename) {
		try {
		    return ImageIO.read(new File(filename));
		}
		catch (Exception e) {}
		
		return null;
	}
	
	public static BufferedImage getJarImage(String name)
	{
		try {
			return ImageIO.read(RedPaper.class.getResourceAsStream(name));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
