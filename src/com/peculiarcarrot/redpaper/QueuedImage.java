package com.peculiarcarrot.redpaper;

import java.io.File;

public class QueuedImage {
	
	public String link,redditID;
	public File file;
	
	public QueuedImage(String link, String redditID)
	{
		this.link=link;
		this.redditID=redditID;
	}
	
	public QueuedImage(File file)
	{
		this.file = file;
	}

}
