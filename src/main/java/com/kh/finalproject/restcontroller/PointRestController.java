package com.kh.finalproject.restcontroller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.service.IconService;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.vo.PointDonateVO;
import com.kh.finalproject.vo.PointHistoryPageVO;
import com.kh.finalproject.vo.PointUseVO;

@RestController
@RequestMapping("/point") // 엔드포인트 구조 통일
@CrossOrigin
public class PointRestController {

    @Autowired private PointService pointService;
    @Autowired private IconService iconService;

    // =============================================================
    // [공통 예외 처리] 모든 메서드의 try-catch를 대신하여 에러 메시지 반환
    // =============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace();
        // 서비스에서 던진 에러 메시지를 프런트엔드로 400 에러와 함께 전송
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // =============================================================
    // [1] 포인트 이용 내역 & 후원
    // =============================================================

    /**
     * 1. 이용 내역 조회 (페이징 + 필터링)
     * GET /point/history?page=1&type=all
     */
    @GetMapping("/history")
    public ResponseEntity<PointHistoryPageVO> history(
            @RequestAttribute(required = false) String loginId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "all") String type) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        PointHistoryPageVO result = pointService.getHistoryList(loginId, page, type); //
        return ResponseEntity.ok(result);
    }

    /**
     * 2. 포인트 후원/선물하기
     * POST /point/donate
     */
    @PostMapping("/donate")
    public ResponseEntity<String> donatePoints(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointDonateVO vo) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        if (loginId.equals(vo.getTargetId())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("자신에게는 보낼 수 없습니다.");
        
        // 비즈니스 로직(금액 검증, 포인트 증감)은 Service에서 처리
        pointService.donatePoints(loginId, vo.getTargetId(), vo.getAmount());
        return ResponseEntity.ok("success");
    }

    // =============================================================
    // [2] 아이콘 사용자 기능 (뽑기, 조회, 장착)
    // =============================================================

    /**
     * 3. 아이콘 뽑기 실행
     * POST /point/icon/draw
     */
    @PostMapping("/icon/draw")
    public ResponseEntity<IconDto> drawIcon(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // 랜덤 뽑기 실행 및 결과 반환
        IconDto drawnIcon = iconService.drawRandomIcon(loginId, vo.getInventoryNo());
        return ResponseEntity.ok(drawnIcon);
    }

    /**
     * 4. 내 아이콘 보관함 조회
     * GET /point/icon/my
     */
    @GetMapping("/icon/my")
    public ResponseEntity<List<MemberIconDto>> myIcons(
            @RequestAttribute(required = false) String loginId) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<MemberIconDto> list = iconService.getMyIcons(loginId); //
        return ResponseEntity.ok(list);
    }
    
    /**
     * 5. 전체 아이콘(도감) 조회
     * GET /point/icon/all
     */
    @GetMapping("/icon/all")
    public ResponseEntity<List<IconDto>> allIcons() {
        return ResponseEntity.ok(iconService.getAllIcons()); //
    }

    /**
     * 6. 아이콘 장착
     * POST /point/icon/equip
     */
    @PostMapping("/icon/equip")
    public ResponseEntity<String> equipIcon(
            @RequestAttribute(required = false) String loginId, 
            @RequestBody IconDto dto) { 
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        iconService.equipIcon(loginId, dto.getIconId()); //
        return ResponseEntity.ok("success");
    }

    /**
     * 7. 아이콘 장착 해제 (모두 해제)
     * POST /point/icon/unequip
     */
    @PostMapping("/icon/unequip")
    public ResponseEntity<String> unequipIcon(@RequestAttribute(required = false) String loginId) {
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        iconService.unequipIcon(loginId); //
        return ResponseEntity.ok("success");
    }
}