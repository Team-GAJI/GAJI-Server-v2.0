package gaji.service.domain.user.converter;

import com.querydsl.core.Tuple;
import gaji.service.domain.enums.PostTypeEnum;
import gaji.service.domain.post.entity.QCommnuityPost;
import gaji.service.domain.room.entity.QRoom;
import gaji.service.domain.user.entity.User;
import gaji.service.domain.user.web.dto.UserResponseDTO;
import gaji.service.global.converter.DateConverter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.stream.Collectors;

public class UserConverter {
    public static UserResponseDTO.CancleResultDTO toCancleResultDTO(User user) {
        return UserResponseDTO.CancleResultDTO.builder()
                .userId(user.getId())
                .userActive(user.getStatus())
                .build();
    }

    public static UserResponseDTO.UpdateNicknameResultDTO toUpdateNicknameResultDTO(User user) {
        return UserResponseDTO.UpdateNicknameResultDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
    }

    public static UserResponseDTO.GetRoomDTO toGetRoomDTO(Tuple tuple) {
        return UserResponseDTO.GetRoomDTO.builder()
                .roomId(tuple.get(QRoom.room.id))
                .name(tuple.get(QRoom.room.name))
                .description(tuple.get(QRoom.room.description))
                .thumbnail_url(tuple.get(QRoom.room.thumbnailUrl))
                .studyStartDay(tuple.get(QRoom.room.studyStartDay))
                .build();
    }

    public static UserResponseDTO.GetRoomListDTO toGetRoomListDTO(Slice<Tuple> roomList) {
        List<UserResponseDTO.GetRoomDTO> getRoomDTOList = roomList.stream()
                .map(UserConverter::toGetRoomDTO)
                .collect(Collectors.toList());

        return UserResponseDTO.GetRoomListDTO.builder()
                .roomList(getRoomDTOList)
                .hasNext(roomList.hasNext())
                .build();
    }

    public static UserResponseDTO.GetPostDTO toGetPostDTO(Tuple tuple) {
        return UserResponseDTO.GetPostDTO.builder()
                .postId(tuple.get(QCommnuityPost.commnuityPost.id))
                .title(tuple.get(QCommnuityPost.commnuityPost.title))
                .body(tuple.get(QCommnuityPost.commnuityPost.body))
                .type(tuple.get(QCommnuityPost.commnuityPost.type))
                .status(tuple.get(QCommnuityPost.commnuityPost.status))
                .userId(tuple.get(QCommnuityPost.commnuityPost.user.id))
                .nickname(tuple.get(QCommnuityPost.commnuityPost.user.nickname))
                .profileImagePth(tuple.get(QCommnuityPost.commnuityPost.user.profileImagePth))
                .createdAt(DateConverter.convertToRelativeTimeFormat(tuple.get(QCommnuityPost.commnuityPost.createdAt)))
                .viewCnt(tuple.get(QCommnuityPost.commnuityPost.hit))
                .likeCnt(tuple.get(QCommnuityPost.commnuityPost.likeCnt))
                .build();
    }

    public static UserResponseDTO.GetPostListDTO toGetPostListDTO(Slice<Tuple> postList, PostTypeEnum type) {
        List<UserResponseDTO.GetPostDTO> getPostDTOList = postList.stream()
                .map(UserConverter::toGetPostDTO)
                .collect(Collectors.toList());

        return UserResponseDTO.GetPostListDTO.builder()
                .postList(getPostDTOList)
                .hasNext(postList.hasNext())
                .build();
    }

    public static UserResponseDTO.GetUserDetailDTO toGetUserDetailDTO(User user) {
        return UserResponseDTO.GetUserDetailDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImagePth(user.getProfileImagePth())
                .build();
    }
}
