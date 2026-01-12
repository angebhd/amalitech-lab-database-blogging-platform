package amalitech.blog.dao;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

  private static final Dotenv dotenv = Dotenv.load();

  private static final String URL = dotenv.get("DB_URL");
  private static final String USER = dotenv.get("DB_USER");
  private static final String PASSWORD = dotenv.get("DB_PASSWORD");

  private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

  private DatabaseConnection() {
  }

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASSWORD);
  }

  public static void testConnection()throws SQLException{
    DriverManager.getConnection(URL, USER, PASSWORD);
    log.info("Database connection test successful !");
  }
}
