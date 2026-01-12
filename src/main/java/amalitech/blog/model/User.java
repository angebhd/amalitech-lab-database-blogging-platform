package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User extends BaseEntity {
  private Long id;
  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private String password;


}
