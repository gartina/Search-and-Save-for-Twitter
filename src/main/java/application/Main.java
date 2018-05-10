package application;

import java.io.File;
import java.io.IOException;

import application.database.DBCollection;
import application.database.DBUserDAO;
import application.database.DatabaseDAO;
import application.exceptions.AccessException;
import application.exceptions.ConnectivityException;
import application.exceptions.DataNotFoundException;
import application.exceptions.DatabaseReadException;
import application.exceptions.DatabaseWriteException;
import application.utils.Browser;
import application.utils.TwitterSessionDAO;
import application.view.LoginViewController;
import application.view.NewHistoricDialogController;
import application.view.SearchViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

public class Main extends Application {

	private static DatabaseDAO databaseDAO;
	private static DBUserDAO dbUserDAO;
	private static TwitterSessionDAO twitterSessionDAO;
	private static Stage primaryStage;
	
	Login login;

	public static DatabaseDAO getDatabaseDAO() {
		return databaseDAO;
	}

	public static void setDatabaseDAO(DatabaseDAO databaseDAO) {
		Main.databaseDAO = databaseDAO;
	}

	public static DBUserDAO getDBUserDAO() {
		return dbUserDAO;
	}

	public static void setDBUserDAO(DBUserDAO dbUserDAO) {
		Main.dbUserDAO = dbUserDAO;
	}

	public static TwitterSessionDAO getTwitterSessionDAO() {
		return twitterSessionDAO;
	}
	
	public static void setTwitterSessionDAO(TwitterSessionDAO twitterSessionDAO) {
		Main.twitterSessionDAO = twitterSessionDAO;
	}
	

	public static Stage getPrimaryStage() {
		return primaryStage;
	}
	
	// @Override
	public void start(Stage primaryStage) {
		Main.primaryStage = primaryStage;
		Main.primaryStage.setTitle("Twitter Searcher");

		showLogin();
	}

	/**
	 * Initializes the login view
	 */
	public void showLogin() {

		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(Main.class.getResource("view/LoginView.fxml"));
		AnchorPane loginView = null;
		try {
			loginView = (AnchorPane) loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Scene scene = new Scene(loginView);
		primaryStage.setScene(scene);
		primaryStage.show();

		LoginViewController controller = loader.getController();
		controller.setMainApp(this);
		controller.setStage(primaryStage);
		login = new Login();
	}

	public void manageNewLogin() throws ConnectivityException, AccessException {
		login.setMainApp(this);
		setDBUserDAO(DBUserDAO.getInstance());
		setTwitterSessionDAO(TwitterSessionDAO.getInstance());
		twitterSessionDAO.setTwitterInstance();
		//this.twitter = login.setTwitterInstance();
		login.createRequest(twitterSessionDAO.getTwitter(), dbUserDAO);
	}

	// Probablemente se pueda retocar mas, ya que nunca saltara al "else" que esta
	// puesto aqui
	public void manageFastLogin(String user) throws ConnectivityException { // FIXME throws DatabaseReadException
		login.setMainApp(this);
		setDBUserDAO(DBUserDAO.getInstance());
		boolean check = false;
		try {
			check = dbUserDAO.checkUser(user);
		} catch (DatabaseReadException | DataNotFoundException e) {
			e.printStackTrace();
		}
		if (check) {
			setTwitterSessionDAO(TwitterSessionDAO.getInstance());
			twitterSessionDAO.setTwitterInstance();
			
			//this.twitter = login.setTwitterInstance();
			login.retrieveSession(twitterSessionDAO.getTwitter(), user, dbUserDAO);
		} else {
			System.out.println("Lo siento, pero este usuario no est� registrado en esta aplicaci�n. Intenta de nuevo.");
		}
	}

	/**
	 * Initializes the webView for the first login
	 */
	public void showWebView(String URL) {

		// create the scene
		Browser browser = new Browser(URL);
		Stage stage = new Stage();
		stage.setTitle("Web View");
		Scene scene = new Scene(browser, 750, 500, Color.web("#666970"));
		stage.setScene(scene);
		stage.show();
		browser.setStage(stage);
		login.retrieveTokens(browser);
	}

	/**
	 * Initializes the search view inside the root layout
	 */
	public void showSearch() {

		// Load login from fxml file
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(Main.class.getResource("view/SearchView.fxml"));
		AnchorPane searchView = new AnchorPane();
		try {
			searchView = (AnchorPane) loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Show the scene containing the search view
		Scene scene = new Scene(searchView);
		primaryStage.setScene(scene);
		primaryStage.show();

		// Give the controller access to the main app
		SearchViewController controller = loader.getController();
		controller.setMainApp(this);
		controller.setUsername(getDBUserDAO().getUser());

	}

	public boolean showNewHistoricSearch(DBCollection c) {

		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(Main.class.getResource("view/NewHistoricDialog.fxml"));
		AnchorPane page = null;
		try {
			page = (AnchorPane) loader.load();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Stage dialogStage = new Stage();
		dialogStage.setTitle("New historic search");
		dialogStage.initOwner(primaryStage);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		NewHistoricDialogController controller = loader.getController();
		controller.setDialogStage(dialogStage);
		controller.setTwitter(twitterSessionDAO.getTwitter());
		controller.setCollection(c);
		
		dialogStage.showAndWait();
		
		
		c.setRepeated(controller.repeatSearch());
		return controller.isOkClicked();

	}

		
	

	public static void main(String[] args) {

		String path = "src/main/resources/twitter.db";
		File file = new File(path);
		setDatabaseDAO(DatabaseDAO.getInstance(path));

		if (!file.exists()) {
			System.out.println("Database does not exist. Create a new one");
			databaseDAO.connect();
			try {
				databaseDAO.createUserTable();
			} catch (DatabaseWriteException e) {
				e.printStackTrace();
			}
			try {
				databaseDAO.createCollectionTable();
			} catch (DatabaseWriteException e) {
				e.printStackTrace();
			}
			try {
				databaseDAO.createTweetTable();
			} catch (DatabaseWriteException e) {
				e.printStackTrace();
			}
		} else {
			try {
				databaseDAO.checkDatabase();
			} catch (DatabaseReadException e) {
				e.printStackTrace();
			}
		}

		launch(args);
	}

	// scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	// //#css!!
}
