package amalitech.blog.dao;

import amalitech.blog.model.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for Review entities.
 * Provides CRUD operations for post reviews/ratings with soft-delete support.
 */
public class ReviewDAO implements DAO<Review, Long> {

  private final Logger log = LoggerFactory.getLogger(ReviewDAO.class);

  /**
   * Creates a new review in the database and sets the generated ID and timestamps.
   *
   * @param entity the review to create (will be modified to include generated ID and timestamps)
   * @return the created review with populated ID and timestamps
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Review create(Review entity) {

    final String INSERT = """
                INSERT INTO reviews (post_id, user_id, rate)
                VALUES (?, ?, ?::e_review)
                RETURNING id, created_at, updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(INSERT)) {

      ps.setLong(1, entity.getPostId());
      ps.setLong(2, entity.getUserId());
      ps.setString(3, entity.getRate());  // assumes getRate() returns "ONE", "TWO", etc.

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(rs.getLong("id"));
          entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
      }

      log.info("Review created successfully - ID: {}, Post: {}, User: {}, Rate: {}",
              entity.getId(), entity.getPostId(), entity.getUserId(), entity.getRate());
      return entity;

    } catch (SQLException e) {
      log.error("Error creating review", e);
      throw new RuntimeException("Failed to create review", e);
    }
  }

  /**
   * Retrieves a review by its ID, excluding soft-deleted records.
   *
   * @param id the ID of the review to retrieve
   * @return the review if found and not deleted, otherwise {@code null}
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Review get(Long id) {

    final String SELECT_BY_ID = """
                SELECT id, post_id, user_id, rate,
                       created_at, updated_at, is_deleted
                FROM reviews
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_BY_ID)) {

      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToReview(rs);
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching review with id {}", id, e);
      throw new RuntimeException("Failed to fetch review", e);
    }

    return null;
  }

  /**
   * Retrieves a paginated list of all non-deleted reviews,
   * ordered by creation date descending.
   * Page numbering starts at 1.
   *
   * @param page     the page number (1-based), defaults to 1 if ≤ 0
   * @param pageSize the number of records per page, defaults to 100 if ≤ 0
   * @return list of reviews for the requested page (may be empty)
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public List<Review> getAll(int page, int pageSize) {

    int effectivePage = Math.max(page, 1);
    int effectivePageSize = Math.max(pageSize, 1);
    int offset = (effectivePage - 1) * effectivePageSize;

    final String SELECT_ALL_PAGED = """
                SELECT id, post_id, user_id, rate,
                       created_at, updated_at, is_deleted
                FROM reviews
                WHERE is_deleted = false
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """;

    List<Review> reviews = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(SELECT_ALL_PAGED)) {

      ps.setInt(1, effectivePageSize);
      ps.setInt(2, offset);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          reviews.add(mapRowToReview(rs));
        }
      }

    } catch (SQLException e) {
      log.error("Error fetching paginated reviews (page={}, size={})", effectivePage, effectivePageSize, e);
      throw new RuntimeException("Failed to fetch reviews", e);
    }

    return reviews;
  }

  /**
   * Convenience method for getting the first page with default page size (100).
   *
   * @return list of up to 100 most recently created non-deleted reviews
   * @throws RuntimeException if a database error occurs
   */
  public List<Review> getAll() {
    return getAll(1, 100);
  }

  /**
   * Updates an existing review (only the rate can be changed).
   * Automatically updates the updated_at timestamp.
   *
   * @param id     the ID of the review to update
   * @param entity the updated review data (only rate is used)
   * @return the updated entity if the update succeeded, {@code null} if review not found or was deleted
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public Review update(Long id, Review entity) {

    final String UPDATE = """
                UPDATE reviews
                SET rate = ?::e_review,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
                RETURNING updated_at
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(UPDATE)) {

      ps.setString(1, entity.getRate());
      ps.setLong(2, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          entity.setId(id);
          entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
          log.info("Review updated successfully - ID: {}, New Rate: {}", id, entity.getRate());
          return entity;
        }
      }

      log.warn("No review found to update with id {}", id);
      return null;

    } catch (SQLException e) {
      log.error("Error updating review with id {}", id, e);
      throw new RuntimeException("Failed to update review", e);
    }
  }

  /**
   * Soft-deletes a review by setting is_deleted = true and recording deletion timestamp.
   *
   * @param id the ID of the review to delete
   * @return {@code true} if the review was found and marked as deleted, {@code false} otherwise
   * @throws RuntimeException if a database error occurs
   */
  @Override
  public boolean delete(Long id) {

    final String DELETE = """
                UPDATE reviews
                SET is_deleted = true,
                    deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_deleted = false
            """;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(DELETE)) {

      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;

      if (deleted) {
        log.info("Review soft-deleted successfully - ID: {}", id);
      } else {
        log.warn("Review not found or already deleted - ID: {}", id);
      }

      return deleted;

    } catch (SQLException e) {
      log.error("Error soft-deleting review with id {}", id, e);
      throw new RuntimeException("Failed to delete review", e);
    }
  }

  /**
   * Maps a ResultSet row to a Review object.
   *
   * @param rs the result set positioned at the current row
   * @return populated Review instance
   * @throws SQLException if column access fails
   */
  private Review mapRowToReview(ResultSet rs) throws SQLException {
    Review review = new Review();
    review.setId(rs.getLong("id"));
    review.setPostId(rs.getLong("post_id"));
    review.setUserId(rs.getLong("user_id"));
    review.setRate(rs.getString("rate"));  // returns "ONE", "TWO", "THREE", "FOUR", "FIVE"
    review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    review.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    review.setDeleted(rs.getBoolean("is_deleted"));
    return review;
  }
}