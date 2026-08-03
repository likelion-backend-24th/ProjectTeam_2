package org.example.backend.post;

import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.post.entity.Post;
import org.example.backend.post.exception.PostAccessDeniedException;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.post.service.PostService;
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
}