package amalitech.blog.utils;

public class ValidatorUtil {
  private ValidatorUtil() {}
  public static boolean validateEmail(String email){
    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }
}
