package amalitech.blog.dao;

import amalitech.blog.dao.enums.PostColumn;
import amalitech.blog.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Post entities.
 * Provides CRUD operations for blog posts with soft-delete support.
 * <p>
 * All read operations exclude soft-deleted records by default,
 * but provide overloads to include them when needed (e.g. admin views, audit, recovery).
 * </p>
 */
public class PostDAO implements DAO<Post, Long> {

  private final Logger log = LoggerFactory.getLogger(PostDAO.class);

  /**
   * Creates a new post in the database and sets the generated ID and timestamps on the entity.
   *
   * @param entity the post to create (will be modified to include generated ID and timestamps)
   * @return the same entity instance with generated fields populated
   * @throws RuntimeException if a database error occurs during insertion
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
   * Retrieves a post by its primary key (ID), excluding soft-deleted records by default.
   *
   * @param id the unique identifier of the post
   * @return the matching post or {@code null} if not found or soft-deleted
   * @throws RuntimeException if a database error occurs
   * @see #get(Long, boolean)
   */
  @Override
  public Post get(Long id) {
    return get(id, false);
  }

  /**
   * Retrieves a post by its primary key (ID), with optional inclusion of soft-deleted records.
   *
   * @param id             the unique identifier of the post
   * @param includeDeleted if {@code true}, returns the post even if marked as deleted
   * @return the matching post or {@code null} if not found
   * @throws RuntimeException if a database error occurs
   */
  public Post get(Long id, boolean includeDeleted) {
    String sql = """
                SELECT id, author_id, title, body, created_at, updated_at, is_deleted
                FROM posts
                WHERE id = ?
            """;

    if (!includeDeleted) {
      sql += " AND is_deleted = false";
    }

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToPost(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching post with id {}", id, e);
      throw new RuntimeException("Failed to fetch post by id", e);
    }

    return null;
  }

  /**
   * Finds all posts matching the given value in the specified column.
   *
   * @param value          the value to search for (e.g. title string, author_id as string)
   * @param column         the column to query (from {@link PostColumn} enum)
   * @param includeDeleted if {@code true}, includes soft-deleted posts
   * @return list of matching posts (typically 0–many)
   * @throws RuntimeException if a database error occurs
   */
  public List<Post> findBy(String value, PostColumn column, boolean includeDeleted) {
    String sql = """
                SELECT id, author_id, title, body, created_at, updated_at, is_deleted
                FROM posts
                WHERE %s = ?
            """.formatted(column.name());

    if (!includeDeleted) {
      sql += " AND is_deleted = false";
    }

    List<Post> posts = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      // Handle type conversion based on column
      if (column == PostColumn.AUTHOR_ID) {
        try {
          ps.setLong(1, Long.parseLong(value));
        } catch (NumberFormatException ex) {
          log.warn("Invalid numeric value for author_id: {}", value);
          return List.of(); // or throw IllegalArgumentException
        }
      } else {
        ps.setString(1, value);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          posts.add(mapRowToPost(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error finding posts by {} = {}", column.name(), value, e);
      throw new RuntimeException("Failed to find posts by " + column.name(), e);
    }

    return posts;
  }

  /**
   * Retrieves exactly one post matching the given value in the specified column.
   * Throws exception if more than one record is found (data inconsistency).
   *
   * @param value          value to search for
   * @param column         column to query against
   * @param includeDeleted whether to include deleted records
   * @return matching post or {@code null} if none found
   * @throws IllegalStateException if multiple matches are found
   * @throws RuntimeException      if a database error occurs
   */
  public Post getBy(String value, PostColumn column, boolean includeDeleted) {
    List<Post> results = findBy(value, column, includeDeleted);

    if (results.size() > 1) {
      log.error("Multiple posts found for {} = {}. This indicates a data integrity issue.",
              column.name(), value);
      throw new IllegalStateException(
              "Multiple posts found for " + column.name() + " = " + value);
    }

    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Modern/optional style variant of getBy
   */
  public Optional<Post> findOneBy(String value, PostColumn column, boolean includeDeleted) {
    return Optional.ofNullable(getBy(value, column, includeDeleted));
  }

  // Convenience overloads — exclude deleted by default
  public List<Post> findBy(String value, PostColumn column) {
    return findBy(value, column, false);
  }

  public Post getBy(String value, PostColumn column) {
    return getBy(value, column, false);
  }

  public Optional<Post> findOneBy(String value, PostColumn column) {
    return findOneBy(value, column, false);
  }

  /**
   * Retrieves a paginated list of posts, excluding soft-deleted records by default.
   *
   * @param page     1-based page number
   * @param pageSize number of records per page
   * @return paginated list of posts
   * @throws RuntimeException if a database error occurs
   * @see #getAll(int, int, boolean)
   */
  @Override
  public List<Post> getAll(int page, int pageSize) {
    return getAll(page, pageSize, false);
  }

  /**
   * Retrieves a paginated list of posts with optional inclusion of soft-deleted records.
   *
   * @param page           1-based page number
   * @param pageSize       number of records per page
   * @param includeDeleted if {@code true}, includes soft-deleted posts
   * @return paginated list of posts
   * @throws RuntimeException if a database error occurs
   */
  public List<Post> getAll(int page, int pageSize, boolean includeDeleted) {
    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    String sql = """
                SELECT id, author_id, title, body, created_at, updated_at, is_deleted
                FROM posts
            """;

    if (!includeDeleted) {
      sql += " WHERE is_deleted = false";
    }

    sql += """
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
            """;

    List<Post> posts = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          posts.add(mapRowToPost(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated posts (page={}, size={}, includeDeleted={})",
              effectivePage, effectivePageSize, includeDeleted, e);
      throw new RuntimeException("Failed to fetch posts", e);
    }

    return posts;
  }

  /**
   * Convenience method: first page (1), 100 records, excludes deleted posts.
   *
   * @return list of up to 100 most recently created non-deleted posts
   */
  public List<Post> getAll() {
    return getAll(1, 100, false);
  }

  /**
   * Updates an existing post's title and body.
   * Automatically updates the updated_at timestamp.
   * Author cannot be changed via this method.
   *
   * @param id     ID of the post to update
   * @param entity updated post data
   * @return updated entity or {@code null} if not found or deleted
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
   * @param id ID of the post to soft-delete
   * @return {@code true} if the post was found and marked deleted, {@code false} otherwise
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