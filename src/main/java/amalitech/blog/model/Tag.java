package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tag extends  BaseEntity{
  private Long id;
  private String name;
}
