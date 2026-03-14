# Backend 상세 정리 (Spring Boot + MyBatis + Oracle)

> **포인트 기반 보상 시스템** 백엔드입니다.  
> 출석/퀘스트/룰렛 등 활동을 포인트로 환산하고, 포인트로 아이템을 구매·선물·환불하며, 인벤토리(소모/장착)와 포인트 이력까지 한 흐름으로 연결했습니다.

---

## 목차
1. 프로젝트 개요
2. 인증/요청 흐름(JWT)
3. 데이터 무결성 설계(포인트/이력)
4. 기능 구현 16개 (사용자 기능)
5. 관리자 기능 (운영 화면/컨트롤러 기준 4개)
6. 트러블슈팅 5건
7. 정리

---

## 1. 프로젝트 개요

- **인증**: JWT 기반, 인증 성공 시 `request.setAttribute("loginId", ...)`로 로그인 사용자 식별  
  → 컨트롤러에서는 `@RequestAttribute loginId`로 사용자 ID를 받는 구조
- **DB**: Oracle
- **SQL 매핑**: MyBatis (쿼리 명시형)
- **핵심 원칙**: **포인트 증감은 반드시 이력(POINT_HISTORY)과 함께 남긴다**

---

## 2. 인증/요청 흐름(JWT)

요청 흐름은 아래처럼 동작합니다.

1) 클라이언트가 `Authorization: Bearer <token>` 헤더로 요청  
2) 서버의 필터/인터셉터가 토큰 파싱  
3) 토큰 유효하면 `loginId` 등을 `RequestAttribute`로 주입  
4) 컨트롤러에서 `@RequestAttribute loginId`로 인증 사용자 식별

> 이 구조 덕분에 서비스 로직은 “로그인한 사용자” 기준으로 단순하게 작성할 수 있고,  
> 인증 실패는 컨트롤러/필터 단계에서 빠르게 차단할 수 있습니다.

---

## 3. 데이터 무결성 설계 (포인트/이력)

### 3-1) 포인트 무결성 원칙

- 포인트 증감은 `PointService.addPoint()` **단일 진입점**
- 차감 시 잔액 검증(음수 방지)
- 포인트 변경이 성공하면 `POINT_HISTORY`를 **무조건 INSERT**

### 핵심 코드: `PointService.addPoint()`

```java
public boolean addPoint(String loginId, int amount, String trxType, String reason) {
    MemberDto currentMember = memberDao.selectOne(loginId);
    if (currentMember == null) throw new RuntimeException("회원 정보가 없습니다.");

    // 차감 시 잔액 검증
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

### 핵심 SQL: `POINT_HISTORY INSERT` (point-history-mapper.xml)

```sql
INSERT INTO point_history(
    point_history_id,
    point_history_member_id,
    point_history_amount,
    point_history_trx_type,
    point_history_reason
)
VALUES(
    seq_point_history.nextval,
    #{pointHistoryMemberId},
    #{pointHistoryAmount},
    #{pointHistoryTrxType},
    #{pointHistoryReason}
)
```

---

## 4. 기능 구현 16개 (사용자 기능)

> 아래 16개는 “사용자 관점에서 실제로 쓰는 흐름” 기준으로 정리했습니다.  
> 각 항목에 **Endpoint / Controller / Service / 핵심 코드 / 핵심 SQL**을 붙였습니다.

---

### 01) 출석체크 (중복 방지 + 연속 출석 + 포인트 지급)

**Endpoint**
- `GET  /point/main/attendance/status`
- `POST /point/main/attendance/check`
- `GET  /point/main/attendance/calendar`

**Controller**
- `AttendanceRestController`
  - `@RequestMapping("/point/main/attendance/")`
  - `@PostMapping("/check")` → `attendanceService.checkAttendance(loginId)`

**처리 흐름**
1. 출석 현황 조회
2. 오늘 이미 출석했으면 예외(중복 방지)
3. 어제 출석이면 연속출석 +1, 아니면 1로 초기화
4. 현황 업데이트 + 출석 히스토리 INSERT
5. 포인트 지급

**핵심 코드: `AttendanceService.checkAttendance()`**
```java
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

    if (status.getAttendanceStatusLastdate() == null) {
        updateStatusAndComplete(status, 1, loginId, "출석 보상");
        return;
    }

    LocalDate lastDate = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();

    if(today.equals(lastDate)) {
        throw new IllegalStateException("이미 오늘 출석체크를 완료했습니다.");
    }

    int currentStreak = 1;
    if(today.minusDays(1).equals(lastDate)) {
        currentStreak = status.getAttendanceStatusCurrent() + 1;
    }

    updateStatusAndComplete(status, currentStreak, loginId, "일일 출석 (" + currentStreak + "일 연속)");
}
```

**핵심 SQL: attendance-status-mapper.xml**
```sql
-- 출석 현황 최초 생성
insert into attendance_status(
    attendance_status_no,
    attendance_status_member_id,
    attendance_status_current,
    attendance_status_max,
    attendance_status_total
) values (
    seq_attendance_status.nextval,
    #{attendanceStatusMemberId},
    1, 1, 1
);

-- 출석 현황 갱신(연속/최고/총 출석/최근 날짜)
update attendance_status set
    attendance_status_current = #{attendanceStatusCurrent},
    attendance_status_max = #{attendanceStatusMax},
    attendance_status_total = attendance_status_total + 1,
    attendance_status_lastdate = systimestamp
where attendance_status_member_id = #{attendanceStatusMemberId};

-- 출석 히스토리 기록
insert into attendance_history(
    attendance_history_no,
    attendance_history_member_id
) values (
    seq_attendance_history.nextval,
    #{attendanceHistoryMemberId}
);
```

---

### 02) 일일 퀘스트 목록 조회 (진행도/보상여부 포함)

**Endpoint**
- `GET /point/quest/list`

**Controller**
- `DailyQuestRestController`
  - `@GetMapping("/list")` → `dailyQuestService.getQuestList(loginId)`

**핵심 SQL: daily-quest-mapper.xml**
```sql
SELECT
    POINT_GET_QUEST_TYPE      AS "type",
    POINT_GET_QUEST_COUNT     AS "count",
    POINT_GET_QUEST_REWARD_YN AS "rewardYn"
FROM POINT_GET_QUEST_LOG
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
```

---

### 03) 퀘스트 진행도 증가 (UPSERT: 없으면 생성, 있으면 +1)

**공통 처리**
- 퀴즈/룰렛 등 여러 기능에서 `questProgress(memberId, type)`로 진행도 상승

**핵심 코드: `DailyQuestService.questProgress()`**
```java
public void questProgress(String memberId, String type) {
    boolean isValid = questProps.getList().stream().anyMatch(q -> q.getType().equals(type));
    if(isValid) {
        questDao.upsertQuestLog(memberId, type, getTodayStr());
    }
}
```

**핵심 SQL: MERGE (daily-quest-mapper.xml)**
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

---

### 04) 퀘스트 보상 수령 (중복 수령 방지 + 포인트 지급)

**Endpoint**
- `POST /point/quest/claim`

**처리 흐름**
1. 오늘 로그 조회 → 해당 type 로그 찾기
2. 목표치 미달이면 실패
3. rewardYn이 Y면 실패(중복수령 방지)
4. rewardYn 업데이트 성공 시 포인트 지급

**핵심 코드: `DailyQuestService.claimReward()`**
```java
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
        pointService.addPoint(memberId, targetQuest.getReward(), "GET",
                "일일 퀘스트 보상: " + targetQuest.getTitle());
        return targetQuest.getReward();
    }
    return 0;
}
```

**핵심 SQL: rewardYn 업데이트**
```sql
UPDATE POINT_GET_QUEST_LOG
SET POINT_GET_QUEST_REWARD_YN = 'Y'
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND POINT_GET_QUEST_TYPE = #{type}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
  AND POINT_GET_QUEST_REWARD_YN = 'N'
```

---

### 05) 데일리 퀴즈 랜덤 출제 (1일 1회 제한)

**Endpoint**
- `GET /point/quest/quiz/random`

**Controller**
- `DailyQuestRestController`
  - `@GetMapping("/quiz/random")`

**핵심 코드**
```java
public DailyQuizVO getRandomQuiz(String memberId) {
    List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
    boolean alreadySolved = logs.stream().anyMatch(m -> "QUIZ".equals(m.get("type")));

    if (alreadySolved) return null;
    return quizDao.getRandomQuiz();
}
```

**핵심 SQL**
```sql
SELECT * FROM (
    SELECT QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER
    FROM DAILY_QUIZ
    ORDER BY DBMS_RANDOM.VALUE
)
WHERE ROWNUM = 1
```

---

### 06) 데일리 퀴즈 정답 검증 + 퀘스트 진행도 반영

**Endpoint**
- `POST /point/quest/quiz/check`

**핵심 코드**
```java
public boolean checkQuizAndProgress(String memberId, int quizNo, String userAnswer) {
    if (userAnswer == null) return false;
    String correctAnswer = quizDao.getAnswer(quizNo);
    if (correctAnswer == null) return false;

    String cleanUser = userAnswer.replace(" ", "").toLowerCase();
    String cleanCorrect = correctAnswer.replace(" ", "").toLowerCase();

    if (cleanUser.contains(cleanCorrect)) {
        this.questProgress(memberId, "QUIZ");
        return true;
    }
    return false;
}
```

**핵심 SQL**
```sql
SELECT QUIZ_ANSWER
FROM DAILY_QUIZ
WHERE QUIZ_NO = #{quizNo}
```

---

### 07) 룰렛 게임 (티켓 소모 + 랜덤 보상 + 퀘스트 진행)

**Endpoint**
- `POST /point/main/store/roulette`

**Controller**
- `PointStoreRestController`
  - `@PostMapping("/roulette")`

**핵심 코드**
```java
public int playRoulette(String loginId) {
    List<InventoryDto> userInventory = inventoryDao.selectListByMemberId(loginId);
    InventoryDto ticket = userInventory.stream()
            .filter(i -> {
                PointItemStoreDto itemInfo = pointItemDao.selectOneNumber(i.getInventoryItemNo());
                return itemInfo != null && "RANDOM_ROULETTE".equals(itemInfo.getPointItemType());
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("룰렛 티켓이 없습니다."));

    int idx = (int)(Math.random() * 6);
    int reward = (idx == 4) ? 2000 : (idx == 0) ? 1000 : 0;

    decreaseInventoryOrDelete(ticket);

    if (reward > 0) {
        addPoint(loginId, reward, "GET", "룰렛 당첨");
    }

    dailyQuestService.questProgress(loginId, "ROULETTE");
    return idx;
}

private void decreaseInventoryOrDelete(InventoryDto inven) {
    if (inven.getInventoryQuantity() > 1) {
        inven.setInventoryQuantity(inven.getInventoryQuantity() - 1);
        inventoryDao.update(inven);
    } else {
        inventoryDao.delete(inven.getInventoryNo());
    }
}
```

**핵심 SQL**
```sql
UPDATE INVENTORY
SET
    inventory_quantity = #{inventoryQuantity},
    inventory_equipped = #{inventoryEquipped}
WHERE
    inventory_no = #{inventoryNo};

DELETE FROM INVENTORY
WHERE inventory_no = #{inventoryNo};
```

---

### 08) 포인트 상점 목록 조회 (검색/필터/페이징)

**Endpoint**
- `GET /point/main/store`

**Controller**
- `PointStoreRestController`
  - `@GetMapping("")`

**핵심 SQL: point-item-store-mapper.xml**
```sql
SELECT * FROM (
    SELECT TMP.*, ROWNUM RN FROM (
        SELECT
            point_item_no as pointItemNo,
            point_item_name as pointItemName,
            point_item_type as pointItemType,
            point_item_content as pointItemContent,
            point_item_price as pointItemPrice,
            point_item_stock as pointItemStock,
            point_item_src as pointItemSrc,
            point_item_reg_date as pointItemRegDate,
            point_item_req_level as pointItemReqLevel,
            POINT_ITEM_IS_LIMITED_PURCHASE as pointItemIsLimitedPurchase,
            POINT_ITEM_DAILY_LIMIT as pointItemDailyLimit
        FROM POINT_ITEM_STORE
        <where>
            <if test="itemType != null and itemType != '' and itemType != 'ALL'">
                AND point_item_type = #{itemType}
            </if>
            <if test="keyword != null and keyword != ''">
                AND UPPER(point_item_name) LIKE UPPER('%' || #{keyword} || '%')
            </if>
        </where>
        ORDER BY point_item_no DESC
    ) TMP
) WHERE RN BETWEEN #{begin} AND #{end}
```

---

### 09) 상품 구매 (포인트 차감 + 재고 감소 + 인벤 지급 + 찜 정리)

**Endpoint**
- `POST /point/main/store/buy`

**Controller**
- `PointStoreRestController`
  - `@PostMapping("/buy")` → `pointService.purchaseItem(loginId, buyItemNo)`

**핵심 코드**
```java
public void purchaseItem(String loginId, long itemNo) {
    PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
    if (item == null) throw new RuntimeException("상품 정보가 없습니다.");

    if (item.getPointItemType() != null && item.getPointItemType().startsWith("DECO_")) {
        InventoryDto existingItem = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
        if (existingItem != null) throw new RuntimeException("이미 보유 중인 꾸미기 아이템입니다.");
    }

    if (item.getPointItemStock() <= 0) throw new RuntimeException("품절된 상품입니다.");

    addPoint(loginId, -(int)item.getPointItemPrice(), "USE", "아이템 구매: " + item.getPointItemName());

    item.setPointItemStock(item.getPointItemStock() - 1);
    pointItemDao.update(item);

    if ("HEART_RECHARGE".equals(item.getPointItemType())) {
        chargeHeart(loginId, 5);
    } else {
        giveItemToInventory(loginId, itemNo);
    }

    PointItemWishVO wishVO = PointItemWishVO.builder().memberId(loginId).itemNo(itemNo).build();
    if (pointWishlistDao.checkWish(wishVO) > 0) pointWishlistDao.delete(wishVO);
}
```

**핵심 SQL**
```sql
-- 인벤토리 보유 여부 체크
SELECT
    inventory_no as inventoryNo,
    inventory_member_id as inventoryMemberId,
    inventory_item_no as inventoryItemNo,
    inventory_quantity as inventoryQuantity,
    inventory_equipped as inventoryEquipped
FROM INVENTORY
WHERE inventory_member_id = #{inventoryMemberId}
  AND inventory_item_no = #{inventoryItemNo};

-- 상점 아이템 재고/정보 update
UPDATE POINT_ITEM_STORE
SET
    point_item_name = #{pointItemName},
    point_item_type = #{pointItemType},
    point_item_content = #{pointItemContent},
    point_item_price = #{pointItemPrice},
    point_item_stock = #{pointItemStock},
    point_item_src = #{pointItemSrc},
    point_item_req_level = #{pointItemReqLevel},
    POINT_ITEM_IS_LIMITED_PURCHASE = #{pointItemIsLimitedPurchase},
    POINT_ITEM_DAILY_LIMIT = #{pointItemDailyLimit}
WHERE point_item_no = #{pointItemNo};
```

---

### 10) 선물하기 (보낸사람 차감 + 재고 감소 + 상대 인벤 지급 + 찜 정리)

**Endpoint**
- `POST /point/main/store/gift`

**Controller**
- `PointStoreRestController`
  - `@PostMapping("/gift")`

**핵심 코드**
```java
public void giftItem(String loginId, String targetId, long itemNo) {
    PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
    if (item == null || item.getPointItemStock() <= 0) throw new RuntimeException("선물 가능한 상품이 없습니다.");

    addPoint(loginId, -(int)item.getPointItemPrice(), "USE", targetId + "님에게 선물: " + item.getPointItemName());
    item.setPointItemStock(item.getPointItemStock() - 1);
    pointItemDao.update(item);
    giveItemToInventory(targetId, itemNo);

    PointItemWishVO wishVO = PointItemWishVO.builder().memberId(loginId).itemNo(itemNo).build();
    if (pointWishlistDao.checkWish(wishVO) > 0) pointWishlistDao.delete(wishVO);
}
```

---

### 11) 찜(위시리스트) 토글 + 내 찜 목록 조회

**Endpoint**
- `POST /point/main/store/wish/toggle`
- `GET  /point/main/store/wish/my`
- `GET  /point/main/store/wish/check`

**핵심 코드**
```java
public boolean toggleWish(String loginId, long itemNo) {
    PointItemWishVO vo = PointItemWishVO.builder().memberId(loginId).itemNo(itemNo).build();
    if (pointWishlistDao.checkWish(vo) > 0) {
        pointWishlistDao.delete(vo);
        return false;
    } else {
        pointWishlistDao.insert(vo);
        return true;
    }
}
```

**핵심 SQL**
```sql
SELECT COUNT(*)
FROM POINT_WISHLIST
WHERE POINT_WISHLIST_MEMBER_ID = #{memberId}
  AND POINT_WISHLIST_ITEM_NO = #{itemNo};

INSERT INTO POINT_WISHLIST (
    POINT_WISHLIST_NO,
    POINT_WISHLIST_MEMBER_ID,
    POINT_WISHLIST_ITEM_NO
)
VALUES (
    SEQ_POINT_WISHLIST.NEXTVAL,
    #{memberId},
    #{itemNo}
);

DELETE FROM POINT_WISHLIST
WHERE POINT_WISHLIST_MEMBER_ID = #{memberId}
  AND POINT_WISHLIST_ITEM_NO = #{itemNo};
```

---

### 12) 인벤토리 조회 (상점 정보 JOIN)

**Endpoint**
- `GET /point/main/store/inventory/my`

**Controller**
- `PointStoreRestController`
  - `@GetMapping("/inventory/my")` → `inventoryDao.selectListByMemberId(loginId)`

**핵심 SQL (inventory-mapper.xml)**  
※ 단일 String 파라미터 바인딩 이슈가 있어 `#{value}`로 수정한 케이스입니다.

```sql
SELECT
    I.INVENTORY_NO          AS "inventoryNo",
    I.INVENTORY_MEMBER_ID   AS "inventoryMemberId",
    I.INVENTORY_ITEM_NO     AS "inventoryItemNo",
    I.INVENTORY_CREATED_AT  AS "inventoryCreatedAt",
    I.INVENTORY_QUANTITY    AS "inventoryQuantity",
    I.INVENTORY_EQUIPPED    AS "inventoryEquipped",
    P.POINT_ITEM_NAME       AS "pointItemName",
    P.POINT_ITEM_SRC        AS "pointItemSrc",
    P.POINT_ITEM_TYPE       AS "pointItemType",
    P.POINT_ITEM_CONTENT    AS "pointItemContent"
FROM INVENTORY I
JOIN POINT_ITEM_STORE P
  ON I.INVENTORY_ITEM_NO = P.POINT_ITEM_NO
WHERE I.INVENTORY_MEMBER_ID = #{value}
ORDER BY I.INVENTORY_CREATED_AT DESC
```

---

### 13) 아이템 사용 (소모/랜덤포인트/장착형 분기)

**Endpoint**
- `POST /point/main/store/inventory/use`

**핵심 코드**
```java
public void useItem(String loginId, long inventoryNo, String extraValue) {
    InventoryDto inven = inventoryDao.selectOne(inventoryNo);
    if (inven == null || !inven.getInventoryMemberId().equals(loginId))
        throw new RuntimeException("아이템 권한 없음");

    PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
    String type = item.getPointItemType();

    switch (type) {
        case "CHANGE_NICK":
            if (extraValue == null || extraValue.trim().isEmpty())
                throw new RuntimeException("새 닉네임을 입력하세요.");
            memberDao.updateNickname(MemberDto.builder()
                    .memberId(loginId)
                    .memberNickname(extraValue)
                    .build());
            decreaseInventoryOrDelete(inven);
            break;

        case "RANDOM_POINT":
            int randomIdx = new java.util.Random().nextInt(31);
            int won = (randomIdx * 100) + 500;
            addPoint(loginId, won, "GET", "포인트 랜덤 박스 사용 + " + won + "원 획득");
            decreaseInventoryOrDelete(inven);
            break;

        case "DECO_NICK": case "DECO_BG": case "DECO_ICON": case "DECO_FRAME":
            unequipByType(loginId, type);
            inven.setInventoryEquipped("Y");
            inventoryDao.update(inven);
            break;

        case "VOUCHER":
            addPoint(loginId, (int)item.getPointItemPrice(), "GET",
                    "상품권 사용 " + item.getPointItemPrice() + "원 획득");
            decreaseInventoryOrDelete(inven);
            break;
    }
}
```

**핵심 SQL: 타입별 선 해제 (unequipByType)**
```sql
UPDATE inventory
SET inventory_equipped = 'N'
WHERE inventory_member_id = #{memberId}
AND inventory_item_no IN (
    SELECT point_item_no
    FROM point_item_store
    WHERE point_item_type = #{type}
)
```

---

### 14) 장착 해제 (단일 아이템 unequip)

**Endpoint**
- `POST /point/main/store/inventory/unequip`

**핵심 코드**
```java
public void unequipItem(String loginId, long inventoryNo) {
    InventoryDto inv = inventoryDao.selectOne(inventoryNo);
    if(inv != null && loginId.equals(inv.getInventoryMemberId())) {
        inv.setInventoryEquipped("N");
        inventoryDao.update(inv);
    }
}
```

---

### 15) 환불 (포인트 반환 + 재고 복구 + 인벤 차감)

**Endpoint**
- `POST /point/main/store/cancel`

**핵심 코드**
```java
public void cancelItem(String loginId, long inventoryNo) {
    InventoryDto inven = inventoryDao.selectOne(inventoryNo);
    if (inven == null || !inven.getInventoryMemberId().equals(loginId))
        throw new RuntimeException("환불 권한 없음");

    PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
    addPoint(loginId, (int)item.getPointItemPrice(), "GET", "환불: " + item.getPointItemName());

    item.setPointItemStock(item.getPointItemStock() + 1);
    pointItemDao.update(item);

    decreaseInventoryOrDelete(inven);
}
```

---

### 16) 포인트 이력 조회 (페이징 + 타입 필터)

**Endpoint**
- `GET /point/history?page=1&type=all|earn|use|item`

**핵심 코드**
```java
public PointHistoryPageVO getHistoryList(String loginId, int page, String type) {
    int size = 10;
    int startRow = (page - 1) * size + 1;
    int endRow = page * size;

    List<PointHistoryDto> list =
        pointHistoryDao.selectListByMemberIdPaging(loginId, startRow, endRow, type);

    int totalCount = pointHistoryDao.countHistory(loginId, type);

    return PointHistoryPageVO.builder()
        .list(list)
        .totalCount(totalCount)
        .totalPage((totalCount + size - 1) / size)
        .currentPage(page)
        .build();
}
```

**핵심 SQL: point-history-mapper.xml**
```sql
SELECT * FROM (
    SELECT TMP.*, ROWNUM RN FROM (
        SELECT
            point_history_id as pointHistoryId,
            point_history_member_id as pointHistoryMemberId,
            point_history_amount as pointHistoryAmount,
            point_history_trx_type as pointHistoryTrxType,
            point_history_reason as pointHistoryReason,
            point_history_created_at as pointHistoryCreatedAt
        FROM point_history
        WHERE point_history_member_id = #{memberId}
        <choose>
            <when test="type == 'earn'">AND point_history_amount > 0</when>
            <when test="type == 'use'">AND point_history_amount &lt; 0</when>
            <when test="type == 'item'">AND point_history_amount = 0</when>
        </choose>
        ORDER BY point_history_id DESC
    ) TMP
)
WHERE RN BETWEEN #{startRow} AND #{endRow}
```

---

## 5. 관리자 기능 4개 (운영 화면/컨트롤러 기준)

> 관리자 기능은 `AdminRestController`의 실제 엔드포인트 기준으로 대표 4개만 정리했습니다.  
> (포인트/퀴즈/상점 아이템/자산 지급·회수)

---

### [관리자-1] 회원 포인트 관리 (리스트 조회 + 지급/회수)

**Endpoint (AdminRestController)**
- `GET  /admin/point/list?keyword=&page=1`
- `POST /admin/point/update` (요청 바디로 memberId/amount)

**Controller**
- `AdminRestController`
  - `@GetMapping("/point/list")` → `memberDao.selectPointAdminList(...)`
  - `@PostMapping("/point/update")` → `pointService.adminUpdatePoint(memberId, amount)`

**핵심 코드: `PointService.adminUpdatePoint()`**
```java
public void adminUpdatePoint(String memberId, int amount) {
    String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
    String AdminType = (amount > 0) ?  "GET" : "USE";
    boolean result = addPoint(memberId, amount, AdminType, reason);
    if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
}
```

**핵심 SQL: member-mapper.xml (리스트/카운트)**
```sql
<select id="selectPointAdminList" resultType="MemberDto">
    SELECT * FROM (
        SELECT TMP.*, ROWNUM RN FROM (
            SELECT 
                member_id AS "memberId", 
                member_nickname AS "memberNickname", 
                member_point AS "memberPoint", 
                member_level AS "memberLevel", 
                to_char(member_join, 'YYYY-MM-DD') as "memberJoinDate"
            FROM member
            WHERE member_level != '관리자'
            <if test="keyword != null and keyword != ''">
                AND (member_id LIKE '%' || #{keyword} || '%' 
                     OR member_nickname LIKE '%' || #{keyword} || '%')
            </if>
            ORDER BY member_join DESC
        ) TMP
    )
    WHERE RN BETWEEN #{start} AND #{end}
</select>

<select id="countPointAdminList" resultType="int">
    SELECT COUNT(*) FROM member
    WHERE member_level != '관리자'
    <if test="keyword != null and keyword != ''">
        AND (member_id LIKE '%' || #{keyword} || '%' 
             OR member_nickname LIKE '%' || #{keyword} || '%')
    </if>
</select>
```

---

### [관리자-2] 데일리 퀴즈 관리 (CRUD + 검색/페이징)

**Endpoint (AdminRestController)**
- `GET    /admin/quiz/list?type=all|question|answer&keyword=&page=1`
- `POST   /admin/quiz` (등록)
- `PUT    /admin/quiz` (수정)
- `DELETE /admin/quiz/{quizNo}` (삭제)

**DAO/Mapper**
- `DailyQuestDao`가 `dailyquiz.*` 네임스페이스로 관리 기능까지 포함

**핵심 SQL: daily-quiz-mapper.xml**
```sql
-- 등록
INSERT INTO DAILY_QUIZ (QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER)
VALUES (SEQ_DAILY_QUIZ_NO.NEXTVAL, #{quizQuestion}, #{quizAnswer});

-- 수정
UPDATE DAILY_QUIZ
SET QUIZ_QUESTION = #{quizQuestion},
    QUIZ_ANSWER = #{quizAnswer}
WHERE QUIZ_NO = #{quizNo};

-- 삭제
DELETE FROM DAILY_QUIZ WHERE QUIZ_NO = #{quizNo};

-- 목록(검색/페이징)
SELECT * FROM (
    SELECT ROWNUM RN, TMP.* FROM (
        SELECT * FROM DAILY_QUIZ 
        WHERE 1=1
        <if test="keyword != null and keyword != ''">
            <if test="type == 'question'">
                AND INSTR(QUIZ_QUESTION, #{keyword}) > 0
            </if>
            <if test="type == 'answer'">
                AND INSTR(QUIZ_ANSWER, #{keyword}) > 0
            </if>
            <if test="type == 'all'">
                AND (INSTR(QUIZ_QUESTION, #{keyword}) > 0 OR INSTR(QUIZ_ANSWER, #{keyword}) > 0)
            </if>
        </if>
        ORDER BY QUIZ_NO DESC
    ) TMP
)
WHERE RN BETWEEN #{startRow} AND #{endRow;
```

---

### [관리자-3] 포인트 상점 아이템 관리 (등록/수정/삭제)

**Endpoint (AdminRestController)**
- `POST   /admin/store/add`
- `PUT    /admin/store/edit`
- `DELETE /admin/store/delete/{itemNo}`

**Service**
- `PointService.editItem()` / `deleteItem()`는 DAO 호출로 단순화  
  (운영 화면에서 상품 정보를 직접 관리하는 용도)

**핵심 코드**
```java
public void editItem(String loginId, PointItemStoreDto d) {
    pointItemDao.update(d);
}

public void deleteItem(String loginId, long itemNo) {
    pointItemDao.delete(itemNo);
}
```

**핵심 SQL: point-item-store-mapper.xml**
```sql
-- 아이템 등록
INSERT INTO POINT_ITEM_STORE(  
    point_item_no, point_item_name, point_item_type,
    point_item_content, point_item_price, point_item_stock,
    point_item_src, point_item_req_level,
    POINT_ITEM_IS_LIMITED_PURCHASE, POINT_ITEM_DAILY_LIMIT
)
VALUES(
    seq_point_item.nextval, #{pointItemName}, #{pointItemType},
    #{pointItemContent}, #{pointItemPrice}, #{pointItemStock},
    #{pointItemSrc}, #{pointItemReqLevel},
    #{pointItemIsLimitedPurchase}, #{pointItemDailyLimit}
);

-- 아이템 수정/재고 변경
UPDATE POINT_ITEM_STORE
SET
    point_item_name = #{pointItemName},
    point_item_type = #{pointItemType},
    point_item_content = #{pointItemContent},
    point_item_price = #{pointItemPrice},
    point_item_stock = #{pointItemStock},
    point_item_src = #{pointItemSrc},
    point_item_req_level = #{pointItemReqLevel},
    POINT_ITEM_IS_LIMITED_PURCHASE = #{pointItemIsLimitedPurchase},
    POINT_ITEM_DAILY_LIMIT = #{pointItemDailyLimit}
WHERE point_item_no = #{pointItemNo};

-- 아이템 삭제
DELETE FROM POINT_ITEM_STORE WHERE point_item_no = #{pointItemNo};
```

---

### [관리자-4] 유저 자산 관리 (인벤/아이콘 조회 + 지급/회수)

**Endpoint (AdminRestController)**
- `GET    /admin/inventory/list?keyword=&page=1`  (유저 목록)
- `GET    /admin/inventory/{memberId}`            (유저 인벤 조회)
- `DELETE /admin/inventory/{inventoryNo}`         (인벤 아이템 회수)
- `POST   /admin/inventory/{memberId}/{itemNo}`   (아이템 지급)

- `GET    /admin/icon/{memberId}`                 (유저 아이콘 조회)
- `POST   /admin/icon/{memberId}/{iconId}`        (아이콘 지급)
- `DELETE /admin/icon/recall/{memberIconId}`      (아이콘 회수)

**Service**
- `AdminAssetService`가 자산 조회/지급/회수를 담당

**핵심 SQL (inventory-mapper.xml)**
```sql
-- 관리자용 유저 목록(검색/페이징)
SELECT * FROM (
    SELECT rownum rn, tmp.* FROM (
        SELECT * FROM member
        <where>
            <if test="keyword != null and keyword != ''">
                (member_id LIKE '%' || #{keyword} || '%'
                 OR member_nickname LIKE '%' || #{keyword} || '%')
            </if>
            AND member_level != '관리자'
        </where>
        ORDER BY member_id ASC
    ) tmp
) WHERE rn BETWEEN #{startRow} AND #{endRow};

-- 관리자용 유저 인벤 조회(상점 정보 조인)
SELECT 
    I.INVENTORY_NO          AS "inventoryNo",
    I.INVENTORY_MEMBER_ID   AS "inventoryMemberId",
    I.INVENTORY_ITEM_NO     AS "inventoryItemNo",
    I.INVENTORY_CREATED_AT  AS "inventoryCreatedAt",
    I.INVENTORY_EQUIPPED    AS "inventoryEquipped",
    I.INVENTORY_QUANTITY    AS "inventoryQuantity",
    P.POINT_ITEM_NAME       AS "pointItemName",
    P.POINT_ITEM_SRC        AS "pointItemSrc",
    P.POINT_ITEM_TYPE       AS "pointItemType"
FROM INVENTORY I
LEFT OUTER JOIN POINT_ITEM_STORE P ON I.INVENTORY_ITEM_NO = P.POINT_ITEM_NO
JOIN MEMBER M ON I.INVENTORY_MEMBER_ID = M.MEMBER_ID
WHERE I.INVENTORY_MEMBER_ID = #{memberId}
  AND M.MEMBER_LEVEL != '관리자'
ORDER BY I.INVENTORY_CREATED_AT DESC;
```

**핵심 SQL (member-icon-mapper.xml)**
```sql
-- 아이콘 중복 보유 체크
SELECT count(*) 
FROM MEMBER_ICONS 
WHERE MEMBER_ICONS_MEMBER_ID = #{memberId} 
  AND MEMBER_ICONS_ICON_ID = #{iconId};

-- 아이콘 지급
INSERT INTO MEMBER_ICONS (
    MEMBER_ICONS_ID,
    MEMBER_ICONS_MEMBER_ID,
    MEMBER_ICONS_ICON_ID,
    MEMBER_ICONS_OBTAIN_TIME
)
VALUES (
    seq_member_icons.nextval,
    #{memberId},
    #{iconId},
    SYSTIMESTAMP
);

-- 아이콘 회수
DELETE FROM MEMBER_ICONS
WHERE MEMBER_ICONS_ID = #{memberIconId};
```

---

## 6. 트러블슈팅 5건 (원인 → 확인 → 수정)

> “현상만 적기”가 아니라, **어디서 막혔고(컨트롤러/서비스/매퍼), 어떤 로그로 확인했는지, 어떤 수정으로 해결했는지**까지 남겼습니다.

---

### (1) 인벤토리 조회가 항상 0건으로 나옴 (MyBatis 단일 파라미터 바인딩)

**현상**
- `GET /point/main/store/inventory/my` 응답이 항상 `[]`
- DB에는 INVENTORY 데이터 존재

**문제 지점**
- Controller: `PointStoreRestController.myInventory()`  
- Mapper: `inventory-mapper.xml`의 `selectListByMemberId`

**원인**
- DAO가 String 1개만 전달하면 MyBatis 기본 파라미터명이 `value`로 잡힘
- mapper에서 `#{loginId}`로 받으면 null 바인딩 → WHERE 조건이 항상 false

**확인**
- MyBatis 바인딩 로그에서 `loginId=null`로 찍히는 것을 확인
- 같은 SQL을 DB에서 직접 실행하면 결과가 나오는 것도 확인

**수정**
```sql
-- 수정 전
WHERE I.INVENTORY_MEMBER_ID = #{loginId}

-- 수정 후
WHERE I.INVENTORY_MEMBER_ID = #{value}
```

---

### (2) 로그인은 됐는데 loginId가 null로 들어옴 (@RequestAttribute 누락/헤더 형식)

**현상**
- 어떤 API는 잘 되는데, 특정 요청에서 `@RequestAttribute loginId`가 null

**문제 지점**
- Filter/Interceptor → Controller로 넘어오는 인증 정보 전달 구간

**원인**
- 특정 요청에서 Authorization 헤더가 누락되거나(`Bearer` 빠짐 포함)
- 또는 axios 인터셉터가 일부 요청에서 적용되지 않아 헤더가 빠진 케이스

**확인**
- 인증 필터에서 Authorization 헤더 수신 로그 확인
- 토큰 파싱 성공/실패 로그 확인

**수정**
- 프론트에서 axios instance를 하나로 통일하고 인터셉터에서 헤더를 강제 주입
- 인증 실패는 401로 명확하게 반환(프론트가 “서버 문제”로 오해하지 않게 처리)

---

### (3) 출석 연속일 계산이 꼬임 (Timestamp 비교 문제)

**현상**
- 연속 출석이 자꾸 1로 초기화되거나, 하루 건너뛰었는데도 유지되는 듯한 오동작

**문제 지점**
- `AttendanceService.checkAttendance()`의 날짜 비교

**원인**
- DB는 `timestamp`인데 시간까지 포함해서 비교하면 자정 근처에서 오차가 발생 가능
- “어제/오늘” 판단은 날짜 단위로 해야 안정적

**확인**
- 마지막 출석 시간이 `23:xx` / `00:xx` 처럼 애매한 케이스에서 재현
- 디버깅으로 `lastDate`가 날짜가 아니라 시간 포함으로 흔들리는지 확인

**수정**
- `LocalDate`로 변환해서 날짜만 비교
```java
LocalDate lastDate = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();
```

---

### (4) 퀘스트 로그가 중복 생성됨 (MERGE 조건의 날짜 포맷 불일치)

**현상**
- 같은 날 같은 type인데 row가 2개 이상 생성
- 퀘스트 진행도가 비정상적으로 빨리 오름

**문제 지점**
- `daily-quest-mapper.xml`의 MERGE ON 조건

**원인**
- 서버에서 date 파라미터를 `YYYY-MM-DD`로 넘기고,
- MERGE는 `TO_CHAR(date,'YYYYMMDD') = #{date}`로 비교 → 조건 불일치

**확인**
- 동일 사용자/동일 type으로 insert가 반복되는 로그 확인
- DB에서 당일 데이터를 조회했을 때 같은 type이 여러 줄 존재

**수정**
- 서버에서 날짜 포맷을 `YYYYMMDD`로 통일하여 전달
- MERGE ON 조건(멤버/타입/날짜)이 모두 들어가는지 재점검

---

### (5) 장착 아이템이 여러 개 Y로 남음 (선 해제 누락)

**현상**
- DECO_BG 같은 타입 아이템이 여러 개 `equipped='Y'`로 남아 화면이 깨짐

**문제 지점**
- `PointService.useItem()` 장착 분기
- `inventory-mapper.xml`의 `unequipByType`

**원인**
- 장착 전에 같은 타입을 모두 N으로 내려주는 쿼리가 누락되면 “Y가 누적”됨

**확인**
- DB에서 해당 사용자 INVENTORY를 조회하면 같은 타입의 여러 row가 Y로 존재

**수정**
- 장착 전에 타입별 선 해제 후, 선택 아이템만 Y로 변경
```java
unequipByType(loginId, type);
inven.setInventoryEquipped("Y");
inventoryDao.update(inven);
```

---

## 7. 정리

- 포인트는 단순 수치가 아니라 **재화**로 다뤘고,
- 증감 로직을 한 군데로 모아서 **잔액/이력의 정합성**을 유지했습니다.
- 사용자는 출석/퀘스트/룰렛으로 포인트를 벌고, 상점/인벤으로 소비하는 구조이며,
- 운영자는 관리자 컨트롤러로 포인트/퀴즈/상점/자산을 관리할 수 있게 구성했습니다.
