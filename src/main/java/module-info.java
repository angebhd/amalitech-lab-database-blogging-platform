module amalitech.blog {
  requires javafx.controls;
  requires javafx.fxml;
  requires static lombok;
  requires java.sql;
  requires io.github.cdimascio.dotenv.java;
  requires org.slf4j;


  opens amalitech.blog to javafx.fxml;
  exports amalitech.blog;

  opens amalitech.blog.controller to javafx.fxml;

  opens amalitech.blog.controller.auth to javafx.fxml;
  exports amalitech.blog.controller.auth;

  opens amalitech.blog.controller.posts to javafx.fxml;
  exports amalitech.blog.controller.posts;
}