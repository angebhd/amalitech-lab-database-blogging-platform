package amalitech.blog.controller.posts;

import amalitech.blog.ApplicationContext;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Post;
import amalitech.blog.model.Tag;
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
import java.util.List;
import java.util.stream.Collectors;

public class UpdatePostController {

  @FXML private TextField titleField;
  @FXML private TextField tagsField;
  @FXML private TextArea bodyArea;
  @FXML private Label errorLabel;
  @FXML private Label successLabel;
  @FXML private Label titleCountLabel;

  private final PostService postService = new PostService();
  private final TagService tagService = new TagService();
  private PostDTO postDTO;
  private Long currentUserId;

  @FXML
  public void initialize() {
    currentUserId = ApplicationContext.getAuthenticatedUser().getId();

    // Add character counter for title
    titleField.textProperty().addListener((observable, oldValue, newValue) -> {
      int length = newValue.length();
      titleCountLabel.setText(length + "/50");

      if (length > 50) {
        titleField.setText(oldValue);
      }
    });
  }

  public void setPost(PostDTO postDTO) {
    this.postDTO = postDTO;
    loadPostData();
  }

  private void loadPostData() {
    if (postDTO == null) return;

    titleField.setText(postDTO.getPost().getTitle());
    bodyArea.setText(postDTO.getPost().getBody());

    // Load tags as comma-separated string
    if (postDTO.getTags() != null && !postDTO.getTags().isEmpty()) {
      String tagsString = postDTO.getTags().stream()
              .map(Tag::getName)
              .collect(Collectors.joining(", "));
      tagsField.setText(tagsString);
    }

    titleCountLabel.setText(postDTO.getPost().getTitle().length() + "/50");
  }

  @FXML
  private void handleCancel(ActionEvent event) throws IOException {
// Go back to post detail
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/posts/post-details.fxml"));
    Scene scene = new Scene(loader.load(), 900, 700);
    PostDetailController controller = loader.getController();
    controller.setPost(postDTO);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Post Details");
  }

  @FXML
  private void handleUpdate(ActionEvent event) {
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
    List<String> tagNames = Arrays.stream(tagsText.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .toList();

    // Update post
    Post post = postDTO.getPost();
    post.setTitle(title);
    post.setBody(body);

    try {
      // Update post in database
      postService.update(post.getId(), post);

      // Update tags
      tagService.updatePostTags(post.getId(), tagNames);

      showSuccess();

      // Wait a moment then redirect to post detail
      new Thread(() -> {
        try {
          Thread.sleep(1000);
          javafx.application.Platform.runLater(() -> {
            try {
              // Reload post data
              PostDTO updatedPost = postService.loadById(post.getId());
              updatedPost.setAuthorId(currentUserId);

              FXMLLoader loader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/posts/post-details.fxml"));
              Scene scene = new Scene(loader.load(), 900, 700);

              PostDetailController controller = loader.getController();
              controller.setPost(updatedPost);

              Stage stage = (Stage) titleField.getScene().getWindow();
              stage.setScene(scene);
              stage.setTitle("B-BLOG - Post Details");
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }).start();

    } catch (Exception e) {
      showError("Failed to update post: " + e.getMessage());
    }
  }

  private void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
    successLabel.setVisible(false);
    successLabel.setManaged(false);
  }

  private void showSuccess() {
    successLabel.setText("Post updated successfully!");
    successLabel.setVisible(true);
    successLabel.setManaged(true);
    errorLabel.setVisible(false);
    errorLabel.setManaged(false);
  }
}
