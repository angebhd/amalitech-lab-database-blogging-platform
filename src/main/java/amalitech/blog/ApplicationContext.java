package amalitech.blog;

import amalitech.blog.model.User;
import lombok.Getter;
import lombok.Setter;


public class ApplicationContext {
  private ApplicationContext() {}

  private static User authenticatedUser = null;

  public static void setAuthenticatedUser(User authenticatedUser) {
    ApplicationContext.authenticatedUser = authenticatedUser;
  }

  public static User getAuthenticatedUser() {
    return authenticatedUser;
  }
}
