package com.peculiarcarrot.redpaper;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SubredditItem {
	
	private StringProperty subName;
	
	public SubredditItem(String name)
	{
		setSubName(name);
	}
	
    public void setSubName(String value)
    {
    	subNameProperty().set(value);
    }
    
    public String getSubName()
    {
    	return subNameProperty().get();
    }
    
    public StringProperty subNameProperty()
    { 
        if (subName == null)
        	subName = new SimpleStringProperty(this, "subName");
        return subName; 
    }
}
