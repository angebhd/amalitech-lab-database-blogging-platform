package amalitech.blog.service;

import amalitech.blog.dao.PostDAO;
import amalitech.blog.model.Post;
import amalitech.blog.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class PostService {

  private final PostDAO postDAO;
  private final TagService tagService;
  private final PostTagsService postTagsService;
  private final Logger log = LoggerFactory.getLogger(PostService.class);

  public PostService(){
    this.postDAO = new PostDAO();
    this.tagService = new TagService();
    this.postTagsService = new PostTagsService();
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

  public Post get(Long id){
    return this.postDAO.get(id);
  }

  public List<Post> getByAuthorId(Long id){
    return this.postDAO.getAll();

  }
}
