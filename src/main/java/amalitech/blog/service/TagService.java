package amalitech.blog.service;

import amalitech.blog.dao.TagDAO;
import amalitech.blog.model.Tag;

import java.util.List;

public class TagService {
  private final TagDAO tagDAO;
  private final PostTagsService postTagsService;

  public TagService(){
    this.tagDAO = new TagDAO();
    this.postTagsService = new PostTagsService();
  }
  public List<Tag> getAll(){
    return this.tagDAO.getAll();
  }


  public Tag get(Long id){
    return this.tagDAO.get(id);
  }

  public Tag create(String name){
    Tag exist = this.tagDAO.get(name);
    Tag t = new Tag();
    t.setName(name);
    if (exist != null){
      return exist;
    }
   return this.tagDAO.create(t);
  }

  public void updatePostTags(Long postId, List<String> tags){
    this.postTagsService.deletePostTags(postId);
    tags.forEach(tagName -> {
      Tag tag = this.create(tagName);
      this.postTagsService.create(postId, tag.getId());
    });
  }

}
