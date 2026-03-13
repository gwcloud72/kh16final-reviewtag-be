package com.kh.finalproject.restcontroller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.BoardDao;
import com.kh.finalproject.dao.ReplyDao;
import com.kh.finalproject.dto.BoardDto;
import com.kh.finalproject.dto.ReplyDto;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.vo.ReplyListVO;

@CrossOrigin
@RestController
@RequestMapping("/reply")
public class ReplyRestController {

	@Autowired
	private ReplyDao replyDao;
	@Autowired
	private BoardDao boardDao;
	
	@PostMapping("/")
	public List<ReplyListVO> list(@RequestParam String loginId, @RequestParam int boardNo) {
		//게시글 정보 조회
		BoardDto boardDto = boardDao.selectOne(boardNo);
		if(boardDto == null) throw new TargetNotfoundException("존재하지 않는 글");
		
		List<ReplyDto> list = replyDao.selectByBoardNo(boardNo);
		List<ReplyListVO> result = new ArrayList<>();
		
		for(ReplyDto replyDto : list) {
			//로그인 한 사용자가 댓글을 단 사람인지
			boolean owner = loginId != null && replyDto.getReplyWriter() != null
														&& loginId.equals(replyDto.getReplyWriter());
			//댓글을 단 사용자가 게시글의 작성자인지
			boolean writer = boardDto.getBoardWriter() != null
									&& replyDto.getReplyWriter() != null
									&& boardDto.getBoardWriter().equals(replyDto.getReplyWriter());
			
			result.add(ReplyListVO.builder()
						.replyNo(replyDto.getReplyNo())
						.replyWriter(replyDto.getReplyWriter())
						.replyTarget(replyDto.getReplyTarget())
						.replyContent(replyDto.getReplyContent())
						.replyWtime(replyDto.getReplyWtime())
						.replyEtime(replyDto.getReplyEtime())
						.owner(owner)
						.writer(writer)
					.build());
		}
		return result;
	}
	
	@PostMapping("/write")
	public void write(@RequestBody ReplyDto replyDto) {
		replyDao.insert(replyDto);
		replyDao.updateBoardReplyCount(replyDto.getReplyTarget());
	}
	
	@DeleteMapping("/{replyNo}")
	public void delete(@PathVariable int replyNo) {
		replyDao.delete(replyNo);
	}
	
	@PutMapping("/{replyNo}")
	public void update(@PathVariable int replyNo, 
			@RequestParam String editContent) {
		ReplyDto originDto =  replyDao.selectOne(replyNo);
		if(originDto == null) throw new TargetNotfoundException("존재하지 않는 댓글");
		
		originDto.setReplyContent(editContent);
		replyDao.update(originDto);
	}
}
