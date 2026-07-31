package org.example.backend.post.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class PostUpdateRequest {
    private String title;
    private String content;
    private String category;
}
