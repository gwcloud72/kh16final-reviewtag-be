# Review Tag | Backend 상세 정리

이 문서는 백엔드 전체가 아닌, 제가 직접 구현한 포인트 기능을 중심으로 코드를 정리한 내용입니다. 프로젝트에는 출석, 퀘스트, 상점 등 다양한 화면이 존재했지만, 곰곰이 살펴보면 결국 모두가 ‘회원의 포인트를 올리고 내린다’는 동일한 목적을 가지고 있었습니다.

처음에는 각 기능마다 별도의 API를 만들 수도 있었지만, 포인트가 더해지고 빠지는 공통 규칙을 명확하게 하나로 정리하는 데 더 집중했습니다. 화면이 여러 개여도 실제 잔액과 이력이 동일한 기준에 따라 움직여야, 나중에 예상치 못한 오류를 막을 수 있다고 판단했기 때문입니다.

---

## 담당 역할

- 일일 출석 및 데일리 퀘스트 기능 개발
- `PointService`를 활용한 포인트 지급/차감/이력 관리
- 상점, 보관함, 선물, 환불 등 핵심 API 작성
- 아이콘 뽑기 및 장착 흐름과의 연결 고리 구축
- 관리자용 포인트 조정, 상품 관리, 자산 조회 기능 연동

---

## 먼저 해결하고자 했던 문제

- 기능별로 포인트를 따로 다루면 잔액과 이력이 맞지 않을 위험
- 출석, 퀘스트, 구매, 환불마다 서로 다른 방식으로 내역이 기록될 가능성
- 관리자 기능과 사용자 기능이 분리되어 불일치가 생길 수 있는 문제

포인트 시스템에서는 화면이 몇 개냐보다 **‘같은 포인트 값을 누가, 어떻게 바꾸는가’**를 정확히 정의하는 것이 훨씬 중요하다고 생각했습니다. 그래서 이 문서 역시 개별 API 설명에 앞서, 포인트를 처리하는 공통 기준을 어떻게 세웠는지부터 먼저 정리했습니다.

---

## 공통 포인트 처리 원칙

출석, 퀘스트, 구매, 환불, 관리자 지급 등 진입점은 다양하지만, 포인트가 변경될 때마다 잔액 검증과 이력 저장은 반드시 함께 이뤄져야 했습니다. 이를 위해 모든 증감 요청은 `PointService.addPoint()`를 반드시 거치도록 설계했습니다.

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

포인트 관련 기능에서는 무엇보다도 핵심 메서드의 일관성과 안정성이 중요했습니다. 이 메서드가 흔들리지 않아야 출석 체크나 상품 구매, 관리자 지급 등 포인트가 변동되는 여러 기능들도 자연스럽게 신뢰할 수 있었습니다. 그래서 포인트가 변경되는 모든 로직이 이 하나의 메서드를 거치도록 구조를 통합했습니다. 메서드의 역할을 명확하게 구분하면서도, 확장성을 고려해 유지보수 역시 용이하게 만들었습니다.

---

## 출석 적립과 이력 저장

<table>
<tr>
<td width="48%"><img src="screenshots/01_attendance.png" width="100%" alt="출석 화면"></td>
<td valign="top">
출석 버튼은 겉보기엔 단순해 보여도, 서버단에서는 여러 검증과 처리가 함께 이루어져야 했습니다.<br><br>
- 오늘 이미 출석했는지 확인<br>
- 연속 출석 일수 계산<br>
- 출석 기록 저장<br>
- 포인트 적립 및 이력 관리<br><br>
이 네 가지가 따로 놀게 되면, 프론트엔드에서 확인하는 도장이나 포인트 잔액이 서로 다르게 표시될 수 있습니다. 이러한 불일치를 방지하려면, 각 단계가 유기적으로 연결되어야 하고 데이터의 일관성이 항상 보장되어야 했습니다.
</td>
</tr>
</table>

로그인 사용자가 출석 체크를 시도하면,
- `POST /point/main/attendance/check` 요청이 서버에 전달됩니다.
- 서버에서는 오늘 출석 여부와 연속 출석 상태를 판별합니다.
- 이후 `attendance_status`와 `attendance_history`를 갱신한 뒤, 공통 포인트 적립 메서드로 연결되어 최종적으로 포인트가 적립됩니다.

이렇게 구조를 설계함으로써, 출석부터 포인트 적립까지의 모든 과정이 한 흐름 안에 자연스럽게 녹아들게 했습니다

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

출석에서는 단순히 버튼을 눌렀는지가 중요한 것이 아니라, 출석 내역과 포인트 적립 기록이 함께 남았는지가 핵심이었습니다. 그래야 프론트엔드에서 확인하는 캘린더 화면과 사용자의 포인트 잔액이 일치하게 표시될 수 있었습니다.

---

## 퀘스트 보상과 중복 수령 방지

<table>
<tr>
<td width="48%"><img src="screenshots/02_quest.png" width="100%" alt="일일 퀘스트 화면"></td>
<td valign="top">
일일 퀘스트 시스템에서는 그날의 퀘스트 진행 상황과 보상 수령 여부를 함께 확인해야 했습니다.<br><br>
- 오늘 어떤 행동을 완료했는지<br>
- 퀘스트 목표를 충족했는지<br>
- 이미 보상을 수령했는지<br><br>
특히, 같은 날 동일한 퀘스트의 보상이 중복으로 지급되면 즉시 문제가 발생할 수 있기 때문에, 이 부분에서는 체크 순서와 조건을 세심하게 정리해 두었습니다.
</td>
</tr>
</table>

### 연결 기준
- 퀘스트 로그는 당일을 기준으로 조회
- 목표 달성 여부와 보상 수령 여부는 별도로 판단
- 보상 수령에 성공할 때만 포인트 적립을 공통 모듈에 요청

이처럼 각 단계와 조건을 명확히 구분해 놓으면서, 중복 지급 없이 신뢰성 있는 보상 시스템을 만들기 위해 노력했습니다.
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

여기서 가장 중요했던 점은 보상 금액이 얼마인지 자체보다는, 이미 한 번 받은 보상이 다시 중복 지급되지 않는지 여부였습니다.

---

## 구매와 보관함 반영

<table>
<tr>
<td width="48%"><img src="screenshots/03_store.png" width="100%" alt="포인트 상점 화면"></td>
<td width="48%"><img src="screenshots/04_inventory.png" width="100%" alt="인벤토리 화면"></td>
</tr>
</table>

구매 버튼을 한 번만 눌러도 서버에서는 여러 값이 동시에 변경됩니다.

- 포인트 차감
- 상품 재고 감소
- 인벤토리 아이템 지급
- 위시리스트 정리
- 구매 이력 저장

이 중 단 하나라도 누락되면, 프론트엔드에서 보이는 정보들이 실제와 다르게 어긋나는 문제가 즉시 발생할 수 있습니다.

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


운영 기능 역시 별도의 우회 로직을 도입해 단순히 편의성만을 추구하기보다는, 사용자 포인트가 실제로 움직이는 방식과 최대한 동일한 기준을 적용하는 것이 훨씬 더 안전하다고 판단했습니다. 누가, 어떤 경로로 포인트를 조작하더라도 결국 하나의 튼튼한 원칙 안에서 관리되어야만, 시간이 흘러도 문제없이 일관성을 유지할 수 있다고 믿었습니다.

---

## 트러블슈팅

### 포인트 금액은 바뀌었는데 이력이 남지 않는 문제  
- 문제: 화면에서 잔액이 변해도, 그 이유가 이력에 기록되지 않으면 사용자는 언제든 혼란을 겪을 수밖에 없었습니다.  
- 원인: 적립과 차감 기능을 각각 따로 처리할 경우, 이력 저장을 깜빡하거나 누락할 가능성이 있었습니다.  
- 해결: `PointService.addPoint()`에서 포인트 증감과 동시에 이력 저장을 반드시 함께 처리하도록 통합하였습니다.

### 퀘스트 보상을 같은 날 여러 번 수령하는 문제  
- 문제: 프론트엔드에서 버튼만 숨긴다고 해서 서버에서 아예 차단하지 않으면, 동일한 보상을 여러 번 받을 수 있는 빈틈이 생겼습니다.  
- 원인: 로그 조회와 보상 수령 갱신이 떨어져 있으면, 중간에 타이밍 이슈 등으로 중복 처리가 가능한 상황이 발생했습니다.  
- 해결: 오늘 날짜의 로그를 먼저 확인하고, 이미 `rewardYn`이 `Y`인지 체크한 뒤에만 보상을 지급하도록 개선하였습니다.

### 상점과 보관함, 위시 간의 데이터 불일치 문제  
- 문제: 구매가 성공했지만, 보관함이나 위시 상태가 즉시 올바르게 반영되지 않아 사용자에게 혼란을 줄 수 있는 상황이 종종 있었습니다.  
- 원인: 구매 직후 포인트 차감, 재고 관리, 인벤토리 지급, 위시 조정 등 여러 작업이 각각 다른 영역에서 처리되어 일부 누락되기 쉬웠습니다.  
- 해결: 구매 로직 내부에서 검증, 차감, 재고 감소, 인벤토리 지급, 위시 정리 등을 한 번에 묶어 처리하게끔 구조를 단순화했습니다.

### 같은 타입의 장착 아이템이 동시에 활성화되는 문제  
- 문제: 배경이나 프레임과 같은 꾸미기 아이템이 같은 타입임에도 여러 개가 동시에 활성화되어, 프론트엔드에서 어색하게 보일 때가 있었습니다.  
- 원인: 새로운 아이템만 `Y`로 변경하고 기존 아이템을 비활성화하지 않으면 중복 장착 상태가 남았습니다.  
- 해결: 장착을 시도하기 전에 반드시 `unequipByType()`를 호출해 동일 타입의 모든 아이템을 먼저 해제시키는 방식으로 정돈하였습니다.

---

## 주요 시나리오 점검

- 출석 후 출석 기록과 포인트 이력이 동시에 남는지 확인했습니다.
- 같은 날 동일 퀘스트의 보상이 중복되어 지급되지 않는지 직접 테스트했습니다.
- 상품 구매 시 포인트 차감, 재고 감소, 인벤토리 반영, 위시 정리까지 모든 변화가 일괄적으로 적용되는지 꼼꼼히 검증했습니다.
- 잔액 부족 상황에서는 차감이 정상적으로 막히는지 확인했습니다.
- 관리자 조정 역시 일반 사용자와 마찬가지로 포인트 이력에 남는지 살폈습니다.
- 동일 타입의 장착 아이템이 한 번에 여러 개 동시에 켜지지 않는지도 재차 점검했습니다.

---

## 마무리하며

포인트 관련 기능을 개발하면서 가장 중점을 둔 부분은, 표면적으로 얼마나 다양한 API를 제공하느냐가 아니었습니다. 사용자가 출석, 상점, 관리자 페이지 등 어떤 경로를 통해 포인트를 획득하거나 사용할지라도, 항상 동일하고 안전한 원칙에 따라 기록과 처리가 이루어지도록 만드는 것이 진짜 중요하다고 생각했습니다. 여러 기능 사이에 숨어 있던 각기 다른 기준들을 하나의 통일된 원칙으로 묶어내고, 그 과정에서 발생할 수 있는 문제들을 미리 찾아 다듬는 데에 가장 많은 노력을 기울였습니다. 이를 바탕으로 앞으로도 신뢰할 수 있는 운영 환경을 지속적으로 유지하겠습니다.