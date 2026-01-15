package amalitech.blog.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LandingController {

  private final Logger log = LoggerFactory.getLogger(LandingController.class);
  @FXML
  private void handleLogin(ActionEvent event) {
    FXMLLoader fxmlLoader = new FXMLLoader(LandingController.class.getResource("/amalitech/blog/view/auth/login.fxml"));
    Scene scene = null;
    try {
      scene = new Scene(fxmlLoader.load(), 800, 600);
    } catch (IOException e) {
      log.info("Failed to open login");
      throw new RuntimeException(e);
    }

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Login");
  }

}