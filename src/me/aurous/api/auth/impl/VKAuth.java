package me.aurous.api.auth.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import me.aurous.config.AppConstants;
import me.aurous.exceptions.ExceptionWidget;

import com.teamdev.jxbrowser.chromium.Browser;

public class VKAuth {

	public static final int VK_APP_ID = 4554985;
	public static String VK_APP_MASK = "audio,offline";
	public static final String REDIRECT_URL = "https://oauth.vk.com/blank.html";
	public static final String VK_AUTH_URL = "https://oauth.vk.com/authorize?client_id="
			+ VK_APP_ID + "&scope=" + VK_APP_MASK + "&response_type=token";
	public static final String APPLICATION_TITLE = "Aurous VKEngine Auth";
	public static final String LOGIN_SUCCESS_PAGE = "blank.html#",
			LOGIN_FAILURE_PAGE = "blank.html#error";
	private volatile boolean loginSuccess = false, loginFailure = false;

	private String formData = null;

	private void changeState(final String Url) {
		if (Url.contains(LOGIN_FAILURE_PAGE)) {
			loginFailure = true;
		} else if (Url.contains(LOGIN_SUCCESS_PAGE)) {
			loginSuccess = true;
			try {
				formData = URLDecoder.decode(
						Url.substring(Url.indexOf(LOGIN_SUCCESS_PAGE)
								+ LOGIN_SUCCESS_PAGE.length()), "UTF-8");
			} catch (final UnsupportedEncodingException ex) {
				ExceptionWidget widget = new ExceptionWidget(ex);
				widget.showWidget();
			}
		}
	}

	public void start(final Stage primaryStage, final Browser browser) {
		final Stage authStage = new Stage();
		authStage.initOwner(primaryStage);
		final File f = new File(AppConstants.dataPath() + "vkauth.dat");
		if (f.exists() && !f.isDirectory()) { /* do something */

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Confirmation Dialog");
			alert.setHeaderText("You already have an OAuth key, if you click yes a new one will be generated.");
			alert.setContentText("Are you ok with this?");

			final Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				f.delete();
				browser.executeJavaScript("checkAuths();");
			} else {
				Platform.runLater(() -> authStage.close());
			}
		}

		authStage.setTitle(APPLICATION_TITLE);
		final WebView view = new WebView();
		final WebEngine engine = view.getEngine();
		engine.load(VK_AUTH_URL);
		engine.getLoadWorker()
				.stateProperty()
				.addListener(
						(ChangeListener<State>) (ov, oldState, newState) -> {
							if (newState == State.SUCCEEDED) {
								changeState(engine.getLocation());
							}
						});
		authStage.setScene(new Scene(view));
		authStage.show();

		new Thread(() -> {
			while (!loginSuccess && !loginFailure && authStage.isShowing()) {

			}
			if (loginFailure || (!authStage.isShowing())) {
				Platform.runLater(() -> authStage.close());
			} else {
				try {

					final PrintWriter out = new PrintWriter(
							AppConstants.dataPath() + "vkauth.dat");
					out.println(formData);
					out.close();
					browser.executeJavaScript("checkAuths();");
					Platform.runLater(() -> authStage.close());
				} catch (final IOException ex) {
					ExceptionWidget widget = new ExceptionWidget(ex);
					widget.showWidget();
			}
		}
	}	).start();
	}

}
