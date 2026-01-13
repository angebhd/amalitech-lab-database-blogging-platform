package amalitech.blog.service;

import amalitech.blog.dao.CommentDAO;
import amalitech.blog.dao.enums.CommentColumn;
import amalitech.blog.model.Comment;

import java.util.List;

public class CommentService {
  private final CommentDAO commentDAO;

  public CommentService(){
    this.commentDAO = new CommentDAO();
  }

  public List<Comment> getByPostId(Long postId){
    return this.commentDAO.findBy(String.valueOf(postId), CommentColumn.POST_ID);
  }

  public Comment create(Comment entity){
    return  this.commentDAO.create(entity);
  }

  public Comment update (Long id, Comment entity){
    return this.commentDAO.update(id, entity);
  }

  public boolean delete (Long id){
    return this.commentDAO.delete(id);
  }


}
