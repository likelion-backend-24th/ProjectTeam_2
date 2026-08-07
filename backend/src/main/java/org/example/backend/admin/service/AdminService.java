package org.example.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.admin.dto.AdminUserResponse;
import org.example.backend.admin.exception.AdminErrorCode;
import org.example.backend.comment.entity.Comment;
import org.example.backend.comment.repository.CommentRepository;
import org.example.backend.common.exception.BusinessException;
import org.example.backend.post.entity.Post;
import org.example.backend.post.repository.PostRepository;
import org.example.backend.study.entity.Study;
import org.example.backend.study.entity.StudyPost;
import org.example.backend.study.entity.StudyPostComment;
import org.example.backend.study.repository.StudyMemberRepository;
import org.example.backend.study.repository.StudyPostCommentRepository;
import org.example.backend.study.repository.StudyPostRepository;
import org.example.backend.study.repository.StudyRepository;
import org.example.backend.user.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StudyRepository studyRepository;
    private final StudyPostRepository studyPostRepository;
    private final StudyPostCommentRepository studyPostCommentRepository;
    private final StudyMemberRepository studyMemberRepository;

    //유저 목록 조회
    public Page<AdminUserResponse> getUsers(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(user -> AdminUserResponse.from(user));
    }

    //유저 상태 변경
    @Transactional
    public void changeUserStatus(Long userId, AccountStatus status){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.USER_NOT_FOUND));

        if(status == AccountStatus.WITHDRAWN){
            throw new BusinessException(AdminErrorCode.INVALID_STATUS_CHANGE);
        }

        user.setStatus(status);
        userRepository.save(user);
    }

    // 게시글 강제 삭제
    @Transactional
    public void deletePost(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.POST_NOT_FOUND));

        postRepository.delete(post);
    }

    //댓글 강제 삭제
    @Transactional
    public void deleteComment(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

    //스터디 강제 삭제 (연관된 게시글, 댓글, 멤버까지 함께 삭제)
    @Transactional
    public void deleteStudy(Long studyId){
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.STUDY_NOT_FOUND));

        List<StudyPost> studyPosts = studyPostRepository.findAllByStudyId(studyId);
        for (StudyPost studyPost : studyPosts) {
            List<StudyPostComment> comments = studyPostCommentRepository.findAllByStudyPostId(studyPost.getId());
            studyPostCommentRepository.deleteAll(comments);
        }
        studyPostRepository.deleteAll(studyPosts);

        studyMemberRepository.deleteByStudyId(studyId);

        studyRepository.delete(study);
    }
}
