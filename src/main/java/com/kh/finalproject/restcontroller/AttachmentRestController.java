package com.kh.finalproject.restcontroller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.AttachmentDao;
import com.kh.finalproject.dto.AttachmentDto;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.service.AttachmentService;

@CrossOrigin
@RestController
@RequestMapping("/attachment")
public class AttachmentRestController {
	
	@Autowired
	private AttachmentService attachmentService;
	@Autowired
	private AttachmentDao attachmentDao;
	
	@GetMapping("/download")
	public ResponseEntity<ByteArrayResource> download(@RequestParam int attachmentNo) throws IOException {
		// 1. 파일 정보 조회
		AttachmentDto attachmentDto = attachmentDao.selectOne(attachmentNo);
		if(attachmentDto == null) throw new TargetNotfoundException("존재하지 않는 파일");
		
		// 2. 실제 파일 로드
		ByteArrayResource resource = attachmentService.load(attachmentNo);
		
		// 3. 응답 헤더 설정 및 파일 전송
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
				//.contentType(MediaType.APPLICATION_OCTET_STREAM)//유형을 모를 때
				.header(HttpHeaders.CONTENT_TYPE, attachmentDto.getAttachmentType())//유형을 알 때
				.contentLength(attachmentDto.getAttachmentSize())
				.header(HttpHeaders.CONTENT_DISPOSITION, 
					ContentDisposition
						.attachment()
						.filename(attachmentDto.getAttachmentName(), StandardCharsets.UTF_8)
						.build().toString()
				)
			.body(resource);
	}
}
