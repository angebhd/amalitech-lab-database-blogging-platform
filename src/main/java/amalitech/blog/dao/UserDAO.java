package amalitech.blog.dao;

import amalitech.blog.dao.enums.UserColumn;
import amalitech.blog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
         PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

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
      throw new RuntimeException("Failed to create user", e);
    }
  }

  @Override
  public User get(Long id) {
    return get(id, false);
  }

  public User get(Long id, boolean includeDeleted) {
    String sql = """
                SELECT * FROM users
                WHERE id = ?
            """;
    if (!includeDeleted) {
      sql += " AND is_deleted = false";
    }

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToUser(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching user with id {}", id, e);
      throw new RuntimeException("Failed to fetch user by id", e);
    }

    return null;
  }

  /**
   * Finds all users matching a specific column value.
   *
   * @param value          value to search for
   * @param column         column to query against (from UserColumn enum)
   * @param includeDeleted whether to include soft-deleted users
   * @return list of matching users (usually 0 or 1, but can be more if data is inconsistent)
   */
  public List<User> findBy(String value, UserColumn column, boolean includeDeleted) {
    String sql = """
                SELECT * FROM users
                WHERE %s = ?
            """.formatted(column.name());

    if (!includeDeleted) {
      sql += " AND is_deleted = false";
    }

    List<User> users = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, value);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          users.add(mapRowToUser(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error finding users by {} = {}", column.name(), value, e);
      throw new RuntimeException("Failed to find users by " + column.name(), e);
    }

    return users;
  }

  /**
   * Retrieves exactly one user by column value (or null if none found).
   * Throws exception if more than one matching user is found.
   */
  public User getBy(String value, UserColumn column, boolean includeDeleted) {
    List<User> results = findBy(value, column, includeDeleted);

    if (results.size() > 1) {
      log.error("Multiple users found for {} = {}. This indicates a data integrity issue.", column.name(), value);
      throw new IllegalStateException(
              "Multiple users found for " + column.name() + " = " + value);
    }

    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Modern/optional style variant of getBy
   */
  public Optional<User> findOneBy(String value, UserColumn column, boolean includeDeleted) {
    return Optional.ofNullable(getBy(value, column, includeDeleted));
  }

  public List<User> findBy(String value, UserColumn column) {
    return findBy(value, column, false);
  }

  public User getBy(String value, UserColumn column) {
    return getBy(value, column, false);
  }

  public Optional<User> findOneBy(String value, UserColumn column) {
    return findOneBy(value, column, false);
  }


  @Override
  public List<User> getAll(int page, int pageSize) {
    return getAll(page, pageSize, false);
  }

  public List<User> getAll(int page, int pageSize, boolean includeDeleted) {
    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    String sql = "SELECT * FROM users";
    if (!includeDeleted) {
      sql += " WHERE is_deleted = false";
    }
    sql += """
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
            """;

    List<User> users = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          users.add(mapRowToUser(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching users (page={}, size={}, includeDeleted={})",
              effectivePage, effectivePageSize, includeDeleted, e);
      throw new RuntimeException("Failed to fetch users", e);
    }

    return users;
  }

  public List<User> getAll() {
    return getAll(1, 100, false);
  }

  // =============================================================================
  // UPDATE & DELETE (unchanged from your original)
  // =============================================================================

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
      throw new RuntimeException("Failed to update user", e);
    }
  }

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
      throw new RuntimeException("Failed to delete user", e);
    }
  }

  // =============================================================================
  // Helpers (unchanged)
  // =============================================================================

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

  private void setUserParams(PreparedStatement ps, User user) throws SQLException {
    ps.setString(1, user.getUsername());
    ps.setString(2, user.getFirstName());
    ps.setString(3, user.getLastName());
    ps.setString(4, user.getEmail());
    ps.setString(5, user.getPassword());
  }
}