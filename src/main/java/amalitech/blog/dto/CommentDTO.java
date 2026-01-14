package amalitech.blog.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CommentDTO {
  private Long id;
  private String commenterName;
  private String body;
  private List<CommentDTO> childComments;
  private LocalDateTime createdAt;
}
