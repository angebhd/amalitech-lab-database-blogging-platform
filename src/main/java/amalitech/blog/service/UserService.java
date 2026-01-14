package amalitech.blog.service;

import amalitech.blog.dao.UserDAO;
import amalitech.blog.dao.enums.UserColumn;
import amalitech.blog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UserService {
  private final PasswordHashService passwordHashService;
  private final UserDAO userDAO;
  private final Logger log = LoggerFactory.getLogger(UserService.class);

  public UserService(){
    this.passwordHashService = new PasswordHashService();
    this.userDAO = new UserDAO();
  }

  public User create(User user){
    String hashedPassword = this.passwordHashService.hash(user.getPassword().toCharArray());
    user.setPassword(hashedPassword);
    return this.userDAO.create(user);
  }

  public User get(Long id){
    return this.userDAO.get(id);
  }

  public User login(String username, String password){
    Optional<User> user = this.userDAO.findOneBy(username, UserColumn.USERNAME);
    if (user.isPresent()){
      boolean match = this.passwordHashService.verify(password.toCharArray(), user.get().getPassword());
      user.get().setPassword(null);
      if (match)
        return user.get();
    }
    return null;
  }

  public User update(Long id, User user){
    User oldUser = this.userDAO.get(id);
    user.setPassword(oldUser.getPassword());
    return this.userDAO.update(id, user);
  }

  public User updatePassword(Long userId, String oldPassword, String newPassword){
    log.info("Update Password | new: {}", newPassword);
    log.info("Update Password | old: {}", oldPassword);
    User user = this.get(userId);
    if (this.passwordHashService.verify(oldPassword.toCharArray(), user.getPassword())) {
      user.setPassword(this.passwordHashService.hash(newPassword.toCharArray()));
      return this.userDAO.update(userId, user);
    }
    throw new RuntimeException("Invalid password");
  }

  public boolean delete (Long id){
    return  this.userDAO.delete(id);
  }
}
