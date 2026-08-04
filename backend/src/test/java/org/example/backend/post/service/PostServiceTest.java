package org.example.backend.post.service;

import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.example.backend.post.exception.PostAccessDeniedException;
import org.example.backend.post.exception.PostNotFoundException;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void 작성자_본인이_아니면_삭제시_예외가_발생한다() {
        // given: 작성자가 1번 유저인 게시글이 있다고 가정
        User author = new User();
        author.setId(1L);

        User requester = new User();
        requester.setId(2L);

        Post post = new Post();
        post.setId(100L);
        post.setUser(author);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        // when & then: 2번 유저가 삭제 시도하면 예외가 터져야 한다
        assertThatThrownBy(() -> postService.deletePost(100L, requester))
                .isInstanceOf(PostAccessDeniedException.class);
    }

    @Test
    void ADMIN이면_작성자_본인이_아니어도_삭제가_허용된다() {
        // given: 작성자는 1번 유저, 삭제 시도자는 ADMIN 권한의 3번 유저
        User author = new User();
        author.setId(1L);

        User admin = new User();
        admin.setId(3L);
        admin.setRole(Role.ADMIN);

        Post post = new Post();
        post.setId(100L);
        post.setUser(author);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        // when & then: ADMIN이면 예외 없이 삭제가 통과되어야 한다
        assertThatCode(() -> postService.deletePost(100L, admin))
                .doesNotThrowAnyException();
    }

    @Test
    void 존재하지_않는_게시글을_상세조회하면_예외가_발생한다() {
        // given: 99999번 게시글은 존재하지 않는다고 가정
        when(postRepository.findById(99999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPostDetail(99999L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 카테고리가_null이면_전체_게시글을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(postRepository.findAll(pageable)).thenReturn(emptyPage);

        // when
        postService.getPosts(null, pageable);

        // then: category가 null이면 findByCategory가 아니라 findAll이 호출되어야 한다
        org.mockito.Mockito.verify(postRepository).findAll(pageable);
        org.mockito.Mockito.verify(postRepository, org.mockito.Mockito.never())
                .findByCategory(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void 카테고리가_있으면_해당_카테고리로_필터링해서_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(postRepository.findByCategory(PostCategory.JOB_INFO, pageable)).thenReturn(emptyPage);

        // when
        postService.getPosts(PostCategory.JOB_INFO, pageable);

        // then: category가 있으면 findByCategory가 호출되어야 한다
        org.mockito.Mockito.verify(postRepository).findByCategory(PostCategory.JOB_INFO, pageable);
        org.mockito.Mockito.verify(postRepository, org.mockito.Mockito.never()).findAll(pageable);
    }
}