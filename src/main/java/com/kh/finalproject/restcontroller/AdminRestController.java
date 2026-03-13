package com.kh.finalproject.restcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession; // [추가]
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // [추가]
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody; // [추가]
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.BoardDao;
import com.kh.finalproject.dao.BoardReportDao;
import com.kh.finalproject.dao.DailyQuestDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.MemberTokenDao;
import com.kh.finalproject.dao.PointHistoryDao;
import com.kh.finalproject.dto.BoardDto;

import com.kh.finalproject.dao.PointItemStoreDao;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.dto.PointHistoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.service.AdminAssetService;
import com.kh.finalproject.service.AdminService;
import com.kh.finalproject.service.IconService;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.service.QuizService;
import com.kh.finalproject.service.TokenService;
import com.kh.finalproject.vo.BoardReportDetailVO;
import com.kh.finalproject.vo.BoardReportStatsVO;
import com.kh.finalproject.vo.DailyQuizVO;
import com.kh.finalproject.vo.IconPageVO;
import com.kh.finalproject.vo.PageResponseVO;
import com.kh.finalproject.vo.PageVO;
import com.kh.finalproject.vo.AdminPointItemPageVO;
import com.kh.finalproject.vo.QuizReportDetailVO;
import com.kh.finalproject.vo.QuizReportStatsVO;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminRestController {

	@Autowired
	private QuizService quizService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private MemberDao memberDao;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private MemberTokenDao memberTokenDao;

	@Autowired
	private IconService iconService;

	@Autowired
	private SqlSession sqlSession; // [추가] 포인트 관리 쿼리 실행을 위해 추가

	@Autowired
	private DailyQuestDao dailyQuizDao;

	@Autowired
	private InventoryDao inventoryDao;
	@Autowired
	private PointItemStoreDao pointItemStoreDao;
	@Autowired
	private MemberIconDao memberIconDao;

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private BoardReportDao boardReportDao;
	
	@Autowired
	private PointService pointService;
    
	@Autowired
	private PointHistoryDao pointHistoryDao;
	@Autowired private AdminAssetService adminAssetService;

	//기존 회원 목록 조회(관리자 제외, 일반 페이징)
	@GetMapping("/members") 
	public PageResponseVO getMemberList(
			@RequestParam int page,
			@RequestParam(required = false) String type, 
			@RequestParam(required = false) String keyword
			){
		PageVO pageVO = new PageVO();
		pageVO.setPage(page);

		if (type != "" && keyword != "") { // 검색일때
			int totalCount = memberDao.countSearchMember(type, keyword);
			pageVO.setTotalCount(totalCount);
			List<MemberDto> list = memberDao.selectAdminMemberList(type, keyword, pageVO);
			return new PageResponseVO<>(list, pageVO);
		} else { // 검색이 아닐때
			int totalCount = memberDao.countMember();
			pageVO.setTotalCount(totalCount);
			List<MemberDto> list = memberDao.selectListExceptAdmin(pageVO);
			return new PageResponseVO<>(list, pageVO);
		}
	}

	// 회원 상세 조회
	@GetMapping("/members/{memberId}")
	public MemberDto getMemberDetail(@PathVariable String memberId) {
		MemberDto member = memberDao.selectOne(memberId);

		if (member == null)
			throw new TargetNotfoundException();

		return member;
	}

	// 회원등급변경 (기존 기능)
	@PatchMapping("/members/{memberId}/memberLevel")
	public void changeLevel(@PathVariable String memberId, @RequestParam String memberLevel) {

		MemberDto memberDto = memberDao.selectOne(memberId);
		if (memberDto == null)
			throw new TargetNotfoundException();

		memberDto.setMemberLevel(memberLevel);
		memberDao.updateMemberLevel(memberDto);
	}

	// 회원 강제 탈퇴
	@DeleteMapping("/members/{memberId}")
	public void delete(@PathVariable String memberId, @RequestHeader("Authorization") String bearerToken) {
		// 계정 삭제
		MemberDto memberDto = memberDao.selectOne(memberId);
		if (memberDto == null)
			throw new TargetNotfoundException("존재하지 않는 회원입니다");
		memberDao.delete(memberId);
		// 토큰 삭제
		TokenVO tokenVO = tokenService.parse(bearerToken);
		memberTokenDao.deleteByTarget(tokenVO.getLoginId());
	}

	// -------------------------------------------------------------
	// [추가] 포인트 관리자 페이지 전용 기능
	// -------------------------------------------------------------

	// 1. 포인트 관리자용 회원 리스트 조회
	@GetMapping("/point/list")
	public Map<String, Object> getPointAdminMemberList(@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {

		List<MemberDto> list = memberDao.selectPointAdminList(keyword, page, size);
		int totalCount = memberDao.countPointAdminList(keyword);
		int totalPage = (totalCount + size - 1) / size;

		Map<String, Object> response = new HashMap<>();
		response.put("list", list);
		response.put("totalPage", totalPage);
		response.put("totalCount", totalCount);

		return response;
	}

	
	@PostMapping("/point/update")
    public String updatePoint(@RequestBody Map<String, Object> body) {
        try {
            String memberId = (String) body.get("memberId");
            int amount = Integer.parseInt(String.valueOf(body.get("amount")));
            pointService.adminUpdatePoint(memberId, amount);
            
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage(); 
        }
    }
	// 3. 회원 정보 수정 (닉네임, 등급) - 포인트 관리 페이지용
	@PostMapping("/point/edit")
	public String editMemberForPointAdmin(@RequestBody MemberDto memberDto) {
		int result = sqlSession.update("member.adminUpdateMemberInfo", memberDto);
		return result > 0 ? "success" : "fail";
	}

	// =============================================================
	// 아이콘 관리자 기능 (목록, 등록, 수정, 삭제)
	// =============================================================
	// 1. 관리자용 아이콘 전체 목록 (페이징)
	@GetMapping("/point/icon/list")
	public IconPageVO adminIconList
	(@RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "ALL") String category, // 카테고리 필터 추가
    @RequestParam(defaultValue = "ALL") String rarity   // 희귀도 필터 추가
) {
    // 서비스 호출 시 두 필터 값을 모두 전달합니다.
    return iconService.getIconList(page, category, rarity);
}

	// 2. 아이콘 등록
	@PostMapping("/point/icon/add")
	public String addIcon(@RequestBody IconDto dto) {
		try {
			iconService.addIcon(dto);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
			return "fail";
		}
	}

	// 3. 아이콘 수정
	@PostMapping("/point/icon/edit") // ★ 맨 앞에 슬래시(/) 추가했습니다.
	public String editIcon(@RequestBody IconDto dto) {
		try {
			iconService.editIcon(dto);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
			return "fail";
		}
	}

	// 4. 아이콘 삭제
	@DeleteMapping("/point/icon/delete/{iconId}")
	public String deleteIcon(@PathVariable int iconId) {
		try {
			iconService.removeIcon(iconId);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
			return "fail";
		}
	}

	@GetMapping("/point/history/{memberId}")
	public Map<String, Object> getMemberPointHistory(
	        @PathVariable String memberId,
	        @RequestParam(defaultValue = "1") int page,
	        @RequestParam(defaultValue = "10") int size,
	        @RequestParam(defaultValue = "ALL") String type) {
	    int startRow = (page - 1) * size + 1;
	    int endRow = page * size;
	    // 1. 목록 조회
	    List<PointHistoryDto> list = pointHistoryDao.selectListByMemberIdPaging(memberId, startRow, endRow, type);
	    // 2. 전체 개수 조회
	    int totalCount = pointHistoryDao.countHistory(memberId, type);
	    // 3. 전체 페이지 계산
	    int totalPage = (totalCount + size - 1) / size;
	    Map<String, Object> response = new HashMap<>();
	    response.put("list", list);
	    response.put("totalPage", totalPage);
	    response.put("totalCount", totalCount);	    
	    return response;
	}
	// -------------------------------------------------------------

	// 퀴즈 신고 관리 페이지
	@GetMapping("/quizzes/reports")
	public List<QuizReportStatsVO> getReportList(@RequestParam String status, @RequestAttribute TokenVO tokenVO,
			@RequestParam(defaultValue = "1") Integer page) {
		int size = 2; // 한 페이지당 보여줄 개수

		// Oracle 페이징 계산 (1페이지: 1~12, 2페이지: 13~24 ...)
		int end = page * size;
		int start = end - (size - 1);

		Map<String, Object> params = new HashMap<>();
		params.put("start", start);
		params.put("end", end);

		String loginLevel = tokenVO.getLoginLevel();
		params.put("loginLevel", loginLevel);
		params.put("status", status);
		List<QuizReportStatsVO> list = adminService.getReportedQuizList(params);
		return list;
	}

	// 퀴즈 신고 상세 내역 페이지
	@GetMapping("/quizzes/{quizId}/reports")
	public List<QuizReportDetailVO> getReportDetail(@PathVariable int quizId, @RequestAttribute TokenVO tokenVO) {

		String loginLevel = tokenVO.getLoginLevel();

		return adminService.getReportDetails(loginLevel, quizId);
	}

	// 퀴즈 삭제ReviewReportRestController
	@DeleteMapping("/quizzes/{quizId}")
	public boolean deleteQuiz(@PathVariable long quizId, @RequestAttribute TokenVO tokenVO) {
		String loginId = tokenVO.getLoginId();
		String loginLevel = tokenVO.getLoginLevel();

		return quizService.deleteQuiz(quizId, loginId, loginLevel);
	}

	// 퀴즈 상태 변경
	@PatchMapping("/quizzes/{quizId}/status/{status}")
	public boolean changeStatus(@PathVariable long quizId, @PathVariable String status,
			@RequestAttribute TokenVO tokenVO) {

		String loginId = tokenVO.getLoginId();
		String loginLevel = tokenVO.getLoginLevel();

		// 퀴즈 상태 변경
		QuizDto quizDto = QuizDto.builder().quizId(quizId).quizStatus(status).build();

		return quizService.changeQuizStatus(quizDto, loginId, loginLevel);
	}

	@GetMapping("/dailyquiz/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "all") String type, 
            @RequestParam(required = false, defaultValue = "") String keyword 
    ) {
        int size = 10;
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        List<DailyQuizVO> list = dailyQuizDao.selectList(startRow, endRow, type, keyword);
        int totalCount = dailyQuizDao.count(type, keyword);
        int totalPage = (totalCount + size - 1) / size;

        return Map.of(
            "list", list,
            "totalPage", totalPage,
            "currentPage", page
        );
    }
    @PostMapping("/dailyquiz/")
    public String insert(@RequestBody DailyQuizVO vo) {
        dailyQuizDao.insert(vo);
        return "success";
    }

    @PutMapping("/dailyquiz/")
    public String update(@RequestBody DailyQuizVO vo) {
        boolean result = dailyQuizDao.update(vo);
        return result ? "success" : "fail";
    }
    @DeleteMapping("/dailyquiz/{quizNo}")
    public String delete(@PathVariable int quizNo) {
        boolean result = dailyQuizDao.delete(quizNo);
        return result ? "success" : "fail";
    }
    

 
    // ------------------------------------------------------------

  	//게시판 신고 관리 페이지
  	@GetMapping("/board/reports")
  	public List<BoardReportStatsVO> getBReportList(
  			@RequestParam String status,
  			@RequestAttribute TokenVO tokenVO,
  			@RequestParam(defaultValue = "1") Integer page
  			) {
      	int size = 2; // 한 페이지당 보여줄 개수 
          
          // Oracle 페이징 계산 (1페이지: 1~12, 2페이지: 13~24 ...)
          int end = page * size;
          int start = end - (size - 1);
          
          Map<String, Object> params = new HashMap<>();
          params.put("start", start);
          params.put("end", end);
          
  		String loginLevel = tokenVO.getLoginLevel();
  		params.put("loginLevel", loginLevel);
  		params.put("status", status);
  		 List<BoardReportStatsVO> list = adminService.getReportedBoardList(params);
  		return list;
  	}

  	//게시글 신고 상세 내역 페이지
  	@GetMapping("/board/{boardNo}/reports")
  	public List<BoardReportDetailVO> getReportBDetail(
  			@PathVariable int boardNo,
  			@RequestAttribute TokenVO tokenVO
  			) {
  		
  		String loginLevel = tokenVO.getLoginLevel();
  		
         return adminService.getReportBDetails(loginLevel, boardNo);
      }
  	
  	//게시글 내용 조회
  	@GetMapping("/board/{boardNo}/text")
  	public BoardDto getBoardText(@PathVariable int boardNo) {
  		return boardReportDao.selectBoardText(boardNo);
  	}
  	
  	//게시글 삭제
  	@DeleteMapping("/board/{boardNo}")
      public boolean deleteBoard(
      		@PathVariable int boardNo,
              @RequestAttribute TokenVO tokenVO
              ) {
          String loginId = tokenVO.getLoginId();
          String loginLevel = tokenVO.getLoginLevel();
          
          if(!"관리자".equals(loginLevel) && loginId == null) throw new NeedPermissionException();
  		
          return boardDao.delete(boardNo);
      }
  	
  	@GetMapping("/inventory/list")
    public Map<String, Object> getInventoryAdminMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return adminAssetService.getAdminMemberList(keyword, page);
    }

    @GetMapping("/inventory/{memberId}")
    public List<InventoryDto> getUserInventory(@PathVariable String memberId) {
        return adminAssetService.getUserInventory(memberId);
    }

    @DeleteMapping("/inventory/{inventoryNo}")
    public ResponseEntity<String> recallItem(@PathVariable long inventoryNo) {
        boolean success = adminAssetService.recallAsset("item", inventoryNo);
        return success ? ResponseEntity.ok("success") : ResponseEntity.notFound().build();
    }

    @GetMapping("/inventory/item-list")
    public Map<String, Object> getItemList(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        return adminAssetService.getMasterItemList(type, keyword, page, size);
    }

    @PostMapping("/inventory/{memberId}/{itemNo}")
    public ResponseEntity<Void> grantItem(@PathVariable String memberId, @PathVariable long itemNo) {
        adminAssetService.grantAsset("item", memberId, (int)itemNo);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/icon/{memberId}")
    public List<MemberIconDto> getUserIcons(@PathVariable String memberId) {
        return adminAssetService.getUserIcons(memberId);
    }

    @GetMapping("/icon/list")
    public Map<String, Object> getIconList(@RequestParam(defaultValue = "1") int page) {
        // AdminAssetService에 만든 페이징 처리 메소드를 호출해야 합니다
        return adminAssetService.getMasterIconList(page);
    }

    @PostMapping("/icon/{memberId}/{iconId}")
    public ResponseEntity<String> grantIcon(@PathVariable String memberId, @PathVariable int iconId) {
        String result = adminAssetService.grantAsset("icon", memberId, iconId);
        if ("duplicate".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 보유 중");
        }
        return ResponseEntity.ok("지급 완료");
    }

    @DeleteMapping("/icon/{memberIconId}")
    public ResponseEntity<Void> recallIcon(@PathVariable long memberIconId) {
        boolean success = adminAssetService.recallAsset("icon", memberIconId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

       
        
     //  아이템 목록 조회 (페이징 + 필터링 반영)
        @GetMapping("/store/list")
        public Map<String, Object> list(@ModelAttribute AdminPointItemPageVO vo) {
            // 필터링된 결과의 전체 개수 조회
            int totalCount = pointItemStoreDao.selectCount(vo);
            
            // 현재 페이지에 해당하는 리스트 조회
            List<PointItemStoreDto> list = pointItemStoreDao.selectAdminList(vo);
            
            // React로 보낼 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("list", list);
            response.put("totalCount", totalCount);
            
            return response;
        }

        //  새 아이템 등록
        @PostMapping("/store/add")
        public ResponseEntity<String> addItem(
                @RequestAttribute TokenVO tokenVO, 
                @RequestBody PointItemStoreDto dto) {
            
            pointService.addItem(tokenVO.getLoginId(), dto);
            return ResponseEntity.ok("아이템이 등록되었습니다.");
        }

        // 아이템 수정 
        @PutMapping("/store/edit")
        public ResponseEntity<String> editItem(
                @RequestAttribute TokenVO tokenVO, 
                @RequestBody PointItemStoreDto dto) {
            
            pointService.editItem(tokenVO.getLoginId(), dto);
            return ResponseEntity.ok("아이템 정보가 수정되었습니다.");
        }

        //아이템 삭제
        @DeleteMapping("/store/delete/{itemNo}")
        public ResponseEntity<String> deleteItem(
                @RequestAttribute TokenVO tokenVO, 
                @PathVariable long itemNo) {
            
            pointService.deleteItem(tokenVO.getLoginId(), itemNo);
            return ResponseEntity.ok("아이템이 삭제되었습니다.");
        }
    

        // 인벤토리 아이템 회수
        @DeleteMapping("/discard/{inventoryNo}")
        public ResponseEntity<String> discardItem(
                @RequestAttribute TokenVO tokenVO, 
                @PathVariable long inventoryNo) {
            
            String loginId = tokenVO.getLoginId();
            pointService.discardItem(loginId, inventoryNo);
            
            return ResponseEntity.ok("인벤토리에서 아이템을 회수했습니다.");
        }
}	