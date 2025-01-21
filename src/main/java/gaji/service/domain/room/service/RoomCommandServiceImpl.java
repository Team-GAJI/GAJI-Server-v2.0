package gaji.service.domain.room.service;

import gaji.service.domain.enums.Role;
import gaji.service.domain.room.code.RoomErrorStatus;
import gaji.service.domain.room.entity.NoticeConfirmation;
import gaji.service.domain.room.entity.Room;
import gaji.service.domain.room.entity.RoomEvent;
import gaji.service.domain.room.entity.RoomNotice;
import gaji.service.domain.room.repository.*;
import gaji.service.domain.room.web.dto.RoomRequestDto;
import gaji.service.domain.room.web.dto.RoomResponseDto;
import gaji.service.domain.roomBoard.code.RoomPostErrorStatus;
import gaji.service.domain.studyMate.entity.Assignment;
import gaji.service.domain.studyMate.entity.StudyMate;
import gaji.service.domain.studyMate.entity.UserAssignment;
import gaji.service.domain.studyMate.entity.WeeklyUserProgress;
import gaji.service.domain.studyMate.repository.StudyMateRepository;
import gaji.service.domain.studyMate.service.StudyMateQueryService;
import gaji.service.domain.user.entity.User;
import gaji.service.domain.user.service.UserQueryServiceImpl;
import gaji.service.global.exception.RestApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoomCommandServiceImpl implements RoomCommandService {

    private final AssignmentRepository assignmentRepository;

    private final StudyMateRepository studyMateRepository;
    private final RoomEventRepository roomEventRepository;
    private final RoomQueryService roomQueryService;
    private final UserAssignmentRepository userAssignmentRepository;
    private final UserQueryServiceImpl userQueryService;
    private final StudyMateQueryService studyMateQueryService;
    private final RoomNoticeRepository roomNoticeRepository;
    private final NoticeConfirmationRepository noticeConfirmationRepository;
    private final RoomQueryRepository roomQueryRepository;
    private final RoomRepository roomRepository;
    private final WeeklyUserProgressRepository weeklyUserProgressRepository;

    //과제생성1
    @Override
    public List<Assignment> createAssignment(Long roomId, Long userId, Integer weeks, RoomRequestDto.AssignmentDto requestDto) {
        RoomEvent roomEvent = roomQueryService.findRoomEventByRoomIdAndWeeks(roomId, weeks);
        List<Assignment> savedAssignments = new ArrayList<>();

        for (String body : requestDto.getBodyList()) {
            Assignment assignment = Assignment.builder()
                    .roomEvent(roomEvent)
                    .body(body)
                    .build();

            Assignment savedAssignment = assignmentRepository.save(assignment);
            savedAssignments.add(savedAssignment);

            createUserAssignmentsForStudyMembers(savedAssignment);
        }

        updateWeeklyUserProgressForNewAssignments(savedAssignments);

        return savedAssignments;
    }

    @Override
    public RoomNotice createNotice(Long roomId, Long userId, RoomRequestDto.RoomNoticeDto requestDto) {
        User user = userQueryService.findUserById(userId);
        Room room = roomQueryService.findRoomById(roomId);
        StudyMate studyMate = studyMateQueryService.findByUserIdAndRoomId(user.getId(), room.getId());

        RoomNotice notice = RoomNotice.builder()
                .title(requestDto.getTitle())
                .body(requestDto.getBody())
                .studyMate(studyMate)
                .build();
        return roomNoticeRepository.save(notice);

    }

    // 과제 생성할 때 user에게 할당해주는 메서드
    @Override
    public void createUserAssignmentsForStudyMembers(Assignment assignment) {
        List<StudyMate> studyMates = studyMateRepository.findByRoom(assignment.getRoomEvent().getRoom());

        for (StudyMate studyMate : studyMates) {
            User user = studyMate.getUser();

            UserAssignment userAssignment = UserAssignment.builder()
                    .user(user)
                    .assignment(assignment)
                    .isComplete(false)
                    .build();
            userAssignmentRepository.save(userAssignment);
        }
    }
    @Override
    public Assignment updateAssignment(Long assignmentId, String newBody) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RestApiException(RoomErrorStatus._ASSIGNMENT_NOT_FOUND));

        assignment.updateBody(newBody);

        return assignment;
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RestApiException(RoomErrorStatus._ASSIGNMENT_NOT_FOUND));

        // UserAssignment들은 CascadeType.ALL과 orphanRemoval = true 설정으로 인해 자동으로 삭제됩니다.
        assignmentRepository.delete(assignment);
    }

    @Override
    public RoomEvent setStudyPeriod(Long roomId, Integer weeks, Long userId, RoomRequestDto.StudyPeriodDto requestDto) {
        User user = userQueryService.findUserById(userId);
        Room room = roomQueryService.findRoomById(roomId);
        StudyMate studyMate = studyMateQueryService.findByUserIdAndRoomId(user.getId(),roomId);

        if (!studyMate.getRole().equals(Role.READER)) {
            throw new RestApiException(RoomErrorStatus._USER_NOT_READER_IN_ROOM);
        }

        RoomEvent roomEvent = roomEventRepository.findRoomEventByRoomIdAndWeeks(roomId,weeks)
                .orElse(RoomEvent.builder().room(room).user(user).build());

        RoomEvent updatedRoomEvent = RoomEvent.builder()
                .id(roomEvent.getId())
                .weeks(weeks)
                .room(room)
                .user(user)
                .startTime(requestDto.getStartDate())
                .endTime(requestDto.getEndDate())
                .title(roomEvent.getTitle())
                .description(roomEvent.getDescription())
                .isPublic(roomEvent.isPublic())
                .build();

        return roomEventRepository.save(updatedRoomEvent);
    }



    @Override
    public RoomEvent setStudyDescription(Long roomId, Integer weeks, Long userId, RoomRequestDto.StudyDescriptionDto requestDto) {
        User user = userQueryService.findUserById(userId);
        Room room = roomQueryService.findRoomById(roomId);
        StudyMate studyMate = studyMateQueryService.findByUserIdAndRoomId(user.getId(),roomId);


        if (!studyMate.getRole().equals(Role.READER)) {
            throw new RestApiException(RoomErrorStatus._USER_NOT_READER_IN_ROOM);
        }

        RoomEvent roomEvent = roomEventRepository.findRoomEventByRoomIdAndWeeks(roomId,weeks)
                .orElse(RoomEvent.builder().room(room).user(user).build());

        RoomEvent updatedRoomEvent = RoomEvent.builder()
                .id(roomEvent.getId())
                .weeks(weeks)
                .room(room)
                .user(user)
                .startTime(roomEvent.getStartTime())
                .endTime(roomEvent.getEndTime())
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .isPublic(roomEvent.isPublic())
                .build();

        return roomEventRepository.save(updatedRoomEvent);
    }

    @Override
    public RoomEvent updateRoomEvent(Long roomId, Integer weeks, RoomRequestDto.RoomEventUpdateDTO updateDTO) {
        RoomEvent roomEvent = roomEventRepository.findRoomEventByRoomIdAndWeeks(roomId,weeks)
                .orElseThrow(() -> new RestApiException(RoomErrorStatus._ROOM_EVENT_NOT_FOUND));

        roomEvent.updateEvent(updateDTO.getStartTime(), updateDTO.getEndTime(), updateDTO.getDescription());
        roomEventRepository.save(roomEvent);

        return roomEvent;
    }

    @Override
    public boolean toggleNoticeConfirmation(Long roomId, Long noticeId, Long userId) {
        RoomNotice roomNotice = roomNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new RestApiException(RoomErrorStatus._NOTICE_NOT_FOUND));

        StudyMate studyMate = studyMateRepository.findByRoomIdAndUserId(roomId,userId);


        NoticeConfirmation existingConfirmation = noticeConfirmationRepository
                .findByRoomNoticeIdAndStudyMateId(noticeId, roomNotice.getStudyMate().getId());
        System.out.println("공지사항 내용: " + existingConfirmation);
        if (existingConfirmation != null) {
            noticeConfirmationRepository.delete(existingConfirmation);
        } else {
            NoticeConfirmation confirmation = NoticeConfirmation.builder()
                    .roomNotice(roomNotice)
                    .studyMate(studyMate)
                    .build();
            noticeConfirmationRepository.save(confirmation);
        }

        // 확인 수 업데이트
        roomQueryRepository.updateConfirmCount(noticeId);

        return existingConfirmation == null; // true if confirmation was added, false if removed

    }

    @Override
    public void saveRoom(Room room) {
        roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Room room) {
        roomRepository.delete(room);
    }
    @Override
    public RoomResponseDto.AssignmentProgressResponse toggleAssignmentCompletion(Long userId, Long userAssignmentId) {
        UserAssignment userAssignment = userAssignmentRepository.findById(userAssignmentId)
                .orElseThrow(() -> new RestApiException(RoomErrorStatus._ASSIGNMENT_NOT_FOUND));

        User user = userQueryService.findUserById(userId);
        RoomEvent roomEvent = userAssignment.getAssignment().getRoomEvent();

        // Toggle completion status
        userAssignment.setComplete(!userAssignment.isComplete());
        userAssignmentRepository.save(userAssignment);

        // Calculate and save progress
        WeeklyUserProgress progress = calculateAndSaveProgress(roomEvent, user);

        // Prepare response
        boolean isCompleted = progress.getProgressPercentage() >= 100.0;
        LocalDate deadline = roomEvent.getEndTime();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

        return RoomResponseDto.AssignmentProgressResponse.builder()
                .progressPercentage(progress.getProgressPercentage())
                .completedAssignments(progress.getCompletedAssignments())
                .totalAssignments(progress.getTotalAssignments())
                .isCompleted(isCompleted)
                .deadline(deadline)
                .daysLeft(daysLeft)
                .build();
    }


    @Override
    public WeeklyUserProgress calculateAndSaveProgress(RoomEvent roomEvent, User user) {
        int totalAssignments = roomEvent.getAssignmentList().size();

        int completedAssignments = (int) roomEvent.getAssignmentList().stream()
                .flatMap(assignment -> assignment.getUserAssignmentList().stream())
                .filter(userAssignment -> userAssignment.getUser().equals(user) && userAssignment.isComplete())
                .count();

        double progressPercentage = totalAssignments > 0
                ? ((double) completedAssignments / totalAssignments) * 100
                : 0.0;

        WeeklyUserProgress progress = weeklyUserProgressRepository
                .findByRoomEventAndUser(roomEvent, user)
                .orElseGet(() -> {
                    WeeklyUserProgress newProgress = WeeklyUserProgress.createEmpty();
                    newProgress.setUser(user);
                    newProgress.setRoomEvent(roomEvent);
                    return newProgress;
                });

        progress.setTotalAssignments(totalAssignments);
        progress.setCompletedAssignments(completedAssignments);
        progress.setProgressPercentage(progressPercentage);

        return weeklyUserProgressRepository.save(progress);
    }
    private void updateWeeklyUserProgressForNewAssignment(Assignment assignment) {
        RoomEvent roomEvent = assignment.getRoomEvent();
        List<StudyMate> studyMates = studyMateRepository.findByRoom(roomEvent.getRoom());

        for (StudyMate studyMate : studyMates) {
            User user = studyMate.getUser();
            WeeklyUserProgress progress = weeklyUserProgressRepository
                    .findByRoomEventAndUser(roomEvent, user)
                    .orElseGet(() -> WeeklyUserProgress.createInitialProgress(user, roomEvent, 0));

            progress.updateProgress(progress.getCompletedAssignments());
            weeklyUserProgressRepository.save(progress);
        }
    }

    private void updateWeeklyUserProgressForNewAssignments(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return;
        }

        RoomEvent roomEvent = assignments.get(0).getRoomEvent();
        List<StudyMate> studyMates = studyMateRepository.findByRoom(roomEvent.getRoom());

        for (StudyMate studyMate : studyMates) {
            User user = studyMate.getUser();
            WeeklyUserProgress progress = weeklyUserProgressRepository
                    .findByRoomEventAndUser(roomEvent, user)
                    .orElseGet(() -> WeeklyUserProgress.createInitialProgress(user, roomEvent, 0));

            int totalAssignments = progress.getTotalAssignments() + assignments.size();
            progress.setTotalAssignments(totalAssignments);
            progress.updateProgress(progress.getCompletedAssignments());
            weeklyUserProgressRepository.save(progress);
        }
    }

    @Override
    public void deleteRoomNotice(Long noticeId, Long userId) {
        User user = userQueryService.findUserById(userId);
        RoomNotice roomNotice = roomQueryService.findRoomNoticeById(noticeId);

        if(roomNotice.getStudyMate().getUser().equals(user)){
            roomNoticeRepository.delete(roomNotice);
        }else{
            throw new RestApiException(RoomPostErrorStatus._USER_NOT_DELETE_AUTH);
        }
    }

    @Override
    public RoomNotice updateRoomNotice(Long noticeId, Long userId, String description) {
        User user = userQueryService.findUserById(userId);
        RoomNotice roomNotice = roomQueryService.findRoomNoticeById(noticeId);

        if(roomNotice.getStudyMate().getUser().equals(user)){
            roomNotice.updateBody(description);
        }else{
            throw new RestApiException(RoomPostErrorStatus._USER_NOT_UPDATE_AUTH);
        }

        return roomNotice;
    }
}
