module amalitech.blog {
  requires javafx.controls;
  requires javafx.fxml;


  opens amalitech.blog to javafx.fxml;
  exports amalitech.blog;
}