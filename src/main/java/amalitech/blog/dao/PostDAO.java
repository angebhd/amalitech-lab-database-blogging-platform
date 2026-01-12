package amalitech.blog.dao;

import amalitech.blog.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Post entities.
 * Provides CRUD operations for blog posts with soft-delete support.
 */
public class PostDAO implements DAO<Post, Long> {

  private final Logger log = LoggerFactory.getLogger(PostDAO.class);

  /**
   * Creates a new post in the database and sets the generated ID on the entity.
   *
   * @param entity the post to create (will be modified to include generated ID)
   * @return the created post with populated ID
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Post create(Post entity) {

    final String INSERT = """
                INSERT INTO posts (author_id, title, body)
                VALUES (?, ?, ?)
                RETURNING id, created_at, updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(INSERT)) {

      ps.setLong(1, entity.getAuthorId());
      ps.setString(2, entity.getTitle());
      ps.setString(3, entity.getBody());

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(rs.getLong("id"));
          entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
      }

      log.info("Post created successfully - ID: {}, Title: {}", entity.getId(), entity.getTitle());
      return entity;

    } catch (SQLException e) {
      log.error("Error creating post", e);
      throw new RuntimeException("Failed to create post", e);
    }
  }

  /**
   * Retrieves a post by its ID, excluding soft-deleted records.
   *
   * @param id the ID of the post to retrieve
   * @return the post if found and not deleted, otherwise {@code null}
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Post get(Long id) {

    final String SELECT_BY_ID = """
                SELECT id, author_id, title, body, created_at, updated_at, is_deleted
                FROM posts
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToPost(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching post with id {}", id, e);
      throw new RuntimeException("Failed to fetch post", e);
    }

    return null;
  }

  /**
   * Retrieves a paginated list of all non-deleted posts, ordered by creation date descending.
   * Page numbering starts at 1.
   *
   * @param page     the page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize the number of records per page, defaults to 100 if ≤ 0
   * @return list of posts for the requested page (may be empty)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<Post> getAll(int page, int pageSize) {

    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT id, author_id, title, body, created_at, updated_at, is_deleted
                FROM posts
                WHERE is_deleted = false
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """;

    List<Post> posts = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          posts.add(mapRowToPost(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated posts (page={}, size={})", effectivePage, effectivePageSize, e);
      throw new RuntimeException("Failed to fetch posts", e);
    }

    return posts;
  }

  /**
   * Convenience method for getting the first page with default page size (100).
   *
   * @return list of up to 100 most recently created non-deleted posts
   * @throws RuntimeException if a database error occurs
   */
  public List<Post> getAll() {
    return getAll(1, 100);
  }

  /**
   * Updates an existing post's title and body.
   * Automatically updates the updated_at timestamp.
   *
   * @param id     the ID of the post to update
   * @param entity the updated post data (author_id is not updated)
   * @return the updated entity if the update succeeded, {@code null} if post not found or was deleted
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Post update(Long id, Post entity) {

    final String UPDATE = """
                UPDATE posts
                SET title = ?,
                    body = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
                RETURNING updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(UPDATE)) {

      ps.setString(1, entity.getTitle());
      ps.setString(2, entity.getBody());
      ps.setLong(3, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(id);
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
          log.info("Post updated successfully - ID: {}", id);
          return entity;
        }
      }

      log.warn("No post found to update with id {}", id);
      return null;

    } catch (SQLException e) {
      log.error("Error updating post with id {}", id, e);
      throw new RuntimeException("Failed to update post", e);
    }
  }

  /**
   * Soft-deletes a post by setting is_deleted = true and recording deletion timestamp.
   *
   * @param id the ID of the post to delete
   * @return {@code true} if the post was found and marked as deleted, {@code false} otherwise
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public boolean delete(Long id) {

    final String DELETE = """
                UPDATE posts
                SET is_deleted = true,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;

      if (deleted) {
        log.info("Post soft-deleted successfully - ID: {}", id);
      } else {
        log.warn("Post not found or already deleted - ID: {}", id);
      }

      return deleted;

    } catch (SQLException e) {
      log.error("Error soft-deleting post with id {}", id, e);
      throw new RuntimeException("Failed to delete post", e);
    }
  }

  /**
   * Maps a ResultSet row to a Post object.
   *
   * @param rs the result set positioned at the current row
   * @return populated Post instance
   * @throws SQLException if column access fails
   */
  private Post mapRowToPost(ResultSet rs) throws SQLException {
    Post post = new Post();
    post.setId(rs.getLong("id"));
    post.setAuthorId(rs.getLong("author_id"));
    post.setTitle(rs.getString("title"));
    post.setBody(rs.getString("body"));
    post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    post.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    post.setDeleted(rs.getBoolean("is_deleted"));
    return post;
  }
}