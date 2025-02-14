package gaji.service.domain.post.service;

import gaji.service.domain.post.entity.CommnuityPost;
import gaji.service.domain.post.entity.CommunityComment;
import gaji.service.domain.post.web.dto.CommunityPostCommentResponseDTO;
import gaji.service.domain.post.web.dto.CommunityPostRequestDTO;
import gaji.service.domain.user.entity.User;
import org.springframework.data.domain.Slice;


public interface CommunityCommentService {

    CommunityComment saveNewComment(CommunityComment comment);
    CommunityComment createCommentByCheckParentCommentIdIsNull(Long parentCommentId, CommunityPostRequestDTO.WriteCommentRequestDTO request, User findUser, CommnuityPost findPost);
    void hardDeleteComment(CommunityComment comment);
    CommunityComment findByCommentId(Long commentId);
    CommunityPostCommentResponseDTO.PostCommentListDTO getCommentListByPost(Long userId, Long postId, Integer lastGroupNum, int page, int size);
    boolean isCommentWriter(Long userId, CommunityComment comment);
    void validCommentWriter(Long userId, CommunityComment comment);

}
