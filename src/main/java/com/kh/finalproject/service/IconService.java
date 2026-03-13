package com.kh.finalproject.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.IconDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.PointHistoryDao;
import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.dto.PointHistoryDto;
import com.kh.finalproject.vo.IconPageVO;

@Service
public class IconService {

    @Autowired private IconDao iconDao;         
    @Autowired private MemberIconDao memberIconDao; 
    @Autowired private InventoryDao inventoryDao;
    @Autowired private MemberDao memberDao;       
    @Autowired private PointHistoryDao pointHistoryDao;

    // -----------------------------------------------------
    // 1. [관리자] CRUD 기능
    // -----------------------------------------------------
    public List<IconDto> getAllIcons() { 
        return iconDao.selectList(); 
    }

    public IconPageVO getIconList(int page, String category, String rarity) {
        int size = 10;
        int endRow = page * size;
        int startRow = endRow - (size - 1);

        // (1) 검색 조건에 따른 전체 개수 조회
        int totalCount = iconDao.countIcons(category, rarity);

        // (2) 검색 조건에 따른 페이징 목록 조회
        List<IconDto> list = iconDao.selectListPaging(startRow, endRow, category, rarity);

        int totalPage = (totalCount + size - 1) / size;

        return IconPageVO.builder()
                .list(list)
                .totalPage(totalPage)
                .currentPage(page)
                .totalCount(totalCount)
                .build();
    }

    @Transactional
    public void addIcon(IconDto dto) { 
        iconDao.insert(dto); 
    }
    
    @Transactional
    public void editIcon(IconDto dto) { 
        iconDao.update(dto); 
    }
    
    @Transactional
    public void removeIcon(int iconId) { 
        iconDao.delete(iconId); 
    }

    // -----------------------------------------------------
    // 2. [사용자] 기능 (보유 목록, 뽑기, 장착)
    // -----------------------------------------------------
    
    // 내 보유 아이콘 목록
    public List<MemberIconDto> getMyIcons(String memberId) {
       
        return memberIconDao.selectMyIcons(memberId);
    }   

    // 대망의 [아이콘 뽑기] 로직
    @Transactional
    public IconDto drawRandomIcon(String memberId, long inventoryNo) { 
        
        // 1. [티켓 차감]
   
        InventoryDto ticket = inventoryDao.selectOne((int)inventoryNo); 
        
        if (ticket == null || !ticket.getInventoryMemberId().equals(memberId)) {
            throw new RuntimeException("티켓을 찾을 수 없거나 사용 권한이 없습니다.");
        }
        
        if (ticket.getInventoryQuantity() > 1) {
            ticket.setInventoryQuantity(ticket.getInventoryQuantity() - 1);
            inventoryDao.update(ticket);
        } else {
            inventoryDao.delete((int)inventoryNo);
        }
    
        // 2. [확률 뽑기]
        List<IconDto> allIcons = iconDao.selectList(); 
        
        List<IconDto> gachaPool = allIcons.stream()
                .filter(i -> !"EVENT".equalsIgnoreCase(i.getIconRarity()))
                .collect(Collectors.toList()); 
    
        if(gachaPool.isEmpty()) throw new RuntimeException("뽑기 가능한 아이콘이 없습니다.");
    
        double random = Math.random() * 100; 
        String targetRarity = "COMMON";
        
        if (random < 0.5) targetRarity = "LEGENDARY";
        else if (random < 3.0) targetRarity = "UNIQUE";
        else if (random < 10.0) targetRarity = "EPIC";
        else if (random < 40.0) targetRarity = "RARE";
    
        String finalRarity = targetRarity;
        List<IconDto> pool = gachaPool.stream()
                .filter(i -> i.getIconRarity().equalsIgnoreCase(finalRarity))
                .collect(Collectors.toList());
    
        if (pool.isEmpty()) { 
            pool = gachaPool.stream()
                    .filter(i -> i.getIconRarity().equalsIgnoreCase("COMMON"))
                    .collect(Collectors.toList());
        }
    
        IconDto picked = pool.get((int)(Math.random() * pool.size()));
    
        // 3. [지급 및 중복 처리]
      
        int count = memberIconDao.checkUserHasIcon(memberId, picked.getIconId());
        
        if (count > 0) {
           
            memberDao.upPoint(MemberDto.builder()
                    .memberId(memberId)
                    .memberPoint(500) 
                    .build());
            
         
            pointHistoryDao.insert(PointHistoryDto.builder()
                    .pointHistoryMemberId(memberId)
                    .pointHistoryAmount(500)    
                    .pointHistoryTrxType("GET")  
                    .build());
            
            picked.setIconName(picked.getIconName() + " (중복 500P 환급)");
            
        } else {
       
            memberIconDao.insertMemberIcon(memberId, picked.getIconId());
        }
    
        return picked;
    }

    // 아이콘 장착
    @Transactional
    public void equipIcon(String memberId, int iconId) {
        // 1. 보유 확인
        int hasIcon = memberIconDao.checkUserHasIcon(memberId, iconId);
        if (hasIcon == 0) throw new RuntimeException("보유하지 않은 아이콘입니다.");

        // 2. 기존 장착 해제
        memberIconDao.unequipAllIcons(memberId);

        // 3. 새 아이콘 장착
        memberIconDao.equipIcon(memberId, iconId);
    }

    // 장착 해제
    @Transactional
    public void unequipIcon(String memberId) {
        memberIconDao.unequipAllIcons(memberId);
    }
}