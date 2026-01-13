package amalitech.blog.service;

import amalitech.blog.dao.ReviewDAO;
import amalitech.blog.model.Review;

import java.util.List;

public class ReviewService {

  private final ReviewDAO reviewDAO;
  public ReviewService (){
    this.reviewDAO = new ReviewDAO();
  }

  public List<Review> getByPost(Long postId){
    return this.reviewDAO.getByPostId(postId);
  }

  public Review create(Review review){
    return this.reviewDAO.create(review);
  }

  public Review update(Long id, Review review){
    return this.reviewDAO.update(id, review);
  }

  public boolean delete(Long id){
    return this.reviewDAO.delete(id);
  }

}
