module amalitech.blog {
  requires javafx.controls;
  requires javafx.fxml;
  requires static lombok;
  requires java.sql;
  requires io.github.cdimascio.dotenv.java;
  requires org.slf4j;
  requires de.mkammerer.argon2.nolibs;
  requires com.sun.jna;


  opens amalitech.blog to javafx.fxml;
  exports amalitech.blog;

  opens amalitech.blog.controller to javafx.fxml;

  opens amalitech.blog.controller.auth to javafx.fxml;
  exports amalitech.blog.controller.auth;

  opens amalitech.blog.controller.posts to javafx.fxml;
  exports amalitech.blog.controller.posts;

  opens amalitech.blog.dto to javafx.fxml;
  exports amalitech.blog.dto;

  opens amalitech.blog.model to javafx.fxml;
  exports amalitech.blog.model;
}