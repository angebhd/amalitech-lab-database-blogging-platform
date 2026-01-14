--- 200 users
INSERT INTO users (username, first_name, last_name, email, password)
SELECT
  'user' || gs,
  'First' || gs,
  'Last' || gs,
  'user' || gs || '@example.com',
  md5(random()::text)
FROM generate_series(1, 200) gs;



-- 50 tags
INSERT INTO tags (name)
SELECT 'TAG_' || gs
FROM generate_series(1, 50) gs;


-- 500 posts
INSERT INTO posts (author_id, title, body)
SELECT
  (SELECT id FROM users ORDER BY random() LIMIT 1),
  'Post Title ' || gs,
  'Body for post #' || gs || '. Lorem ipsum dolor sit amet.'
FROM generate_series(1, 500) gs;

-- post tags, 3 by tags
INSERT INTO post_tags (post_id, tag_id)
SELECT
  p.id,
  t.id
FROM posts p
JOIN LATERAL (
  SELECT id FROM tags ORDER BY random() LIMIT 3
) t ON true
ON CONFLICT DO NOTHING;


-- 1500 comments
INSERT INTO comments (post_id, user_id, body)
SELECT
  p.id,
  u.id,
  'Comment on post ' || p.id || ' by user ' || u.id
FROM posts p
JOIN LATERAL (
  SELECT id
  FROM users
  ORDER BY random()
  LIMIT (5 + floor(random() * 6))  -- 5–10 comments
) u ON true;



--- 1000 reviews
INSERT INTO reviews (post_id, user_id, rate)
SELECT
  p.id,
  u.id,
  (ARRAY['ONE','TWO','THREE','FOUR','FIVE'])[floor(random()*5)+1]::e_review
FROM posts p
JOIN LATERAL (
  SELECT id
  FROM users
  ORDER BY random()
  LIMIT (2 + floor(random() * 4)) -- 2–5 reviews
) u ON true;


