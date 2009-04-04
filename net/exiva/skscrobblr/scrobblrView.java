package net.exiva.skscrobblr;

import danger.app.Application;
import danger.app.Event;
import danger.app.IPCMessage;
import danger.app.Registrar;

import danger.ui.Bitmap;
import danger.ui.DialogWindow;
import danger.ui.Font;
import danger.ui.LogTextBox;
import danger.ui.Menu;
import danger.ui.MenuItem;
import danger.ui.Pen;
import danger.ui.ScreenWindow;
import danger.ui.Scrollbar;
import danger.ui.TextField;
import danger.ui.TextInputAlertWindow;

import danger.text.span.*;

public class scrobblrView extends ScreenWindow implements Resources, Commands {
	TextInputAlertWindow lLogin;
	MenuItem menuPause;
	private LogTextBox mLog;
	private TextField tTagsField;
	public static int state;
	
	public scrobblrView() {
	 	lLogin = getApplication().getTextInputAlert(ID_LASTFM_LOGIN, this);
		LogTextBox ltb = new LogTextBox();
		ltb.setSize(getWidth()-2, getHeight()-20);
		ltb.setBorderShowsScrolling(true);
		ltb.show();
		ltb.setGrowFromBottom(true);
		ltb.setHasBorder(false);
		ltb.setBlankAfterFinalNewline(false);
		addChild(ltb);
		setFocusedChild(ltb);

		Scrollbar sb = new Scrollbar();
		sb.setSize(2, getHeight() - 4);
		sb.setPosition(getWidth() - 2, 2);
		sb.show();
		addChild(sb);
		ltb.attachScrollbar(sb);

		tTagsField = new TextField();
		tTagsField.setSize(150,0);
		addChild(tTagsField);
		tTagsField.setPosition(getHeight(),1);
		tTagsField.show();
		
		mLog = ltb;
	}

	public static scrobblrView create() {
		scrobblrView me = (scrobblrView) Application.getCurrentApp().getResources().getScreen(ID_MAIN_SCREEN, null);
		return me;
	}

	public void setState(int inState) {
		state=inState;
	}
	
	public final void adjustActionMenuState(Menu menu) {
		menu.removeAllItems();
		menu.addFromResource(Application.getCurrentApp().getResources(), ID_MAIN_MENU, this);
		menuPause = menu.getItemWithID(sMenuPause);
		if (state == 1) {
			menuPause.setTitle("Enable Scrobbling");			
		} else {
			menuPause.setTitle("Pause Scrobbling");
		}
			
    }

	public void setStatus(String inStatus, int type) {
		SpannableString ss = new SpannableString(inStatus + '\r');
		ConversationSpan cs;

		if (type == 1)
			cs = new ConversationSpan(Resources.ID_OUTGOING, 0);
		else
			cs = new ConversationSpan(Resources.ID_INCOMING, Font.F_BOLD);

		ss.setSpan(cs, 0, ss.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE | Spannable.SPAN_DELETE_ON_EMPTY | Spannable.SPAN_PARAGRAPH);
		mLog.append(ss);
	}

	public void clear() {
		mLog.clear();
	}
	
	public void showLastFMLogin() {
		lLogin.setFocusedChild((TextField)(lLogin.getChildWithID(ID_USERNAME)));
		lLogin.show();
	}

	public void setLastFMLogin(String user, String pass) {
		((TextField)lLogin.getDescendantWithID(ID_USERNAME)).setText(user);
		((TextField)lLogin.getDescendantWithID(ID_PASSWORD)).setText(pass);
	}

	public boolean receiveEvent(Event e) {
		switch (e.type) {
			case EVENT_SIGNUP:
			{
				try{
					danger.net.URL.gotoURL("https://www.last.fm/join/");
				}
				catch (danger.net.URLException exc) { }
				return false;
			}
			case EVENT_STATUS: {
				try{
					danger.net.URL.gotoURL("http://status.last.fm");
				}
				catch (danger.net.URLException exc) { }
				return false;
			}
			case EVENT_CANCEL: {
				if (skScrobblr.getUser() == null || skScrobblr.getPassword() == null) {
					showLastFMLogin();
				}
				return true;
			}
			case EVENT_SETUP: {
				showLastFMLogin();
				return false;
			}
			case EVENT_CONTACT_SUPPORT: {
				IPCMessage ipc = new IPCMessage();
				ipc.addItem("action" , "send");
				ipc.addItem("to","support@exiva.net");
				Registrar.sendMessage("Email", ipc, null);
				return false;
			}
			case EVENT_HELP: {
				DialogWindow help = getApplication().getDialog(helpDialog, this);
				help.show();
				return true;
			}
			case EVENT_ABOUT: {
				DialogWindow about = getApplication().getDialog(aboutDialog, this);
				about.show();
				return true;
			}
			case EVENT_PAUSE: { //paused = 1 unpaused = 0
				state = skScrobblr.getState();
				if (state == 1) {
					state = 0;
					skScrobblr.setPaused(0);
					setStatus("Scrobbling enabled, submissions will resume.", 2);
				} else {
					state = 1;
					skScrobblr.setPaused(1);
					setStatus("Scrobbling paused, submissions will not be sent.", 2	);
				}
				return true;
			}
			case EVENT_STORE_LASTFM_LOGIN: {
				if ("".equals(lLogin.getTextFieldValue((IPCMessage) e.argument, ID_USERNAME))  || "".equals(lLogin.getTextFieldValue((IPCMessage) e.argument, ID_PASSWORD))) {
					showLastFMLogin();
				} else {
				skScrobblr.storeLastFMLogin(lLogin.getTextFieldValue((IPCMessage) e.argument, ID_USERNAME), lLogin.getTextFieldValue((IPCMessage) e.argument, ID_PASSWORD));
				skScrobblr.scrobbleHandshake();
				}
				return true;
			}
			case EVENT_DASHBOARD: {
				try{
					danger.net.URL.gotoURL("http://www.last.fm/dashboard/");
				}
				catch (danger.net.URLException exc) { }
				return true;
			}
			case EVENT_PROFILE: {
				try{
					danger.net.URL.gotoURL("http://www.last.fm/user/"+skScrobblr.getUsername()+"/");
				}
				catch (danger.net.URLException exc) { }
				return true;
			}
			case EVENT_INBOX: {
				try{
					danger.net.URL.gotoURL("http://www.last.fm/inbox/");
				}
				catch (danger.net.URLException exc) { }
				return true;
			}
			case EVENT_SETTINGS: {
				try{
					danger.net.URL.gotoURL("http://www.last.fm/settings/");
				}
				catch (danger.net.URLException exc) { }
				return true;
			}
			default:
			break;
		}
		return super.receiveEvent(e);
	}

    public boolean eventWidgetUp(int inWidget, Event e) {
		switch (inWidget) {
			case Event.DEVICE_BUTTON_CANCEL:
			Application.getCurrentApp().returnToLauncher();
			return true;
			case Event.DEVICE_BUTTON_BACK:
			Application.getCurrentApp().returnToLauncher();
			return true;
		}
		return super.eventWidgetUp(inWidget, e);
	}
		
//thanks Danger :D
	private class ConversationSpan extends StyleSpan.Standard implements LeftMarginSpan {
		public ConversationSpan(int bitmap, int style) {
			super(style);
			mBitmap = bitmap;
			mBitmapWidth = getApplication().getResources().getBitmap(mBitmap).getWidth();
		}

		public int modifyLeftMargin(int margin, Object[] spans, Font f, CharSequence text, int parStart, int lineStart, int paragraphDistance) {
			return margin + mBitmapWidth + GAP;
		}

		public int drawLeftMargin(Pen p, int x, int top, int baseline, int bottom, Object[] spans, CharSequence text, int parStart, int lineStart, int paragraphDistance, Object[] characterSpans) {
			Bitmap b = getApplication().getResources().getBitmap(mBitmap);
			CharacterSpan.Set.drawBlank(p, x, mBitmapWidth + GAP, top, baseline, bottom, characterSpans);
			if (lineStart == ((Spanned) text).getSpanStart(this))
				p.drawBitmap(x, baseline - b.getHeight(), b);
			return mBitmapWidth + GAP;
		}
		private int mBitmap;
		private int mBitmapWidth;
		private static final int GAP = 2;
	}
} //NOM NOM NOM