package me.aurous.ui.window;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import com.teamdev.jxbrowser.chromium.javafx.BrowserView;

/**
 * @author Andrew
 *
 */
public class AurousWindowManager {
	private static double xOffset = 0;
	private static double yOffset = 0;

	public static void addResizeListener(final Stage stage,
			final BrowserView browserView) {
		final ResizeListener resizeListener = new ResizeListener(stage,
				browserView);
		stage.getScene()
		.addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener);
		stage.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED,
				resizeListener);
		stage.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED,
				resizeListener);
		final ObservableList<Node> children = stage.getScene().getRoot()
				.getChildrenUnmodifiable();
		for (final Node child : children) {
			addListenerDeeply(child, resizeListener);
		}
	}

	public static void addListenerDeeply(final Node node,
			final EventHandler<MouseEvent> listener) {
		node.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
		node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
		node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
		if (node instanceof Parent) {
			final Parent parent = (Parent) node;
			final ObservableList<Node> children = parent
					.getChildrenUnmodifiable();
			for (final Node child : children) {
				addListenerDeeply(child, listener);
			}
		}
	}

	static class ResizeListener implements EventHandler<MouseEvent> {
		private final Stage stage;
		private final BrowserView browserView;
		private Cursor cursorEvent = Cursor.DEFAULT;
		private final int border = 2;
		private double startX = 0;
		private double startY = 0;

		public ResizeListener(final Stage stage, final BrowserView browserView) {
			this.stage = stage;
			this.browserView = browserView;
		}

		@Override
		public void handle(final MouseEvent mouseEvent) {
			final EventType<? extends MouseEvent> mouseEventType = mouseEvent
					.getEventType();
			final Scene scene = stage.getScene();

			final double mouseEventX = mouseEvent.getSceneX(), mouseEventY = mouseEvent
					.getSceneY(), sceneWidth = scene.getWidth(), sceneHeight = scene
					.getHeight();

			if (MouseEvent.MOUSE_MOVED.equals(mouseEventType) == true) {
				if ((mouseEventX < border) && (mouseEventY < border)) {
					cursorEvent = Cursor.NW_RESIZE;

				} else if ((mouseEventX < border)
						&& (mouseEventY > (sceneHeight - border))) {
					cursorEvent = Cursor.SW_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if ((mouseEventX > (sceneWidth - border))
						&& (mouseEventY < border)) {
					cursorEvent = Cursor.NE_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if ((mouseEventX > (sceneWidth - border))
						&& (mouseEventY > (sceneHeight - border))) {
					cursorEvent = Cursor.SE_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if (mouseEventX < border) {
					cursorEvent = Cursor.W_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if (mouseEventX > (sceneWidth - border)) {
					cursorEvent = Cursor.E_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if (mouseEventY < border) {
					cursorEvent = Cursor.N_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else if (mouseEventY > (sceneHeight - border)) {
					cursorEvent = Cursor.S_RESIZE;
					scene.setCursor(cursorEvent);
					browserView.setCursor(cursorEvent);
				} else {
					cursorEvent = Cursor.DEFAULT;

				}

			} else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType) == true) {
				xOffset = mouseEvent.getSceneX();
				yOffset = mouseEvent.getSceneY();
				startX = stage.getWidth() - mouseEventX;
				startY = stage.getHeight() - mouseEventY;

			} else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType) == true) {
				if (Cursor.DEFAULT.equals(cursorEvent) == true) {
					if (mouseEventY < 25.0) {
						scene.setCursor(Cursor.CLOSED_HAND);
						browserView.setCursor(Cursor.CLOSED_HAND);
						stage.setX(mouseEvent.getScreenX() - xOffset);
						stage.setY(mouseEvent.getScreenY() - yOffset);

					}

				}
				if (Cursor.DEFAULT.equals(cursorEvent) == false) {
					if ((Cursor.W_RESIZE.equals(cursorEvent) == false)
							&& (Cursor.E_RESIZE.equals(cursorEvent) == false)) {
						final double minHeight = stage.getMinHeight() > (border * 2) ? stage
								.getMinHeight() : (border * 2);
								if ((Cursor.NW_RESIZE.equals(cursorEvent) == true)
										|| (Cursor.N_RESIZE.equals(cursorEvent) == true)
										|| (Cursor.NE_RESIZE.equals(cursorEvent) == true)) {
									if ((stage.getHeight() > minHeight)
											|| (mouseEventY < 0)) {
										stage.setHeight((stage.getY() - mouseEvent
												.getScreenY()) + stage.getHeight());
										stage.setY(mouseEvent.getScreenY());
									}
								} else {
									if ((stage.getHeight() > minHeight)
											|| (((mouseEventY + startY) - stage
													.getHeight()) > 0)) {
										stage.setHeight(mouseEventY + startY);
									}
								}
					}

					if ((Cursor.N_RESIZE.equals(cursorEvent) == false)
							&& (Cursor.S_RESIZE.equals(cursorEvent) == false)) {
						final double minWidth = stage.getMinWidth() > (border * 2) ? stage
								.getMinWidth() : (border * 2);
								if ((Cursor.NW_RESIZE.equals(cursorEvent) == true)
										|| (Cursor.W_RESIZE.equals(cursorEvent) == true)
										|| (Cursor.SW_RESIZE.equals(cursorEvent) == true)) {
									if ((stage.getWidth() > minWidth)
											|| (mouseEventX < 0)) {
										stage.setWidth((stage.getX() - mouseEvent
												.getScreenX()) + stage.getWidth());
										stage.setX(mouseEvent.getScreenX());
									}
								} else if (Cursor.S_RESIZE.equals(cursorEvent) == true) {
									if ((stage.getWidth() > minWidth)
											|| (((mouseEventX + startX) - stage
													.getWidth()) > 0)) {
										stage.setWidth(mouseEventX + startX);
									}
								} else {
									if ((stage.getWidth() > minWidth)
											|| (((mouseEventX + startX) - stage
													.getWidth()) > 0)) {
										stage.setWidth(mouseEventX + startX);
									}
								}
					}
				}

			}
		}
	}
}