package com.kh.finalproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BoardService {
	
	@Autowired
	private AttachmentService attachmentService;
}
