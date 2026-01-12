package amalitech.blog.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Review extends BaseEntity {
  private Long id;
  private Long postId;
  private Long userId;
  private EReview rate;

  public String getRate() {
    return this.rate.name();
  }
  public void setRate(String name){
    this.rate = EReview.valueOf(name);
  }
}
