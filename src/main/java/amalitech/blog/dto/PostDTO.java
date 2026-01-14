package amalitech.blog.dto;

import amalitech.blog.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostDTO {

  private Post post;
  private Long authorId;
  private String authorName;
  private List<CommentDTO> commentDTOS;
  private List<Comment> comments;
  private List<Tag> tags;
  private List<Review> reviews;

}
