package amalitech.blog.controller;

import amalitech.blog.ApplicationContext;
import amalitech.blog.model.User;
import amalitech.blog.service.UserService;
import amalitech.blog.service.PostService;
import amalitech.blog.service.CommentService;
import amalitech.blog.service.ReviewService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class ProfileController {

  @FXML private Label usernameDisplayLabel;
  @FXML private Label joinDateLabel;
  @FXML private Label postsCountLabel;
  @FXML private Label commentsCountLabel;
  @FXML private Label reviewsCountLabel;
  @FXML private TextField usernameField;
  @FXML private TextField firstNameField;
  @FXML private TextField lastNameField;
  @FXML private TextField emailField;
  @FXML private PasswordField currentPasswordField;
  @FXML private PasswordField newPasswordField;
  @FXML private PasswordField confirmPasswordField;
  @FXML private Label errorLabel;
  @FXML private Label successLabel;

  private final UserService userService = new UserService();
  private final PostService postService = new PostService();
  private final CommentService commentService = new CommentService();
  private final ReviewService reviewService = new ReviewService();

  private User currentUser;

  @FXML
  public void initialize() {
    currentUser = ApplicationContext.getAuthenticatedUser();
    loadUserProfile();
    loadUserStats();
  }

  private void loadUserProfile() {
    if (currentUser == null) return;

    usernameDisplayLabel.setText("@" + currentUser.getUsername());
    usernameField.setText(currentUser.getUsername());
    firstNameField.setText(currentUser.getFirstName());
    lastNameField.setText(currentUser.getLastName());
    emailField.setText(currentUser.getEmail());

    if (currentUser.getCreatedAt() != null) {
      joinDateLabel.setText("Member since " +
              currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }
  }

  private void loadUserStats() {
    if (currentUser == null) return;

    // Get counts from services
    Map<String, Integer> userStats = this.userService.getUserStats(currentUser.getId());
    int postsCount = userStats.get("postCount");
    int commentsCount = userStats.get("commentsCount");
    int reviewsCount = userStats.get("reviewsCount");

    postsCountLabel.setText(String.valueOf(postsCount));
    commentsCountLabel.setText(String.valueOf(commentsCount));
    reviewsCountLabel.setText(String.valueOf(reviewsCount));
  }

  @FXML
  private void handleBack(ActionEvent event) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/home.fxml"));
    Scene scene = new Scene(loader.load(), 1000, 700);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Home");
  }

  @FXML
  private void handleUpdateProfile(ActionEvent event) {
    String username = usernameField.getText().trim();
    String firstName = firstNameField.getText().trim();
    String lastName = lastNameField.getText().trim();
    String email = emailField.getText().trim();

    // Validation
    if (username.isEmpty() || email.isEmpty()) {
      showError("Username and email are required");
      return;
    }

    if (username.length() > 12 || username.length() < 3) {
      showError("Username must be between 4 & 12 characters");
      return;
    }

    if (!isValidEmail(email)) {
      showError("Please enter a valid email address");
      return;
    }

    // Update user
    currentUser.setUsername(username);
    currentUser.setFirstName(firstName);
    currentUser.setLastName(lastName);
    currentUser.setEmail(email);

    try {
      userService.update(currentUser.getId(), currentUser);
      ApplicationContext.setAuthenticatedUser(currentUser);

      showSuccess("Profile updated successfully!");
      loadUserProfile();
    } catch (Exception e) {
      showError("Failed to update profile: " + e.getMessage());
    }
  }

  @FXML
  private void handleChangePassword(ActionEvent event) {
    String currentPassword = currentPasswordField.getText();
    String newPassword = newPasswordField.getText();
    String confirmPassword = confirmPasswordField.getText();

    // Validation
    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
      showError("All password fields are required");
      return;
    }

    if (newPassword.length() < 4) {
      showError("New password must be at least 4 characters");
      return;
    }

    if (!newPassword.equals(confirmPassword)) {
      showError("New passwords do not match");
      return;
    }

    try {
      userService.updatePassword(currentUser.getId(), currentPassword, newPassword);

      showSuccess("Password changed successfully!");

      currentPasswordField.clear();
      newPasswordField.clear();
      confirmPasswordField.clear();
    } catch (Exception e) {
      showError("Failed to change password: " + e.getMessage());
    }
  }

  @FXML
  private void handleDeleteAccount(ActionEvent event) throws IOException {
    // Confirmation dialog
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Delete Account");
    alert.setHeaderText("Are you absolutely sure?");
    alert.setContentText("This will permanently delete your account and all your data:\n" +
            "• All your posts\n" +
            "• All your comments\n" +
            "• All your reviews\n\n" +
            "This action cannot be undone!");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      // Second confirmation
      TextInputDialog confirmDialog = new TextInputDialog();
      confirmDialog.setTitle("Final Confirmation");
      confirmDialog.setHeaderText("Type 'DELETE' to confirm");
      confirmDialog.setContentText("Type DELETE:");

      Optional<String> confirmResult = confirmDialog.showAndWait();
      if (confirmResult.isPresent() && confirmResult.get().equals("DELETE")) {
        try {
          userService.delete(currentUser.getId());
          ApplicationContext.setAuthenticatedUser(null);

          // Redirect to landing page
          FXMLLoader loader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/landing.fxml"));
          Scene scene = new Scene(loader.load(), 1000, 700);
          Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
          stage.setScene(scene);
          stage.setTitle("B-BLOG - Home");
        } catch (Exception e) {
          showError("Failed to delete account: " + e.getMessage());
        }
      }
    }
  }

  private boolean isValidEmail(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
    successLabel.setVisible(false);
    successLabel.setManaged(false);
  }

  private void showSuccess(String message) {
    successLabel.setText(message);
    successLabel.setVisible(true);
    successLabel.setManaged(true);
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);
  }
}

