package amalitech.blog.dao;

import amalitech.blog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for User entities.
 * Provides CRUD operations for users with soft-delete support.
 */
public class UserDAO implements DAO<User, Long> {
  private final Logger log = LoggerFactory.getLogger(UserDAO.class);

  /**
   * Creates a new user in the database and sets the generated ID on the entity.
   *
   * @param entity the user to create (will be modified to include generated ID)
   * @return the created user with populated ID
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public User create(User entity) {
    final String INSERT = """
                INSERT INTO users (username, first_name, last_name, email, password)
                VALUES (?, ?, ?, ?, ?)
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps =
                 connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

      setUserParams(ps, entity);
      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          entity.setId(rs.getLong(1));
        }
      }

      log.info("User {} saved successfully", entity.getUsername());
      return entity;

    } catch (SQLException e) {
      log.error("Error creating user", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a user by their ID, excluding soft-deleted records.
   *
   * @param id the ID of the user to retrieve
   * @return the user if found and not deleted, otherwise {@code null}
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public User get(Long id) {
    final String SELECT_BY_ID = """
                SELECT * FROM users
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToUser(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching user with id {}", id, e);
      throw new RuntimeException(e);
    }

    return null;
  }

  /**
   * Retrieves a paginated list of all non-deleted users, ordered by creation date descending.
   * Page numbering starts at 1.
   *
   * @param page     the page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize the number of records per page, defaults to 100 if ≤ 0
   * @return list of users for the requested page (may be empty)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<User> getAll(int page, int pageSize) {
    // Apply defaults
    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1); // at minimum 1 to avoid division issues

    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT * FROM users
                WHERE is_deleted = false
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """;

    List<User> users = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          users.add(mapRowToUser(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated users (page={}, size={})", effectivePage, effectivePageSize, e);
      throw new RuntimeException(e);
    }

    return users;
  }

  /**
   * Convenience method for getting the first page with default page size (100).
   *
   * @return list of up to 100 most recently created non-deleted users
   * @throws RuntimeException if a database error occurs
   */
  public List<User> getAll() {
    return getAll(1, 100);
  }

  /**
   * Updates an existing user's profile information.
   * Does not check for concurrent modifications.
   *
   * @param id     the ID of the user to update
   * @param entity the updated user data
   * @return the updated entity if the update succeeded, {@code null} if user not found or was deleted
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public User update(Long id, User entity) {
    final String UPDATE = """
                UPDATE users
                SET username = ?, first_name = ?, last_name = ?, email = ?, password = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(UPDATE)) {

      setUserParams(ps, entity);
      ps.setLong(6, id);

      int updated = ps.executeUpdate();

      if (updated == 0) {
        log.warn("No user found to update with id {}", id);
        return null;
      }

      entity.setId(id);
      log.info("User {} updated successfully", id);
      return entity;

    } catch (SQLException e) {
      log.error("Error updating user with id {}", id, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Soft-deletes a user by setting is_deleted = true and recording deletion timestamp.
   *
   * @param id the ID of the user to delete
   * @return {@code true} if the user was found and marked as deleted, {@code false} otherwise
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public boolean delete(Long id) {
    final String DELETE = """
                UPDATE users
                SET is_deleted = true,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;

      if (deleted) {
        log.info("User {} deleted successfully", id);
      } else {
        log.warn("User {} not found or already deleted", id);
      }

      return deleted;

    } catch (SQLException e) {
      log.error("Error deleting user with id {}", id, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Maps a ResultSet row to a User object.
   *
   * @param rs the result set positioned at the current row
   * @return populated User instance
   * @throws SQLException if column access fails
   */
  private User mapRowToUser(ResultSet rs) throws SQLException {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    user.setFirstName(rs.getString("first_name"));
    user.setLastName(rs.getString("last_name"));
    user.setEmail(rs.getString("email"));
    user.setPassword(rs.getString("password"));
    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    user.setDeleted(rs.getBoolean("is_deleted"));
    return user;
  }

  /**
   * Sets the common user parameters on a PreparedStatement.
   *
   * @param ps   the prepared statement
   * @param user the user data source
   * @throws SQLException if parameter setting fails
   */
  private void setUserParams(PreparedStatement ps, User user) throws SQLException {
    ps.setString(1, user.getUsername());
    ps.setString(2, user.getFirstName());
    ps.setString(3, user.getLastName());
    ps.setString(4, user.getEmail());
    ps.setString(5, user.getPassword());
  }
}