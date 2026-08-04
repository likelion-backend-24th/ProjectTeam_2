package org.example.backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.post.entity.PostCategory;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

    private String title;
    private String content;
    private PostCategory category;
}
