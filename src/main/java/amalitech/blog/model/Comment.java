package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comment extends BaseEntity{
  private Long id;
  private Long postId;
  private Long userId;
  private String body;
  private Long parentCommentId;
}
