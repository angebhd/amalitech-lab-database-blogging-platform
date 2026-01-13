package amalitech.blog.dao;

import amalitech.blog.model.PostTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the post_tags many-to-many relationship.
 * Manages associations between posts and tags.
 * Note: This is a junction table with composite primary key (post_id, tag_id).
 * No single surrogate ID exists, and soft-delete is typically not used here.
 */
public class PostTagsDAO implements DAO<PostTags, Long> {

  private final Logger log = LoggerFactory.getLogger(PostTagsDAO.class);

  /**
   * Creates a new post-tag association.
   * Does nothing if the association already exists (idempotent).
   *
   * @param entity the post-tag association to create
   * @return the entity (unchanged, as no generated fields)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public PostTags create(PostTags entity) {

    final String INSERT = """
                INSERT INTO post_tags (post_id, tag_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(INSERT)) {

      ps.setLong(1, entity.getPostId());
      ps.setLong(2, entity.getTagId());

      int rows = ps.executeUpdate();

      if (rows > 0) {
        log.info("Post-tag association created - Post: {}, Tag: {}",
                entity.getPostId(), entity.getTagId());
      } else {
        log.debug("Post-tag association already exists - Post: {}, Tag: {}",
                entity.getPostId(), entity.getTagId());
      }

      return entity;

    } catch (SQLException e) {
      log.error("Error creating post-tag association", e);
      throw new RuntimeException("Failed to associate tag with post", e);
    }
  }

  /**
   * Not supported for junction table with composite key.
   * Use {@link #exists(Long, Long)} or specific finder methods instead.
   *
   * @param id ignored
   * @return always throws UnsupportedOperationException
   * @throws UnsupportedOperationException always
   */
  @Override
  public PostTags get(Long id) {
    throw new UnsupportedOperationException(
            "get(Long) is not supported for composite-key junction table post_tags. " +
                    "Use exists(postId, tagId) or find methods instead.");
  }

  /**
   * Retrieves a paginated list of all post-tag associations.
   * Ordered by post_id, then tag_id.
   * Rarely used in practice — consider using post- or tag-specific finders.
   *
   * @param page     page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize number of records per page, defaults to 100 if ≤ 0
   * @return list of post-tag associations
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<PostTags> getAll(int page, int pageSize) {

    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT post_id, tag_id
                FROM post_tags
                ORDER BY post_id, tag_id
                LIMIT ? OFFSET ?
            """;

    List<PostTags> associations = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          associations.add(mapRowToPostTags(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated post-tag associations (page={}, size={})",
              effectivePage, effectivePageSize, e);
      throw new RuntimeException("Failed to fetch post-tag associations", e);
    }

    return associations;
  }

  /**
   * Convenience method — first page, default size 100.
   */
  public List<PostTags> getAll() {
    return getAll(1, 100);
  }

  /**
   * Not supported for junction table — associations are not updatable.
   *
   * @param id     ignored
   * @param entity ignored
   * @return always throws UnsupportedOperationException
   */
  @Override
  public PostTags update(Long id, PostTags entity) {
    throw new UnsupportedOperationException(
            "update is not supported for junction table post_tags. " +
                    "Delete and re-create the association if needed.");
  }

  /**
   * Removes a post-tag association (hard delete).
   * Cascades via ON DELETE CASCADE are handled by the database.
   *
   * @param id ignored (composite key)
   * @return always false — use {@link #delete(Long, Long)} instead
   */
  @Override
  public boolean delete(Long id) {
    throw new UnsupportedOperationException(
            "delete(Long) is not supported for composite-key junction table. " +
                    "Use delete(postId, tagId) instead.");
  }


  /**
   * Removes the association between a specific post and tag.
   *
   * @param postId the post ID
   * @param tagId  the tag ID
   * @return true if an association was deleted, false if it didn't exist
   */
  public boolean delete(Long postId, Long tagId) {

    final String DELETE = """
                DELETE FROM post_tags
                WHERE post_id = ? AND tag_id = ?
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, postId);
      ps.setLong(2, tagId);

      boolean removed = ps.executeUpdate() > 0;

      if (removed) {
        log.info("Post-tag association removed - Post: {}, Tag: {}", postId, tagId);
      } else {
        log.debug("No post-tag association found to remove - Post: {}, Tag: {}", postId, tagId);
      }

      return removed;

    } catch (SQLException e) {
      log.error("Error removing post-tag association (post={}, tag={})", postId, tagId, e);
      throw new RuntimeException("Failed to remove tag from post", e);
    }
  }

  /**
   * Checks if a post is associated with a specific tag.
   *
   * @param postId post ID
   * @param tagId  tag ID
   * @return true if the association exists
   */
  public boolean exists(Long postId, Long tagId) {

    final String EXISTS = """
                SELECT 1 FROM post_tags
                WHERE post_id = ? AND tag_id = ?
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(EXISTS)) {

      ps.setLong(1, postId);
      ps.setLong(2, tagId);

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }

    } catch (SQLException e) {
      log.error("Error checking post-tag existence (post={}, tag={})", postId, tagId, e);
      throw new RuntimeException("Failed to check tag association", e);
    }
  }

  /**
   * Gets all tag IDs associated with a given post.
   *
   * @param postId the post to query
   * @return list of tag IDs (empty if none)
   */
  public List<Long> findTagIdsByPost(Long postId) {

    final String SELECT_BY_POST = """
                SELECT tag_id
                FROM post_tags
                WHERE post_id = ?
                ORDER BY tag_id
            """;

    List<Long> tagIds = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_POST)) {

      ps.setLong(1, postId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          tagIds.add(rs.getLong("tag_id"));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching tags for post {}", postId, e);
      throw new RuntimeException("Failed to fetch post tags", e);
    }

    return tagIds;
  }

  /**
   * Gets all post IDs associated with a given tag.
   *
   * @param tagId the tag to query
   * @return list of post IDs (empty if none)
   */
  public List<Long> findPostIdsByTag(Long tagId) {

    final String SELECT_BY_TAG = """
                SELECT post_id
                FROM post_tags
                WHERE tag_id = ?
                ORDER BY post_id
            """;

    List<Long> postIds = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_TAG)) {

      ps.setLong(1, tagId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          postIds.add(rs.getLong("post_id"));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching posts for tag {}", tagId, e);
      throw new RuntimeException("Failed to fetch posts by tag", e);
    }

    return postIds;
  }

  private PostTags mapRowToPostTags(ResultSet rs) throws SQLException {
    PostTags pt = new PostTags();
    pt.setPostId(rs.getLong("post_id"));
    pt.setTagId(rs.getLong("tag_id"));
    return pt;
  }
}