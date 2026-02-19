# Backend (Spring Boot + MyBatis + Oracle)

## 0. 개요

본 프로젝트 백엔드는 **포인트 기반 보상 시스템**을 중심으로 동작합니다.
사용자의 활동(출석/퀘스트/룰렛 등)을 포인트로 환산하고, 포인트로 상점 아이템을 구매/선물/환불하며, 인벤토리(소모/장착) 및 랭킹/이력/관리자 운영 기능까지 포함합니다.

- 인증: JWT 기반 (요청 시 `@RequestAttribute loginId`로 인증 사용자 식별)
- DB: Oracle
- ORM: MyBatis (SQL 명시형)
- 무결성 핵심: **포인트 증감은 반드시 이력(POINT_HISTORY)과 함께 처리**

---

## 1. 핵심 설계 원칙 (무결성 의도)

### 1) 포인트 무결성

- 포인트 증감은 `PointService.addPoint()` 단일 경로로 통일
- 잔액 부족 시 차감 불가(음수 방지)
- 포인트 변경 성공 시 `POINT_HISTORY` 무조건 INSERT

**핵심 코드 (PointService.addPoint)**

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

**핵심 쿼리 (POINT_HISTORY insert / MyBatis: point-history-mapper.xml)**

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

# 2. 핵심 기능 16개 (코드 + 쿼리 + 처리설명)

> 기능은 “사용자 기능 12개 + 관리자 기능 4개”로 구성했습니다.
> 각 기능마다 **Endpoint / 처리 흐름 / 핵심 코드 / 핵심 SQL**을 같이 적었습니다.

---

## [사용자] 01) 출석체크 (중복 방지 + 연속 출석 + 포인트 지급)

### Endpoint

- `GET  /point/main/attendance/status`
- `POST /point/main/attendance/check`
- `GET  /point/main/attendance/calendar`

### 처리 흐름

1. 출석 현황(AttendanceStatus) 조회
2. 오늘 이미 출석했으면 예외(중복 방지)
3. 어제 출석이면 연속출석 +1, 아니면 1로 초기화
4. 현황 업데이트 + 출석 히스토리 INSERT
5. 포인트 지급(기본 100, 7일마다 +50)

### 핵심 코드 (AttendanceService.checkAttendance)

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

### 핵심 쿼리 (attendance-status-mapper.xml)

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

## [사용자] 02) 일일 퀘스트 목록 조회 (진행도/보상여부 포함)

### Endpoint

- `GET /point/quest/list`

### 처리 흐름

- 오늘 날짜 기준으로 `POINT_GET_QUEST_LOG`를 조회하여
  각 퀘스트의 (현재 count / rewardYn) 값을 반환에 포함합니다.

### 핵심 쿼리 (daily-quest-mapper.xml)

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

## [사용자] 03) 퀘스트 진행도 증가 (UPSERT: 없으면 생성, 있으면 +1)

### Endpoint

- 퀘스트는 여러 기능에서 진행도가 올라가며(퀴즈/룰렛 등), 공통으로 `questProgress()`를 사용합니다.

### 핵심 코드 (DailyQuestService.questProgress)

```java
public void questProgress(String memberId, String type) {
    boolean isValid = questProps.getList().stream().anyMatch(q -> q.getType().equals(type));
    if(isValid) {
        questDao.upsertQuestLog(memberId, type, getTodayStr());
    }
}
```

### 핵심 쿼리 (daily-quest-mapper.xml: MERGE)

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

## [사용자] 04) 퀘스트 보상 수령 (중복 수령 방지 + 포인트 지급)

### Endpoint

- `POST /point/quest/claim`

### 처리 흐름

1. 오늘 로그 조회 → 내 type 로그 찾기
2. 목표치 미달이면 실패
3. rewardYn이 Y면 실패(중복수령 방지)
4. rewardYn을 Y로 업데이트 성공 시 포인트 지급

### 핵심 코드 (DailyQuestService.claimReward)

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

### 핵심 쿼리 (daily-quest-mapper.xml)

```sql
UPDATE POINT_GET_QUEST_LOG
SET POINT_GET_QUEST_REWARD_YN = 'Y'
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND POINT_GET_QUEST_TYPE = #{type}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
  AND POINT_GET_QUEST_REWARD_YN = 'N'
```

---

## [사용자] 05) 데일리 퀴즈 랜덤 출제 (1일 1회 제한)

### Endpoint

- `GET /point/quest/quiz/random`

### 핵심 코드 (DailyQuestService.getRandomQuiz)

```java
public DailyQuizVO getRandomQuiz(String memberId) {
    List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
    boolean alreadySolved = logs.stream().anyMatch(m -> "QUIZ".equals(m.get("type")));

    if (alreadySolved) return null;
    return quizDao.getRandomQuiz();
}
```

### 핵심 쿼리 (daily-quiz-mapper.xml)

```sql
SELECT * FROM (
    SELECT QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER
    FROM DAILY_QUIZ
    ORDER BY DBMS_RANDOM.VALUE
)
WHERE ROWNUM = 1
```

---

## [사용자] 06) 데일리 퀴즈 정답 검증 + 퀘스트 진행도 반영

### Endpoint

- `POST /point/quest/quiz/check`

### 핵심 코드 (DailyQuestService.checkQuizAndProgress)

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

### 핵심 쿼리 (daily-quiz-mapper.xml)

```sql
SELECT QUIZ_ANSWER
FROM DAILY_QUIZ
WHERE QUIZ_NO = #{quizNo}
```

---

## [사용자] 07) 룰렛 게임 (티켓 소모 + 랜덤 보상 + 퀘스트 진행)

### Endpoint

- `POST /point/main/store/roulette`

### 처리 흐름

1. 내 인벤토리 조회
2. 아이템 상세 조회하면서 `RANDOM_ROULETTE` 타입 티켓 찾기
3. 랜덤 인덱스 생성 → 일부 인덱스는 포인트 보상
4. 티켓 수량 감소(1개 남았으면 delete)
5. 당첨이면 포인트 지급
6. 퀘스트 진행도(ROULETTE) 반영

### 핵심 코드 (PointService.playRoulette + decreaseInventoryOrDelete)

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

### 핵심 쿼리 (inventory-mapper.xml)

```sql
-- 인벤토리 수량/장착 수정
UPDATE INVENTORY
SET
    inventory_quantity = #{inventoryQuantity},
    inventory_equipped = #{inventoryEquipped}
WHERE
    inventory_no = #{inventoryNo};

-- 인벤토리 삭제(수량 1개일 때)
DELETE FROM INVENTORY
WHERE inventory_no = #{inventoryNo};
```

---

## [사용자] 08) 포인트 상점 목록 조회 (검색/필터/페이징)

### Endpoint

- `GET /point/main/store`

### 핵심 쿼리 (point-item-store-mapper.xml)

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

## [사용자] 09) 상품 구매 (포인트 차감 + 재고 감소 + 인벤 지급 + 찜 정리)

### Endpoint

- `POST /point/main/store/buy`

### 처리 흐름

1. 상품 조회
2. 꾸미기(DECO_) 아이템은 “중복 보유” 구매 차단
3. 재고 확인
4. 포인트 차감 (이력 저장 포함)
5. 재고 -1 업데이트
6. 인벤토리 지급(이미 있으면 수량 +1)
7. 찜 되어 있으면 삭제

### 핵심 코드 (PointService.purchaseItem + giveItemToInventory)

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

private void giveItemToInventory(String loginId, long itemNo) {
    InventoryDto existing = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
    if (existing != null) {
        existing.setInventoryQuantity(existing.getInventoryQuantity() + 1);
        inventoryDao.update(existing);
    } else {
        inventoryDao.insert(InventoryDto.builder()
            .inventoryMemberId(loginId)
            .inventoryItemNo(itemNo)
            .inventoryQuantity(1)
            .inventoryEquipped("N")
            .build());
    }
}
```

### 핵심 쿼리

**(1) 인벤토리 보유 여부 체크 / inventory-mapper.xml**

```sql
SELECT
    inventory_no as inventoryNo,
    inventory_member_id as inventoryMemberId,
    inventory_item_no as inventoryItemNo,
    inventory_quantity as inventoryQuantity,
    inventory_equipped as inventoryEquipped
FROM INVENTORY
WHERE inventory_member_id = #{inventoryMemberId}
  AND inventory_item_no = #{inventoryItemNo}
```

**(2) 인벤토리 insert / inventory-mapper.xml**

```sql
INSERT INTO INVENTORY(
    inventory_no,
    inventory_member_id,
    inventory_item_no,
    inventory_equipped
)
VALUES(
    seq_inventory.nextval,
    #{inventoryMemberId},
    #{inventoryItemNo},
    'N'
)
```

**(3) 상점 아이템 update (재고 포함) / point-item-store-mapper.xml**

```sql
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
WHERE point_item_no = #{pointItemNo}
```

---

## [사용자] 10) 선물하기 (보낸사람 차감 + 재고 감소 + 상대 인벤 지급 + 찜 정리)

### Endpoint

- `POST /point/main/store/gift`

### 핵심 코드 (PointService.giftItem)

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

### 핵심 쿼리

- 재고 update: 위 09와 동일
- 인벤 지급/수량 증가: 위 09와 동일
- 포인트 차감/이력 insert: 위 0의 addPoint/point_history insert 동일

---

## [사용자] 11) 찜(위시리스트) 토글 + 내 찜 목록 조회

### Endpoint

- `POST /point/main/store/wish/toggle`
- `GET  /point/main/store/wish/my`
- `GET  /point/main/store/wish/check`

### 핵심 코드 (PointService.toggleWish)

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

### 핵심 쿼리 (point-wishlist-mapper.xml)

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

**내 찜 목록 조회 JOIN (point-wishlist-mapper.xml)**

```sql
SELECT
    T1.POINT_WISHLIST_NO         AS pointWishlistNo,
    T1.POINT_WISHLIST_MEMBER_ID  AS pointWishlistMemberId,
    T1.POINT_WISHLIST_ITEM_NO    AS pointWishlistItemNo,
    T1.POINT_WISHLIST_TIME       AS pointWishlistTime,
    T2.POINT_ITEM_NAME           AS pointItemName,
    T2.POINT_ITEM_SRC            AS pointItemSrc,
    T2.POINT_ITEM_PRICE          AS pointItemPrice
FROM POINT_WISHLIST T1
INNER JOIN POINT_ITEM_STORE T2
ON T1.POINT_WISHLIST_ITEM_NO = T2.POINT_ITEM_NO
WHERE T1.POINT_WISHLIST_MEMBER_ID = #{memberId}
ORDER BY T1.POINT_WISHLIST_TIME DESC
```

---

## [사용자] 12) 인벤토리 조회 (상점 정보 JOIN)

### Endpoint

- `GET /point/main/store/inventory/my`

### 핵심 코드 (PointStoreRestController.myInventory)

```java
@GetMapping("/inventory/my")
public List<InventoryDto> myInventory(@RequestAttribute(required = false) String loginId) {
    if(loginId == null) return List.of();
    return inventoryDao.selectListByMemberId(loginId);
}
```

### 핵심 쿼리 (inventory-mapper.xml) ✅

> 아래 쿼리는 **트러블슈팅에서 수정 포인트(#{loginId} → #{value})**가 있습니다.

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
WHERE I.INVENTORY_MEMBER_ID = #{loginId}   -- ⚠️ 트러블슈팅 포인트
ORDER BY I.INVENTORY_CREATED_AT DESC
```

---

## [사용자] 13) 아이템 사용 (소모/랜덤포인트/장착형 분기)

### Endpoint

- `POST /point/main/store/inventory/use`

### 처리 흐름

- 인벤 번호로 내 아이템인지 검증
- 아이템 타입별 분기
  - 닉네임 변경(CHANGE_NICK)
  - 하트 충전(HEART_RECHARGE)
  - 랜덤 포인트 지급(RANDOM_POINT)
  - 꾸미기 장착(DECO_*)
  - 상품권(VOUCHER)

### 핵심 코드 (PointService.useItem)

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

### 핵심 쿼리 (inventory-mapper.xml: unequipByType)

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

## [사용자] 14) 장착 해제 (단일 아이템 unequip)

### Endpoint

- `POST /point/main/store/inventory/unequip`

### 핵심 코드 (PointService.unequipItem)

```java
public void unequipItem(String loginId, long inventoryNo) {
    InventoryDto inv = inventoryDao.selectOne(inventoryNo);
    if(inv != null && loginId.equals(inv.getInventoryMemberId())) {
        inv.setInventoryEquipped("N");
        inventoryDao.update(inv);
    }
}
```

### 핵심 쿼리 (inventory-mapper.xml: update)

```sql
UPDATE INVENTORY
SET
    inventory_quantity = #{inventoryQuantity},
    inventory_equipped = #{inventoryEquipped}
WHERE
    inventory_no = #{inventoryNo}
```

---

## [사용자] 15) 환불 (포인트 반환 + 재고 복구 + 인벤 차감)

### Endpoint

- `POST /point/main/store/cancel`

### 핵심 코드 (PointService.cancelItem)

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

## [사용자] 16) 포인트 이력 조회 (페이징 + 타입 필터)

### Endpoint

- `GET /point/history?page=1&type=all|earn|use|item`

### 핵심 코드 (PointService.getHistoryList)

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

### 핵심 쿼리 (point-history-mapper.xml)

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


---

# 5. 트러블슈팅 (실제 겪은 문제 중심 정리)

> 단순 오류 나열이 아니라,  
> “왜 문제가 발생했는지 → 어떻게 확인했는지 → 어떻게 수정했는지 → 다시 발생하지 않도록 무엇을 고려했는지” 기준으로 정리했습니다.


## 1) 인벤토리 조회 0건 문제

### ① 발생 상황
- `/inventory/my` 호출 시 항상 빈 배열 반환
- DB에는 실제 데이터 존재

### ② 원인 분석
- MyBatis 단일 파라미터 바인딩 불일치
- DAO는 String 하나만 전달 → 기본 파라미터명은 `value`
- mapper에서는 `#{loginId}` 사용 → null 바인딩

### ③ 수정 전
```sql
WHERE I.INVENTORY_MEMBER_ID = #{loginId}
```

### ④ 수정 후
```sql
WHERE I.INVENTORY_MEMBER_ID = #{value}
```

### ⑤ 재발 방지
- 단일 파라미터 사용 시 `@Param` 명시 습관화
- SQL 로그 항상 확인

---
## 인벤토리 조회 0건 문제

### 원인
MyBatis 단일 파라미터 바인딩 불일치

### 해결
DAO가 문자열 1개만 넘길 때 mapper에서는 `#{value}`로 받아야 합니다.

**수정 전**
```sql
WHERE I.INVENTORY_MEMBER_ID = #{loginId}
```

**수정 후**
```sql
WHERE I.INVENTORY_MEMBER_ID = #{value}
```

---


### 트러블슈팅 모음 (확장) (실제 운영/개발에서 자주 터지는 것들)

> 아래는 “왜 이런 문제가 났고, 어떻게 확인했고, 어떻게 고쳤는지”를 남기기 위한 섹션입니다.  
> 면접에서 가장 질문이 많이 나오는 파트라서 일부러 상세하게 적었습니다.

#### 1) 인벤토리 조회가 항상 0건 (MyBatis 단일 파라미터 바인딩)

**증상**
- `/inventory/my` 호출은 성공(200)인데 응답이 항상 `[]`
- DB에는 INVENTORY 데이터가 존재

**원인**
- DAO에서 문자열 1개만 전달하는 경우, MyBatis 기본 파라미터명이 `value`로 잡힘
- mapper에서 `#{loginId}` / `#{memberId}`로 받으면 null 바인딩 → 조건이 항상 false

**확인 방법**
- mapper SQL 바인딩 로그 확인(파라미터가 null로 찍힘)
- SQL을 직접 실행하면 결과가 나오는데, API만 0건

**해결**
- 단일 String 파라미터는 `#{value}`로 통일하거나 `@Param("loginId")`로 명시

---

#### 2) 로그인은 했는데 loginId가 null (RequestAttribute 누락)

**증상**
- 특정 API에서 `@RequestAttribute(required=false) String loginId`가 null
- 프론트는 토큰을 보냈다고 생각하는데 서버에서는 인증이 안 된 것처럼 동작

**원인 후보**
- 필터/인터셉터에서 `request.setAttribute("loginId", ...)` 누락
- Authorization 헤더 형식 문제(`Bearer` 누락 등)
- axios 인터셉터 적용이 일부 요청에서 빠짐

**확인 방법**
- 필터에서 Authorization 헤더 수신 여부 로그
- 토큰 파싱 성공/실패 로그
- 특정 라우트만 빠지는지 비교

**해결**
- axios instance 단일화 + 인터셉터에서 Authorization 강제 주입
- 인증 실패는 401로 명확히 내려 프론트가 오해하지 않게 처리

---

#### 3) 출석 연속일 계산이 꼬임 (LocalDate vs Timestamp)

**증상**
- 연속 출석이 자꾸 1로 초기화되거나, 하루 건너뛰었는데도 유지됨

**원인**
- DB는 timestamp인데, 서버에서 시간 포함 비교를 하면 오차 발생
- 자정 근처 요청/타임존 차이로 어제가 오늘로 인식되는 케이스

**해결**
- `LocalDate`로 변환해서 “날짜”만 비교
- 필요 시 DB 조회도 `TRUNC(timestamp)` 기준으로 판단

---

#### 4) 퀘스트 로그가 중복 생성됨 (MERGE ON 조건 실수)

**증상**
- 같은 날 같은 type인데 row가 2개 이상 생김
- 퀘스트 진행도가 비정상적으로 빨리 오름

**원인**
- MERGE ON 조건에서 날짜 포맷이 불일치
  - 예: `TO_CHAR(date,'YYYYMMDD') = #{date}`인데 `#{date}`가 `YYYY-MM-DD`로 들어옴

**해결**
- 서버에서 날짜 포맷을 `YYYYMMDD`로 통일
- ON 조건에 member/type/date 3개가 모두 들어가는지 재점검

---

#### 5) 포인트가 음수로 떨어질 뻔한 케이스 (동시성)

**증상**
- 짧은 시간에 구매 요청이 연속으로 들어오면 잔액 검증을 통과하는 것처럼 보임

**원인**
- “잔액 조회 → 검증 → 업데이트” 사이에 동시 요청이 끼면 레이스가 가능

**현재 방어**
- 서비스 레벨에서 음수 방지 검증(1차 방어)

**개선 방향(확장 포인트)**
- `SELECT ... FOR UPDATE` 행 잠금
- 낙관적 락(version 컬럼)
- DB CHECK 제약(가능한 구조면)

---

#### 6) 상점 재고가 -1까지 내려갈 수 있는 위험

**증상**
- 동시에 여러 사용자가 구매하면 재고가 마이너스가 될 수 있음

**해결 방향**
- 조건부 업데이트로 원자성 확보  
  `UPDATE ... SET stock = stock - 1 WHERE item_no = ? AND stock > 0`
- 영향 row count가 0이면 품절 처리

---

#### 7) 장착 아이템이 여러 개 Y가 됨 (선 해제 누락)

**증상**
- 같은 타입(DECO_BG 등)이 여러 개 Y로 남아 UI가 이상해짐

**해결**
- 장착 전에 타입별 선 해제 쿼리 실행 후 선택 아이템만 Y

---

#### 8) Oracle 페이징에서 정렬이 깨짐 (ROWNUM 평가 순서)

**증상**
- 1페이지는 최신인데 2페이지부터 순서가 이상함

**원인**
- ROWNUM이 ORDER BY보다 먼저 평가되면 정렬이 깨짐

**해결**
- ORDER BY를 내부 쿼리에 두고 바깥에서 ROWNUM으로 자르는 2중 감싸기

---

#### 9) DBMS_RANDOM 랜덤 퀴즈가 편향되는 느낌

**증상**
- 랜덤인데 특정 문제가 자주 나오는 것처럼 보임

**원인**
- 데이터 수가 적으면 체감 편향이 커짐

**해결**
- 현재 방식 유지(단순/명확)
- 데이터가 커지면 키 범위 랜덤 샘플링 등 고려

---

#### 10) FK 제약으로 삭제가 안 됨 (회원 탈퇴/연쇄 삭제)

**증상**
- 회원 삭제 시 토큰/이력/인벤 등이 남아 FK 에러

**해결**
- 핵심 테이블에 `ON DELETE CASCADE` 적용 또는
- 서비스에서 삭제 순서 명확히 관리

---

#### 11) CORS/프록시 문제로 브라우저에서만 실패

**증상**
- Postman은 되는데 브라우저에서만 막힘(OPTIONS preflight)

**해결**
- CORS 설정에 Authorization 헤더 허용
- 프록시(/api)와 백엔드 매핑 prefix 일치

---

#### 12) “내 정보” 아이콘/프레임이 안 나옴 (NULL 처리)

**증상**
- 장착 전에는 UI가 깨짐(src null)

**해결**
- 서버에서 기본 아이콘/스타일 fallback 제공
- 프론트에서도 null 대비 fallback 처리

---

# 6. 마무리

이 백엔드는 “포인트”를 중심으로 **출석/퀘스트/상점/인벤/랭킹/이력/운영**까지 흐름이 한 번에 연결되는 구조입니다.
특히 포인트 증감과 이력을 강제로 묶어서 관리해서, 운영/정산/분석 관점에서도 확장 가능한 형태로 구성했습니다.


---

# 🔎 추가 심화 설계 섹션

## 17. 포인트 재화 시스템 설계 의도

이 프로젝트에서 포인트는 단순 숫자가 아니라 재화로 취급했습니다.

원칙:
1. 포인트 변경은 단일 진입점(addPoint)
2. 음수 불가
3. 모든 증감은 history 기록
4. 구매/환불/선물은 트랜잭션 묶음

이를 통해 정산 가능성과 디버깅 가능성을 확보했습니다.

---

## 18. 트랜잭션 경계 설계

구매 흐름:
1. 상품 조회
2. 재고 확인
3. 포인트 차감
4. 재고 감소
5. 인벤토리 반영

이 모든 단계는 서비스 레벨 @Transactional로 묶여 원자성을 보장합니다.

---

## 19. 동시성 고려

현재는 서비스 레벨 음수 검증으로 1차 방어합니다.

향후 개선 가능성:
- SELECT FOR UPDATE
- 낙관적 락(version)
- DB CHECK 제약 조건

---

## 20. MyBatis 선택 이유

- MERGE 사용
- ROWNUM 페이징
- 복잡한 JOIN
- 동적 조건 검색

SQL을 명시적으로 제어하는 것이 적합하다고 판단했습니다.

---

## 21. 데이터 정합성 전략

### 장착 단일화
같은 타입 아이템은 선 해제 후 장착.

### 퀘스트 중복 방지
reward_yn 컬럼으로 DB 레벨 차단.

### 환불 순서
1. 포인트 반환
2. 재고 복구
3. 인벤 차감

트랜잭션으로 일관성 유지.

---

## 22. 확장 가능성

- 이벤트 시스템
- 포인트 배율 정책
- 관리자 통계 대시보드
- 시즌 보상 구조
