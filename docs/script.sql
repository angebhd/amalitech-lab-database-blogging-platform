-- Script for creating Tables
-- Am using PostgreSQL
CREATE DATABASE blogging;
CREATE TABLE "users" (
  "id" BIGSERIAL PRIMARY KEY,
  "username" varchar(12) UNIQUE NOT NULL,
  "first_name" varchar(50),
  "last_name" varchar(50),
  "email" varchar(50) UNIQUE NOT NULL,
  "password" varchar(150) NOT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted_at" TIMESTAMP,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "tags" (
  "id" BIGSERIAL PRIMARY KEY,
  "name" varchar(50) UNIQUE NOT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted_at" TIMESTAMP ,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "posts" (
  "id" BIGSERIAL PRIMARY KEY,
  "author_id" BIGINT REFERENCES users (id) ON DELETE SET NULL,
  "title" varchar(50) NOT NULL,
  "body" text NOT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted_at" TIMESTAMP ,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "comments" (
  "id" BIGSERIAL PRIMARY KEY,
  "post_id" BIGINT NOT NULL  REFERENCES posts (id) ON DELETE CASCADE,
  "user_id" BIGINT NOT NULL  REFERENCES users (id) ON DELETE SET NULL,
  "body" varchar NOT NULL,
  "parent_comment" BIGINT  REFERENCES comments (id) ON DELETE CASCADE,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted_at" TIMESTAMP ,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false
);
CREATE TYPE e_review as ENUM('ONE', 'TWO', 'THREE', 'FOUR', 'FIVE');
CREATE TABLE "reviews" (
  "id" BIGSERIAL PRIMARY KEY,
  "post_id" BIGINT NOT NULL REFERENCES posts (id) ON DELETE SET NULL,
  "user_id" BIGINT NOT NULL REFERENCES users (id) ON DELETE SET NULL,
  "rate" e_review NOT NULL,
  "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted_at" TIMESTAMP ,
  "is_deleted" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "post_tags" (
  "post_id" BIGINT NOT NULL  REFERENCES posts (id) ON DELETE CASCADE,
  "tag_id" BIGINT NOT NULL  REFERENCES tags (id) ON DELETE CASCADE,
  PRIMARY KEY (post_id, tag_id)
);


CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_posts_created_at_desc  ON posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts (author_id);
CREATE INDEX IF NOT EXISTS idx_comments_post_id_created_at ON comments (post_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id ON post_tags (tag_id);

-- Improves search ILIKE %keyword%
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_posts_title_trgm ON posts USING GIN (title gin_trgm_ops);
