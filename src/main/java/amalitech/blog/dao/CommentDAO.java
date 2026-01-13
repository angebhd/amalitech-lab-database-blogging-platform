package amalitech.blog.dao;

import amalitech.blog.dao.enums.CommentColumn;
import amalitech.blog.model.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Comment entities.
 * Provides CRUD operations for post comments with soft-delete support.
 * Supports nested/reply comments via parent_comment reference.
 * <p>
 * All read operations exclude soft-deleted records by default,
 * but provide overloads to include them when needed (e.g. admin views, audit, recovery).
 * </p>
 */
public class CommentDAO implements DAO<Comment, Long> {

  private final Logger log = LoggerFactory.getLogger(CommentDAO.class);

  /**
   * Creates a new comment in the database and sets the generated ID and timestamps.
   *
   * @param entity the comment to create (will be modified to include generated ID and timestamps)
   * @return the created comment with populated ID and timestamps
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Comment create(Comment entity) {

    final String INSERT = """
                INSERT INTO comments (post_id, user_id, body, parent_comment)
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at, updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(INSERT)) {

      ps.setLong(1, entity.getPostId());
      ps.setLong(2, entity.getUserId());
      ps.setString(3, entity.getBody());
      if (entity.getParentCommentId() != null) {
        ps.setLong(4, entity.getParentCommentId());
      } else {
        ps.setNull(4, Types.BIGINT);
      }

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(rs.getLong("id"));
          entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
      }

      log.info("Comment created successfully - ID: {}, Post: {}, User: {}",
              entity.getId(), entity.getPostId(), entity.getUserId());
      return entity;

    } catch (SQLException e) {
      log.error("Error creating comment", e);
      throw new RuntimeException("Failed to create comment", e);
    }
  }

  /**
   * Retrieves a comment by its primary key (ID), excluding soft-deleted records by default.
   *
   * @param id the unique identifier of the comment
   * @return the matching comment or {@code null} if not found or soft-deleted
   * @throws RuntimeException if a database error occurs
   * @see #get(Long, boolean)
   */
  @Override
  public Comment get(Long id) {
    return get(id, false);
  }

  /**
   * Retrieves a comment by its primary key (ID), with optional inclusion of soft-deleted records.
   *
   * @param id             the unique identifier of the comment
   * @param includeDeleted if {@code true}, returns the comment even if marked as deleted
   * @return the matching comment or {@code null} if not found
   * @throws RuntimeException if a database error occurs
   */
  public Comment get(Long id, boolean includeDeleted) {
    String sql = """
                SELECT id, post_id, user_id, body, parent_comment,
                       created_at, updated_at, is_deleted
                FROM comments
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
          return mapRowToComment(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching comment with id {}", id, e);
      throw new RuntimeException("Failed to fetch comment by id", e);
    }

    return null;
  }

  /**
   * Finds all comments matching the given value in the specified column.
   *
   * @param value          the value to search for (e.g. post_id or user_id as string)
   * @param column         the column to query (from {@link CommentColumn} enum)
   * @param includeDeleted if {@code true}, includes soft-deleted comments
   * @return list of matching comments
   * @throws RuntimeException if a database error occurs
   */
  public List<Comment> findBy(String value, CommentColumn column, boolean includeDeleted) {
    String sql = """
                SELECT id, post_id, user_id, body, parent_comment,
                       created_at, updated_at, is_deleted
                FROM comments
                WHERE %s = ?
            """.formatted(column.name());

    if (!includeDeleted) {
      sql += " AND is_deleted = false";
    }

    List<Comment> comments = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      try {
        ps.setLong(1, Long.parseLong(value));
      } catch (NumberFormatException ex) {
        log.warn("Invalid numeric value for {}: {}", column.name(), value);
        return List.of(); // or throw IllegalArgumentException
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          comments.add(mapRowToComment(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error finding comments by {} = {}", column.name(), value, e);
      throw new RuntimeException("Failed to find comments by " + column.name(), e);
    }

    return comments;
  }

  /**
   * Retrieves exactly one comment matching the given value in the specified column.
   * Throws exception if more than one record is found (data inconsistency).
   *
   * @param value          value to search for
   * @param column         column to query against
   * @param includeDeleted whether to include deleted records
   * @return matching comment or {@code null} if none found
   * @throws IllegalStateException if multiple matches are found
   * @throws RuntimeException      if a database error occurs
   */
  public Comment getBy(String value, CommentColumn column, boolean includeDeleted) {
    List<Comment> results = findBy(value, column, includeDeleted);

    if (results.size() > 1) {
      log.error("Multiple comments found for {} = {}. This indicates a data integrity issue.",
              column.name(), value);
      throw new IllegalStateException(
              "Multiple comments found for " + column.name() + " = " + value);
    }

    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Modern/optional style variant of getBy
   */
  public Optional<Comment> findOneBy(String value, CommentColumn column, boolean includeDeleted) {
    return Optional.ofNullable(getBy(value, column, includeDeleted));
  }

  public List<Comment> findBy(String value, CommentColumn column) {
    return findBy(value, column, false);
  }


  /**
   * Retrieves a paginated list of comments, excluding soft-deleted records by default.
   *
   * @param page     1-based page number
   * @param pageSize number of records per page
   * @return paginated list of comments
   * @throws RuntimeException if a database error occurs
   * @see #getAll(int, int, boolean)
   */
  @Override
  public List<Comment> getAll(int page, int pageSize) {
    return getAll(page, pageSize, false);
  }

  /**
   * Retrieves a paginated list of comments with optional inclusion of soft-deleted records.
   *
   * @param page           1-based page number
   * @param pageSize       number of records per page
   * @param includeDeleted if {@code true}, includes soft-deleted comments
   * @return paginated list of comments
   * @throws RuntimeException if a database error occurs
   */
  public List<Comment> getAll(int page, int pageSize, boolean includeDeleted) {
    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    String sql = """
                SELECT id, post_id, user_id, body, parent_comment,
                       created_at, updated_at, is_deleted
                FROM comments
            """;

    if (!includeDeleted) {
      sql += " WHERE is_deleted = false";
    }

    sql += """
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
            """;

    List<Comment> comments = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          comments.add(mapRowToComment(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated comments (page={}, size={}, includeDeleted={})",
              effectivePage, effectivePageSize, includeDeleted, e);
      throw new RuntimeException("Failed to fetch comments", e);
    }

    return comments;
  }

  /**
   * Convenience method: first page (1), 100 records, excludes deleted comments.
   *
   * @return list of up to 100 most recently created non-deleted comments
   */
  public List<Comment> getAll() {
    return getAll(1, 100, false);
  }

  /**
   * Updates the body of an existing comment.
   * Automatically updates the updated_at timestamp.
   *
   * @param id     the ID of the comment to update
   * @param entity the updated comment data (only body is updated)
   * @return the updated entity if the update succeeded, {@code null} if comment not found or was deleted
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Comment update(Long id, Comment entity) {

    final String UPDATE = """
                UPDATE comments
                SET body = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
                RETURNING updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(UPDATE)) {

      ps.setString(1, entity.getBody());
      ps.setLong(2, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(id);
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
          log.info("Comment updated successfully - ID: {}", id);
          return entity;
        }
      }

      log.warn("No comment found to update with id {}", id);
      return null;

    } catch (SQLException e) {
      log.error("Error updating comment with id {}", id, e);
      throw new RuntimeException("Failed to update comment", e);
    }
  }

  /**
   * Soft-deletes a comment by setting is_deleted = true and recording deletion timestamp.
   * Note: Due to ON DELETE CASCADE constraints in the schema,
   * hard-deleting a parent comment or post will affect child comments.
   *
   * @param id the ID of the comment to delete
   * @return {@code true} if the comment was found and marked as deleted, {@code false} otherwise
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public boolean delete(Long id) {

    final String DELETE = """
                UPDATE comments
                SET is_deleted = true,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;

      if (deleted) {
        log.info("Comment soft-deleted successfully - ID: {}", id);
      } else {
        log.warn("Comment not found or already deleted - ID: {}", id);
      }

      return deleted;

    } catch (SQLException e) {
      log.error("Error soft-deleting comment with id {}", id, e);
      throw new RuntimeException("Failed to delete comment", e);
    }
  }

  /**
   * Maps a ResultSet row to a Comment object.
   *
   * @param rs the result set positioned at the current row
   * @return populated Comment instance
   * @throws SQLException if column access fails
   */
  private Comment mapRowToComment(ResultSet rs) throws SQLException {
    Comment comment = new Comment();
    comment.setId(rs.getLong("id"));
    comment.setPostId(rs.getLong("post_id"));
    comment.setUserId(rs.getLong("user_id"));
    comment.setBody(rs.getString("body"));

    long parentId = rs.getLong("parent_comment");
    if (!rs.wasNull()) {
      comment.setParentCommentId(parentId);
    }

    comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    comment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    comment.setDeleted(rs.getBoolean("is_deleted"));
    return comment;
  }
}