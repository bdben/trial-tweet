package com.bdben.trialtwitter;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Activity to show on launch. Checks if user is logged in and launches
 * {@link TimelineActivity}. Prompts user to log in with Twitter if not logged
 * in.
 * 
 * @author Benjamin Mullin-Lamarche
 * 
 */
public class MainActivity extends Activity {

	static List<String[]> users = new ArrayList<String[]>();

	static List<Twitter> twitters = new ArrayList<Twitter>();

	/**
	 * Callback URL to send with Twitter OAuth request
	 */
	private static final String TWITTER_CALLBACK_URL = "oauth://bdbentweet";

	/**
	 * The Twitter OAuth consumer key
	 */
	static final String TWITTER_CONSUMER_KEY = "gheVsBKyDbWRAxhSss4yQ";

	/**
	 * The twitter OAuth consumer secret
	 */
	static final String TWITTER_CONSUMER_SECRET = "Xl1FPpgd9xDFrMjssqBtdSiWRIChYAYkyNP7kjoGjo";

	// Twitter
	private static Twitter twitter;
	private static RequestToken requestToken;

	// Twitter OAuth URLs
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

	// Internet Connection detector
	private ConnectionDetector cd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (android.os.Build.VERSION.SDK_INT > 8) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitNetwork().build();
			StrictMode.setThreadPolicy(policy);
		}

		tokenCallback();

		if (loadToken()) {
			Intent intent = new Intent(this, TimelineActivity.class);
			for (int i = 0; i < users.size(); i++) {
				intent.putExtra("user" + i, users.get(i));
			}
			startActivity(intent);
		}
		setBtn();
	}

	private boolean tokenCallback() {
		Uri uri = getIntent().getData();
		// System.out.println("callback");
		if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
			String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
			try {
				AccessToken accessToken = twitter.getOAuthAccessToken(
						requestToken, verifier);
				addUser(accessToken);
				Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
			} catch (Exception e) {
				Log.e("Exception on callback", "> " + e.getMessage());
				Log.e("", e.toString());
				Log.e("", "Caused by: " + e.getCause());
				return false;
			}
			return true;
		}
		return false;
	}

	private void loginToTwitter() {
		cd = new ConnectionDetector(getApplicationContext());
		if (!cd.isConnectedToInternet()) {
			// TODO connection error
			return;
		}
		// Set up configuration for bdbenTweet
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		Configuration configuration = builder.build();

		Twitter twitter = new TwitterFactory(configuration).getInstance();

		try {
			// Get a request token and take the user to the appropriate URL
			requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken
					.getAuthenticationURL())));
			addUser(twitter);
		} catch (TwitterException te) {
			te.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Saves a new twitter user in shared preferences and adds it to the list of
	 * users by calling addUser(AccessToken).
	 * 
	 * @param t
	 *            an instance of {@link Twitter} representing the user to add
	 * @throws IllegalStateException
	 * @throws TwitterException
	 * @see #addUser(AccessToken)
	 */
	private void addUser(Twitter t) throws IllegalStateException,
			TwitterException {
		addUser(t.getOAuthAccessToken());
	}

	/**
	 * Saves a new twitter user in shared preferences and adds it to the list of
	 * users.
	 * 
	 * @param accessToken
	 *            an instance of {@link AccessToken} representing the user to
	 *            add
	 * @throws IllegalStateException
	 * @throws TwitterException
	 * @see #addUser(Twitter)
	 */
	private void addUser(AccessToken accessToken) throws IllegalStateException,
			TwitterException {
		// Twitter t = new TwitterFactory().getInstance(accessToken);
		// addUser(t);

		SharedPreferences prefs = getPreferences(MODE_PRIVATE);

		// Find number of users saved
		int i = 0;
		String test = null;
		do {
			test = prefs.getString("com.bdben.trialtwitter.token" + i, null);
			i++;
		} while (test != null);

		String[] user = { accessToken.getToken(), accessToken.getTokenSecret(),
				accessToken.getScreenName() };

		// Save user in shared preferences
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("com.bdben.trialtwitter.token" + i, user[0]);
		editor.putString("com.bdben.trialtwitter.token_secret" + i, user[1]);
		editor.putString("com.bdben.trialtwitter.screenname" + i, user[2]);
		editor.apply();

		// Add to list of users
		users.add(user);
		twitters.add(new TwitterFactory().getInstance(accessToken));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void setBtn() {
		Button btn = (Button) findViewById(R.id.btn_login);
		btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loginToTwitter();
				loadToken();
			}
		});
	}

	/**
	 * Loads the twitter access token from SharedPreferences. It uses the
	 * following preferences, prefixed with package name: <br>
	 * token; <br>
	 * token_secret; and <br>
	 * screenname
	 */
	private boolean loadToken() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		int i = 0;
		while (true) {
			String token = prefs.getString("com.bdben.trialtwitter.token" + i,
					null);
			String secret = prefs.getString(
					"com.bdben.trialtwitter.token_secret" + i, null);
			String name = prefs.getString("com.bdben.trialtwitter.screenname"
					+ i, null);
			if ((token != null) && (secret != null) && (name != null)) {
				String[] user = { name, token, secret };
				users.add(user); // Add screen name and token to list
			} else {
				break;
			}
			i++;
		}
		// Check if token(s) loaded successfully
		if (i == 0 || users.size() == 0) {
			return false;
		}
		return true;
	}
}
