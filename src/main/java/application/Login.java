package application;

import java.util.HashMap;
import java.util.Map;

import application.database.DBUserDAO;
import application.exceptions.AccessException;
import application.exceptions.NetworkException;
import application.tasks.LoginTask;
import application.tasks.SignUpTask;
import application.exceptions.DatabaseReadException;
import application.exceptions.DatabaseWriteException;
import application.utils.Browser;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Class to manage any login of the user: sign ups and sign ins
 * @author Maria Cristina, github: cgg09
 *
 */

public class Login {

	private Twitter twitter;
	private AccessToken accessToken = null;
	private RequestToken requestToken = null;
	private WebEngine webEngine;
	private DBUserDAO dbu;
	private int UNAUTHORIZED = 401;

	public Login() {

	}
	
	public RequestToken getRequestToken() {
		return requestToken;
	}

	/**
	 * Request an authorization to Twitter
	 * 
	 * @throws ConnectivityException
	 * @throws AccessException
	 */
	public void createRequest(Twitter twitter, DBUserDAO dbu, SignUpTask signUpTask) throws NetworkException, AccessException {
		
		this.dbu = dbu;
		this.twitter = twitter;

		signUpTask.progressMessage("Creating Twitter request...");
		
		try {
			requestToken = twitter.getOAuthRequestToken(Main.getTwitterSessionDAO().getCallbackUrl());
		} catch (TwitterException e) {
			if (e.getStatusCode() == UNAUTHORIZED) {
				throw new AccessException("401: Unable to get the access token. Please check your credentials.", e);
			} else {
				throw new NetworkException("You do not have internet connection. Please check it out before continue",
						e);
			}
		}

	}

	/**
	 * 2n step from sign up: Retrieve tokens from callback url and redirect to the verifying page
	 */
	public void retrieveTokens(Browser browser) {
		
		webEngine = browser.getWebEngine();
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

			@Override
			public void changed(ObservableValue<? extends javafx.concurrent.Worker.State> observable,
					javafx.concurrent.Worker.State oldState, javafx.concurrent.Worker.State newState) {

				if (newState.equals(javafx.concurrent.Worker.State.FAILED)) {
					String location = webEngine.getLocation();
					if (location.startsWith(Main.getTwitterSessionDAO().getCallbackUrl())) {
						String callbackURLWithTokens = location;
						browser.closeBrowser();
						try {
							verifyTokens(callbackURLWithTokens);
						} catch (AccessException | NetworkException e) {
							e.printStackTrace();
						}

					} else {
						// TODO Pending to show an alert window "Couldn't connect to " + location ...
						
					}
				}

			}
		});
		
	}
	
	/**
	 * Retrieve parameters of a url (to obtain access tokens)
	 * 
	 * @param query
	 * @return info from url
	 */
	public static Map<String, String> getQueryMap(String query) {
		String url = query.substring(query.indexOf("?") + 1);
		String[] params = url.split("&");
		Map<String, String> map = new HashMap<String, String>();

		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, value);
		}
		return map;
	}
	

	/**
	 * 3rd step from sign up: Verify identity and sign in
	 * 
	 * @throws AccessException
	 * @throws ConnectivityException
	 */
	public void verifyTokens(String callbackURL) throws AccessException, NetworkException {
		
		String oauthToken;
		String oauthVerifier;

		Map<String, String> urlMap = getQueryMap(callbackURL);
		oauthToken = urlMap.get("oauth_token");
		oauthVerifier = urlMap.get("oauth_verifier");

		if (oauthVerifier != null && ((requestToken.getToken().toString().equalsIgnoreCase(oauthToken)))) {
			webEngine.getLoadWorker().cancel();
			try {
				accessToken = twitter.getOAuthAccessToken(requestToken, oauthVerifier);
			} catch (TwitterException e) {
				if (e.getStatusCode() == UNAUTHORIZED) {
					throw new AccessException("401: Unable to get the access token. Please check your credentials.", e);
				} else {
					throw new NetworkException(
							"You do not have internet connection. Please check it out before continue", e);
				}
			}
		}

		else {
			// TODO I don't know if this is necessary
		}

		try {
			dbu.saveLogin(twitter.verifyCredentials().getScreenName(), accessToken.getToken().toString(),
					accessToken.getTokenSecret().toString());
		} catch (DatabaseWriteException | TwitterException e) {
			e.printStackTrace();
		}
		Main.showSearch();
	}
	
	

	/**
	 * Retrieve session when the user signs in
	 * 
	 * @param twitter
	 * @param user
	 * 
	 * @throws AccessException
	 * @throws ConnectivityException
	 * @throws DatabaseReadException
	 */
	public void retrieveSession(Twitter twitter, String user, DBUserDAO dbu, LoginTask loginTask)
			throws NetworkException, AccessException {

		this.dbu = dbu;

		String token = null;

		try {
			token = dbu.getUserData("access_token", user);
		} catch (DatabaseReadException e) {
			e.printStackTrace();
		}
		String secret = null;
		try {
			secret = dbu.getUserData("access_secret", user);
		} catch (DatabaseReadException e) {
			e.printStackTrace();
		}

		AccessToken at = new AccessToken(token, secret);
		twitter.setOAuthAccessToken(at);
		
		loginTask.progressMessage("Keys retrieved");

		try {
			twitter.verifyCredentials().getId();
		} catch (TwitterException e1) {
			if (e1.getStatusCode() == UNAUTHORIZED) {
				throw new AccessException(e1.getErrorMessage(), e1);
			}
			throw new NetworkException("You do not have internet connection. Please check it out before continue", e1);
		}
		
		loginTask.progressMessage("Showing search menu...");
	}

	
}