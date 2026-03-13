package com.kh.finalproject.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.IconDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.vo.AdminPointItemPageVO;

@Service
public class AdminAssetService {
    @Autowired private InventoryDao inventoryDao;
    @Autowired private MemberIconDao memberIconDao;
    @Autowired private PointItemStoreDao pointItemStoreDao;
    @Autowired private IconDao iconDao;

    // [1] 유저 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminMemberList(String keyword, int page) {
        int size = 10;
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;
        List<MemberDto> list = inventoryDao.fetchAdminMemberList(keyword, startRow, endRow);
        int totalCount = inventoryDao.countAdminMembers(keyword);
        int totalPage = (totalCount + size - 1) / size;

        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("totalPage", totalPage);
        return response;
    }

    // [2] 유저별 보유 자산 조회
    @Transactional(readOnly = true)
    public List<InventoryDto> getUserInventory(String memberId) {
        return inventoryDao.selectListByAdmin(memberId);
    }
    @Transactional(readOnly = true)
    public List<MemberIconDto> getUserIcons(String memberId) {
        return memberIconDao.selectUserIcon(memberId);
    }

    // [3] 마스터 아이템 목록 조회 (페이징 계산 추가)
    @Transactional(readOnly = true)
    public Map<String, Object> getMasterItemList(String type, String keyword, int page, int size) {
        AdminPointItemPageVO vo = new AdminPointItemPageVO();
        vo.setItemType(type);
        vo.setKeyword(keyword);
        vo.setPage(page);
        vo.setSize(size);
        
        List<com.kh.finalproject.dto.PointItemStoreDto> list = pointItemStoreDao.selectList(vo);
        int totalCount = pointItemStoreDao.selectCount(vo);
        int totalPage = (totalCount + size - 1) / size; // 여기서 페이지 수 계산

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage); // 프론트엔드와 이름 맞춤
        return result;
    }

    // [4] 마스터 아이콘 목록 조회 (페이징 적용)
    @Transactional(readOnly = true)
    public Map<String, Object> getMasterIconList(int page) {
        int size = 15;
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        List<IconDto> list = iconDao.selectListPaging(startRow, endRow, "ALL", "ALL");
        int totalCount = iconDao.countIcons("ALL", "ALL");
        int totalPage = (totalCount + size - 1) / size;

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage);
        return result;
    }

    // [5] 자산 회수 및 지급
    @Transactional
    public boolean recallAsset(String type, long id) {
        return "item".equals(type) ? inventoryDao.delete(id) : memberIconDao.deleteMemberIcon(id) > 0;
    }
    @Transactional
    public String grantAsset(String type, String memberId, int targetNo) {
        if ("item".equals(type)) {
            InventoryDto dto = new InventoryDto();
            dto.setInventoryMemberId(memberId);
            dto.setInventoryItemNo((long)targetNo);
            inventoryDao.insert(dto);
            return "success";
        } else {
            if (memberIconDao.checkUserHasIcon(memberId, targetNo) > 0) return "duplicate";
            memberIconDao.insertMemberIcon(memberId, targetNo);
            return "success";
        }
    }
}