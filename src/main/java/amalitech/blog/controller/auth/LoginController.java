package amalitech.blog.controller.auth;

import amalitech.blog.ApplicationContext;
import amalitech.blog.model.User;
import amalitech.blog.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LoginController {
  @FXML
  private TextField usernameField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Label errorLabel;

  private final UserService userService = new UserService();

  @FXML
  private void handleLogin(ActionEvent event) {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();

    // Validation
    if (username.isEmpty() || password.isEmpty()) {
      showError("Please fill in all fields");
      return;
    }

    User user = this.userService.login(username, password);
    if (user != null) {
      ApplicationContext.setAuthenticatedUser(user);
      try {
        goToFeed(event);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      showError("Invalid email or password");
    }
  }

  @FXML
  private void handleGoToSignup(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/auth/signup.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 800, 600);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Sign Up");
  }

  private void goToFeed(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/home.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Feed");
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
  }
}
