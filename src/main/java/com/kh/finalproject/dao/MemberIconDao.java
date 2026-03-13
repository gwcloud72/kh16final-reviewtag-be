package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.MemberIconDto;

@Repository
public class MemberIconDao {

    @Autowired 
    private SqlSession sqlSession;

    // 1. 내 보유 아이콘 목록 조회
    public List<MemberIconDto> selectMyIcons(String memberId) {
        // Mapper의 #{memberId}와 연결됨
        return sqlSession.selectList("memberIcon.selectMyIcons", memberId);
    }

    // 2. 유저가 이미 해당 아이콘을 가졌는지 확인
    public int checkUserHasIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId); // Mapper의 #{memberId}
        params.put("iconId", iconId);     // Mapper의 #{iconId}
        return sqlSession.selectOne("memberIcon.checkUserHasIcon", params);
    }

    // 3. 유저에게 아이콘 지급
    public int insertMemberIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId); // Mapper의 #{memberId}
        params.put("iconId", iconId);     // Mapper의 #{iconId}
        return sqlSession.insert("memberIcon.insertMemberIcon", params);
    }

    // 4. 모든 아이콘 장착 해제
    public void unequipAllIcons(String memberId) {
        sqlSession.update("memberIcon.unequipAllIcons", memberId);
    }

    // 5. 특정 아이콘 장착
    public void equipIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId); // Mapper의 #{memberId}
        params.put("iconId", iconId);     // Mapper의 #{iconId}
        sqlSession.update("memberIcon.equipIcon", params);
    }

    public String selectEquippedIconSrc(String memberId) {
        Map<String, Object> param = new HashMap<>();
        param.put("memberId", memberId);
        return sqlSession.selectOne("memberIcon.selectEquippedIconSrc", param);
    }
    public String selectEquippedFrameStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedFrameStyle", memberId);
    }
    public String selectEquippedBgStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedBgStyle", memberId);
    }
    public String selectEquippedNickStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedNickStyle", memberId);
    }
    
 // 10. 특정 타입(테두리/배경/닉네임)의 모든 아이템 장착 해제
    public void unequipAllItemsByType(String memberId, String itemType) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("itemType", itemType); // 'deco_frame', 'deco_bg' 등
        sqlSession.update("memberIcon.unequipAllItemsByType", params);
    }

    // 11. 특정 인벤토리 아이템 장착
    public void equipItem(int inventoryNo) {
        // 인벤토리 번호(PK)만 알면 바로 장착 가능
        sqlSession.update("memberIcon.equipItem", inventoryNo);
    }
 
    public List<IconDto> selectIconList() {
    	return sqlSession.selectList("memberIcon.selectIconList");
    }

    public int deleteMemberIcon(long memberIconId) {
        return sqlSession.delete("memberIcon.deleteMemberIcon",memberIconId);
    }
        public List<MemberIconDto> selectUserIcon(String memberId) {
            return sqlSession.selectList("memberIcon.selectUserIconsDetail", memberId);
        }
    }

