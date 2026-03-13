package com.kh.finalproject.restcontroller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.AttendanceHistoryDto;
import com.kh.finalproject.dto.AttendanceStatusDto;
import com.kh.finalproject.service.AttendanceService;

@RestController
@RequestMapping("/point/main/attendance/")
@CrossOrigin 
public class AttendanceRestController {

	@Autowired
	private AttendanceService attendanceService;
	
	// 
	@GetMapping("/status")
	public ResponseEntity<Boolean> getStatus(@RequestAttribute String loginId) {
	    boolean checked = attendanceService.isTodayChecked(loginId);
	    return ResponseEntity.ok(checked);
	}

	//
	@PostMapping("/check")
	public ResponseEntity<String> doCheck(
			@RequestAttribute String loginId) {
		
		try {
			attendanceService.checkAttendance(loginId);
			// 프론트에서 success 문자열로 성공 여부 판단하므로 형식 맞춤
			return ResponseEntity.ok("success:100"); 
		} catch (IllegalStateException e) {
			return ResponseEntity.ok("fail:" + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.ok("fail:에러 발생");
		}
	}

	// 
	@GetMapping("/calendar")
	public ResponseEntity<List<String>> getCalendar(
			@RequestAttribute String loginId) { 
		
		List<String> dates = attendanceService.getMyAttendanceDates(loginId);
		return ResponseEntity.ok(dates);
	}
}