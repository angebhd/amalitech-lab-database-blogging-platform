package amalitech.blog.dto;

import amalitech.blog.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostDTO {

  private Post post;
  private User author;
  private List<Tag> tags;
  private List<Comment> comments;
  private List<Review> reviews;

}
