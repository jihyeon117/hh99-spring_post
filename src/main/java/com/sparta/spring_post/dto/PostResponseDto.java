package com.sparta.spring_post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sparta.spring_post.entity.Comment;
import com.sparta.spring_post.entity.Post;
import com.sparta.spring_post.entity.Users;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private Users user;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "YYYY-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "YYYY-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
    private List<Comment> comments;
    private int like;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.user = post.getUsers();
        this.createdAt = post.getCreatedAt();
        this.modifiedAt = post.getModifiedAt();
        this.comments = post.getComments();
        this.like = post.getLike();
    }
}