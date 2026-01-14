package amalitech.blog.dto;

import amalitech.blog.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostDTO {

  private Post post;
  private String authorName;
  private User author;  // TODO:  To be removed
  private List<CommentDTO> commentDTOS;
  private List<Tag> tags;
  private List<Comment> comments; // TODO:  To be removed
  private List<Review> reviews;

}
