package org.example.backend.comment;

import org.example.backend.comment.exception.CommentAccessDeniedException;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.comment.service.CommentService;
import org.example.backend.comment.entity.Comment;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
        // given: 작성자가 1번 유저인 댓글이 있다고 가정
        User author = new User();
        author.setId(1L);

        Comment comment = new Comment();
        comment.setId(100L);
        comment.setUser(author);

        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        // when & then: 2번 유저가 삭제 시도하면 예외가 터져야 한다
        assertThatThrownBy(() -> commentService.deleteComment(100L, 2L))
                .isInstanceOf(CommentAccessDeniedException.class);
    }
}