package amalitech.blog.controller.auth;
import amalitech.blog.ApplicationContext;
import amalitech.blog.model.User;
import amalitech.blog.service.UserService;
import amalitech.blog.utils.ValidatorUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SignUpController {

  @FXML
  private TextField usernameField;

  @FXML
  private TextField firstNameField;

  @FXML
  private TextField lastNameField;

  @FXML
  private TextField emailField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private PasswordField confirmPasswordField;

  @FXML
  private Label errorLabel;

  private final UserService userService = new UserService();
  private final Logger log = LoggerFactory.getLogger(SignUpController.class);

  @FXML
  private void handleSignup(ActionEvent event) {
    String username = usernameField.getText().trim();
    String firstName = firstNameField.getText().trim();
    String lastName = lastNameField.getText().trim();
    String email = emailField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword = confirmPasswordField.getText();

    // Validation
    if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
      showError("Username, email, and password are required");
      return;
    }

    if (username.length() > 12 || username.length() < 4) {
      showError("Username must be 4 characters or less");
      return;
    }

    if (!ValidatorUtil.validateEmail(email)) {
      showError("Please enter a valid email address");
      return;
    }

    if (password.length() < 4) {
      showError("Password must be at least 4 characters");
      return;
    }

    if (!password.equals(confirmPassword)) {
      showError("Passwords do not match");
      return;
    }

    try {
      User user = registerUser(username, firstName, lastName, email, password);
      if (user != null) {
        goToFeed(event);
      } else {
        showError("Username or email already exists");
      }
    } catch (Exception e) {
      showError("Registration failed: " + e.getMessage());
    }
  }

  @FXML
  private void handleGoToLogin(ActionEvent event)  {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/auth/login.fxml"));
    Scene scene = null;
    try {
      scene = new Scene(fxmlLoader.load(), 800, 600);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Login");
  }

  private void goToFeed(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/home.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Feed");
  }

  private User registerUser(String username, String firstName, String lastName,
                               String email, String password) {
    User user = new User();
    user.setUsername(username);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    user.setPassword(password);
    User newUser = this.userService.create(user);
    ApplicationContext.setAuthenticatedUser(newUser);
    log.info("User with id: {}", newUser.getId());
    return newUser;

  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
  }
}
