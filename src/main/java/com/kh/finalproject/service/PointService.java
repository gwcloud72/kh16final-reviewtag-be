package com.kh.finalproject.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.HeartDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.PointHistoryDao;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dao.PointWishlistDao;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.PointHistoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.vo.MemberPointVO;
import com.kh.finalproject.vo.PointHistoryPageVO;
import com.kh.finalproject.vo.PointItemWishVO;

@Service
public class PointService {

    @Autowired private PointItemStoreDao pointItemDao;
    @Autowired private MemberDao memberDao;
    @Autowired private InventoryDao inventoryDao; 
    @Autowired private PointHistoryDao pointHistoryDao;
    @Autowired private PointWishlistDao pointWishlistDao;
    @Autowired private DailyQuestService dailyQuestService;
    @Autowired private MemberIconDao memberIconDao;
    @Autowired private HeartDao heartDao;

    // [1] 포인트 증감 공통 메서드
    @Transactional
    public boolean addPoint(String loginId, int amount, String trxType, String reason) {
        MemberDto currentMember = memberDao.selectOne(loginId);
        if (currentMember == null) throw new RuntimeException("회원 정보가 없습니다.");

        // 차감 시 잔액 검증
        if (amount < 0 && (currentMember.getMemberPoint() + amount) < 0) {
            throw new RuntimeException("보유 포인트가 부족합니다.");
        }

      
        MemberDto updateDto = MemberDto.builder()
                .memberId(loginId)
                .memberPoint(amount) 
                .build();
        
        if (memberDao.upPoint(updateDto)) {
            pointHistoryDao.insert(PointHistoryDto.builder()
                .pointHistoryMemberId(loginId)
                .pointHistoryAmount(amount)
                .pointHistoryTrxType(trxType)
                .pointHistoryReason(reason)
                .build());
            return true;
        }
        return false;
    }

    
    @Transactional
    public void adminUpdatePoint(String memberId, int amount) {
        String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
        String AdminType = (amount > 0) ?  "GET" : "USE";
        boolean result = addPoint(memberId, amount, AdminType, reason);
        if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
    }
    
   

    @Transactional
    public void purchaseItem(String loginId, long itemNo) {
        // 1. 상품 정보 조회 및 기본 검증
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null) throw new RuntimeException("상품 정보가 없습니다.");

        // 2. 치장 아이템(DECO_) 중복 구매 체크 로직
        if (item.getPointItemType() != null && item.getPointItemType().startsWith("DECO_")) {
            // 제공해주신 DAO 메서드를 사용하여 인벤토리 조회
            InventoryDto existingItem = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
            
            if (existingItem != null) {
                // 이미 데이터가 존재한다면 구매 중단
                throw new RuntimeException("이미 보유 중인 꾸미기 아이템입니다.");
            }
        }

        // 3. 재고 확인
        if (item.getPointItemStock() <= 0) throw new RuntimeException("품절된 상품입니다.");

        // 4. 포인트 차감 (이후 로직은 동일)
        addPoint(loginId, -(int)item.getPointItemPrice(), "USE", "아이템 구매: " + item.getPointItemName());
        
        // 5. 재고 감소 및 DB 업데이트
        item.setPointItemStock(item.getPointItemStock() - 1);
        pointItemDao.update(item);

        // 6. 아이템 지급 처리
        if ("HEART_RECHARGE".equals(item.getPointItemType())) {
            chargeHeart(loginId, 5); 
        } else {
            giveItemToInventory(loginId, itemNo); 
        }

        // 7. 찜 목록 정리
        PointItemWishVO wishVO = PointItemWishVO.builder()
                .memberId(loginId)
                .itemNo(itemNo)
                .build();
        if (pointWishlistDao.checkWish(wishVO) > 0) {
            pointWishlistDao.delete(wishVO);
        }
    }
    // [3] 포인트 후원
    @Transactional
    public void donatePoints(String loginId, String targetId, int amount) {
        if (amount <= 0) throw new RuntimeException("후원 금액은 0보다 커야 합니다.");
        if (loginId.equals(targetId)) throw new RuntimeException("자신에게 후원할 수 없습니다.");

        addPoint(loginId, -amount, "USE", targetId + "님에게 후원");
        addPoint(targetId, amount, "GET", loginId + "님으로부터 후원");
    }

    // [4] 아이템 선물하기
    @Transactional
    public void giftItem(String loginId, String targetId, long itemNo) {
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null || item.getPointItemStock() <= 0) throw new RuntimeException("선물 가능한 상품이 없습니다.");

        // (기존 포인트 차감 및 재고 감소 로직...)
        addPoint(loginId, -(int)item.getPointItemPrice(), "USE", targetId + "님에게 선물: " + item.getPointItemName());
        item.setPointItemStock(item.getPointItemStock() - 1);
        pointItemDao.update(item);
        giveItemToInventory(targetId, itemNo);

        // ★ 추가: 선물 완료 후 보낸 사람의 위시리스트에서도 제거
        PointItemWishVO wishVO = PointItemWishVO.builder()
                .memberId(loginId)
                .itemNo(itemNo)
                .build();
        if (pointWishlistDao.checkWish(wishVO) > 0) {
            pointWishlistDao.delete(wishVO);
        }
    }

    // [5] 내 포인트 요약 정보 조회
    public MemberPointVO getMyPointInfo(String id) {
        MemberDto m = memberDao.selectOne(id); 
        if (m == null) return null;

        String iconSrc = memberIconDao.selectEquippedIconSrc(id);
        String frameStyle = memberIconDao.selectEquippedFrameStyle(id); 
        String bgStyle = memberIconDao.selectEquippedBgStyle(id); 
        String nickStyle = memberIconDao.selectEquippedNickStyle(id);

        if (iconSrc == null) iconSrc = "https://i.postimg.cc/tJdMNh4T/-Image-2025nyeon-12wol-21il-ohu-09-13-24.png";

        return MemberPointVO.builder()
                .memberId(m.getMemberId()) 
                .nickname(m.getMemberNickname())
                .point(m.getMemberPoint())
                .level(m.getMemberLevel()) 
                .iconSrc(iconSrc)
                .frameSrc(frameStyle) 
                .bgSrc(bgStyle)
                .nickStyle(nickStyle)
                .build();
    }

    // [6] 아이템 사용 및 장착 
    @Transactional
    public void useItem(String loginId, long inventoryNo, String extraValue) {
        InventoryDto inven = inventoryDao.selectOne(inventoryNo);
        if (inven == null || !inven.getInventoryMemberId().equals(loginId)) 
            throw new RuntimeException("아이템 권한 없음");

        PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
        String type = item.getPointItemType();

        switch (type) {
            case "CHANGE_NICK":
                if (extraValue == null || extraValue.trim().isEmpty()) 
                    throw new RuntimeException("새 닉네임을 입력하세요.");
                memberDao.updateNickname(MemberDto.builder()
                        .memberId(loginId)
                        .memberNickname(extraValue)
                        .build());
                decreaseInventoryOrDelete(inven);
                break;

            case "HEART_RECHARGE":
                chargeHeart(loginId, 5); 
                decreaseInventoryOrDelete(inven);
                break;

            case "RANDOM_POINT": // ⭐ 새로 추가된 랜덤 포인트 로직
                // 500 ~ 3500 사이의 100단위 숫자 생성
                // (0~30 사이 난수) * 100 + 500
                int randomIdx = new java.util.Random().nextInt(31); // 0~30
                int won = (randomIdx * 100) + 500;
                
                addPoint(loginId, won, "GET", "포인트 랜덤 박스 사용 + " + won + "원 획득");
                decreaseInventoryOrDelete(inven);
                break;

            case "DECO_NICK": case "DECO_BG": case "DECO_ICON": case "DECO_FRAME":
                // 중복 장착 방지: 해당 카테고리의 모든 'Y'를 'N'으로 먼저 변경
                unequipByType(loginId, type); 
                // 현재 아이템 장착
                inven.setInventoryEquipped("Y");
                inventoryDao.update(inven);
                break;
            
            case "VOUCHER":
                addPoint(loginId, (int)item.getPointItemPrice(), "GET", "상품권 사용 " + item.getPointItemPrice() + "원 획득");
                decreaseInventoryOrDelete(inven);
                break;
        }
    }

    // [7] 기타 환불 및 유틸리티
    @Transactional
    public void cancelItem(String loginId, long inventoryNo) {
        InventoryDto inven = inventoryDao.selectOne(inventoryNo);
        if (inven == null || !inven.getInventoryMemberId().equals(loginId)) throw new RuntimeException("환불 권한 없음");
        
        PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
        addPoint(loginId, (int)item.getPointItemPrice(), "GET", "환불: " + item.getPointItemName());

        item.setPointItemStock(item.getPointItemStock() + 1);
        pointItemDao.update(item);
        decreaseInventoryOrDelete(inven);
    }

    private void giveItemToInventory(String loginId, long itemNo) {
        InventoryDto existing = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
        if (existing != null) {
            existing.setInventoryQuantity(existing.getInventoryQuantity() + 1);
            inventoryDao.update(existing);
        } else {
            inventoryDao.insert(InventoryDto.builder()
                .inventoryMemberId(loginId)
                .inventoryItemNo(itemNo)
                .inventoryQuantity(1)
                .inventoryEquipped("N")
                .build());
        }
    }

    private void decreaseInventoryOrDelete(InventoryDto inven) {
        if (inven.getInventoryQuantity() > 1) {
            inven.setInventoryQuantity(inven.getInventoryQuantity() - 1);
            inventoryDao.update(inven);
        } else {
            inventoryDao.delete(inven.getInventoryNo());
        }
    }

    @Transactional
    public void unequipItem(String loginId, long inventoryNo) { 
        InventoryDto inv = inventoryDao.selectOne(inventoryNo);
        if(inv != null && loginId.equals(inv.getInventoryMemberId())) {
            inv.setInventoryEquipped("N"); 
            inventoryDao.update(inv);
        }
    }

    // 특정 카테고리(타입) 전체 해제 로직 통합
    private void unequipByType(String loginId, String type) {
        inventoryDao.unequipByType(loginId, type);
    }

    public PointHistoryPageVO getHistoryList(String loginId, int page, String type) {
        int size = 10;
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;
        List<PointHistoryDto> list = pointHistoryDao.selectListByMemberIdPaging(loginId, startRow, endRow, type);
        int totalCount = pointHistoryDao.countHistory(loginId, type);
        return PointHistoryPageVO.builder().list(list).totalCount(totalCount).totalPage((totalCount + size - 1) / size).currentPage(page).build();
    }

    @Transactional public int playRoulette(String loginId) {
        // 티켓 보유 여부 확인
        List<InventoryDto> userInventory = inventoryDao.selectListByMemberId(loginId);
        InventoryDto ticket = userInventory.stream()
                .filter(i -> {
                    PointItemStoreDto itemInfo = pointItemDao.selectOneNumber(i.getInventoryItemNo());
                    return itemInfo != null && "RANDOM_ROULETTE".equals(itemInfo.getPointItemType());
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("룰렛 티켓이 없습니다."));

        int idx = (int)(Math.random() * 6);
        int reward = (idx == 4) ? 2000 : (idx == 0) ? 1000 : 0;
        
        decreaseInventoryOrDelete(ticket);
        
        if (reward > 0) {
            addPoint(loginId, reward, "GET", "룰렛 당첨");
        }
        
        dailyQuestService.questProgress(loginId, "ROULETTE");
        return idx;
    }

    // 관리자 및 기타 기능
    @Transactional public void addItem(String loginId, PointItemStoreDto d) { pointItemDao.insert(d); }
    @Transactional public void editItem(String loginId, PointItemStoreDto d) { pointItemDao.update(d); }
    @Transactional public void deleteItem(String loginId, long itemNo) { pointItemDao.delete(itemNo); }
    @Transactional public void discardItem(String loginId, long inventoryNo) { inventoryDao.delete(inventoryNo); }
    
    @Transactional public boolean toggleWish(String loginId, long itemNo) {
        PointItemWishVO vo = PointItemWishVO.builder().memberId(loginId).itemNo(itemNo).build();
        if (pointWishlistDao.checkWish(vo) > 0) { 
            pointWishlistDao.delete(vo); 
            return false; 
        } else { 
            pointWishlistDao.insert(vo); 
            return true; 
        }
    }
    
    public List<Long> getMyWishItemNos(String loginId) { 
    	return pointWishlistDao.selectMyWishItemNos(loginId);
    	}
    
    public List<PointWishlistDto> getMyWishlist(String loginId) { 
    	return pointWishlistDao.selectMyWishlist(loginId); 
    	}
    
    @Transactional
    public void chargeHeart(String memberId, int amount) {
        if (memberId == null) throw new RuntimeException("로그인이 필요합니다.");
        if (heartDao.selectHeart(memberId) == null) {
            heartDao.createHeartWallet(memberId);
        }
        heartDao.increaseHeart(memberId, amount);
    }
}