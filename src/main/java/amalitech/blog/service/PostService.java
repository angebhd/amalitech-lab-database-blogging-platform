package amalitech.blog.service;

import amalitech.blog.dao.PostDAO;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Post;
import amalitech.blog.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PostService {

  private final PostDAO postDAO;
  private final TagService tagService;
  private final PostTagsService postTagsService;
  private final UserService userService;
  private final ReviewService reviewService;
  private final CommentService commentService;
  private final Logger log = LoggerFactory.getLogger(PostService.class);

  public PostService(){
    this.postDAO = new PostDAO();
    this.tagService = new TagService();
    this.postTagsService = new PostTagsService();
    this.userService = new UserService();
    this.reviewService = new ReviewService();
    this.commentService = new CommentService();
  }

  public Post create(Post post, Set<String> tags){

    Post newPost =  this.postDAO.create(post);
    log.debug("post created with id: {}", newPost.getId());

    tags.forEach(name -> {
      log.debug("Tag name: {}", name);

      Tag t = this.tagService.create(name);
      log.debug("Tag created id: {}", t.getId());
      log.debug("Tag created name: {}", t.getName());
      this.postTagsService.create(newPost.getId(), t.getId());
    });

    return  newPost;
  }

  public Post update(Long id, Post post){
    return this.postDAO.update(id, post);
  }

  public List<PostDTO> loadFeed(){
    List<Post> posts = this.postDAO.getAll();
    List<PostDTO> postDetails = new ArrayList<>();

    posts.forEach(post -> {
      PostDTO dto = new PostDTO();
      dto.setPost(post);
      dto.setAuthor(this.userService.get(post.getAuthorId()));

      List<Long> tagsId = this.postTagsService.getTagsIdByPostId(post.getId());
      List<Tag> tags = new ArrayList<>();
      tagsId.forEach(a -> tags.add(this.tagService.get(a)) );
      dto.setTags(tags);

      dto.setReviews(this.reviewService.getByPostId(post.getId()));
      dto.setComments(this.commentService.getByPostId(post.getId()));
      postDetails.add(dto);
    });

    return  postDetails;
  }

  public Post get(Long id){
    return this.postDAO.get(id);
  }

  public List<Post> getByAuthorId(Long id){
    return this.postDAO.getAll();

  }
}
