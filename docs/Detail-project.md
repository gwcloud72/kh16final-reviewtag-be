# Review Tag | Backend 상세 정리

## 1. 프로젝트 개요

Review Tag는 영화·콘텐츠 기반 리뷰 플랫폼입니다.  
리뷰, 별점, 가격 평가, 신뢰도 시스템, 자유게시판, 퀴즈, 포인트 기능을 통해 사용자가 단순히 콘텐츠를 소비하는 데서 끝나지 않고, 서비스 안에서 계속 참여하도록 만드는 것을 목표로 했습니다.

그중 저는 **포인트 생태계 구축을 전담**했습니다.  
프로젝트 전체를 설명하는 문서라기보다, 제가 맡았던 도메인에서 어떤 기준으로 백엔드를 설계했고 실제로 어떤 문제를 해결했는지를 정리한 문서입니다.

---

## 2. 맡은 역할

제가 담당한 백엔드 범위는 아래와 같습니다.

- 출석체크 및 연속 출석 보상
- 일일 퀘스트 목록, 진행도, 보상 수령
- 데일리 퀴즈 출제 및 정답 검증
- 룰렛 보상 처리
- 포인트 상점, 구매, 선물하기, 위시리스트
- 인벤토리 조회, 사용, 장착, 해제, 환불
- 포인트 이력 조회
- 관리자 포인트 지급/회수, 퀴즈 관리, 상점 관리, 자산 관리

핵심은 기능 수를 늘리는 것보다, **포인트가 오가는 모든 흐름을 일관되게 묶는 것**이었습니다.

---

## 3. 백엔드 설계 포인트

### 3-1. 인증 정보는 요청 초반에 정리했습니다.

JWT 인증을 통과한 뒤 `loginId`를 `RequestAttribute`에 넣고, 컨트롤러에서는 그 값을 기준으로 로그인 사용자를 식별하도록 구성했습니다.

이 구조를 선택한 이유는 두 가지였습니다.

- 컨트롤러와 서비스에서 매번 토큰을 다시 해석하지 않아도 됨
- 인증 실패를 초반에 걸러서 비즈니스 로직을 단순하게 유지할 수 있음

```java
@PostMapping("/check")
public void checkAttendance(@RequestAttribute String loginId) {
    attendanceService.checkAttendance(loginId);
}
```

포인트 도메인은 대부분 로그인 사용자 기준으로 동작하기 때문에, 인증 정보가 안정적으로 전달되는 구조를 먼저 잡는 것이 중요했습니다.

---

### 3-2. 포인트는 반드시 이력과 함께 움직이도록 만들었습니다.

출석, 퀘스트, 구매, 환불, 관리자 지급처럼 포인트가 변하는 지점은 많았지만, 실제 처리 기준은 하나로 통일했습니다.  
`PointService.addPoint()`를 **단일 진입점**으로 두고,

- 차감 시 잔액 검증
- 포인트 변경
- 이력 저장

이 세 단계가 항상 같이 가도록 설계했습니다.

```java
public boolean addPoint(String loginId, int amount, String trxType, String reason) {
    MemberDto currentMember = memberDao.selectOne(loginId);
    if (currentMember == null) throw new RuntimeException("회원 정보가 없습니다.");

    if (amount < 0 && (currentMember.getMemberPoint() + amount) < 0) {
        throw new RuntimeException("보유 포인트가 부족합니다.");
    }

    MemberDto updateDto = MemberDto.builder()
            .memberId(loginId)
            .memberPoint(amount)
            .build();

    if (memberDao.upPoint(updateDto)) {
        pointHistoryDao.insert(PointHistoryDto.builder()
            .pointHistoryMemberId(loginId)
            .pointHistoryAmount(amount)
            .pointHistoryTrxType(trxType)
            .pointHistoryReason(reason)
            .build());
        return true;
    }
    return false;
}
```

이렇게 해두니 포인트 기능이 늘어나도 처리 기준이 흔들리지 않았고, 이력 누락 문제도 줄일 수 있었습니다.

---

### 3-3. 출석과 퀘스트는 중복 처리 방지에 집중했습니다.

출석과 퀘스트는 사용자가 매일 반복적으로 사용하는 기능이라, 작은 중복도 금방 문제로 이어질 수 있었습니다.  
그래서 다음 기준을 우선으로 뒀습니다.

- 오늘 출석을 이미 했는지 확인
- 연속 출석은 날짜 기준으로 계산
- 퀘스트 로그는 같은 날 같은 타입이면 UPSERT
- 보상은 한 번만 수령 가능하게 처리

출석 처리 로직에서는 `Timestamp`를 그대로 비교하지 않고 `LocalDate`로 변환해 날짜만 기준으로 계산했습니다.

```java
LocalDate lastDate = status.getAttendanceStatusLastdate()
        .toLocalDateTime()
        .toLocalDate();

if(today.equals(lastDate)) {
    throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
}
```

퀘스트 로그는 Oracle `MERGE`를 사용해, 같은 날 같은 타입의 로그가 있으면 증가시키고 없으면 새로 만들었습니다.

```sql
MERGE INTO POINT_GET_QUEST_LOG L
USING DUAL
   ON (L.POINT_GET_QUEST_MEMBER_ID = #{memberId}
       AND L.POINT_GET_QUEST_TYPE = #{type}
       AND TO_CHAR(L.POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date})
WHEN MATCHED THEN
    UPDATE SET L.POINT_GET_QUEST_COUNT = L.POINT_GET_QUEST_COUNT + 1
WHEN NOT MATCHED THEN
    INSERT (
        POINT_GET_QUEST_LOG_ID,
        POINT_GET_QUEST_MEMBER_ID,
        POINT_GET_QUEST_TYPE,
        POINT_GET_QUEST_DATE,
        POINT_GET_QUEST_COUNT,
        POINT_GET_QUEST_REWARD_YN,
        POINT_GET_QUEST_CREATED_AT
    ) VALUES (
        seq_point_get_quest_log.nextval,
        #{memberId},
        #{type},
        TO_DATE(#{date}, 'YYYYMMDD'),
        1,
        'N',
        SYSTIMESTAMP
    )
```

이 흐름을 잡아둔 덕분에 출석, 퀴즈, 룰렛이 모두 퀘스트 진행도와 자연스럽게 연결됐습니다.

---

### 3-4. 상점과 인벤토리는 적립 이후의 소비 흐름으로 묶었습니다.

포인트 기능을 단순 적립에만 머무르게 하지 않으려고, 상점과 인벤토리를 같은 흐름으로 연결했습니다.

- 상점 목록 조회, 검색, 필터, 페이징
- 구매 및 선물하기
- 위시리스트 토글
- 인벤토리 지급 및 조회
- 아이템 사용, 장착, 해제, 환불

구매 로직에서는 다음 순서가 흔들리지 않도록 처리했습니다.

1. 상품 존재 여부 확인
2. 중복 보유 여부 확인
3. 재고 확인
4. 포인트 차감
5. 재고 감소
6. 인벤토리 반영
7. 필요하면 위시리스트 정리

```java
public void purchaseItem(String loginId, long itemNo) {
    PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
    if (item == null) throw new RuntimeException("상품 정보가 없습니다.");
    if (item.getPointItemStock() <= 0) throw new RuntimeException("품절된 상품입니다.");

    addPoint(loginId, -(int)item.getPointItemPrice(), "USE", "아이템 구매: " + item.getPointItemName());

    item.setPointItemStock(item.getPointItemStock() - 1);
    pointItemDao.update(item);
    giveItemToInventory(loginId, itemNo);
}
```

장착형 아이템은 같은 타입이 여러 개 동시에 활성화되지 않도록, 장착 전에 같은 타입을 모두 해제하는 방식으로 처리했습니다.

```java
unequipByType(loginId, type);
inven.setInventoryEquipped("Y");
inventoryDao.update(inven);
```

포인트가 실제 소비와 프로필 변화로 이어지게 하려면, 이 단계의 무결성이 특히 중요하다고 봤습니다.

---

### 3-5. 관리자 기능도 같은 기준으로 운영할 수 있게 했습니다.

사용자 기능이 완성돼도 운영 화면이 없으면 관리가 어렵다고 생각해서, 관리자 기능도 같이 구성했습니다.

- 회원 포인트 지급 및 회수
- 데일리 퀴즈 등록, 수정, 삭제
- 상점 상품 등록, 수정, 삭제
- 회원 인벤토리 및 아이콘 자산 조회/지급/회수

관리자 포인트 조정 역시 일반 사용자 기능과 다른 별도 로직으로 만들지 않고, 기존 포인트 처리 기준을 재사용했습니다.

```java
public void adminUpdatePoint(String memberId, int amount) {
    String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
    String trxType = (amount > 0) ? "GET" : "USE";
    boolean result = addPoint(memberId, amount, trxType, reason);
    if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
}
```

운영 기능까지 같은 규칙으로 묶어두니, 관리자 액션 때문에 정합성이 무너지는 문제를 줄일 수 있었습니다.

---

## 4. 트러블슈팅

### 4-1. 인벤토리 조회가 계속 0건으로 내려오던 문제
- **상황**: DB에는 데이터가 있는데 응답이 항상 빈 배열이었습니다.
- **원인**: MyBatis에서 단일 `String` 파라미터를 받는데 mapper에서 `#{loginId}`로 써서 null 바인딩이 발생했습니다.
- **해결**: 단일 파라미터 기준에 맞춰 `#{value}`로 수정했습니다.

```sql
WHERE I.INVENTORY_MEMBER_ID = #{value}
```

### 4-2. 출석 연속일 계산이 자정 부근에서 어긋나던 문제
- **상황**: 연속 출석이 유지돼야 하는데 1로 초기화되거나, 반대로 끊겨야 하는데 유지되는 경우가 있었습니다.
- **원인**: `Timestamp`를 그대로 비교하면서 시간까지 섞여 판단이 흔들렸습니다.
- **해결**: `LocalDate`로 변환해 날짜만 기준으로 비교했습니다.

### 4-3. 퀘스트 로그가 같은 날 중복 생성되던 문제
- **상황**: 하루에 같은 타입 로그가 여러 줄 생겨 보상 조건이 비정상적으로 빨리 채워졌습니다.
- **원인**: 서버와 SQL의 날짜 포맷이 달라 `MERGE` 조건이 맞지 않았습니다.
- **해결**: 서버에서 전달하는 날짜 포맷을 `YYYYMMDD`로 통일했습니다.

### 4-4. 장착 아이템이 여러 개 동시에 Y로 남는 문제
- **상황**: 같은 타입 꾸미기 아이템이 여러 개 장착된 상태로 남아 화면 표시가 꼬였습니다.
- **원인**: 새 아이템 장착 전에 동일 타입 아이템을 먼저 해제하지 않았기 때문입니다.
- **해결**: 장착 직전에 `unequipByType()`을 호출해 같은 타입을 전부 N으로 바꾼 뒤, 선택한 아이템만 Y로 설정했습니다.

### 4-5. 일부 요청에서 loginId가 null로 들어오던 문제
- **상황**: 같은 도메인 기능인데 어떤 API만 인증 실패처럼 동작했습니다.
- **원인**: 프론트 요청 중 일부에 Authorization 헤더가 누락돼 필터에서 인증 정보가 세팅되지 않았습니다.
- **해결**: 프론트와 함께 axios instance를 통일하고, 백엔드는 인증 실패를 401로 명확히 반환해 원인을 바로 확인할 수 있게 했습니다.

---

## 5. 정리

이번 프로젝트에서 제가 맡은 포인트 도메인은 단순 보상 기능이 아니라, 사용자 행동을 서비스 안에서 이어주는 구조를 만드는 작업에 가까웠습니다.

출석, 퀘스트, 퀴즈, 룰렛으로 적립하고, 상점과 인벤토리에서 소비하며, 그 결과가 프로필과 자산에 반영되도록 설계했습니다.  
그 과정에서 가장 중요하게 본 기준은 아래 두 가지였습니다.

- 포인트가 움직이면 반드시 근거가 남아야 한다.
- 중복 처리나 예외 상황이 생겨도 흐름이 무너지지 않아야 한다.

기능을 많이 넣는 것보다, **한 도메인 안에서 정합성을 유지하면서 끝까지 연결하는 경험**을 할 수 있었던 프로젝트였습니다.
