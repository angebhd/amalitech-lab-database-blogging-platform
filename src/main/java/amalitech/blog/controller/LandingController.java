package amalitech.blog.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LandingController {
  @FXML
  private void handleLogin(ActionEvent event) {
    FXMLLoader fxmlLoader = new FXMLLoader(LandingController.class.getResource("/amalitech/blog/view/auth/login.fxml"));
    Scene scene = null;
    try {
      scene = new Scene(fxmlLoader.load(), 800, 600);
    } catch (IOException e) {
      System.out.println("Failed to open login");
      throw new RuntimeException(e);
    }

    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.setScene(scene);
    stage.setTitle("B-BLOG - Login");
  }

  @FXML
  private void handleLearnMore(ActionEvent event) {
    System.out.println("Learn More clicked - Implement feature showcase");
  }
}