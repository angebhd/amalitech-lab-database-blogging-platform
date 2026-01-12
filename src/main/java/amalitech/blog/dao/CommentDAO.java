package amalitech.blog.dao;

import amalitech.blog.model.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Comment entities.
 * Provides CRUD operations for post comments with soft-delete support.
 * Supports nested/reply comments via parent_comment reference.
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
   * Retrieves a comment by its ID, excluding soft-deleted records.
   *
   * @param id the ID of the comment to retrieve
   * @return the comment if found and not deleted, otherwise {@code null}
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Comment get(Long id) {

    final String SELECT_BY_ID = """
                SELECT id, post_id, user_id, body, parent_comment,
                       created_at, updated_at, is_deleted
                FROM comments
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToComment(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching comment with id {}", id, e);
      throw new RuntimeException("Failed to fetch comment", e);
    }

    return null;
  }

  /**
   * Retrieves a paginated list of all non-deleted comments,
   * ordered by creation date descending.
   * Page numbering starts at 1.
   *
   * @param page     the page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize the number of records per page, defaults to 100 if ≤ 0
   * @return list of comments for the requested page (may be empty)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<Comment> getAll(int page, int pageSize) {

    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT id, post_id, user_id, body, parent_comment,
                       created_at, updated_at, is_deleted
                FROM comments
                WHERE is_deleted = false
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """;

    List<Comment> comments = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          comments.add(mapRowToComment(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated comments (page={}, size={})", effectivePage, effectivePageSize, e);
      throw new RuntimeException("Failed to fetch comments", e);
    }

    return comments;
  }

  /**
   * Convenience method for getting the first page with default page size (100).
   *
   * @return list of up to 100 most recently created non-deleted comments
   * @throws RuntimeException if a database error occurs
   */
  public List<Comment> getAll() {
    return getAll(1, 100);
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
   * Due to ON DELETE CASCADE on parent_comment and post_id references,
   * child comments will also be affected if the database enforces it.
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