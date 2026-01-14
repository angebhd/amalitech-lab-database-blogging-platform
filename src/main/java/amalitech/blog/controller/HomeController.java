package amalitech.blog.controller;

import amalitech.blog.ApplicationContext;
import amalitech.blog.controller.posts.PostDetailController;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Tag;
import amalitech.blog.service.PostService;
import amalitech.blog.service.TagService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HomeController {

  @FXML
  private TextField searchField;

  @FXML
  private ComboBox<String> sortComboBox;

  @FXML
  private VBox postsContainer;

  @FXML
  private VBox tagsButtonsContainer;

  private final PostService postService = new PostService();
  private final TagService tagService = new TagService();
  private List<PostDTO> allPosts; // Cache all posts
  private String currentFilter = "All";

  @FXML
  public void initialize() {
    // Initialize sort options
    sortComboBox.setItems(FXCollections.observableArrayList(
            "Latest", "Most Popular", "Most Commented", "Oldest"
    ));
    sortComboBox.getSelectionModel().selectFirst();

    // Load dynamic tags
    loadDynamicTags();

    // Load posts from database
    loadAndDisplayPosts();

    // Add listener for search
    searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch(newValue));

    // Add listener for sort
    sortComboBox.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> handleSort(newValue));
  }

  private void loadDynamicTags() {
    // Clear existing tags
    tagsButtonsContainer.getChildren().clear();

    // Add "All" button first
    Button allButton = createTagButton("All", true);
    tagsButtonsContainer.getChildren().add(allButton);

    // Load top tags from database
    List<Tag> topTags = tagService.getTop(10); // Load top 10 tags

    // Create button for each tag
    for (Tag tag : topTags) {
      Button tagButton = createTagButton(tag.getName(), false);
      tagsButtonsContainer.getChildren().add(tagButton);
    }
  }

  private Button createTagButton(String tagName, boolean isActive) {
    Button button = new Button(tagName);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setFont(Font.font("System", 14));

    // Set initial style
    if (isActive) {
      button.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
    } else {
      button.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-cursor: hand; -fx-padding: 10;");
    }

    // Add click handler
    button.setOnAction(event -> {
      currentFilter = tagName;
      filterPosts(tagName);
      updateActiveButton(button);
    });

    return button;
  }

  private void loadAndDisplayPosts() {
    allPosts = loadFeed();
    displayPosts(allPosts);
  }

  private void displayPosts(List<PostDTO> posts) {
    // Clear existing posts
    postsContainer.getChildren().clear();

    // If no posts, show message
    if (posts == null || posts.isEmpty()) {
      Label noPostsLabel = new Label("No posts found. Be the first to create one!");
      noPostsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px; -fx-padding: 40;");
      postsContainer.getChildren().add(noPostsLabel);
      return;
    }

    // Create post cards dynamically
    for (PostDTO postDTO : posts) {
      VBox postCard = createPostCard(postDTO);
      postsContainer.getChildren().add(postCard);
    }
  }

  private VBox createPostCard(PostDTO postDTO) {
    // Main card container
    VBox card = new VBox(12);
    card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
    card.setUserData(postDTO);
    card.setOnMouseClicked(event -> {
      try {
        handlePostClick(event);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    // Post Header (Author info)
    HBox header = new HBox(12);
    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

    Label avatar = new Label("👤");
    avatar.setStyle("-fx-font-size: 24;");

    VBox authorInfo = new VBox(2);

    // Get author name (firstName + lastName or username)
    String authorName = getAuthorName(postDTO);
    Label authorNameLabel = new Label(authorName);
    authorNameLabel.setStyle("-fx-text-fill: #2c3e50;");
    authorNameLabel.setFont(Font.font("System Bold", 14));

    // Get time ago and first tag
    String timeAgo = formatTimeAgo(postDTO.getPost().getCreatedAt());
    String tags = getAllTags(postDTO);
    Label postMeta = new Label(timeAgo + (tags != null ? " · " + tags : ""));
    postMeta.setStyle("-fx-text-fill: #6c757d;");
    postMeta.setFont(Font.font("System", 12));

    authorInfo.getChildren().addAll(authorNameLabel, postMeta);
    header.getChildren().addAll(avatar, authorInfo);

    // Post Title
    Label title = new Label(postDTO.getPost().getTitle());
    title.setStyle("-fx-text-fill: #2c3e50;");
    title.setFont(Font.font("System Bold", 18));
    title.setWrapText(true);

    // Post Excerpt (first 150 characters of body)
    String excerpt = postDTO.getPost().getBody();
    if (excerpt != null && excerpt.length() > 150) {
      excerpt = excerpt.substring(0, 150) + "...";
    }
    Label body = new Label(excerpt);
    body.setStyle("-fx-text-fill: #6c757d;");
    body.setFont(Font.font("System", 14));
    body.setWrapText(true);

    // Post Footer (stats)
    HBox footer = new HBox(20);
    footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

    // Likes count (from reviews)
    int reviewsCount = postDTO.getReviews() != null ? postDTO.getReviews().size() : 0;
    Label likes = new Label(reviewsCount + " reviews");
    likes.setStyle("-fx-text-fill: #6c757d;");
    likes.setFont(Font.font("System", 13));

    // Comments count
    int commentsCount = postDTO.getComments() != null ? postDTO.getComments().size() : 0;
    Label comments = new Label(commentsCount + " comments");
    comments.setStyle("-fx-text-fill: #6c757d;");
    comments.setFont(Font.font("System", 13));

    // Read time estimation (rough: 200 words per minute)
    int readTime = calculateReadTime(postDTO.getPost().getBody());
    Label readTimeLabel = new Label("📖 " + readTime + " min read");
    readTimeLabel.setStyle("-fx-text-fill: #6c757d;");
    readTimeLabel.setFont(Font.font("System", 13));

    footer.getChildren().addAll(likes, comments, readTimeLabel);

    // Add all components to card
    card.getChildren().addAll(header, title, body, footer);

    return card;
  }

  private String getAuthorName(PostDTO postDTO) {
    if (postDTO.getAuthor() != null) {
      String firstName = postDTO.getAuthor().getFirstName();
      String lastName = postDTO.getAuthor().getLastName();

      if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
        return firstName + " " + lastName;
      } else if (firstName != null && !firstName.isEmpty()) {
        return firstName;
      } else {
        return postDTO.getAuthor().getUsername();
      }
    }
    return "Anonymous";
  }

  private String getAllTags(PostDTO postDTO) {
    if (postDTO.getTags() != null && !postDTO.getTags().isEmpty()) {
      StringBuilder tags = new StringBuilder();
      postDTO.getTags().forEach(t -> tags.append(t.getName()).append(" "));
      return tags.toString().trim();
    }
    return null;
  }

  private String formatTimeAgo(LocalDateTime createdAt) {
    if (createdAt == null) return "Recently";

    LocalDateTime now = LocalDateTime.now();
    long minutes = ChronoUnit.MINUTES.between(createdAt, now);
    long hours = ChronoUnit.HOURS.between(createdAt, now);
    long days = ChronoUnit.DAYS.between(createdAt, now);
    long weeks = ChronoUnit.WEEKS.between(createdAt, now);
    long months = ChronoUnit.MONTHS.between(createdAt, now);

    if (minutes < 1) return "Just now";
    if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
    if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
    if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
    if (weeks < 4) return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
    if (months < 12) return months + " month" + (months > 1 ? "s" : "") + " ago";

    long years = ChronoUnit.YEARS.between(createdAt, now);
    return years + " year" + (years > 1 ? "s" : "") + " ago";
  }

  private int calculateReadTime(String body) {
    if (body == null || body.isEmpty()) return 1;
    int wordCount = body.split("\\s+").length;
    int minutes = wordCount / 200; // Average reading speed
    return Math.max(minutes, 1);
  }

  @FXML
  private void handleCreatePost(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/posts/create-post.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 900, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Create Post");
  }

  @FXML
  private void handleProfile(ActionEvent event) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/profile.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Profile");
  }

  @FXML
  private void handleLogout(ActionEvent event) throws IOException {
    ApplicationContext.setAuthenticatedUser(null);
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/landing.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Home");
  }

  @FXML
  private void handlePostClick(MouseEvent event) throws IOException {
    VBox clickedPost = (VBox) event.getSource();
    PostDTO post = (PostDTO) clickedPost.getUserData();

    // Load post detail screen
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/amalitech/blog/view/posts/post-details.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 900, 700);

    // Pass post ID to detail controller
    PostDetailController controller = fxmlLoader.getController();
    controller.setPost(post);

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Post Details");
  }

  private void filterPosts(String tagName) {
    if ("All".equals(tagName)) {
      displayPosts(allPosts);
      return;
    }

    // Filter posts by tag
    List<PostDTO> filtered = allPosts.stream()
            .filter(postDTO -> {
              if (postDTO.getTags() != null) {
                return postDTO.getTags().stream()
                        .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));
              }
              return false;
            })
            .toList();

    displayPosts(filtered);
  }

  private void handleSearch(String query) {
    if (query == null || query.trim().isEmpty()) {
      // If search is empty, apply current filter
      filterPosts(currentFilter);
      return;
    }

    String searchLower = query.toLowerCase();

    // Search in title and body
    List<PostDTO> searchResults = allPosts.stream()
            .filter(postDTO -> {
              String title = postDTO.getPost().getTitle().toLowerCase();
              String body = postDTO.getPost().getBody().toLowerCase();
              return title.contains(searchLower) || body.contains(searchLower);
            })
            .toList();

    displayPosts(searchResults);
  }

  private void handleSort(String sortOption) {
    // Get the currently filtered posts
    List<PostDTO> postsToSort = getFilteredPosts();

    // Sort the filtered posts
    List<PostDTO> sortedPosts = new ArrayList<>(postsToSort);

    switch (sortOption) {
      case "Latest":
        sortedPosts.sort((a, b) ->
                b.getPost().getCreatedAt().compareTo(a.getPost().getCreatedAt()));
        break;
      case "Oldest":
        sortedPosts.sort(Comparator.comparing(a -> a.getPost().getCreatedAt()));
        break;
      case "Most Popular":
        sortedPosts.sort((a, b) -> {
          int popularityA = getPopularityScore(a);
          int popularityB = getPopularityScore(b);
          return Integer.compare(popularityB, popularityA);
        });
        break;
      case "Most Commented":
        sortedPosts.sort((a, b) -> {
          int commentsA = a.getComments() != null ? a.getComments().size() : 0;
          int commentsB = b.getComments() != null ? b.getComments().size() : 0;
          return Integer.compare(commentsB, commentsA);
        });
        break;
      default:
    }

    displayPosts(sortedPosts);
  }

  private List<PostDTO> getFilteredPosts() {
    if ("All".equals(currentFilter)) {
      return new ArrayList<>(allPosts);
    }

    // Filter posts by current tag
    return allPosts.stream()
            .filter(postDTO -> {
              if (postDTO.getTags() != null) {
                return postDTO.getTags().stream()
                        .anyMatch(tag -> tag.getName().equalsIgnoreCase(currentFilter));
              }
              return false;
            })
            .collect(Collectors.toList());
  }

  private int getPopularityScore(PostDTO postDTO) {
    int reviews = postDTO.getReviews() != null ? postDTO.getReviews().size() : 0;
    int comments = postDTO.getComments() != null ? postDTO.getComments().size() : 0;
    return reviews + comments;
  }

  private void updateActiveButton(Node clickedButton) {
    // Reset all tag buttons to default style
    for (Node node : tagsButtonsContainer.getChildren()) {
      if (node instanceof Button) {
        node.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-cursor: hand; -fx-padding: 10;");
      }
    }

    // Set clicked button as active
    clickedButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
  }

  private List<PostDTO> loadFeed() {
    return this.postService.loadFeed();
  }
}