package amalitech.blog.dao;

import amalitech.blog.model.User;

import java.util.List;

public interface DAO <T, K>{

  T create(T entity);
  T get(K id);

  List<T> getAll(int page, int pageSize);
  T update(K id, T entity);
  boolean delete(K id);
}
