package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Post extends BaseEntity {
  private Long id;
  private Long authorId;
  private String title;
  private String body;

}
