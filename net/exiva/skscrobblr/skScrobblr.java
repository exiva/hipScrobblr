package net.exiva.skscrobblr;

import danger.app.Application;
import danger.app.AppResources;
import danger.app.Alarm;
import danger.app.Event;
import danger.app.EventType;
import danger.app.SettingsDB;
import danger.app.SettingsDBException;

import danger.crypto.MD5;

import danger.mime.Base64;

import danger.net.HTTPConnection;
import danger.net.HTTPTransaction;

import danger.player.MusicPlayer;
import danger.player.AudioTrack;

import danger.ui.SplashScreen;
import danger.ui.StaticTextBox;
import danger.ui.View;
import danger.ui.Pen;
import danger.ui.MarqueeAlert;
import danger.ui.NotificationManager;

import danger.util.DEBUG;
import danger.util.StringUtils;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.lang.System;
import java.net.URLEncoder;

public class skScrobblr extends Application implements Resources, Commands {
	public boolean firstLaunch = true;
	static public scrobblrView mWindow;
	static private String username = new String();
	static private String password = new String();
	static private String Base64Login = new String();	
	public static SettingsDB scrobblrPrefs;
	public static AudioTrack audio;
	private static Alarm scrobleTrackAlarm;
	private static Alarm scrobbleNowPlayingAlarm;
    public SplashScreen mSplashScreen;
	private StaticTextBox mLoginStatus;
	private static StaticTextBox mPauseStatus;
	public static String artist;
	public static String song;
	public static String artistEnc;
	public static String comment;
	public static String songEnc;
	public static int trackNum;
	public static String album;
	public static int state;
	public static int length;
	public static int newLength;
	public static int lengthS;
	public static int newLengthS;
	public static long unixtimestamp;
	public static String clientID = "hsc";
	public static String clientVer = "0.1";
	public static String[] response;
	public static String status;
	public static String sessionID;
	public static String nowPlayingURL;
	public static String scrobbleURL;
	public static long startedPlaying;
	public static String version;
	public static String build;
	public static String className;
	private static boolean isLoggedin = false;
	private static boolean mIsAppForeground;

	public skScrobblr() {
		mSplashScreen = getResources().getSplashScreen(AppResources.ID_SPLASH_SCREEN_RESOURCE);
		mLoginStatus = (StaticTextBox) mSplashScreen.getDescendantWithID(ID_LOGIN_STATUS);
		mLoginStatus.setVisible(true);
		mPauseStatus = (StaticTextBox) mSplashScreen.getDescendantWithID(ID_PAUSE_STATUS);
		mWindow = scrobblrView.create();
		mWindow.show();
		Application.registerForEvent(this, EventType.WHAT_MEDIA_PLAYER_START_TRACK);
		Application.registerForEvent(this, EventType.WHAT_MEDIA_PLAYER_UPDATE_TRACK);
		Application.registerForEvent(this, EventType.WHAT_MEDIA_PLAYER_STOP_TRACK);
		Application.registerForEvent(this, EventType.EVENT_MEDIA_PLAYER_STATE_CHANGED);
	}

	public void launch() {
		restoreData();
		if (state == 1) {
			mWindow.setStatus("Scrobbling paused, submissions will not be sent.", 2	);
			mWindow.setState(1);
		}
		updatePreviewScreen();
	}

	public void resume() {
		if (firstLaunch)
			return;
		updatePreviewScreen();
		mIsAppForeground=true;
		if (!isLoggedin) {
			scrobbleHandshake();
		}
	}

	public void suspend() {
		mIsAppForeground=false;
	}
	
	public void restoreData() {
		if (SettingsDB.findDB("scrobblrSettings") == false) {
			scrobblrPrefs = new SettingsDB("scrobblrSettings", true);
			scrobblrPrefs.setAutoSyncNotifyee(this);
			login();
		} else {
			scrobblrPrefs = new SettingsDB("scrobblrSettings", true);
			username = scrobblrPrefs.getStringValue("username");
			password = scrobblrPrefs.getStringValue("password");
			try {
				state = scrobblrPrefs.getIntValue("state");
			} catch (SettingsDBException exception) {
				DEBUG.p("Caught!");
			}
			login();
			mWindow.setLastFMLogin(username, password);
			if (state == 1) {
				mPauseStatus.setVisible(true);
			}
		}
	}
	
	public void login() {
		if ((username != null) || (password != null)) {
			scrobbleHandshake();
		}
		if ((username == null) || (password == null)) {
			mWindow.showLastFMLogin();
		}
	}
	
	//paused = 1 unpaused = 0
	public static void setPaused(int inState) {
		if (state == 1) {
			state = 0;
			mPauseStatus.setVisible(false);
		} else {
			state = 1;
			mPauseStatus.setVisible(true);
		}
		scrobblrPrefs.setIntValue("state", inState);
	}
	
	public static String getUser() {
		return username;
	}
	
	public static String getPassword() {
		return password;
	}
	
	public static void storeLastFMLogin(String inUser, String inPass) {
		scrobblrPrefs.setStringValue("username", inUser);
		scrobblrPrefs.setStringValue("password", inPass);
		username = inUser;
		password = inPass;
	}

	public static String getUsername() {
		return username;
	}
	
	public static int getState() {
		return state;
	}

	public static void scrobbleHandshake() {		
		if ((username == null) || (password == null)) {
			mWindow.showLastFMLogin();
		}
		mWindow.clear();
		MD5 passdigest = new MD5();
		passdigest.update((password).getBytes());
		String passtoken = StringUtils.bytesToHexString(passdigest.digest()).toLowerCase();
		MD5 digest = new MD5();
		digest.update((passtoken+System.currentTimeMillis()/1000).getBytes());
		String token = StringUtils.bytesToHexString(digest.digest()).toLowerCase();
		HTTPConnection.get("http://post.audioscrobbler.com/?hs=true&p=1.2&c="+clientID+"&v="+clientVer+"&u="+username+"&t="+System.currentTimeMillis()/1000+"&a="+token, null, (short) 0, 1);
	}

	public void setup(){
		audio = MusicPlayer.getCurrentTrack();
		length = audio.getMillisecondLength();
		comment = audio.getComment();
		newLength = length/2;
		lengthS = length/1000;
		newLengthS = newLength/1000;
		startedPlaying = System.currentTimeMillis()/1000;
		DEBUG.p("====================================");
		DEBUG.p("Track Updated/Started!");
		DEBUG.p("Length (ms): "+length);
		DEBUG.p("LengthS: "+lengthS);
		DEBUG.p("New Length S: "+newLength);
		DEBUG.p("Comment: "+comment+" Comment to caps: "+comment.toUpperCase());
		DEBUG.p("====================================");
		if (!comment.toUpperCase().equals("NOSCROBBLE")) {
			DEBUG.p("Track is scrobbleable!");
			scrobbleNowPlaying();
			scrobleTrackAlarm = new Alarm(newLengthS, this, 1, null);
			scrobleTrackAlarm.activate();
		}
	}

	public static void scrobbleNowPlaying() {
		if(isLoggedin && state==0) {
			audio = MusicPlayer.getCurrentTrack();
			artist = audio.getArtist();
			song = audio.getTitle();
			album = audio.getAlbum();
			trackNum = audio.getTrackNumber();
			length = audio.getMillisecondLength();
			DEBUG.p("====================================");
			DEBUG.p("Now Playing:");
			DEBUG.p("Artist: "+artist+" Song: "+song);
			DEBUG.p("Album: "+album+" Track Number: "+trackNum);
			DEBUG.p("Length (ms): "+length);
			DEBUG.p("====================================");
			String headers = "User-Agent: skScrobblr\r\n" +
				"Content-type: application/x-www-form-urlencoded\r\n";
			try { 
				artistEnc = URLEncoder.encode(artist, "UTF-8");
				songEnc = URLEncoder.encode(song, "UTF-8");
				album = URLEncoder.encode(album,"UTF-8");
			}
			catch (UnsupportedEncodingException e) { }
			HTTPConnection.post(nowPlayingURL, headers, "s="+sessionID+"&a="+artistEnc+"&t="+songEnc+"&b="+album+"&l="+length/1000+"&n="+trackNum+"&m=", (short) 0, 2);
		}
	}

	public void renderSplashScreen(View inView, Pen inPen) {
		mSplashScreen.paint(inView, inPen);
	}

	public static void scrobbleTrack() {
		if(isLoggedin && state==0) {
			audio = MusicPlayer.getCurrentTrack();
			artist = audio.getArtist();
			song = audio.getTitle();
			album = audio.getAlbum();
			trackNum = audio.getTrackNumber();
			length = audio.getMillisecondLength();
			String headers = "User-Agent: skScrobblr\r\n" +
				"Content-type: application/x-www-form-urlencoded\r\n";
			try {
				artistEnc = URLEncoder.encode(artist, "UTF-8");
				songEnc = URLEncoder.encode(song, "UTF-8");
				album = URLEncoder.encode(album,"UTF-8");
			}
			catch (UnsupportedEncodingException e) { }
			HTTPConnection.post(scrobbleURL, headers, "s="+sessionID+"&a[0]="+artistEnc+"&t[0]="+songEnc+"&i[0]="+startedPlaying+"&o[0]=P&r[0]=&l[0]="+length/1000+"&b[0]="+album+"&n[0]="+trackNum+"&m[0]=", (short) 0, 3);
		}
	}
	
	public final void networkEvent(Object obj) {
		if (obj instanceof HTTPTransaction) {
			HTTPTransaction ht = (HTTPTransaction) obj;
			int seqID = ht.getSequenceID();
			if (seqID == 1) {
				response = ht.getString().split("\n");
				status = response[0];
				if ("BADAUTH".equals(status) || "BADTIME".equals(status)) {
					mWindow.showLastFMLogin();
					mWindow.setStatus("There was an authentication error. Most likely your password was entered incorrectly, please try again.", 2);
					mLoginStatus.setText("Not Logged In");
					updatePreviewScreen();
					isLoggedin = false;
				} else if ("BANNED".equals(status)) {
					mWindow.setStatus("The client has been banned.", 2);
					mLoginStatus.setText("Not Logged In");
					updatePreviewScreen();
					isLoggedin = false;
				} else if ("OK".equals(status)) {
					sessionID = response[1];
					nowPlayingURL = response[2];
					scrobbleURL = response[3];
					try {
						mWindow.setStatus("Successfully logged into last.fm", 2);
						mLoginStatus.setText("Logged In");
						updatePreviewScreen();
						isLoggedin = true;
						
					} catch (Exception e) { }
				} else {
					mWindow.setStatus("Something went horribly wrong.", 2);
				}
			} else if (seqID == 2) {
				try {
					response = ht.getString().split("\n");
					status = response[0];
					if ("OK".equals(status)) {
						if (!this.mIsAppForeground) { 
							MarqueeAlert nowPlayingAlert = new MarqueeAlert("Now Playing: "+artist+" - "+song, getBundle().getSmallIcon(), 1);
							NotificationManager.marqueeAlertNotify(nowPlayingAlert);
						}
						mWindow.setStatus("Now Playing: "+artist+" - "+song, 1);
					} else if ("BADSESSION".equals(status)) {
						mWindow.setStatus("Session Error. Logging in again.", 2);
						isLoggedin = false;
						scrobbleHandshake();
						scrobbleNowPlaying();
					} else if (!"OK".equals(status)) {
							mWindow.setStatus(status, 2);
					}
				} catch (Exception e) { }
			} else if (seqID == 3) {
				try {
					response = ht.getString().split("\n");
					status = response[0];
					if ("OK".equals(status)) {
						if (!this.mIsAppForeground) { 
							MarqueeAlert scrobbleAlert = new MarqueeAlert("Scrobbled track.", getBundle().getSmallIcon(), 1);
							NotificationManager.marqueeAlertNotify(scrobbleAlert);
						}
						mWindow.setStatus("Successfully Scrobbled!", 2);
						String sendMessage = "Listened to "+artist+" - "+song;
						String sendWord=new String(sendMessage);
						byte[] sendBytes=new byte[sendWord.length()*2];
						sendBytes=sendWord.getBytes();
					} else if ("BADSESSION".equals(status)) {
						mWindow.setStatus("Session Error. Logging in again.", 2);
						isLoggedin = false;
						scrobbleHandshake();
						scrobbleTrack();
					} else if (!"OK".equals(status)) {
						mWindow.setStatus(status, 2);
					}
				} catch (Exception e) { }
			}
		}
	}

	public boolean receiveEvent(Event e) {
		switch (e.what) {
		case EventType.WHAT_MEDIA_PLAYER_START_TRACK:
		// stop any active Alarms. Just incase.
		try {
			scrobleTrackAlarm.deactivate();
			scrobbleNowPlayingAlarm.deactivate();
		} catch (Exception exception) {}
		scrobbleNowPlayingAlarm = new Alarm(1, this, 2, null);
		scrobbleNowPlayingAlarm.activate();
		return true;

		case EventType.WHAT_MEDIA_PLAYER_UPDATE_TRACK:
		// stop any active Alarms. Just incase.
		try {
			scrobleTrackAlarm.deactivate();
			scrobbleNowPlayingAlarm.deactivate();
		} catch (Exception exception) {}
		scrobbleNowPlayingAlarm = new Alarm(1, this, 2, null);
		scrobbleNowPlayingAlarm.activate();
		return true;

		case EventType.WHAT_MEDIA_PLAYER_STOP_TRACK:
		try {
			scrobleTrackAlarm.deactivate();
			scrobbleNowPlayingAlarm.deactivate();
		} catch (Exception exception) {}
		return true;

		case Event.EVENT_DATASTORE_RESTORED: {
			restoreData();
			return true;
		}
		}
				
		switch (e.type)
		{
			case Event.EVENT_ALARM:
			if(e.data==1){
				scrobbleTrack();
				scrobleTrackAlarm.deactivate();
			} else {
				setup();
				scrobbleNowPlayingAlarm.deactivate();
			}
			break;
			default:
			break;
		}
		return super.receiveEvent(e);
	}
} // NOM NOM NOM