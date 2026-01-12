package amalitech.blog.dao;

import amalitech.blog.model.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Tags entities.
 * Provides CRUD operations for blog post tags with soft-delete support.
 */
public class TagsDAO implements DAO<Tags, Long> {

  private final Logger log = LoggerFactory.getLogger(TagsDAO.class);

  /**
   * Creates a new tag in the database and sets the generated ID on the entity.
   *
   * @param entity the tag to create (will be modified to include generated ID)
   * @return the created tag with populated ID
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Tags create(Tags entity) {

    final String INSERT = """
                INSERT INTO tags (name)
                VALUES (?)
                RETURNING id, created_at, updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(INSERT)) {

      ps.setString(1, entity.getName());

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(rs.getLong("id"));
          entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
      }

      log.info("Tag created successfully - ID: {}, Name: {}", entity.getId(), entity.getName());
      return entity;

    } catch (SQLException e) {
      log.error("Error creating tag", e);
      throw new RuntimeException("Failed to create tag", e);
    }
  }

  /**
   * Retrieves a tag by its ID, excluding soft-deleted records.
   *
   * @param id the ID of the tag to retrieve
   * @return the tag if found and not deleted, otherwise {@code null}
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Tags get(Long id) {

    final String SELECT_BY_ID = """
                SELECT id, name, created_at, updated_at, is_deleted
                FROM tags
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToTag(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching tag with id {}", id, e);
      throw new RuntimeException("Failed to fetch tag", e);
    }

    return null;
  }

  /**
   * Retrieves a paginated list of all non-deleted tags, ordered by creation date descending.
   * Page numbering starts at 1.
   *
   * @param page     the page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize the number of records per page, defaults to 100 if ≤ 0
   * @return list of tags for the requested page (may be empty)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<Tags> getAll(int page, int pageSize) {

    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT id, name, created_at, updated_at, is_deleted
                FROM tags
                WHERE is_deleted = false
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """;

    List<Tags> tags = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          tags.add(mapRowToTag(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated tags (page={}, size={})", effectivePage, effectivePageSize, e);
      throw new RuntimeException("Failed to fetch tags", e);
    }

    return tags;
  }

  /**
   * Convenience method for getting the first page with default page size (100).
   *
   * @return list of up to 100 most recently created non-deleted tags
   * @throws RuntimeException if a database error occurs
   */
  public List<Tags> getAll() {
    return getAll(1, 100);
  }

  /**
   * Updates an existing tag's name.
   * Automatically updates the updated_at timestamp.
   *
   * @param id     the ID of the tag to update
   * @param entity the updated tag data
   * @return the updated entity if the update succeeded, {@code null} if tag not found or was deleted
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Tags update(Long id, Tags entity) {

    final String UPDATE = """
                UPDATE tags
                SET name = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
                RETURNING updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(UPDATE)) {

      ps.setString(1, entity.getName());
      ps.setLong(2, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(id);
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
          log.info("Tag updated successfully - ID: {}, Name: {}", id, entity.getName());
          return entity;
        }
      }

      log.warn("No tag found to update with id {}", id);
      return null;

    } catch (SQLException e) {
      log.error("Error updating tag with id {}", id, e);
      throw new RuntimeException("Failed to update tag", e);
    }
  }

  /**
   * Soft-deletes a tag by setting is_deleted = true and recording deletion timestamp.
   *
   * @param id the ID of the tag to delete
   * @return {@code true} if the tag was found and marked as deleted, {@code false} otherwise
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public boolean delete(Long id) {

    final String DELETE = """
                UPDATE tags
                SET is_deleted = true,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;

      if (deleted) {
        log.info("Tag soft-deleted successfully - ID: {}", id);
      } else {
        log.warn("Tag not found or already deleted - ID: {}", id);
      }

      return deleted;

    } catch (SQLException e) {
      log.error("Error soft-deleting tag with id {}", id, e);
      throw new RuntimeException("Failed to delete tag", e);
    }
  }

  /**
   * Maps a ResultSet row to a Tags object.
   *
   * @param rs the result set positioned at the current row
   * @return populated Tags instance
   * @throws SQLException if column access fails
   */
  private Tags mapRowToTag(ResultSet rs) throws SQLException {
    Tags tag = new Tags();
    tag.setId(rs.getLong("id"));
    tag.setName(rs.getString("name"));
    tag.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    tag.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    tag.setDeleted(rs.getBoolean("is_deleted"));
    return tag;
  }
}