package com.kh.finalproject.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.IconDao;
import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.service.IconService;
import com.kh.finalproject.vo.PointUseVO;

@RestController
@RequestMapping("/icon")
@CrossOrigin
public class IconRestController {

    @Autowired private IconService iconService;
    @Autowired private IconDao iconDao;

    // ---------------------------------------------------
    // [관리자] 기능
    // ---------------------------------------------------
    @GetMapping("/admin/list")
    public List<IconDto> adminList() {
        return iconService.getAllIcons();
    }

    @PostMapping("/admin/add")
    public String add(@RequestBody IconDto dto) {
        iconService.addIcon(dto);
        return "success";
    }

    @PostMapping("/admin/edit")
    public String edit(@RequestBody IconDto dto) {
        iconService.editIcon(dto);
        return "success";
    }

    @DeleteMapping("/admin/delete/{iconId}")
    public String delete(@PathVariable int iconId) {
        iconService.removeIcon(iconId);
        return "success";
    }

    // ---------------------------------------------------
    // [사용자] 기능
    // ---------------------------------------------------

    // 1. 뽑기 실행
    // (수정됨) URL 중복 방지를 위해 "/icon/draw" -> "/draw" 로 변경
    // 최종 URL: POST /icon/draw
    @PostMapping("/draw") 
    public IconDto drawIcon(
            @RequestAttribute(value="loginId", required=false) String loginId,
            @RequestBody PointUseVO vo) { 
        
        if(loginId == null) throw new RuntimeException("로그인 필요");
        
        // Service에 inventoryNo(long) 전달
        return iconService.drawRandomIcon(loginId, vo.getInventoryNo());
    }
    // 2. 내 아이콘함 조회
    // 최종 URL: GET /icon/my
    @GetMapping("/my")
    public List<MemberIconDto> myIcons(@RequestAttribute("loginId") String loginId) {
        // DTO 필드명이 memberId, iconId로 바뀌었으므로
        // React에서도 res.data[0].memberId 처럼 접근해야 함을 주의하세요!
        return iconService.getMyIcons(loginId);
    }
    
    // 3. (추가됨) 아이콘 장착
    // 최종 URL: POST /icon/equip
    // Body: { "iconId": 5 }
    @PostMapping("/equip")
    public String equipIcon(
            @RequestAttribute("loginId") String loginId,
            @RequestBody Map<String, Integer> params) { // 간단하게 Map으로 받음
        
        int iconId = params.get("iconId");
        iconService.equipIcon(loginId, iconId);
        return "success";
    }
    
    // 4. (추가됨) 아이콘 장착 해제 (전체 해제)
    // 최종 URL: POST /icon/unequip
    @PostMapping("/unequip")
    public String unequipIcon(@RequestAttribute("loginId") String loginId) {
        iconService.unequipIcon(loginId);
        return "success";
    }
    
    @GetMapping("/contents/{iconContents}")
    public List<IconDto> getIconList(@PathVariable long iconContents) {
        return iconDao.selectListByContents(iconContents);
    }
    
    
}