package com.kh.finalproject.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kh.finalproject.dao.AttachmentDao;
import com.kh.finalproject.dto.AttachmentDto;
import com.kh.finalproject.error.TargetNotfoundException;

@Service
public class AttachmentService {
	
	@Autowired
	private AttachmentDao attachmentDao;
	
	//파일 저장을 위한 경로 설정
	private File home = new File(System.getProperty("user.home"));
	private File upload = new File(home, "upload");
	
	//파일 저장 (실물 + DB)
	@Transactional
	public Integer save(MultipartFile attach) throws IllegalStateException, IOException {
		int attachmentNo = attachmentDao.sequence();
		
		//파일을 저장하려면 파일 인스턴스가 필요
		if(upload.exists() == false ) {//업로드할 폴더가 존재하지 않는다면
			upload.mkdir(); //생성
		}
		
		File target = new File(upload, String.valueOf(attachmentNo));//저장할 파일의 인스턴스(아직없음)
		attach.transferTo(target);//저장하세요!
		
		AttachmentDto attachmentDto = new AttachmentDto();
		attachmentDto.setAttachmentNo(attachmentNo);
		attachmentDto.setAttachmentName(attach.getOriginalFilename()); //파일이름
		attachmentDto.setAttachmentType(attach.getContentType()); //파일 유형
		attachmentDto.setAttachmentSize(attach.getSize()); //파일 크기
		
		attachmentDao.insert(attachmentDto);
		
		return attachmentNo; //생성한 파일의 번호 반환
	}
	
	// 파일 내용 불러오기
	public ByteArrayResource load(int attachmentNo) throws IOException {
		//파일을 찾는다
		File target = new File(upload, String.valueOf(attachmentNo));
		if(target.isFile() == false) throw new TargetNotfoundException("존재하지 않는 파일");
				
		//파일의 내용을 읽어온다
		byte[] data = Files.readAllBytes(target.toPath());//한번에 다 읽기(java.nio 패키지의 명령)
		ByteArrayResource resource = new ByteArrayResource(data);//포장
				
		return resource;
	}
	
	//파일 삭제 (DB 및 실물 파일 삭제)
	//- 없는 첨부파일 번호라면 예외처리
	public void delete(int attachmentNo) {
		AttachmentDto attachmentDto = attachmentDao.selectOne(attachmentNo);
		if(attachmentDto == null) throw new TargetNotfoundException("존재하지 않는 파일");
		
		//실제 파일 삭제
		File target = new File(upload, String.valueOf(attachmentNo));
		target.delete();
		
		//DB 정보 삭제
		attachmentDao.delete(attachmentNo);
	}
	
}
