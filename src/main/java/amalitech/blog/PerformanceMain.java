package amalitech.blog;

import amalitech.blog.service.PostService;
import amalitech.blog.service.UserService;
import amalitech.blog.utils.PerformanceResult;
import amalitech.blog.utils.PerformanceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PerformanceMain {
  public static void main(String[] args) {
    final Logger log = LoggerFactory.getLogger(PerformanceMain.class);
    PostService postService = new PostService();
    UserService userService = new UserService();
    PerformanceResult feedBefore = PerformanceUtil.measure("Loading feed            ", postService::loadFeed, 3 );
    PerformanceResult postBefore = PerformanceUtil.measure("Post by author          ", () -> postService.getByAuthorId(1L), 3 );
    PerformanceResult statBefore = PerformanceUtil.measure("Loading user stats      ", () ->userService.getUserStats(1L), 3 );

    PerformanceResult feedAfter = PerformanceUtil.measure("Loading feed      (after)", () -> postService.loadFeed(true), 3 );
    PerformanceResult postAfter = PerformanceUtil.measure("Post by author    (after)", () -> postService.getByAuthorId(1L, true), 3 );
    PerformanceResult statAfter = PerformanceUtil.measure("Loading user stats(After)", () ->userService.getUserStats(1L, true), 3 );


    log.info("{}", feedBefore);
    log.info("{}", postBefore);
    log.info("{}\n", statBefore);

    log.info("{}", feedAfter);
    log.info("{}", postAfter);
    log.info("{}", statAfter);

  }
}
