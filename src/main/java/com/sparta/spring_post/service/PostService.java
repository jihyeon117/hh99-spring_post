package com.sparta.spring_post.service;

import com.sparta.spring_post.dto.PostRequestDto;
import com.sparta.spring_post.dto.PostResponseDto;
import com.sparta.spring_post.dto.UserResponseDto;
import com.sparta.spring_post.entity.Post;
import com.sparta.spring_post.entity.PostLike;
import com.sparta.spring_post.entity.Users;
import com.sparta.spring_post.exception.CustomException;
import com.sparta.spring_post.repository.PostLikeRepository;
import com.sparta.spring_post.repository.PostRepository;
import com.sparta.spring_post.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.sparta.spring_post.exception.ErrorCode.INVALID_USER;
import static com.sparta.spring_post.exception.ErrorCode.POST_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    // 전체 게시물 목록 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts(Pageable pageable) {
        List<PostResponseDto> posts = postRepository.findAll(pageable).stream().map(PostResponseDto::new).toList();
        if (posts.isEmpty()) {
            throw new CustomException(POST_NOT_FOUND);
        }
        return posts;
    }

    // 선택한 게시물 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(POST_NOT_FOUND)
        );
        return new PostResponseDto(post);
    }

    // 게시물 등록
    @Transactional
    public PostResponseDto createPost(PostRequestDto postRequestDto, Users user) {
        Post post = postRepository.saveAndFlush(new Post(postRequestDto, user));
        return new PostResponseDto(post);
    }

    // 게시물 수정
    @Transactional
    public PostResponseDto updatePost(Long id, PostRequestDto postRequestDto, Users user) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(POST_NOT_FOUND)
        );

        if (post.getUsers().getUsername().equals(user.getUsername()) || user.getRole().equals(user.getRole().ADMIN)) {
            post.update(postRequestDto);
            return new PostResponseDto(post);
        } else {
            throw new CustomException(INVALID_USER);
        }
    }

    // 게시물 삭제
    @Transactional
    public UserResponseDto<Post> deletePost(Long id, Users user) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(POST_NOT_FOUND)
        );

        if (post.getUsers().getUsername().equals(user.getUsername()) || user.getRole().equals(user.getRole().ADMIN)) {
            postRepository.delete(post);
            return UserResponseDto.setSuccess("게시글 삭제 성공");
        } else {
            throw new CustomException(INVALID_USER);
        }
    }

    // 좋아요
    @Transactional
    public UserResponseDto<Post> updateLike(Long id) {
        Post post = postRepository.findById(id).orElseThrow(
                () -> new CustomException(POST_NOT_FOUND)
        );

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Users user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            throw new CustomException(INVALID_USER);
        }

        if (postLikeRepository.findByPostAndUser(post, user) == null) {
            postLikeRepository.save(new PostLike(post, user));
            post.updateLike(true);
            return UserResponseDto.setSuccess("좋아요 성공");
        } else {
            PostLike postLike = postLikeRepository.findByPostAndUser(post, user);
            postLikeRepository.delete(postLike);
            post.updateLike(false);
            return UserResponseDto.setSuccess("좋아요 취소");
        }
    }

}