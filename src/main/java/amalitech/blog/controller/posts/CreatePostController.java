package amalitech.blog.controller.posts;

import amalitech.blog.ApplicationContext;
import amalitech.blog.model.Post;
import amalitech.blog.service.PostService;
import amalitech.blog.service.TagService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CreatePostController {
  @FXML
  private TextField titleField;

  @FXML
  private TextField tagsField;

  @FXML
  private TextArea bodyArea;

  @FXML
  private Label errorLabel;

  @FXML
  private Label successLabel;

  @FXML
  private Label titleCountLabel;

  private PostService postService = new PostService();
  private TagService tagService = new TagService();

  @FXML
  public void initialize() {
    // Add character counter for title
    titleField.textProperty().addListener((observable, oldValue, newValue) -> {
      int length = newValue.length();
      titleCountLabel.setText(length + "/50");

      if (length > 50) {
        titleField.setText(oldValue);
      }
    });
  }

  @FXML
  private void handleBack(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/home.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Home");
  }

  @FXML
  private void handlePublish(ActionEvent event) throws IOException {
    String title = titleField.getText().trim();
    String tagsText = tagsField.getText().trim();
    String body = bodyArea.getText().trim();

    // Validation
    if (title.isEmpty() || body.isEmpty()) {
      showError("Title and content are required");
      return;
    }

    if (title.length() > 50) {
      showError("Title must be 50 characters or less");
      return;
    }

    // Parse tags
    Set<String> tags = Arrays.stream(tagsText.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toSet());

    // Get current user ID from session/auth service
    Long authorId = getCurrentUserId();

    if (savePost(authorId, title, body, tags)) {
      showSuccess("Post published successfully!");

      // Redirect to home after 1 second
      new Thread(() -> {
        try {
          Thread.sleep(1000);
          javafx.application.Platform.runLater(() -> {
            try {
              handleBack(event);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }).start();
    } else {
      showError("Failed to publish post. Please try again.");
    }
  }

  private boolean savePost(Long authorId, String title, String body, Set<String> tags) {
    Post post = new Post();
    post.setAuthorId(authorId);
    post.setTitle(title);
    post.setBody(body);

    this.postService.create(post, tags);

    return true;
  }

  private Long getCurrentUserId() {
    return ApplicationContext.getAuthenticatedUser().getId();
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
