package amalitech.blog;

import amalitech.blog.dao.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class Main extends Application {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  @Override
  public void start(Stage stage) throws IOException, SQLException {
    try {
      DatabaseConnection.testConnection();
    } catch (SQLException e) {
      log.error("Database connection failed...");
      log.info("Make sure to put the correct credential in the .env file");
      throw e;
    }
    FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/landing.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
    stage.setTitle("B-BLOG - Home");
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}