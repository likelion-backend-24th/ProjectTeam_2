package org.example.backend.study.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.study.dto.StudyPostRequest;
import org.example.backend.study.dto.StudyPostResponse;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.exception.StudyMemberNotFoundException;
import org.example.backend.study.exception.StudyNotFoundException;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPostService {

    private final StudyPostRepository studyPostRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;

    public StudyPostResponse createStudyPost(Long userId, Long id, StudyPostRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);

        StudyPost post = new StudyPost(study, user, request.getTitle(), request.getContent());
        StudyPost saved = studyPostRepository.save(post);

        return StudyPostResponse.from(saved);
    }

    public List<StudyPostResponse> getStudyPosts(Long userId, Long id) {
        Study study = studyRepository.findById(id).orElseThrow(StudyNotFoundException::new);
        studyMemberRepository.findByStudyIdAndUserId(id, userId)
                .orElseThrow(StudyMemberNotFoundException::new);

        List<StudyPost> posts = studyPostRepository.findAllByStudyId(id);
        return posts.stream()
                .map(StudyPostResponse::from)
                .toList();
    }
}
