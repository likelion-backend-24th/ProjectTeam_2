package org.example.backend.post.service;

import org.example.backend.comment.entity.Comment;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.post.entity.Post;
import org.example.backend.post.entity.PostCategory;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.exception.PostErrorCode;
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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_ACCESS_DENIED);
    }

    @Test
    void ADMIN이어도_작성자_본인이_아니면_일반_삭제는_거부된다() {
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

        // when & then: 일반 deletePost는 소유자만 허용 — ADMIN도 본인 글이 아니면 거부된다.
        // 관리자 강제 삭제는 adminDeletePost 전용 경로로만 가능하다.
        assertThatThrownBy(() -> postService.deletePost(100L, admin))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_ACCESS_DENIED);
    }

    @Test
    void adminDeletePost는_소유권_체크_없이_삭제하고_딸린_댓글도_함께_지운다() {
        // given
        User author = new User();
        author.setId(1L);

        Post post = new Post();
        post.setId(100L);
        post.setUser(author);

        Comment comment = new Comment();
        comment.setId(1L);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.findAllByPostId(100L)).thenReturn(List.of(comment));

        // when
        postService.adminDeletePost(100L);

        // then
        org.mockito.Mockito.verify(commentRepository).deleteAll(List.of(comment));
        org.mockito.Mockito.verify(postRepository).delete(post);
    }

    @Test
    void 존재하지_않는_게시글을_상세조회하면_예외가_발생한다() {
        // given: 99999번 게시글은 존재하지 않는다고 가정
        when(postRepository.findById(99999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPostDetail(99999L, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 카테고리와_키워드가_없으면_전체_게시글을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(postRepository.searchPosts(null, null, pageable)).thenReturn(emptyPage);

        // when
        postService.getPosts(null, null, pageable);

        // then: category와 keyword가 둘 다 null이면 searchPosts(null, null, pageable)로 호출되어야 한다
        org.mockito.Mockito.verify(postRepository).searchPosts(null, null, pageable);
    }

    @Test
    void 카테고리가_있으면_해당_카테고리로_필터링해서_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(postRepository.searchPosts(PostCategory.JOB_INFO, null, pageable)).thenReturn(emptyPage);

        // when
        postService.getPosts(PostCategory.JOB_INFO, null, pageable);

        // then: category가 있으면 searchPosts(JOB_INFO, null, pageable)로 호출되어야 한다
        org.mockito.Mockito.verify(postRepository).searchPosts(PostCategory.JOB_INFO, null, pageable);
    }

    @Test
    void 키워드가_있으면_제목_내용으로_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(postRepository.searchPosts(null, "스터디", pageable)).thenReturn(emptyPage);

        // when
        postService.getPosts(null, "스터디", pageable);

        // then: keyword가 있으면 searchPosts(null, "스터디", pageable)로 호출되어야 한다
        org.mockito.Mockito.verify(postRepository).searchPosts(null, "스터디", pageable);
    }
}