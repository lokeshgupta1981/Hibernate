package com.howtodoinjava.basics.batch;

import com.howtodoinjava.basics.AbstractTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestBatching extends AbstractTest {

  @Override
  public List<Class<?>> getEntities() {
    return List.of(Post.class, Comment.class);
  }

  @Test
  public void testWithoutPeriodicFlush() {
    doInTransaction(session -> {

      //session.setJdbcBatchSize(10);

      for (int i = 1; i <= 10; i++) {
        System.out.println("Statement Queued : " + i);
        session.persist(new Post.PostBuilder()
            .title("title_" + i)
            .content("content_" + i)
            .build());
      }

    });
  }

  @Test
  public void testWithPeriodicFlush() {
    doInTransaction(session -> {

      for (int i = 1; i <= 10; i++) {
        System.out.println("Statement Queued : " + i);

        session.persist(new Post.PostBuilder()
            .title("title_" + i)
            .content("content_" + i)
            .build());

        if (i % 5 == 0) {
          session.flush();
          session.clear();
        }
      }

    });
  }

  @Test
  public void testInsertOrdering() {
    doInTransaction(session -> {

      for (int i = 1; i <= 10; i++) {

        List<Comment> comments = new ArrayList<>();

        for (int j = 1; j <= 4; j++) {
          Comment comment =
              new Comment.CommentBuilder().text("Comment - " + j).build();
          session.persist(comment);
          comments.add(comment);
        }

        Post post = new Post.PostBuilder()
            .title("title" + i)
            .content("content" + i)
            .comments(comments)
            .build();

        session.persist(post);
      }

    });
  }

  @Test
  public void testUpdatesOrdering() {
    List<Post> posts = new ArrayList<>();
    doInTransaction(session -> {
      for (int i = 1; i <= 10; i++) {
        Post post = new Post.PostBuilder()
            .title("title" + i)
            .content("content" + i)
            .build();
        session.persist(post);
        posts.add(post);
      }
    });

    doInTransaction(session -> {
      for (Post post : posts) {
        post.setTitle(post.getTitle() + "_Updated");
        session.merge(post);
      }
    });
  }
}