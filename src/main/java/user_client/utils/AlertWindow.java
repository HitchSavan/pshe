package user_client.utils;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class AlertWindow {
    public static Alert alertWindow = new Alert(AlertType.ERROR);

    public static void showErrorWindow(String message) {
        alertWindow.setAlertType(AlertType.ERROR);
        showWindow(message);
    }
    public static boolean showConfirmationWindow(String message) {
        alertWindow.setAlertType(AlertType.CONFIRMATION);
        showWindow(message);
        
        Optional<ButtonType> buttonType = alertWindow.showAndWait();
        return (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK));
    }
    public static void showInfoWindow(String message) {
        alertWindow.setAlertType(AlertType.INFORMATION);
        showWindow(message);
    }
    public static void showWarningWindow(String message) {
        alertWindow.setAlertType(AlertType.WARNING);
        showWindow(message);
    }
    private static void showWindow(String message) {
        alertWindow.setContentText(message);
        alertWindow.show();
    }
}
