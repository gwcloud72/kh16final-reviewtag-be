package com.kh.finalproject.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.AttendanceHistoryDao;
import com.kh.finalproject.dao.AttendanceStatusDao;
import com.kh.finalproject.dto.AttendanceHistoryDto;
import com.kh.finalproject.dto.AttendanceStatusDto;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceStatusDao statusDao; // 현황판 DAO
    
    @Autowired
    private AttendanceHistoryDao historyDao; // 기록용 DAO
    
    @Autowired
    private PointService pointService; // 수정된 PointService 주입
    
    // [1] 출석체크 수행 (트랜잭션 관리)
    @Transactional
    public void checkAttendance(String loginId) {
        // 1. 내 현황 가져오기
        AttendanceStatusDto status = statusDao.selectOne(loginId);
        LocalDate today = LocalDate.now();
        
        if(status == null) {
            AttendanceStatusDto newStatus = AttendanceStatusDto.builder()
                    .attendanceStatusMemberId(loginId)
                    .attendanceStatusCurrent(1) // 첫날이므로 1일
                    .attendanceStatusMax(1)
                    .build();
            statusDao.insert(newStatus);
            
            processAttendanceCompletion(loginId, 1, "첫 출석 환영 보너스");
            return;
        }
        

        if (status.getAttendanceStatusLastdate() == null) {
            updateStatusAndComplete(status, 1, loginId, "출석 보상");
            return;
        }

        LocalDate lastDate = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();
        // 1) 오늘 이미 출석했는지 확인
        if(today.equals(lastDate)) {
            throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
        }
        
        // 2) 연속 출석 여부 계산 (어제 출석했으면 +1, 아니면 1)
        int currentStreak = 1;
        if(today.minusDays(1).equals(lastDate)) {
            currentStreak = status.getAttendanceStatusCurrent() + 1;
        }
        
        // 3) 현황 업데이트 및 완료 처리
        updateStatusAndComplete(status, currentStreak, loginId, "일일 출석 (" + currentStreak + "일 연속)");
    }

    // [2] 현황판 업데이트 및 최종 처리 (내부 헬퍼)
    private void updateStatusAndComplete(AttendanceStatusDto status, int streak, String loginId, String reason) {
        // 최고 기록 갱신
        if(streak > status.getAttendanceStatusMax()) {
            status.setAttendanceStatusMax(streak);
        }
        
        status.setAttendanceStatusCurrent(streak);
        statusDao.update(status); // 쿼리에서 TotalCount 및 LastDate 갱신 가정
        
        processAttendanceCompletion(loginId, streak, reason);
    }

    // [3] 기록 저장 및 포인트 지급 (PointService 로직 반영)
    private void processAttendanceCompletion(String loginId, int streak, String reason) {
        // 1. History 기록
        AttendanceHistoryDto history = AttendanceHistoryDto.builder()
                .attendanceHistoryMemberId(loginId)
                .build();
        historyDao.insert(history);
        
        // 2. 포인트 계산
        int point = 100; // 기본 점수
        if(streak > 0 && streak % 7 == 0) {
            point += 50; // 7일마다 보너스
        }
        
        // 3. 수정된 PointService의 addPoint 호출 (trxType: "GET")
        pointService.addPoint(loginId, point, "GET", reason);
    }

    // [4] 정보 조회 메서드들
    public AttendanceStatusDto getMyStatus(String loginId) {
        return statusDao.selectOne(loginId);
    }
    
    public List<String> getMyAttendanceDates(String loginId) {
        return historyDao.selectCalendarDates(loginId);
    }
    
    public boolean isTodayChecked(String loginId) {
        AttendanceStatusDto status = statusDao.selectOne(loginId);
        if (status == null || status.getAttendanceStatusLastdate() == null) {
            return false;
        }

        LocalDate last = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();
        return last.isEqual(LocalDate.now());
    }
}