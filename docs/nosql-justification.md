# NoSQL Justification - Blogging Platform

While the current implementation of the Blogging Platform uses a relational database (PostgreSQL), certain unstructured data types like **Comments** and **Reviews** could benefit from a NoSQL approach (e.g., MongoDB).

## Why NoSQL?

### 1. Schema Flexibility
Comments and reviews often evolve. One day we might want to add "Reactions" (like/dislike), multiple "Metadata" fields (user device, sentiment analysis results), or rich media (images/videos) within a comment. In SQL, this requires frequent migrations. In NoSQL, each document can have a different structure without affecting others.

### 2. Hierarchical Data (Threading)
Comments are inherently hierarchical. In SQL, retrieving deep comment threads often requires complex Recursive Common Table Expressions (CTEs) or multiple queries. NoSQL documents can store an entire comment tree in a single document (Embedding), allowing for extremely fast retrieval of a post's entire comment section in one operation.

### 3. Scalability
As a blogging platform grows, the volume of comments and reviews can explode. NoSQL databases are designed for horizontal scaling (sharding), allowing the data to be distributed across many servers much more easily than traditional relational databases.

## Proposed NoSQL Design for Comments
If implemented in MongoDB, the structure for comments might look like this:

```json
{
  "_id": "65a123...",
  "post_id": "987...",
  "user": {
    "id": "123...",
    "username": "jdoe",
    "avatar_url": "..."
  },
  "content": "This is a great post!",
  "reactions": {
    "likes": 50,
    "fire": 10
  },
  "replies": [
    {
      "user_id": "456...",
      "content": "I agree!",
      "timestamp": "2026-01-14..."
    }
  ],
  "timestamp": "2026-01-14..."
}
```

## Comparison Summary

| Feature | Relational (SQL) | NoSQL (Document) |
|---------|------------------|------------------|
| **Structure** | Tabular, Strict Schema | Flexible, JSON-like Documents |
| **Relationships** | Joins (Complex for nested data) | Embedding (Fast for retrieval) |
| **Scalability** | Vertical Scaling | Horizontal Scaling (Sharding) |
| **Integrity** | ACID Compliance | Eventual Consistency (Tunable) |

> [!NOTE]
> For this project, SQL was chosen to leverage robust ACID guarantees and clear referential integrity for core entities like Users and Posts.
