package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tags extends  BaseEntity{
  private Long id;
  private String name;
}
