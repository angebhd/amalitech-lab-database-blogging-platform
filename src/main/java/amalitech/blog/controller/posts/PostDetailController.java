package amalitech.blog.controller.posts;

import amalitech.blog.ApplicationContext;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Comment;
import amalitech.blog.model.Tag;
import amalitech.blog.service.CommentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    Long authorId = postDTO.getAuthor().getId();
    String authorName = postDTO.getAuthor().getFirstName() + " " + postDTO.getAuthor().getLastName();
    LocalDateTime createdAt = postDTO.getPost().getCreatedAt();
    String title = postDTO.getPost().getTitle();
    String body  = postDTO.getPost().getBody();

    authorLabel.setText(authorName);
    dateLabel.setText("Posted on " + createdAt.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
    titleLabel.setText(title);
    bodyLabel.setText(body);

    reviewsLabel.setText(postDTO.getReviews().size() + " Reviews");

    boolean isAuthor = authorId.equals(currentUserId);
    editButton.setVisible(isAuthor);
    editButton.setManaged(isAuthor);
    deleteButton.setVisible(isAuthor);
    deleteButton.setManaged(isAuthor);

    loadTags();
    loadComments();
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

    List<Comment> comments = commentService.getByPostId(postDTO.getPost().getId());

    for (Comment comment : comments) {
      VBox commentBox = createCommentNode(comment);
      commentsContainer.getChildren().add(commentBox);
    }
  }

  private VBox createCommentNode(Comment comment) {
    VBox container = new VBox(10);
    container.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");

    HBox header = new HBox(12);
    header.setAlignment(Pos.CENTER_LEFT);

    Label avatar = new Label("👤");
    avatar.setStyle("-fx-font-size: 24;");

    VBox userInfo = new VBox(2);
    // TODO: Replace with real username lookup service
    Label nameLabel = new Label("User " + comment.getUserId());
    nameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

    Label timeLabel = new Label(formatRelativeTime(comment.getCreatedAt()));
    timeLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12;");

    userInfo.getChildren().addAll(nameLabel, timeLabel);
    header.getChildren().addAll(avatar, userInfo);

    Label contentLabel = new Label(comment.getBody());
    contentLabel.setWrapText(true);
    contentLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14;");

    HBox actions = new HBox(15);

    Button replyBtn = new Button("Reply");
    replyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #667eea; -fx-font-size: 12; -fx-cursor: hand;");

    if (comment.getUserId().equals(currentUserId)) {
      Button editBtn = new Button("Edit");
      editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-size: 12; -fx-cursor: hand;");
      editBtn.setOnAction(e -> startEditingComment(comment, contentLabel, container));
      actions.getChildren().add(editBtn);
    }

    actions.getChildren().add(replyBtn);

    container.getChildren().addAll(header, contentLabel, actions);
    return container;
  }

  private void startEditingComment(Comment comment, Label contentLabel, VBox container) {
    int contentIndex = container.getChildren().indexOf(contentLabel);

    TextArea editArea = new TextArea(comment.getBody());
    editArea.setWrapText(true);
    editArea.setPrefHeight(60);

    HBox buttonBar = new HBox(10);
    buttonBar.setAlignment(Pos.CENTER_RIGHT);

    Button saveBtn = new Button("Save");
    saveBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white;");
    saveBtn.setOnAction(e -> {
      String newText = editArea.getText().trim();
      if (!newText.isEmpty()) {
        comment.setBody(newText);
        this.commentService.update(comment.getId(), comment);
        loadComments();
      }
    });

    Button cancelBtn = new Button("Cancel");
    cancelBtn.setOnAction(e -> loadComments());

    buttonBar.getChildren().addAll(saveBtn, cancelBtn);

    container.getChildren().remove(contentLabel);
    container.getChildren().add(contentIndex, editArea);
    container.getChildren().add(contentIndex + 1, buttonBar);
  }

  private String formatRelativeTime(LocalDateTime time) {
    if (time == null) return "just now";
    // Very basic — improve with proper relative time library or logic later
    return time.format(DateTimeFormatter.ofPattern("MMM d 'at' HH:mm"));
  }

  // ────────────────────────────────────────
  //              Event Handlers
  // ────────────────────────────────────────

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
    // TODO: open edit post view
    System.out.println("Edit post → " + postDTO.getPost().getId());
  }

  @FXML
  private void handleDelete(ActionEvent event) throws IOException {
    // TODO: confirmation dialog + actual delete
    System.out.println("Delete post → " + postDTO.getPost().getId());
    handleBack(event);
  }

  @FXML
  private void handleReview(ActionEvent event) {
    // TODO: open review/rating dialog (stars + optional text)
    System.out.println("Add review for post → " + postDTO.getPost().getId());

    // Placeholder fake update
    int current = postDTO.getReviews().size();
    reviewsLabel.setText((current + 1) + " Reviews");
  }

  @FXML
  private void handlePostComment(ActionEvent event) {
    String text = commentArea.getText().trim();
    if (text.isEmpty()) return;

    Comment comment = new Comment();
    comment.setParentCommentId(null);
    comment.setPostId(postDTO.getPost().getId());
    comment.setBody(text);
    comment.setUserId(currentUserId);

    commentService.create(comment);
    commentArea.clear();
    loadComments();
  }
}