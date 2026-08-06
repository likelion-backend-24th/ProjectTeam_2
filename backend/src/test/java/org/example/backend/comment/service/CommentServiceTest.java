package org.example.backend.comment.service;

import org.example.backend.comment.exception.CommentErrorCode;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.comment.entity.Comment;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.entity.Post;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.user.entity.Role;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void 작성자_본인이_아니면_삭제시_예외가_발생한다() {
        // given: 1번 게시글에 달린, 작성자가 1번 유저인 댓글이 있다고 가정
        Post post = new Post();
        post.setId(1L);

        User author = new User();
        author.setId(1L);

        User requester = new User();
        requester.setId(2L);

        Comment comment = new Comment();
        comment.setId(100L);
        comment.setPost(post);
        comment.setUser(author);

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        // when & then: 2번 유저가 삭제 시도하면 예외가 터져야 한다
        assertThatThrownBy(() -> commentService.deleteComment(1L, 100L, requester))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_ACCESS_DENIED);
    }

    @Test
    void ADMIN이면_작성자_본인이_아니어도_삭제가_허용된다() {
        // given: 1번 게시글에 달린, 작성자는 1번 유저, 삭제 시도자는 ADMIN 권한의 3번 유저
        Post post = new Post();
        post.setId(1L);

        User author = new User();
        author.setId(1L);

        User admin = new User();
        admin.setId(3L);
        admin.setRole(Role.ADMIN);

        Comment comment = new Comment();
        comment.setId(100L);
        comment.setPost(post);
        comment.setUser(author);

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        // when & then: ADMIN이면 예외 없이 삭제가 통과되어야 한다
        assertThatCode(() -> commentService.deleteComment(1L, 100L, admin))
                .doesNotThrowAnyException();
    }

    @Test
    void 존재하지_않는_댓글을_수정하면_예외가_발생한다() {
        // given: 99999번 댓글은 존재하지 않는다고 가정
        when(commentRepository.findById(99999L)).thenReturn(Optional.empty());

        User requester = new User();
        requester.setId(1L);

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(1L, 99999L, null, requester))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 댓글이_다른_게시글_소속이면_예외가_발생한다() {
        // given: 댓글은 1번 게시글 소속인데, 2번 게시글 기준으로 접근 시도
        Post post = new Post();
        post.setId(1L);

        User author = new User();
        author.setId(1L);

        Comment comment = new Comment();
        comment.setId(100L);
        comment.setPost(post);
        comment.setUser(author);

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        // when & then: postId(2L)가 댓글의 실제 postId(1L)와 다르므로 예외가 터져야 한다
        assertThatThrownBy(() -> commentService.updateComment(2L, 100L, null, author))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(CommentErrorCode.COMMENT_POST_MISMATCH);
    }
}