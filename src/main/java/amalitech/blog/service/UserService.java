package amalitech.blog.service;

import amalitech.blog.dao.UserDAO;
import amalitech.blog.dao.enums.UserColumn;
import amalitech.blog.model.User;

import java.util.Optional;

public class UserService {
  private final PasswordHashService passwordHashService;
  private final UserDAO userDAO;

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
    return this.userDAO.update(id, user);
  }

  public boolean delete (Long id){
    return  this.userDAO.delete(id);
  }
}
