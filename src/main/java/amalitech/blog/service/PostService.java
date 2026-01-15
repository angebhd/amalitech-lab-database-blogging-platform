package amalitech.blog.service;

import amalitech.blog.dao.PostDAO;
import amalitech.blog.dto.PostDTO;
import amalitech.blog.model.Post;
import amalitech.blog.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PostService {

  private final PostDAO postDAO;
  private final TagService tagService;
  private final PostTagsService postTagsService;
  private final UserService userService;
  private final ReviewService reviewService;
  private final CommentService commentService;
  private final Logger log = LoggerFactory.getLogger(PostService.class);
  private Map<String, List<PostDTO>> cachedPostDTOs = new HashMap<>();
  private final Map<Long, List<Post>> cachedPostByAuthor = new HashMap<>();

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
    this.cachedPostDTOs = new HashMap<>();
    this.cachedPostByAuthor.remove(post.getAuthorId());
    return  newPost;
  }

  public Post update(Long id, Post post){
    this.cachedPostByAuthor.remove(post.getAuthorId());
    return this.postDAO.update(id, post);
  }

  public void delete(Long id){
    this.postDAO.delete(id);
  }

  public List<PostDTO> loadFeed() {
    return this.loadFeed(1, 20);
  }
    public List<PostDTO> loadFeed(int page, int pageSize){
    List<Post> posts = this.postDAO.getAll(page, pageSize);
    List<PostDTO> postDetails = new ArrayList<>();

    posts.forEach(post -> {
      PostDTO dto = new PostDTO();
      dto.setPost(post);
      var author = this.userService.get(post.getAuthorId());
      dto.setAuthorId(author.getId());
      dto.setAuthorName(author.getFirstName() + " " + author.getLastName());

      List<Long> tagsId = this.postTagsService.getTagsIdByPostId(post.getId());
      List<Tag> tags = new ArrayList<>();
      tagsId.forEach(a -> tags.add(this.tagService.get(a)) );
      dto.setTags(tags);

      dto.setReviews(this.reviewService.getByPostId(post.getId()));
      this.commentService.getByPostId(post.getId());
      postDetails.add(dto);
    });
      this.cachedPostDTOs = new HashMap<>();
    return  postDetails;
  }

  public List<PostDTO> loadFeed(boolean withPerformance) {
    return loadFeed(1, 20, withPerformance);
  }

    public List<PostDTO> loadFeed(int page, int pageSize, boolean withPerformance){
    if (!withPerformance)
      return loadFeed();
    if(this.cachedPostDTOs.get(this.makeCacheKeyforLoadFeed(page, pageSize))!= null)
      return this.cachedPostDTOs.get(this.makeCacheKeyforLoadFeed(page, pageSize));

    var res = this.postDAO.getPostDTOs(page, pageSize, null, null, null, false );
    this.cachedPostDTOs.put(makeCacheKeyforLoadFeed(page, pageSize), res);
    return res;
  }

  public PostDTO loadById(Long id){
    Post post = this.postDAO.get(id);

    PostDTO dto = new PostDTO();
    dto.setPost(post);
    var author = this.userService.get(post.getAuthorId());
    dto.setAuthorName(author.getFirstName() + " " + author.getLastName());
    List<Long> tagsId = this.postTagsService.getTagsIdByPostId(post.getId());
    List<Tag> tags = new ArrayList<>();
    tagsId.forEach(a -> tags.add(this.tagService.get(a)) );
    dto.setTags(tags);

    dto.setReviews(this.reviewService.getByPostId(post.getId()));
    this.commentService.getByPostId(post.getId());

    return  dto;
  }

  public Post getById(Long id){
    return this.postDAO.get(id);
  }

  public List<Post> getByAuthorId(Long id){
    return this.postDAO.getByAuthorId(id);
  }

  public List<Post> getByAuthorId(Long id, boolean withPerformance){
    if(!withPerformance)
      return this.postDAO.getByAuthorId(id);

    if ( cachedPostByAuthor.get(id) == null) {
      var posts = this.postDAO.getByAuthorId(id);
      cachedPostByAuthor.put(id, posts);
    }
    return this.cachedPostByAuthor.get(id);
  }

  private String makeCacheKeyforLoadFeed(int page, int pageSize){
    return page+"~"+pageSize;
  }
}
