package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberProfileDto;
import com.kh.finalproject.vo.PageVO;

@Repository
public class MemberDao {

	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/// 등록
	public void insert(MemberDto memberDto) {
		// ++ 비밀번호 암호화
		String origin = memberDto.getMemberPw();
		String encoded = passwordEncoder.encode(origin); // 암호화
		memberDto.setMemberPw(encoded);
		sqlSession.insert("member.insert", memberDto);
	}

	/// 조회
	// 기본조회(목록)
	public List<MemberDto> selectList() {
		return sqlSession.selectList("member.selectList");
	}

	// 상세조회
	public MemberDto selectOne(String memberId) {
		return sqlSession.selectOne("member.detail", memberId);
	}
	
	public MemberProfileDto selectProfile(String memberId) {
		return sqlSession.selectOne("member.selectProfile", memberId);
	}

	// 닉네임 중복 검사용 조회
	public MemberDto selectOneByMemberNickname(String memberNickname) {
		return sqlSession.selectOne("member.detailByNickname", memberNickname);
	}

	// 관리자 제외하고 조회
	public List<MemberDto> selectListExceptAdmin(PageVO pageVO) {
		return sqlSession.selectList("member.selectListExceptAdmin", pageVO);
	}

	public int countMember() {
		return sqlSession.selectOne("member.countMember");
	}

	// 회원검색
	public List<MemberDto> selectAdminMemberList(String type, String keyword, PageVO pageVO) {
		Map<String, Object> params = new HashMap<>();
		params.put("pageVO", pageVO);
		params.put("type", type);
		params.put("keyword", keyword);
		return sqlSession.selectList("member.selectAdminList", params);
	}

	public int countSearchMember(String type, String keyword) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", type);
		params.put("keyword", keyword);
		return sqlSession.selectOne("member.countSearchMember", params);
	}

	/// 수정
	// (회원기본정보 수정)
	public boolean update(MemberDto memberDto) {
		return sqlSession.update("member.update", memberDto) > 0;
	}

	// (닉네임 수정)
	// + 컨트롤러에서 닉네임 수정할때 포인트 차감이 필요할지?
	// + 포인트가 부족하면 닉네임 수정이 불가능할지
	public boolean updateNickname(MemberDto memberDto) {
		return sqlSession.update("member.updateNickname", memberDto) > 0;
	}

	// (비밀번호 수정)
	public boolean updatePassword(MemberDto memberDto) {
		// ++ 비밀번호 암호화
		String origin = memberDto.getMemberPw();
		String encoded = passwordEncoder.encode(origin); // 암호화
		memberDto.setMemberPw(encoded);
		return sqlSession.update("member.updatePassword", memberDto) > 0;
	}

	// (포인트 갱신)
	public boolean updatePoint(MemberDto memberDto) {
		return sqlSession.update("member.updatePoint", memberDto) > 0;
	}

	public boolean upPoint(MemberDto memberDto) {
		return sqlSession.update("member.upPoint", memberDto) > 0;
	}

	// (신뢰도 갱신)
	public void updateReliability(String memberId, int rel) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("rel", rel);
		sqlSession.update("member.updateReliability", param);
	}

	// (회원등급 수정)
	public boolean updateMemberLevel(MemberDto memberDto) {
		return sqlSession.update("member.updateMemberLevel", memberDto) > 0;
	}

	/// 삭제 (회원탈퇴)
	public boolean delete(String memberId) {
		return sqlSession.delete("member.delete", memberId) > 0;
	}

	// 좋아요 신뢰도
	public void updateReliabilitySet(String memberId, int rel) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("rel", rel);
		sqlSession.update("member.updateReliabilitySet", param);
	}

	public MemberDto selectMap(String memberId) {
	    return sqlSession.selectOne("member.selectMap", memberId);

	}
	// -------------------------------------------------------------
		// [추가] 관리자 포인트 관리 페이지 전용 메서드
		// -------------------------------------------------------------

		// 1. 포인트 관리용 회원 리스트 조회 (검색 기능 포함)
    public List<MemberDto> selectPointAdminList(String keyword, int page, int size) {
        int end = page * size;
        int start = end - (size - 1);

        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("start", start);
        params.put("end", end);

        return sqlSession.selectList("member.selectPointAdminList", params);
    }

    public int countPointAdminList(String keyword) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        return sqlSession.selectOne("member.countPointAdminList", params);
    }
		// 2. 관리자 포인트 강제 지급/차감
		public boolean adminUpdatePoint(String memberId, int amount) {
			Map<String, Object> params = new HashMap<>();
			params.put("memberId", memberId);
			params.put("amount", amount);
			return sqlSession.update("member.adminUpdatePoint", params) > 0;
		}

		// 3. 관리자 회원 정보 수정 (닉네임, 등급)
		public boolean adminUpdateMemberInfo(MemberDto memberDto) {
			return sqlSession.update("member.adminUpdateMemberInfo", memberDto) > 0;
		}
		// 현재 신뢰도 조회
		public int selectReliability(String memberId) {
			return sqlSession.selectOne("member.selectReliability", memberId);
		}
}
