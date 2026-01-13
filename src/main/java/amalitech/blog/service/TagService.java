package amalitech.blog.service;

import amalitech.blog.dao.TagDAO;
import amalitech.blog.model.Tag;

import java.util.List;

public class TagService {
  private final TagDAO tagDAO;

  public TagService(){
    this.tagDAO = new TagDAO();
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
}
