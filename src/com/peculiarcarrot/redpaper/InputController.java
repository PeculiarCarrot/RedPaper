package com.peculiarcarrot.redpaper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import mslinks.ShellLink;

public class InputController{

	@FXML
	private TableView<SubredditItem> subredditList;
	@FXML
	private TableColumn<SubredditItem, String> subredditNames;
    @FXML
    private CheckBox nsfw;
    @FXML
    private CheckBox startup;
    @FXML
    private CheckBox useOlderImages;
    @FXML
    private Button addSubButton;
    @FXML
    private Button saveChangesButton;
    @FXML
    private Button removeChangesButton;
    @FXML
    private Button restoreDefaultsButton;
    @FXML
    private Slider hoursPerUpdate;
    @FXML
    private Slider numTopPosts;
    @FXML
    private Slider minutesPerChange;
    @FXML
    private TextField minWidthField;
    @FXML
    private TextField minHeightField;
    @FXML
    private Slider numStoredImages;
    @FXML
    private Label connectIndicator;
    
    private RedPaper redPaper;
	
    @FXML
	public void initialize()
	{
		subredditNames.setCellValueFactory(new PropertyValueFactory<SubredditItem, String>("subName"));
		subredditNames.setCellFactory(TextFieldTableCell.<SubredditItem>forTableColumn());
		
		subredditNames.setOnEditCommit(
	            (CellEditEvent<SubredditItem, String> t) -> {
	                ((SubredditItem) t.getTableView().getItems().get(
	                        t.getTablePosition().getRow())
	                        ).setSubName(t.getNewValue());
	                subredditList.requestFocus();
	        });

		makeSliderMinOne(numTopPosts);
		makeSliderMinOne(minutesPerChange);
		makeSliderMinOne(hoursPerUpdate);
		makeSliderMinOne(numStoredImages);

		makeTextFieldNumeric(minWidthField);
		makeTextFieldNumeric(minHeightField);
	}
    
    public void changeConnectIndicator(boolean connected)
    {
    	connectIndicator.setTextFill(connected ? Color.FORESTGREEN : Color.DARKRED);
    	Platform.runLater(new Runnable()
    	{
        	public void run()
        	{
        		connectIndicator.setText(connected ? "Online" : "Offline");
        	}
    	});
    }
    
    private void makeSliderMinOne(Slider slider)
    {
    	slider.valueProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> source, Number oldValue, Number newValue) {
				if(newValue.intValue() <= 0)
					slider.setValue(1);
			}
		});
    }
    
    private void makeTextFieldNumeric(TextField field)
    {
    	field.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				if(newValue.length() > 6)
				{
					field.setText(oldValue);
					return;
				}
				if(!newValue.matches("\\d*"))
				{
					field.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});
    }
    
    @FXML
    void onAddSub(ActionEvent event)
    {
    	subredditList.getItems().add(new SubredditItem("pics"));
    }
    
    @FXML
    void onSaveChanges(ActionEvent event)
    {
    	setData();
    	redPaper.userPrefs.save();
    	redPaper.queueImageUpdate = true;
    }
    
    @FXML
    void onRemoveChanges(ActionEvent event)
    {
    	redPaper.userPrefs.load();
    	setFields();
    }
    
    @FXML
    void onRestoreDefaults(ActionEvent event)
    {
    	redPaper.userPrefs.changeToDefaults();
    	setFields();
    }
    
    @FXML
    void onRemoveSub(ActionEvent event)
    {
    	int i = subredditList.getSelectionModel().getSelectedIndex();
    	if(i < subredditList.getItems().size() && subredditList.getItems().size() > 1)
    		subredditList.getItems().remove(i < 0 ? subredditList.getItems().size() - 1 : i);
    	if(i != 0 && i < subredditList.getItems().size())
    		subredditList.getSelectionModel().select(subredditList.getSelectionModel().getSelectedIndex() + 1);
    }
    
    private void setData()
    {
    	redPaper.userPrefs.allowNSFW = nsfw.isSelected();
    	redPaper.userPrefs.minHeight = Integer.parseInt(minHeightField.getText());
    	redPaper.userPrefs.minWidth = Integer.parseInt(minWidthField.getText());
    	redPaper.userPrefs.maxStoredImages = (int)numStoredImages.getValue();
    	redPaper.userPrefs.numTop = (int)numTopPosts.getValue();
    	redPaper.userPrefs.updateHours = (int)hoursPerUpdate.getValue();
    	redPaper.userPrefs.changeMins = (int)minutesPerChange.getValue();
    	redPaper.userPrefs.useOlderImages = useOlderImages.isSelected();
    	
    	String[] subs = new String[subredditList.getItems().size()];
    	for(int i = 0; i < subs.length; i++)
    		subs[i] = subredditList.getItems().get(i).getSubName();
    	redPaper.userPrefs.subreddits = subs;
    	
    	if(startup.isSelected())
    	{
	    	try {
				File jarDir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				ShellLink.createLink(jarDir.getAbsolutePath(), 
						RedPaper.getStartupLocation());
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
    	}
    	else
    	{
    		new File(RedPaper.getStartupLocation()).delete();
    	}
    }

    private void setFields() {
		nsfw.setSelected(redPaper.userPrefs.allowNSFW);
		useOlderImages.setSelected(redPaper.userPrefs.useOlderImages);
		minHeightField.setText("" + redPaper.userPrefs.minHeight);
		minWidthField.setText("" + redPaper.userPrefs.minWidth);
		numStoredImages.setValue(redPaper.userPrefs.maxStoredImages);
		numTopPosts.setValue(redPaper.userPrefs.numTop);
		hoursPerUpdate.setValue(redPaper.userPrefs.updateHours);
		minutesPerChange.setValue(redPaper.userPrefs.changeMins);

		ObservableList<SubredditItem> list = FXCollections.observableArrayList();
		for(String s: redPaper.userPrefs.subreddits)
		{
			list.add(new SubredditItem(s));
		}
		subredditList.setItems(list);
		
		startup.setSelected(new File(RedPaper.getStartupLocation()).exists());
	}

	public void setRedPaper(RedPaper redPaper) {
		this.redPaper = redPaper;
		setFields();
	}
}
