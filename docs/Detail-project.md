# Review Tag | Backend 상세 정리

이 문서는 백엔드 전체가 아니라, 제가 직접 구현한 포인트 기능을 중심으로 코드를 정리한 기록입니다. 프로젝트에는 출석, 퀘스트, 상점 등 여러 화면이 있었지만, 곰곰이 생각해 보니 결국 '회원의 포인트를 올리고 내린다'는 점은 모두 똑같았습니다.

그래서 기능마다 API를 따로따로 만들기보다는, 포인트가 더해지고 빠지는 공통 규칙을 하나로 확실하게 잡아두는 데 집중했습니다. 화면은 나뉘어 있어도 실제 잔액과 내역은 하나의 기준표대로 움직여야 나중에 오류가 생기지 않을 것이라고 판단했기 때문입니다.

---

## 담당 역할

- 일일 출석 , 데일리 퀘스트 구현
- `PointService` 중심 포인트 지급 / 차감 / 이력 처리
- 상점 / 보관함 / 선물 / 환불 API 구현
- 아이콘 뽑기와 장착 흐름 연결
- 관리자 포인트 조정, 상품 관리, 자산 조회 기능 연동

---

## 먼저 해결하려고 본 문제

- 기능마다 포인트를 따로 바꾸면 잔액과 이력이 어긋나기 쉬운 문제
- 출석, 퀘스트, 구매, 환불이 다른 규칙으로 기록될 수 있는 문제
- 관리자 기능이 사용자 기능과 다른 방식으로 움직일 수 있는 문제

포인트 시스템에서는 화면이 몇 개인가보다 **'같은 포인트 값을 누가, 어떻게 바꾸는가'**를 확실히 하는 것이 훨씬 중요했습니다. 그래서 이 문서에서도 개별 API를 하나씩 나열하여 설명하기 전에, 포인트를 다루는 공통 기준을 어떻게 세웠는지부터 먼저 정리했습니다.

---

## 공통 포인트 처리

출석, 퀘스트, 구매, 환불, 관리자 지급처럼 진입점은 달라도 포인트가 바뀌면 잔액 검증과 이력 저장은 항상 같이 가야 했습니다. 그래서 증감은 전부 `PointService.addPoint()`를 타게 두었습니다.

### Service

```java
@Transactional
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

포인트 관련 기능에서는 결국 이 핵심 메서드가 흔들리지 않아야 다른 기능들도 안정적으로 동작할 수 있었습니다. 그래서 출석 체크, 상품 구매, 관리자 지급 등 포인트가 변동되는 모든 로직은 최대한 이 하나의 메서드를 거치도록 구조를 모았습니다.

---

## 출석 적립과 이력 저장

<table>
<tr>
<td width="48%"><img src="screenshots/01_attendance.png" width="100%" alt="출석 화면"></td>
<td valign="top">
출석 버튼은 단순해 보여도 서버에서는 몇 가지를 같이 맞춰야 했습니다.<br><br>
- 오늘 이미 출석했는지 확인<br>
- 연속 출석 일수 계산<br>
- 출석 기록 저장<br>
- 포인트 적립과 이력 저장<br><br>
이 네 가지가 따로 놀면 프론트에서 보는 도장과 잔액도 금방 어긋나게 됩니다.
</td>
</tr>
</table>

연결 기준
- `POST /point/main/attendance/check` 요청을 받음
- 서비스에서 오늘 출석 여부와 연속 출석을 계산
- `attendance_status`, `attendance_history`를 갱신한 뒤 공통 포인트 적립으로 연결

### Controller

```java
@PostMapping("/check")
public ResponseEntity<String> doCheck(@RequestAttribute String loginId) {
    try {
        attendanceService.checkAttendance(loginId);
        return ResponseEntity.ok("success:100");
    } catch (IllegalStateException e) {
        return ResponseEntity.ok("fail:" + e.getMessage());
    }
}
```

### Service

```java
@Transactional
public void checkAttendance(String loginId) {
    AttendanceStatusDto status = statusDao.selectOne(loginId);
    LocalDate today = LocalDate.now();

    if(status == null) {
        AttendanceStatusDto newStatus = AttendanceStatusDto.builder()
                .attendanceStatusMemberId(loginId)
                .attendanceStatusCurrent(1)
                .attendanceStatusMax(1)
                .build();
        statusDao.insert(newStatus);
        processAttendanceCompletion(loginId, 1, "첫 출석 환영 보너스");
        return;
    }

    LocalDate lastDate = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();
    if(today.equals(lastDate)) {
        throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
    }

    int currentStreak = today.minusDays(1).equals(lastDate)
            ? status.getAttendanceStatusCurrent() + 1
            : 1;

    updateStatusAndComplete(status, currentStreak, loginId, "일일 출석 (" + currentStreak + "일 연속)");
}
```

### Query

```xml
<update id="update">
    update attendance_status set
        attendance_status_current = #{attendanceStatusCurrent},
        attendance_status_max = #{attendanceStatusMax},
        attendance_status_total = attendance_status_total + 1,
        attendance_status_lastdate = systimestamp
    where attendance_status_member_id = #{attendanceStatusMemberId}
</update>
```

```xml
<insert id="insert">
    INSERT INTO point_history(
        point_history_id,
        point_history_member_id,
        point_history_amount,
        point_history_trx_type,
        point_history_reason
    ) VALUES(
        seq_point_history.nextval,
        #{pointHistoryMemberId},
        #{pointHistoryAmount},
        #{pointHistoryTrxType},
        #{pointHistoryReason}
    )
</insert>
```

출석에서 중요했던 건 버튼이 눌렸다는 사실보다, 출석 기록과 포인트 기록이 같이 남았는가였습니다. 그래야 프론트에서 보는 캘린더와 잔액도 같은 결과를 보여줄 수 있었습니다.

---

## 퀘스트 보상과 중복 수령 방지

<table>
<tr>
<td width="48%"><img src="screenshots/02_quest.png" width="100%" alt="일일 퀘스트 화면"></td>
<td valign="top">
일일 퀘스트는 당일 기준 진행도와 보상 수령 여부를 같이 봐야 했습니다.<br><br>
- 오늘 어떤 행동을 했는지<br>
- 목표를 채웠는지<br>
- 보상을 이미 받았는지<br><br>
같은 날 같은 퀘스트에서 보상이 두 번 나가면 바로 문제가 되기 때문에, 이 부분은 체크 순서를 먼저 정리했습니다.
</td>
</tr>
</table>

연결 기준
- 퀘스트 로그는 당일 기준으로 조회
- 목표 달성 여부와 보상 수령 여부를 분리해 판단
- 보상 수령 성공 시에만 공통 포인트 적립 호출

### Controller

```java
@PostMapping("/claim")
public String claim(@RequestAttribute("loginId") String loginId, @RequestBody Map<String, String> body) {
    try {
        String type = body.get("type");
        int reward = dailyQuestService.claimReward(loginId, type);
        return "success:" + reward;
    } catch (Exception e) {
        return "fail:" + e.getMessage();
    }
}
```

### Service

```java
@Transactional
public int claimReward(String memberId, String type) {
    DailyQuestProperties.QuestDetail targetQuest = questProps.getList().stream()
            .filter(q -> q.getType().equals(type)).findFirst()
            .orElseThrow(() -> new RuntimeException("존재하지 않는 퀘스트입니다."));

    List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
    Map<String, Object> myLog = logs.stream().filter(m -> m.get("type").equals(type)).findFirst().orElse(null);

    if (myLog == null) throw new RuntimeException("기록 없음");
    int current = Integer.parseInt(String.valueOf(myLog.get("count")));
    if (current < targetQuest.getTarget()) throw new RuntimeException("목표 미달성");
    if ("Y".equals(myLog.get("rewardYn"))) throw new RuntimeException("이미 수령");

    if (questDao.updateRewardStatus(memberId, type, getTodayStr()) > 0) {
        pointService.addPoint(memberId, targetQuest.getReward(), "GET", "일일 퀘스트 보상: " + targetQuest.getTitle());
        return targetQuest.getReward();
    }
    return 0;
}
```

### Query

```xml
<update id="updateRewardStatus">
    UPDATE POINT_GET_QUEST_LOG
    SET POINT_GET_QUEST_REWARD_YN = 'Y'
    WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
      AND POINT_GET_QUEST_TYPE = #{type}
      AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
      AND POINT_GET_QUEST_REWARD_YN = 'N'
</update>
```

여기서 중요했던 건 보상 금액 자체보다도, 오늘 한 번 받은 보상이 다시 나가지 않는지가 더 중요했습니다.

---

## 구매와 보관함 반영

<table>
<tr>
<td width="48%"><img src="screenshots/03_store.png" width="100%" alt="포인트 상점 화면"></td>
<td width="48%"><img src="screenshots/04_inventory.png" width="100%" alt="인벤토리 화면"></td>
</tr>
</table>

구매 버튼 하나만 눌러도 서버에서는 여러 값이 같이 바뀝니다.

- 포인트 차감
- 재고 감소
- 인벤토리 지급
- 위시리스트 정리
- 이력 저장

이 중 하나라도 빠지면 프론트에서 보는 상태가 금방 어긋날 수 있었습니다.

### Controller

```java
@PostMapping("/buy")
public ResponseEntity<String> buy(
        @RequestAttribute(required = false) String loginId,
        @RequestBody PointBuyVO vo) {
    if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

    pointService.purchaseItem(loginId, vo.getBuyItemNo());
    return ResponseEntity.ok("success");
}
```

### Service

```java
@Transactional
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

```java
public void useItem(String loginId, long inventoryNo, String extraValue) {
    InventoryDto inven = inventoryDao.selectOne(inventoryNo);
    PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
    String type = item.getPointItemType();

    switch (type) {
        case "CHANGE_NICK":
            memberDao.updateNickname(MemberDto.builder().memberId(loginId).memberNickname(extraValue).build());
            decreaseInventoryOrDelete(inven);
            break;
        case "DECO_NICK": case "DECO_BG": case "DECO_ICON": case "DECO_FRAME":
            unequipByType(loginId, type);
            inven.setInventoryEquipped("Y");
            inventoryDao.update(inven);
            break;
    }
}
```

### Query

```xml
<select id="selectListByMemberId" resultType="InventoryDto">
    SELECT 
        I.INVENTORY_NO        AS "inventoryNo",
        I.INVENTORY_MEMBER_ID AS "inventoryMemberId",
        I.INVENTORY_ITEM_NO   AS "inventoryItemNo",
        I.INVENTORY_QUANTITY  AS "inventoryQuantity",
        I.INVENTORY_EQUIPPED  AS "inventoryEquipped",
        P.POINT_ITEM_NAME     AS "pointItemName",
        P.POINT_ITEM_SRC      AS "pointItemSrc",
        P.POINT_ITEM_TYPE     AS "pointItemType"
    FROM INVENTORY I
    JOIN POINT_ITEM_STORE P ON I.INVENTORY_ITEM_NO = P.POINT_ITEM_NO
    WHERE I.INVENTORY_MEMBER_ID = #{loginId}
    ORDER BY I.INVENTORY_CREATED_AT DESC
</select>
```

상점과 보관함은 개별적인 화면처럼 보이지만, 백엔드에서는 단순한 CRUD로 끝나는 기능이 아닙니다. 아이템 구매 직후 사용자가 포인트 잔액과 보관함 내역을 확인했을 때 모든 값이 동시에 정확하게 갱신되어 있어야만 하는 중요한 흐름이었습니다.

---

## 이력과 운영 화면

<table>
<tr>
<td width="48%"><img src="screenshots/05_history.png" width="100%" alt="포인트 이력 화면"></td>
<td width="48%"><img src="screenshots/06_admin.png" width="100%" alt="관리자 화면"></td>
</tr>
</table>

이력 화면은 적립과 차감의 결과를 한 번에 확인하는 곳이었고, 운영 화면은 관리자 액션이 사용자 화면과 다른 규칙으로 움직이지 않게 확인하는 구간이었습니다.

### Controller

```java
@PostMapping("/point/update")
public String updatePoint(@RequestBody Map<String, Object> body) {
    try {
        String memberId = (String) body.get("memberId");
        int amount = Integer.parseInt(String.valueOf(body.get("amount")));
        pointService.adminUpdatePoint(memberId, amount);
        return "success";
    } catch (Exception e) {
        return e.getMessage();
    }
}
```

### Service

```java
@Transactional
public void adminUpdatePoint(String memberId, int amount) {
    String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
    String adminType = (amount > 0) ? "GET" : "USE";
    boolean result = addPoint(memberId, amount, adminType, reason);
    if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
}
```

### Query

```xml
<select id="selectListByMemberIdPaging" resultType="PointHistoryDto">
    SELECT * FROM (
        SELECT ROWNUM rn, tmp.* FROM (
            SELECT point_history_id as pointHistoryId,
                   point_history_member_id as pointHistoryMemberId,
                   point_history_amount as pointHistoryAmount,
                   point_history_trx_type as pointHistoryTrxType,
                   point_history_reason as pointHistoryReason,
                   point_history_created_at as pointHistoryCreatedAt
            FROM point_history
            WHERE point_history_member_id = #{memberId}
            ORDER BY point_history_id DESC
        ) tmp
    )
    WHERE rn BETWEEN #{beginRow} AND #{endRow}
</select>
```

운영 기능도 별도의 우회 로직으로 편하게 처리하기보다는, 사용자 포인트가 움직일 때와 최대한 비슷한 기준을 타게 두는 쪽이 훨씬 안전하다고 보았습니다. 누가 포인트를 건드리든 결국 하나의 튼튼한 규칙 안에서 움직여야 나중에 문제가 안 생길 것이라 생각했기 때문입니다.

---

## 트러블슈팅

### 포인트 금액만 바뀌고 이력이 빠지면 바로 이상해 보였습니다
- 문제: 화면에서는 잔액이 변했는데 왜 변했는지 남지 않으면 사용자가 바로 헷갈릴 수 있었습니다.
- 원인: 적립과 차감을 기능마다 따로 처리하면 이력 저장이 빠질 여지가 있었습니다.
- 해결: `PointService.addPoint()`로 포인트 증감과 이력 저장을 같이 처리했습니다.

### 퀘스트 보상을 같은 날 여러 번 받을 수 있는 문제
- 문제: 화면에서 버튼을 숨겨도 서버에서 한 번 더 막지 않으면 중복 수령이 생길 수 있었습니다.
- 원인: 로그 조회와 보상 수령 갱신이 분리돼 있으면 틈이 생길 수 있었습니다.
- 해결: 오늘 로그를 먼저 확인하고, `rewardYn`이 이미 `Y`면 막는 구조로 정리했습니다.

### 상점과 보관함을 따로 보면 값이 어긋나기 쉬웠습니다
- 문제: 구매는 성공했는데 보관함이나 위시 상태가 바로 맞지 않는 상황이 생길 수 있었습니다.
- 원인: 구매 뒤 포인트, 재고, 인벤토리, 위시가 한 번에 바뀌는데 각 로직을 따로 보면 누락이 생기기 쉬웠습니다.
- 해결: 구매 처리 안에서 검증, 차감, 재고 감소, 인벤토리 지급, 위시 정리를 한 번에 묶었습니다.

### 같은 타입 장착 아이템이 동시에 켜질 수 있었습니다
- 문제: 배경, 프레임 같은 꾸미기 아이템이 같은 타입에서 여러 개 켜지면 프론트 화면이 어색해졌습니다.
- 원인: 새 아이템만 `Y`로 올리고 기존 타입을 내리지 않으면 중복 장착 상태가 남을 수 있었습니다.
- 해결: 장착 전에 `unequipByType()`를 먼저 호출해 같은 타입을 모두 내리도록 정리했습니다.

---

## 확인한 시나리오

- 출석 뒤 출석 기록과 포인트 이력이 함께 남는지
- 같은 날 같은 퀘스트에서 보상이 중복 지급되지 않는지
- 구매 뒤 포인트 차감, 재고 감소, 인벤토리 지급, 위시 정리가 같이 반영되는지
- 잔액이 부족할 때 차감이 막히는지
- 관리자 조정도 포인트 이력에 남는지
- 장착 아이템이 같은 타입에서 여러 개 동시에 남지 않는지

---

## 정리하며

포인트 관련 기능들을 개발하면서 가장 중요하게 생각한 것은 단순히 API 개수를 늘리는 것이 아니었습니다. 사용자가 출석, 상점, 관리자 페이지 등 어떤 경로로 포인트를 얻고 쓰든 항상 같은 규칙으로 안전하게 기록이 남도록 만드는 것이었습니다. 여러 기능들을 하나의 일관된 기준으로 묶어내는 과정에 가장 많은 공을 들였습니다.
