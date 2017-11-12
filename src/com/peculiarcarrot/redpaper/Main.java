package com.peculiarcarrot.redpaper;
	
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class Main extends Application {
	
	private RedPaper redPaper;
	private InputController inputController;
	
	public String version = "1.0.0";
	
	@Override
	public void start(Stage stage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("RedPaperScene.fxml"));
			Parent root = loader.load();
			inputController = loader.<InputController>getController();
			Scene scene = new Scene(root, 600, 450);
			
			stage.setResizable(false);
			stage.getIcons().add(new Image(Main.class.getResourceAsStream("/windowIcon.png")));
	        stage.setTitle("RedPaper v"+version);
			stage.setScene(scene);
			
			redPaper = new RedPaper(inputController);
			redPaper.initializePrefs();
			inputController.setRedPaper(redPaper);
			Platform.setImplicitExit(false);
			stage.show();
			
			if (SystemTray.isSupported()) {         
	            SystemTray tray = SystemTray.getSystemTray();
	            
	            PopupMenu menu=new PopupMenu();
	    		MenuItem open=new MenuItem("Open");
	    		MenuItem pause=new MenuItem("Pause");
	    		MenuItem disableDownloads=new MenuItem("Disable Downloads");
	    		MenuItem refresh=new MenuItem("Refresh");
	    		MenuItem quit=new MenuItem("Quit");
	    		
	    		menu.add(open);
	    		//menu.add(refresh);
	    		menu.add(pause);
	    		menu.add(disableDownloads);
	    		menu.add(quit);

	            TrayIcon trayIcon = new TrayIcon(RedPaper.getJarImage("/icon.png"), "RedPaper", menu); 
	    		
	    		open.addActionListener(new ActionListener(){
	    			public void actionPerformed(ActionEvent e) {
	                	Platform.runLater(new Runnable()
	                	{
		                	public void run()
		                	{
		                		stage.show();
		                	}
	                	});
	    			}
	    		});
	    		
	    		pause.addActionListener(new ActionListener(){
	    			public void actionPerformed(ActionEvent e) {
	    				redPaper.paused = !redPaper.paused;
	    				
	    				if(redPaper.paused)
	    					pause.setLabel("Unpause");
	    				else
	    					pause.setLabel("Pause");
	    			}
	    		});
	    		
	    		disableDownloads.addActionListener(new ActionListener(){
	    			public void actionPerformed(ActionEvent e) {
	    				redPaper.allowDownloading = !redPaper.allowDownloading;
	    				
	    				if(redPaper.allowDownloading)
	    					disableDownloads.setLabel("Disable Downloads");
	    				else
	    					disableDownloads.setLabel("Enable Downloads");
	    			}
	    		});
	    		
	    		refresh.addActionListener(new ActionListener(){
	    			public void actionPerformed(ActionEvent e) {
	    				redPaper.updates = 0;
	    				redPaper.startTime = System.currentTimeMillis();
	    				redPaper.userPrefs.load();
	    				redPaper.isConnected = true;
	    				redPaper.updateImages();
	    	            redPaper.iterateThroughImages();
	    			}
	    		});

	    		quit.addActionListener(new ActionListener(){
	    			public void actionPerformed(ActionEvent e) {
	    				SystemTray.getSystemTray().remove(trayIcon);
	    				Platform.exit();
	    				redPaper.kill();
	    				System.exit(1);
	    			}
	    		});
	    		
	    		trayIcon.addMouseListener(new MouseAdapter() {
	    		    public void mouseClicked(MouseEvent e) {
	    		    	if(e.getButton() == MouseEvent.BUTTON1)
	    		    	{
		    		        Platform.runLater(new Runnable() {
								@Override
								public void run() {
									if(stage.isShowing())
										stage.hide();
									else
										stage.show();
								}
		    		        });
	    		    	}
	    		    }
	    		}); 

	            try
	            {
	              tray.add(trayIcon);
	            }
	            catch (Exception e) {}
	            
	          }
			
			new Thread()
			{
				public void run()
				{  
					redPaper.begin();
				}
			}.start();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop()
	{
		redPaper.kill();
		Platform.exit();
		System.exit(0);
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
