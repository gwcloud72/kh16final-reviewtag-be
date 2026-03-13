package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;

@Repository
public class InventoryDao {

    @Autowired
    private SqlSession sqlSession;

    // [1] 아이템 신규 추가 (구매 시)
    public int insert(InventoryDto inventoryDto) {
        return sqlSession.insert("inventory.insert", inventoryDto);
    }
    // [2] 아이템 수량/상태 수정 (수량 증가 또는 장착 여부 변경)
    public boolean update(InventoryDto inventoryDto) {
        return sqlSession.update("inventory.update", inventoryDto) > 0;    
    }
    // [3] 특정 회원의 전체 인벤토리 조회
    public List<InventoryDto> selectListByMemberId(String memberId) {
        return sqlSession.selectList("inventory.selectListByMemberId", memberId);
    }

    // [4] 인벤토리 번호로 단일 항목 조회 (사용/환불 시)
    public InventoryDto selectOne(long inventoryNo) {
        return sqlSession.selectOne("inventory.selectOne", inventoryNo);
    }
    
    // [5] ★ 중요: 특정 회원이 특정 아이템을 가지고 있는지 확인 (서비스 로직용)
    // PointService의 giveItemToInventory 메서드에서 사용됩니다.
    public InventoryDto selectOneByMemberAndItem(String memberId, long itemNo) {
        InventoryDto params = new InventoryDto();
        params.setInventoryMemberId(memberId);
        params.setInventoryItemNo(itemNo);
        return sqlSession.selectOne("inventory.selectOneByMemberAndItem", params);
    }

    // [6] 특정 아이템 보유 개수 확인 (단순 카운트용)
    public int selectCountMyItem(String memberId, long itemNo) {
        InventoryDto params = new InventoryDto();
        params.setInventoryMemberId(memberId);
        params.setInventoryItemNo(itemNo); 
        return sqlSession.selectOne("inventory.selectCountMyItem", params);
    }

    // [7] 아이템 삭제 (수량 0일 때 또는 관리자 강제 삭제)
    public boolean delete(long inventoryNo) {
        return sqlSession.delete("inventory.delete", inventoryNo) > 0;
    }

    // [8] 관리자용 특정 사용자 인벤토리 조회
    public List<InventoryDto> selectListByAdmin(String memberId) {
        return sqlSession.selectList("inventory.selectListByAdmin", memberId);
    }
    
    public void unequipByType(String memberId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        sqlSession.update("inventory.unequipByType", params);
    }
    
 // [1] 관리자용 유저 목록 조회 (검색 + 페이징)
    public List<MemberDto> fetchAdminMemberList(String keyword, int startRow, int endRow) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("startRow", startRow);
        params.put("endRow", endRow);
        return sqlSession.selectList("inventory.fetchAdminMemberList", params);
    }

    // [2] 관리자용 유저 카운트 (페이징용)
    public int countAdminMembers(String keyword) {
        return sqlSession.selectOne("inventory.countAdminMembers", keyword);
    }
    
}

	