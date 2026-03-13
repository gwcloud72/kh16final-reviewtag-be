package com.kh.finalproject.restcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.vo.*;

@RestController
@RequestMapping("/point/main/store")
@CrossOrigin
public class PointStoreRestController {

    @Autowired private PointService pointService;
    @Autowired private PointItemStoreDao pointItemDao;
    @Autowired private InventoryDao inventoryDao;

    // =========================================================
    // [공통 예외 처리] 컨트롤러 내 에러 발생 시 자동 처리
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // =========================================================
    // [1] 상점 기능
    // =========================================================

    // 1. 전체 상품 조회
    @GetMapping("")
    public Map<String, Object> list(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        AdminPointItemPageVO vo = new AdminPointItemPageVO();
        vo.setItemType(type);
        vo.setKeyword(keyword);
        vo.setPage(page);
        vo.setSize(size);
        
        // VO 내부의 begin, end 계산 로직이 있다고 가정 (없다면 vo.getBegin() 등에서 계산 필요)
        List<PointItemStoreDto> list = pointItemDao.selectList(vo);
        int count = pointItemDao.selectCount(vo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalCount", count);
        return result;
    }

    // 2. 상품 구매
    @PostMapping("/buy")
    public ResponseEntity<String> buy(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointBuyVO vo) {
        
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        
        pointService.purchaseItem(loginId, vo.getBuyItemNo());
        return ResponseEntity.ok("success");
    }

    // 3. 선물하기
    @PostMapping("/gift")
    public ResponseEntity<String> gift(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointGiftVO vo) {
        
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        
        pointService.giftItem(loginId, vo.getTargetId(), vo.getItemNo());
        return ResponseEntity.ok("success");
    }

    // =========================================================
    // [2] 인벤토리 및 아이템 관리
    // =========================================================

    // 4. 내 보관함 조회
    @GetMapping("/inventory/my")
    public List<InventoryDto> myInventory(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return inventoryDao.selectListByMemberId(loginId);
    }

    // 5. 아이템 사용 (닉네임 변경 등)
    @PostMapping("/inventory/use")
    public ResponseEntity<String> useItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.useItem(loginId, vo.getInventoryNo(), vo.getExtraValue());
        return ResponseEntity.ok("success");
    }
    
    // 6. 구매 취소/환불
    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointCancelVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.cancelItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }
    
    // 7. 아이템 버리기 (영구 삭제)
    @PostMapping("/inventory/delete")
    public ResponseEntity<String> discardItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointCancelVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("fail");
        
        pointService.discardItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }

    // 8. 아이템 장착 해제
    @PostMapping("/inventory/unequip")
    public ResponseEntity<String> unequipItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.unequipItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }

    // =========================================================
    // [3] 위시리스트
    // =========================================================

    @PostMapping("/wish/toggle")
    public boolean toggleWish(@RequestAttribute(required = false) String loginId, @RequestBody PointItemWishVO vo) {
        if(loginId == null) return false;
        return pointService.toggleWish(loginId, vo.getItemNo());
    }

    @GetMapping("/wish/check")
    public List<Long> myWishItemNos(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return pointService.getMyWishItemNos(loginId);
    }

    @GetMapping("/wish/my")
    public List<PointWishlistDto> myWishlist(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return pointService.getMyWishlist(loginId);
    }

    // =========================================================
    // [4] 부가 기능
    // =========================================================

    @PostMapping("/roulette")
    public ResponseEntity<Integer> startRoulette(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(pointService.playRoulette(loginId));
    }

    @GetMapping("/my-info")
    public MemberPointVO getMyInfo(@RequestAttribute(required = false) String loginId) {
        if (loginId == null) return null; 
        return pointService.getMyPointInfo(loginId);
    }
    
    // =========================================================
    // [5] 관리자 기능
    // =========================================================

    @PostMapping("/item/add")
    public ResponseEntity<String> addItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.addItem(loginId, dto);
        return ResponseEntity.ok("success");
    }
    
    @PostMapping("/item/edit")
    public ResponseEntity<String> editItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.editItem(loginId, dto);
        return ResponseEntity.ok("success");
    }
    
    @PostMapping("/item/delete")
    public ResponseEntity<String> deleteItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.deleteItem(loginId, dto.getPointItemNo());
        return ResponseEntity.ok("success");
    }
    @GetMapping("/detail/{itemNo}")
    public ResponseEntity<PointItemStoreDto> detail(@PathVariable long itemNo) {
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }
    @PostMapping("/detail/{itemNo}/buy")
    public ResponseEntity<String> buy(
            @RequestAttribute(required = false) String loginId,
            @PathVariable long itemNo) { // @PathVariable로 경로상의 번호를 바로 받음

        if (loginId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // 서비스 호출 (상세 페이지에서 받은 itemNo 전달)
        pointService.purchaseItem(loginId, itemNo);

        return ResponseEntity.ok("success");
    }
}