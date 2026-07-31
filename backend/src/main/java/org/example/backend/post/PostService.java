package org.example.backend.post;

import lombok.RequiredArgsConstructor;
import org.example.backend.domain.Post;
import org.example.backend.post.dto.PostCreateRequest;
import org.example.backend.user.entity.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    public Long createPost(PostCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(request.getCategory());
        post.setUser(user);

        Post savedPost = postRepository.save(post);
        return savedPost.getId();
    }
}