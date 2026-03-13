package com.kh.finalproject.restcontroller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kh.finalproject.dao.BoardDao;
import com.kh.finalproject.dao.BoardResponseDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.ReplyDao;
import com.kh.finalproject.dto.BoardDto;
import com.kh.finalproject.dto.BoardResponseDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.error.UnauthorizationException;
import com.kh.finalproject.service.AttachmentService;
import com.kh.finalproject.service.DailyQuestService;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.vo.BoardResponseVO;
import com.kh.finalproject.vo.PageResponseVO;
import com.kh.finalproject.vo.PageVO;
import com.kh.finalproject.vo.TokenVO;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/board")
public class BoardRestController {

	@Autowired
	private BoardDao boardDao;
	@Autowired
	private AttachmentService attachmentService;
	@Autowired
	private BoardResponseDao boardResponseDao;
	@Autowired
	private DailyQuestService dailyQuestService;
	@Autowired
	private ReplyDao replyDao;

	@Autowired
	private PointService pointService;
	@Autowired
	private MemberDao memberDao;

	// 게시글 등록
	@PostMapping("/")
	public void insert(@RequestAttribute TokenVO tokenVO,
								@RequestBody BoardDto boardDto) {
		String loginId = tokenVO.getLoginId();

		int boardNo =  boardDao.insert(boardDto);
		int getPoint = 10;
		pointService.addPoint(loginId, getPoint, "GET","게시글작성");
		if(boardDto.getAttachmentNoList() != null) {
			for(int attachmentNo : boardDto.getAttachmentNoList()) {
				boardDao.connect(boardNo, attachmentNo);
			}
		}
	}


	//글 등록 전에 미리 이미지를 업로드하는 매핑 (임시이미지)
	@PostMapping("/temp")
	public int temp(@RequestParam MultipartFile attach) throws IllegalStateException, IOException {
		if(attach.isEmpty()) {
			throw new TargetNotfoundException("파일이 없습니다");
		}
		
		return attachmentService.save(attach);
	}


	//전체 조회
	@GetMapping("/")
	public List<BoardDto> selectList(){
		return boardDao.selectList();
	}

	//전체 조회
	@GetMapping("/page/{page}")
	public PageResponseVO selectList(@PathVariable int page,
			@RequestParam(required = false) String column,
			@RequestParam(required = false) String keyword){
		int totalCount =boardDao.countBoard(column, keyword);

		System.out.println("---------column : "+column);
		System.out.println("---------keyword : "+keyword);
		
		PageVO pageVO = new PageVO();
		pageVO.setPage(page);
		pageVO.setTotalCount(totalCount);

		List<BoardDto> list = boardDao.selectWithPage(pageVO, column, keyword);
		return new PageResponseVO<>(list, pageVO);
	}


	//상세 조회
	@GetMapping("/detail/{boardNo}")
	public BoardDto selectOne(@PathVariable int boardNo) {
		replyDao.updateBoardReplyCount(boardNo);
		return boardDao.selectOne(boardNo);
	}


	// 컨텐츠별 조회
	@GetMapping("/contentsId/{contentsId}/{page}")
	public PageResponseVO selesctListByContents(
			@PathVariable long contentsId, @PathVariable int page){
		int totalCount = boardDao.countContentsBoard(contentsId);

		PageVO pageVO = new PageVO();
		pageVO.setPage(page);
		pageVO.setTotalCount(totalCount);

		List<BoardDto> list = boardDao.selectListByContents(contentsId, pageVO);
		return new PageResponseVO<>(list, pageVO);
	}

	// 컨텐츠별 5개 조회
	@GetMapping("/contentsId/{contentsId}/five")
	public List<BoardDto> selesctListBy5Contents(@PathVariable long contentsId){
		return boardDao.selectListBy5Contents(contentsId);
	}



	// 게시글 수정
	@PutMapping("/{boardNo}")
	public void update(@PathVariable int boardNo, @RequestBody BoardDto boardDto) {
		BoardDto beforeDto = boardDao.selectOne(boardNo);
		
		if(boardDto == null) throw new TargetNotfoundException("존재하지 않는 글");

		Set<Integer> before = new HashSet<>();
		Document beforeDocument = Jsoup.parse(beforeDto.getBoardText());
		Elements beforeElements = beforeDocument.select(".custom-image");

		for(Element element : beforeElements) {
			int attachmentNo = Integer.parseInt(element.attr("data-pk"));
			before.add(attachmentNo);
		}

		Set<Integer> after = new HashSet<>();
		Document afterDocument = Jsoup.parse(boardDto.getBoardText());
		Elements afterElements = afterDocument.select(".custom-image");

		for(Element element : afterElements) {
			int attachmentNo = Integer.parseInt(element.attr("data-pk"));
			after.add(attachmentNo);
		}


		Set<Integer> minus = new HashSet<>(before);
		minus.removeAll(after);

		for(int attachmentNo : minus) {
			attachmentService.delete(attachmentNo);
		}

		boardDto.setBoardNo(boardNo);
		boardDao.update(boardDto);
	}

	// 게시글 삭제
	@DeleteMapping("/{boardNo}")
	public void delete(@RequestAttribute(value = "tokenVO", required = false) TokenVO tokenVO, @PathVariable int boardNo) {
		if (tokenVO == null) {
			throw new UnauthorizationException("로그인이 필요한 기능입니다."); 
		}
		
		String loginId = tokenVO.getLoginId();
		BoardDto boardDto = boardDao.selectOne(boardNo);
		if(boardDto == null) throw new TargetNotfoundException("존재하지 않는 글");
			
	
		 //유저가 가지고있는 포인트 확인해서 변경
		MemberDto memberDto = memberDao.selectOne(loginId);
		int losePoint = -10; // 삭제로 잃는 포인트
		int point = memberDto.getMemberPoint();  
		if(point >= 10) {
			pointService.addPoint(loginId, losePoint,"GET","게시글 삭제");
		}
		else {
			pointService.addPoint(loginId, -point, "USE", "게시글삭제");
		}

		Document document = Jsoup.parse(boardDto.getBoardText());
		Elements elements = document.select(".custom-image");

		for(Element element : elements) {
			int attachmentNo = Integer.parseInt(element.attr("data-pk"));
			attachmentService.delete(attachmentNo);
		}

		boardDao.delete(boardNo);
	}


	/////////////////////////좋아요 & 싫어요


	//눌렀는지 안눌렀는지 확인
	@PostMapping("/check")
	public BoardResponseVO check(@RequestParam int boardNo, @RequestParam String loginId) {
		boolean response = boardResponseDao.countForCheck(loginId, boardNo);

		String responseType = boardResponseDao.selectMemberResponse(loginId, boardNo);

		int likeCount = boardResponseDao.countLikeByboardNo(boardNo);
		int unlikeCount = boardResponseDao.countUnlikeByboardNo(boardNo);

		return BoardResponseVO.builder()
				.response(response)
				.responseType(responseType)
				.likeCount(likeCount)
				.unlikeCount(unlikeCount)
				.build();
	}


	@PostMapping("/action")
	public BoardResponseVO action(@RequestBody BoardResponseDto boardResponseDto) {
		boolean before = boardResponseDao.countForCheck(boardResponseDto.getMemberId(), boardResponseDto.getBoardNo());

		if (before) {// 좋아요나 싫어요를 눌렀던 상태면
			//좋아요를 눌렀었는지 싫어요를 눌렀었는지 조회
			String responseType =
					boardResponseDao.selectMemberResponse(boardResponseDto.getMemberId(), boardResponseDto.getBoardNo());
			
			//좋아요를 누른 상태에서 또 좋아요를 누른 경우 or 싫어요를 누른 상태에서 또 싫어요를 누른 경우
			if(responseType.equals(boardResponseDto.getResponseType())) {
				//기존의 response 삭제
					boardResponseDao.delete(boardResponseDto.getMemberId(), boardResponseDto.getBoardNo());
			}

			//기존의 것과 다른 것을 누른 경우
			else {
				//기존의 response 삭제 후 등록
				boardResponseDao.delete(boardResponseDto.getMemberId(), boardResponseDto.getBoardNo());
				boardResponseDao.insert(boardResponseDto);
			}

		} else {// 좋아요나 싫어요 안한 상태면
			boardResponseDao.insert(boardResponseDto);
			dailyQuestService.questProgress(boardResponseDto.getMemberId(), "LIKE");
		}

		int likeCount = boardResponseDao.countLikeByboardNo(boardResponseDto.getBoardNo());
		int unlikeCount = boardResponseDao.countUnlikeByboardNo(boardResponseDto.getBoardNo());

		//board 테이블에 좋아요 수 & 싫어요 수 갱신
		boardResponseDao.updateBoardLike(boardResponseDto.getBoardNo());
		boardResponseDao.updateBoardUnlike(boardResponseDto.getBoardNo());
		
		String changedType = boardResponseDao.selectMemberResponse(boardResponseDto.getMemberId(), boardResponseDto.getBoardNo());

		return BoardResponseVO.builder()
				.response(!before)
				.responseType(changedType)
				.likeCount(likeCount)
				.unlikeCount(unlikeCount)
				.build();
	}


	
    // 서버 메모리에서 로그인ID 기준 조회 기록 저장
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>> viewHistoryMap = new ConcurrentHashMap<>();
    private final long limitTime = 24 * 60 * 60 * 1000; // 24시간
    

	// 조회수 증가 로직 구현
	@PostMapping("/viewUpdate/{boardNo}")
	public void increaseViewCount(@RequestAttribute TokenVO tokenVO,
			@PathVariable int boardNo,
			HttpSession session) {
		if(tokenVO == null) return;

		String loginId = tokenVO.getLoginId();

		long now = System.currentTimeMillis();
		
        // 로그인ID별 조회 기록 Map 가져오기
		// - 세션을 이용할시 요청시 세션이 달라지는 문제
		// - 세션id를 발급해서 프론트에 전달할시, 로그인을 새로할때마다 갱신되는 문제
		// -> 서버 메모리에 임시 저장하여 중복검사
        ConcurrentHashMap<Integer, Long> userHistory =
                viewHistoryMap.computeIfAbsent(loginId, k -> new ConcurrentHashMap<>());
        Long lastViewed = userHistory.get(boardNo);
        System.out.println("lastViewed"+lastViewed);
        if (lastViewed != null && now - lastViewed < limitTime) { // 중복조회면 return
        	System.out.println("중복된 값이 존재");
        	return;
        }
        userHistory.put(boardNo, now); // 조회 기록 갱신

        // 오래된 기록 정리 (24시간 이상 지난 기록 삭제)
        userHistory.entrySet().removeIf(e -> now - e.getValue() > limitTime);
        boardDao.increaseViewCount(boardNo);
        System.out.println("조회수 증가 성공");
    }




}
