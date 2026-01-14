package amalitech.blog.controller.posts;

import amalitech.blog.ApplicationContext;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Comment;
import amalitech.blog.model.Review;
import amalitech.blog.model.Tag;
import amalitech.blog.service.CommentService;
import amalitech.blog.service.ReviewService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class PostDetailController {

  @FXML private Label authorLabel;
  @FXML private Label dateLabel;
  @FXML private Label titleLabel;
  @FXML private Label bodyLabel;
  @FXML private Label reviewsLabel;
  @FXML private HBox tagsContainer;
  @FXML private VBox commentsContainer;
  @FXML private TextArea commentArea;
  @FXML private Button editButton;
  @FXML private Button deleteButton;

  private PostDTO postDTO;
  private Long currentUserId;
  private final CommentService commentService = new CommentService();
  private final ReviewService reviewService = new ReviewService();
  private Comment replyingToComment = null; // Track which comment we're replying to

  @FXML
  public void initialize() {
    currentUserId = ApplicationContext.getAuthenticatedUser().getId();
  }

  public void setPost(PostDTO post) {
    this.postDTO = post;
    loadPostData();
  }

  private void loadPostData() {
    if (postDTO == null) return;

    Long authorId = postDTO.getAuthorId();

    String authorName = postDTO.getAuthorName();
    if (authorName.trim().isEmpty()) {
      authorName = "Anonymous";
    }

    LocalDateTime createdAt = postDTO.getPost().getCreatedAt();
    String title = postDTO.getPost().getTitle();
    String body = postDTO.getPost().getBody();

    authorLabel.setText(authorName.trim());
    dateLabel.setText("Posted on " + createdAt.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
    titleLabel.setText(title);
    bodyLabel.setText(body);

    updateReviewsLabel();

    boolean isAuthor = authorId.equals(currentUserId);
    editButton.setVisible(isAuthor);
    editButton.setManaged(isAuthor);
    deleteButton.setVisible(isAuthor);
    deleteButton.setManaged(isAuthor);

    loadTags();
    loadComments();
  }

  private void updateReviewsLabel() {
    List<Review> reviews = reviewService.getByPostId(postDTO.getPost().getId());
    int reviewCount = reviews.size();

    if (reviewCount == 0) {
      reviewsLabel.setText("No reviews yet");
      return;
    }

    // Calculate average rating
    double average = calculateAverageRating(reviews);
    String stars = getStarsForRating(average);

    reviewsLabel.setText(String.format("%s %.1f (%d Review%s)",
            stars, average, reviewCount, reviewCount != 1 ? "s" : ""));
  }

  private double calculateAverageRating(List<Review> reviews) {
    if (reviews.isEmpty()) return 0.0;

    int total = 0;
    for (Review review : reviews) {
      total += getRatingValue(review.getRate());
    }

    return (double) total / reviews.size();
  }

  private int getRatingValue(String rate) {
    return switch (rate) {
      case "ONE" -> 1;
      case "TWO" -> 2;
      case "THREE" -> 3;
      case "FOUR" -> 4;
      case "FIVE" -> 5;
      default -> 0;
    };
  }

  private String getStarsForRating(double rating) {
    int fullStars = (int) rating;
    boolean hasHalfStar = (rating - fullStars) >= 0.5;

    StringBuilder stars = new StringBuilder();
    stars.append("⭐".repeat(Math.max(0, fullStars)));
    if (hasHalfStar && fullStars < 5) {
      stars.append("✨");
    }

    return stars.toString();
  }

  private void loadTags() {
    tagsContainer.getChildren().clear();
    for (Tag tag : postDTO.getTags()) {
      Label tagLabel = new Label(tag.getName());
      tagLabel.setStyle("-fx-background-color: #e7f3ff; -fx-text-fill: #667eea; -fx-background-radius: 15; -fx-padding: 5 15;");
      tagLabel.setFont(Font.font("System", 12));
      tagsContainer.getChildren().add(tagLabel);
    }
  }

  private void loadComments() {
    commentsContainer.getChildren().clear();

    // Get all comments for this post
    List<Comment> allComments = commentService.getByPostId(postDTO.getPost().getId());

    // Filter top-level comments (no parent)
    List<Comment> topLevelComments = allComments.stream()
            .filter(c -> c.getParentCommentId() == null)
            .toList();

    // Display each top-level comment with its replies
    for (Comment comment : topLevelComments) {
      VBox commentBox = createCommentNode(comment, allComments, 0);
      commentsContainer.getChildren().add(commentBox);
    }
  }

  private VBox createCommentNode(Comment comment, List<Comment> allComments, int depth) {
    VBox container = new VBox(10);
    container.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");

    // Add left margin for nested replies
    if (depth > 0) {
      VBox.setMargin(container, new Insets(0, 0, 0, 30 * depth));
    }

    // Header with avatar and user info
    HBox header = new HBox(12);
    header.setAlignment(Pos.CENTER_LEFT);

    Label avatar = new Label("👤");
    avatar.setStyle("-fx-font-size: 24;");

    VBox userInfo = new VBox(2);
    // TODO: Replace with real username lookup from User service
    Label nameLabel = new Label("User " + comment.getUserId());
    nameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

    Label timeLabel = new Label(formatRelativeTime(comment.getCreatedAt()));
    timeLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12;");

    userInfo.getChildren().addAll(nameLabel, timeLabel);
    header.getChildren().addAll(avatar, userInfo);

    // Comment content
    Label contentLabel = new Label(comment.getBody());
    contentLabel.setWrapText(true);
    contentLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14;");

    // Action buttons
    HBox actions = new HBox(15);

    Button replyBtn = new Button("Reply");
    replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #667eea; -fx-font-size: 12; -fx-cursor: hand;");
    replyBtn.setOnAction(e -> startReplyingToComment(comment));

    actions.getChildren().add(replyBtn);

    // Show edit/delete only for comment author
    if (comment.getUserId().equals(currentUserId)) {
      Button editBtn = new Button("Edit");
      editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-size: 12; -fx-cursor: hand;");
      editBtn.setOnAction(e -> startEditingComment(comment));

      Button deleteBtn = new Button("Delete");
      deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; -fx-font-size: 12; -fx-cursor: hand;");
      deleteBtn.setOnAction(e -> handleDeleteComment(comment));

      actions.getChildren().addAll(editBtn, deleteBtn);
    }

    container.getChildren().addAll(header, contentLabel, actions);

    // Get and display replies (nested comments)
    List<Comment> replies = allComments.stream()
            .filter(c -> comment.getId().equals(c.getParentCommentId()))
            .toList();

    if (!replies.isEmpty() && depth < 3) { // Limit nesting to 3 levels
      VBox repliesContainer = new VBox(10);
      repliesContainer.setStyle("-fx-padding: 10 0 0 0;");

      for (Comment reply : replies) {
        VBox replyBox = createCommentNode(reply, allComments, depth + 1);
        repliesContainer.getChildren().add(replyBox);
      }

      container.getChildren().add(repliesContainer);
    }

    return container;
  }

  private void startReplyingToComment(Comment parentComment) {
    replyingToComment = parentComment;
    commentArea.setPromptText("Replying to comment...");
    commentArea.requestFocus();

    // Add cancel reply button if not already present
    HBox commentActionArea = (HBox) commentArea.getParent().lookup(".comment-actions");
    if (commentActionArea == null) {
      commentActionArea = new HBox(10);
      commentActionArea.getStyleClass().add("comment-actions");
      commentActionArea.setAlignment(Pos.CENTER_RIGHT);

      Button cancelReplyBtn = new Button("Cancel Reply");
      cancelReplyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-cursor: hand;");
      cancelReplyBtn.setOnAction(e -> cancelReply());

      int areaIndex = commentArea.getParent().getChildrenUnmodifiable().indexOf(commentArea);
      ((VBox) commentArea.getParent()).getChildren().add(areaIndex, commentActionArea);
      commentActionArea.getChildren().add(cancelReplyBtn);
    }
  }

  private void cancelReply() {
    replyingToComment = null;
    commentArea.setPromptText("Write a comment...");

    // Remove cancel button
    HBox commentActionArea = (HBox) commentArea.getParent().lookup(".comment-actions");
    if (commentActionArea != null) {
      ((VBox) commentArea.getParent()).getChildren().remove(commentActionArea);
    }
  }

  private void startEditingComment(Comment comment) {
    // Create edit dialog
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Edit Comment");
    dialog.setHeaderText("Edit your comment");

    ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    TextArea editArea = new TextArea(comment.getBody());
    editArea.setWrapText(true);
    editArea.setPrefHeight(100);
    editArea.setPrefWidth(400);

    dialog.getDialogPane().setContent(editArea);

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == saveButtonType) {
        return editArea.getText();
      }
      return null;
    });

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(newText -> {
      if (!newText.trim().isEmpty()) {
        comment.setBody(newText.trim());
        commentService.update(comment.getId(), comment);
        loadComments();
      }
    });
  }

  private void handleDeleteComment(Comment comment) {
    // Confirmation dialog
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Delete Comment");
    alert.setHeaderText("Are you sure you want to delete this comment?");
    alert.setContentText("This action cannot be undone.");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      commentService.delete(comment.getId());
      loadComments();
    }
  }

  private String formatRelativeTime(LocalDateTime time) {
    if (time == null) return "just now";

    LocalDateTime now = LocalDateTime.now();
    long minutes = ChronoUnit.MINUTES.between(time, now);
    long hours = ChronoUnit.HOURS.between(time, now);
    long days = ChronoUnit.DAYS.between(time, now);

    if (minutes < 1) return "just now";
    if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
    if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
    if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";

    return time.format(DateTimeFormatter.ofPattern("MMM d 'at' HH:mm"));
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
  private void handleEdit(ActionEvent event) {

    // Load post detail screen
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/posts/update-post.fxml"));
    Scene scene = null;
    try {
      scene = new Scene(fxmlLoader.load(), 900, 700);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Pass post ID to detail controller
    UpdatePostController controller = fxmlLoader.getController();
    controller.setPost(postDTO);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Post Details");
  }

  @FXML
  private void handleDelete(ActionEvent event) throws IOException {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Delete Post");
    alert.setHeaderText("Are you sure you want to delete this post?");
    alert.setContentText("This action cannot be undone. All comments and reviews will also be deleted.");

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
      // TODO: Implement actual delete via PostService
      // postService.delete(postDTO.getPost().getId());
      System.out.println("Delete post → " + postDTO.getPost().getId());
      handleBack(event);
    }
  }

  @FXML
  private void handleReview(ActionEvent event) {
    // Create review dialog
    Dialog<Review> dialog = new Dialog<>();
    dialog.setTitle("Add Review");
    dialog.setHeaderText("Rate this post");
    dialog.initModality(Modality.APPLICATION_MODAL);

    // Dialog content
    VBox content = new VBox(15);
    content.setAlignment(Pos.CENTER);
    content.setPadding(new Insets(20));

    Label ratingLabel = new Label("Select Rating:");
    ratingLabel.setStyle("-fx-font-weight: bold;");

    // Rating buttons (stars)
    HBox starsBox = new HBox(10);
    starsBox.setAlignment(Pos.CENTER);

    ToggleGroup ratingGroup = new ToggleGroup();
    String[] ratings = {"ONE", "TWO", "THREE", "FOUR", "FIVE"};
    String[] starLabels = {"⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"};

    for (int i = 0; i < ratings.length; i++) {
      RadioButton rb = new RadioButton(starLabels[i]);
      rb.setUserData(ratings[i]);
      rb.setToggleGroup(ratingGroup);
      rb.setStyle("-fx-font-size: 16;");
      if (i == 4) rb.setSelected(true); // Default to 5 stars
      starsBox.getChildren().add(rb);
    }

    content.getChildren().addAll(ratingLabel, starsBox);
    dialog.getDialogPane().setContent(content);

    // Buttons
    ButtonType submitButtonType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

    // Result converter
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == submitButtonType) {
        RadioButton selected = (RadioButton) ratingGroup.getSelectedToggle();
        if (selected != null) {
          Review review = new Review();
          review.setPostId(postDTO.getPost().getId());
          review.setUserId(currentUserId);
          review.setRate((String) selected.getUserData());
          return review;
        }
      }
      return null;
    });

    // Show dialog and handle result
    Optional<Review> result = dialog.showAndWait();
    result.ifPresent(review -> {
      // Check if user already reviewed
      List<Review> existingReviews = reviewService.getByPostId(postDTO.getPost().getId());
      Optional<Review> userReview = existingReviews.stream()
              .filter(r -> r.getUserId().equals(currentUserId))
              .findFirst();

      if (userReview.isPresent()) {
        // Update existing review
        Review existing = userReview.get();
        existing.setRate(review.getRate());
        reviewService.update(existing.getId(), existing);

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Review Updated");
        info.setHeaderText(null);
        info.setContentText("Your review has been updated!");
        info.showAndWait();
      } else {
        // Create new review
        reviewService.create(review);

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Review Added");
        info.setHeaderText(null);
        info.setContentText("Thank you for your review!");
        info.showAndWait();
      }

      updateReviewsLabel();
    });
  }

  @FXML
  private void handlePostComment(ActionEvent event) {
    String text = commentArea.getText().trim();
    if (text.isEmpty()) return;

    Comment comment = new Comment();
    comment.setPostId(postDTO.getPost().getId());
    comment.setBody(text);
    comment.setUserId(currentUserId);

    // Set parent comment if replying
    if (replyingToComment != null) {
      comment.setParentCommentId(replyingToComment.getId());
    } else {
      comment.setParentCommentId(null);
    }

    commentService.create(comment);
    commentArea.clear();
    cancelReply(); // Reset reply state
    loadComments();
  }
}