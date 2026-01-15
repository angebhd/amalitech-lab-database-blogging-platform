package amalitech.blog.dao;

import amalitech.blog.dao.enums.UserColumn;
import amalitech.blog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object (DAO) for User entities.
 * Provides CRUD operations for users with soft-delete support.
 * <p>
 * All read operations exclude soft-deleted records by default,
 * but provide overloads to include them when needed (e.g. admin views, audit).
 * </p>
 */
public class UserDAO implements DAO<User, Long> {

  private final Logger log = LoggerFactory.getLogger(UserDAO.class);

  /**
   * Creates a new user in the database and sets the generated ID on the provided entity.
   *
   * @param entity the user entity to persist (will be modified to include the generated ID)
   * @return the same entity instance with the generated ID populated
   * @throws RuntimeException if a database error occurs during insertion
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
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Retrieves a user by their primary key (ID), excluding soft-deleted records by default.
   *
   * @param id the unique identifier of the user
   * @return the matching user or {@code null} if not found or soft-deleted
   * @throws RuntimeException if a database error occurs
   * @see #get(Long, boolean)
   */
  @Override
  public User get(Long id) {
    return get(id, false);
  }

  /**
   * Retrieves a user by their primary key (ID), with optional inclusion of soft-deleted records.
   *
   * @param id             the unique identifier of the user
   * @param includeDeleted if {@code true}, returns the user even if marked as deleted
   * @return the matching user or {@code null} if not found
   * @throws RuntimeException if a database error occurs
   */
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
   * Finds all users matching the given value in the specified column.
   *
   * @param value          the value to search for (e.g. username, email)
   * @param column         the column to query (must be from {@link UserColumn} enum)
   * @param includeDeleted if {@code true}, includes soft-deleted users
   * @return list of matching users (typically 0 or 1, but can be more if data integrity is broken)
   * @throws RuntimeException if a database error occurs
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
   * Retrieves exactly one user matching the given value in the specified column.
   * <p>
   * Throws an exception if more than one record is found (indicating data inconsistency).
   * </p>
   *
   * @param value          the value to search for
   * @param column         the column to query against
   * @param includeDeleted whether to include soft-deleted records
   * @return the matching user or {@code null} if none found
   * @throws IllegalStateException if more than one matching user is found
   * @throws RuntimeException      if a database error occurs
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
   * Modern variant of {@link #getBy(String, UserColumn, boolean)} using {@link Optional}.
   *
   * @param value          value to search for
   * @param column         column to query
   * @param includeDeleted whether to include deleted records
   * @return an Optional containing the user if found, empty otherwise
   * @throws IllegalStateException if multiple matches are found
   * @throws RuntimeException      if a database error occurs
   */
  public Optional<User> findOneBy(String value, UserColumn column, boolean includeDeleted) {
    return Optional.ofNullable(getBy(value, column, includeDeleted));
  }

  /**
   * Convenience overload — excludes deleted records by default.
   *
   * @see #findOneBy(String, UserColumn, boolean)
   */
  public Optional<User> findOneBy(String value, UserColumn column) {
    return findOneBy(value, column, false);
  }

  /**
   * Retrieves a paginated list of users, excluding soft-deleted records by default.
   *
   * @param page     1-based page number
   * @param pageSize number of records per page
   * @return paginated list of users
   * @throws RuntimeException if a database error occurs
   * @see #getAll(int, int, boolean)
   */
  @Override
  public List<User> getAll(int page, int pageSize) {
    return getAll(page, pageSize, false);
  }

  /**
   * Retrieves a paginated list of users with optional inclusion of soft-deleted records.
   *
   * @param page           1-based page number
   * @param pageSize       number of records per page
   * @param includeDeleted if {@code true}, includes soft-deleted users
   * @return paginated list of users
   * @throws RuntimeException if a database error occurs
   */
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

  /**
   * Convenience method: returns first page (1) with 100 records, excluding deleted users.
   *
   * @return list of up to 100 most recently created non-deleted users
   */
  public List<User> getAll() {
    return getAll(1, 100, false);
  }

  /**
   * Updates an existing user's fields (username, names, email, password).
   * Only updates non-deleted users.
   *
   * @param id     ID of the user to update
   * @param entity updated user data
   * @return updated entity or {@code null} if user not found or was deleted
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
      throw new RuntimeException("Failed to update user", e);
    }
  }

  /**
   * Soft-deletes a user by setting {@code is_deleted = true} and recording {@code deleted_at}.
   *
   * @param id ID of the user to soft-delete
   * @return {@code true} if the user was found and marked deleted, {@code false} otherwise
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
      throw new RuntimeException("Failed to delete user", e);
    }
  }

  /**
   * Retrieves basic statistics for a given user (post count, comment count, review count).
   * Uses efficient COUNT queries instead of loading full lists.
   * Excludes soft-deleted records by default.
   *
   * @param userId the ID of the user to get stats for
   * @return a Map containing:
   *         - "postCount" → number of non-deleted posts
   *         - "commentCount" → number of non-deleted comments
   *         - "reviewCount" → number of non-deleted reviews
   * @throws RuntimeException if a database error occurs
   */
  public Map<String, Integer> getUserStats(Long userId) {
    Map<String, Integer> stats = new HashMap<>();

    try (Connection conn = DatabaseConnection.getConnection()) {

      // 1. Post count
      String postSql = """
                SELECT COUNT(*) 
                FROM posts 
                WHERE author_id = ? AND is_deleted = false
            """;

      try (PreparedStatement ps = conn.prepareStatement(postSql)) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            stats.put("postCount", rs.getInt(1));
          }
        }
      }

      // 2. Comment count
      String commentSql = """
                SELECT COUNT(*) 
                FROM comments 
                WHERE user_id = ? AND is_deleted = false
            """;

      try (PreparedStatement ps = conn.prepareStatement(commentSql)) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            stats.put("commentCount", rs.getInt(1));
          }
        }
      }

      // 3. Review count
      String reviewSql = """
                SELECT COUNT(*) 
                FROM reviews 
                WHERE user_id = ? AND is_deleted = false
            """;

      try (PreparedStatement ps = conn.prepareStatement(reviewSql)) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            stats.put("reviewCount", rs.getInt(1));
          }
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching user stats for userId {}", userId, e);
      throw new RuntimeException("Failed to retrieve user statistics", e);
    }

    return stats;
  }
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