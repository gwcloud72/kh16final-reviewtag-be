# 포인트 생태계 백엔드 상세 정리

> 이 문서는 팀 프로젝트 전체 설명이 아니라, 프로젝트에서 **제가 전담한 포인트 생태계 영역**을 정리한 문서입니다.  
> 사용자의 참여를 포인트로 보상하고, 그 포인트를 다시 소비와 커스터마이징으로 연결하는 흐름을 백엔드 기준으로 정리했습니다.

---

## 목차
1. 담당 범위와 구현 목표
2. 기술 스택과 인증 흐름
3. 설계하면서 중요하게 본 점
4. 사용자 기능 구현
5. 관리자 기능 구현
6. 트러블슈팅
7. 마무리

---

## 1. 담당 범위와 구현 목표

팀 프로젝트에서 저는 **포인트 보상·소비 시스템 전반**을 맡았습니다.

구체적으로는 아래 영역을 담당했습니다.

- 출석체크와 연속 출석 보상
- 일일 퀘스트, 데일리 퀴즈, 룰렛
- 포인트 적립 / 차감 / 이력 관리
- 포인트 상점, 구매, 선물, 환불
- 위시리스트와 인벤토리
- 장착형 아이템 처리
- 관리자용 포인트 / 퀴즈 / 상점 / 자산 관리 기능

이 영역을 맡으면서 가장 중요하게 본 점은 두 가지였습니다.

첫째, **사용자가 다시 들어올 이유가 생기는 흐름**을 만드는 것이었습니다.  
단순히 포인트를 주는 데서 끝나는 것이 아니라, 출석과 퀘스트로 적립하고, 상점과 인벤토리에서 소비하며, 프로필에 변화가 보이도록 연결했습니다.

둘째, **포인트를 실제 재화처럼 다루는 것**이었습니다.  
포인트가 잘못 차감되거나 이력이 남지 않으면 서비스 신뢰도가 바로 떨어지기 때문에, 적립·차감·이력 기록을 한 흐름으로 묶어 정합성을 유지했습니다.

---

## 2. 기술 스택과 인증 흐름

### 사용 기술 및 협업 도구
- ⚙️ **Backend**: Spring Boot
- 📦 **Data Access**: MyBatis
- 🗄️ **Database**: Oracle
- 🔐 **Authentication**: JWT
- 🧱 **Architecture**: MVC Pattern
- 🤝 **Collaboration**: Git, GitHub

### 인증 처리 방식
클라이언트는 `Authorization: Bearer <token>` 헤더로 요청하고,  
서버는 필터 또는 인터셉터에서 토큰을 확인한 뒤 인증에 성공하면 `loginId`를 `RequestAttribute`에 담아 넘깁니다.

이 방식으로 구성한 이유는 컨트롤러와 서비스가 토큰 파싱 로직을 직접 다루지 않고,  
이미 인증된 사용자 기준으로 비즈니스 로직에만 집중할 수 있도록 하기 위해서였습니다.

흐름은 아래와 같습니다.

1. 클라이언트가 JWT를 담아 요청
2. 필터/인터셉터가 토큰 유효성 검사
3. 유효한 경우 `loginId`를 request attribute에 저장
4. 컨트롤러에서 `@RequestAttribute loginId`로 사용자 식별

---

## 3. 설계하면서 중요하게 본 점

### 3-1) 포인트는 한 곳에서만 증감되도록 처리
포인트는 출석, 퀘스트, 구매, 환불, 관리자 지급처럼 여러 기능에서 공통으로 사용됩니다.  
그래서 기능별로 각각 포인트를 직접 수정하게 두지 않고, `PointService.addPoint()`를 **단일 진입점**으로 두었습니다.

이 메서드에서 처리한 내용은 다음과 같습니다.

- 회원 존재 여부 확인
- 차감 시 잔액 부족 여부 검사
- 포인트 증감 수행
- 성공 시 `POINT_HISTORY`에 이력 기록

### 핵심 코드: `PointService.addPoint()`

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

### 핵심 SQL: `POINT_HISTORY` 기록

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

### 3-2) 날짜 기준 기능은 시간값과 분리해서 처리
출석과 일일 퀘스트는 "오늘인지", "어제의 연속인지"가 중요합니다.  
DB에는 `timestamp`가 저장되기 때문에, 시간까지 그대로 비교하면 자정 근처에서 오차가 생길 수 있습니다.

그래서 출석은 `LocalDate`로 변환해 날짜만 비교했고,  
퀘스트는 `YYYYMMDD` 형식으로 통일해서 같은 날의 중복 로그가 생기지 않도록 맞췄습니다.

### 3-3) 장착형 아이템은 같은 타입이 동시에 여러 개 장착되지 않도록 처리
닉네임 꾸미기, 배경, 아이콘, 프레임 같은 장착형 아이템은 타입별로 하나만 활성화되어야 합니다.  
그래서 새 아이템을 장착하기 전에 같은 타입 아이템을 전부 해제한 뒤, 선택한 아이템만 `Y`로 바꾸는 방식으로 처리했습니다.

---

## 4. 사용자 기능 구현

아래 내용은 실제 사용자가 경험하는 흐름 기준으로 정리했습니다.

---

### 01) 출석체크

**Endpoint**
- `GET  /point/main/attendance/status`
- `POST /point/main/attendance/check`
- `GET  /point/main/attendance/calendar`

출석체크는 단순한 버튼 기능이 아니라, 재방문을 만들기 위한 가장 기본적인 장치였습니다.  
오늘 이미 출석했는지 확인하고, 어제 출석 여부에 따라 연속 출석 일수를 계산한 뒤, 결과를 저장하고 포인트를 지급하도록 구현했습니다.

### 핵심 코드: `AttendanceService.checkAttendance()`

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

### 핵심 SQL

```sql
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

update attendance_status set
    attendance_status_current = #{attendanceStatusCurrent},
    attendance_status_max = #{attendanceStatusMax},
    attendance_status_total = attendance_status_total + 1,
    attendance_status_lastdate = systimestamp
where attendance_status_member_id = #{attendanceStatusMemberId};

insert into attendance_history(
    attendance_history_no,
    attendance_history_member_id
) values (
    seq_attendance_history.nextval,
    #{attendanceHistoryMemberId}
);
```

---

### 02) 일일 퀘스트 목록 조회

**Endpoint**
- `GET /point/quest/list`

사용자가 오늘 어떤 퀘스트를 얼마나 진행했는지, 보상을 받았는지 한 번에 볼 수 있도록 조회 기능을 만들었습니다.

### 핵심 SQL

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

### 03) 퀘스트 진행도 증가

여러 기능에서 공통으로 퀘스트 진행도를 올릴 수 있어야 했기 때문에, 퀴즈 정답 처리나 룰렛 실행 같은 흐름에서 `questProgress()`를 재사용하도록 구성했습니다.

**핵심 포인트**
- 오늘 같은 타입 로그가 있으면 count 증가
- 없으면 새로 생성
- Oracle `MERGE`를 사용해 UPSERT 형태로 구현

### 핵심 코드: `DailyQuestService.questProgress()`

```java
public void questProgress(String memberId, String type) {
    boolean isValid = questProps.getList().stream().anyMatch(q -> q.getType().equals(type));
    if(isValid) {
        questDao.upsertQuestLog(memberId, type, getTodayStr());
    }
}
```

### 핵심 SQL

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

### 04) 퀘스트 보상 수령

**Endpoint**
- `POST /point/quest/claim`

보상 수령은 목표 달성 여부와 중복 수령 여부를 함께 체크해야 했습니다.  
그래서 당일 로그를 읽어 현재 count를 확인하고, 아직 보상을 받지 않은 경우에만 상태를 `Y`로 바꾼 뒤 포인트를 지급했습니다.

### 핵심 코드: `DailyQuestService.claimReward()`

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

### 핵심 SQL

```sql
UPDATE POINT_GET_QUEST_LOG
SET POINT_GET_QUEST_REWARD_YN = 'Y'
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND POINT_GET_QUEST_TYPE = #{type}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
  AND POINT_GET_QUEST_REWARD_YN = 'N'
```

---

### 05) 데일리 퀴즈 랜덤 출제

**Endpoint**
- `GET /point/quest/quiz/random`

퀴즈는 1일 1회만 풀 수 있도록 했습니다.  
오늘 이미 퀴즈 관련 로그가 있으면 더 이상 내려주지 않고, 아니라면 랜덤으로 한 문제를 출제합니다.

### 핵심 코드

```java
public DailyQuizVO getRandomQuiz(String memberId) {
    List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
    boolean alreadySolved = logs.stream().anyMatch(m -> "QUIZ".equals(m.get("type")));

    if (alreadySolved) return null;
    return quizDao.getRandomQuiz();
}
```

### 핵심 SQL

```sql
SELECT * FROM (
    SELECT QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER
    FROM DAILY_QUIZ
    ORDER BY DBMS_RANDOM.VALUE
)
WHERE ROWNUM = 1
```

---

### 06) 데일리 퀴즈 정답 검증

**Endpoint**
- `POST /point/quest/quiz/check`

사용자 입력은 공백이나 대소문자 차이가 있을 수 있어 그대로 비교하지 않았습니다.  
문자열을 정리한 뒤 정답 여부를 확인하고, 정답이면 퀘스트 진행도를 올리도록 연결했습니다.

### 핵심 코드

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

---

### 07) 룰렛

**Endpoint**
- `POST /point/main/store/roulette`

룰렛은 티켓이 있어야만 실행되도록 했고, 사용 후에는 인벤토리 수량을 줄이거나 삭제하도록 처리했습니다.  
결과에 따라 포인트를 지급하고, 동시에 퀘스트 진행도도 함께 반영했습니다.

### 핵심 코드

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

---

### 08) 포인트 상점 목록 조회

**Endpoint**
- `GET /point/main/store`

상점 목록은 아이템 타입 필터, 키워드 검색, 페이징을 함께 지원하도록 만들었습니다.  
사용자 입장에서 상품이 많아졌을 때도 원하는 아이템을 빠르게 찾을 수 있게 하려는 목적이었습니다.

### 핵심 SQL

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

### 09) 상품 구매

**Endpoint**
- `POST /point/main/store/buy`

구매 기능에서는 아래 순서를 신경 써서 처리했습니다.

1. 상품 존재 여부 확인
2. 꾸미기 아이템의 중복 보유 여부 확인
3. 재고 확인
4. 포인트 차감
5. 재고 감소
6. 인벤토리 지급 또는 기능성 아이템 처리
7. 찜 목록 정리

### 핵심 코드

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

---

### 10) 선물하기

**Endpoint**
- `POST /point/main/store/gift`

선물 기능은 구매와 비슷하지만, 인벤토리 지급 대상이 본인이 아니라 다른 사용자라는 점이 다릅니다.  
보내는 사람의 포인트를 차감하고 재고를 줄인 뒤, 상대 인벤토리에 아이템을 지급하도록 구현했습니다.

### 핵심 코드

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

### 11) 위시리스트

**Endpoint**
- `POST /point/main/store/wish/toggle`
- `GET  /point/main/store/wish/my`
- `GET  /point/main/store/wish/check`

위시리스트는 토글 방식으로 구현했습니다.  
이미 찜한 상품이면 삭제하고, 아니면 새로 추가하는 방식이라 프론트에서도 다루기 편했습니다.

### 핵심 코드

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

---

### 12) 인벤토리 조회

**Endpoint**
- `GET /point/main/store/inventory/my`

인벤토리는 단순히 보유 수량만 보여주는 것이 아니라, 상점 아이템 정보까지 함께 내려줄 필요가 있었습니다.  
그래서 `INVENTORY`와 `POINT_ITEM_STORE`를 JOIN해 이름, 이미지, 타입, 설명까지 같이 조회하도록 만들었습니다.

### 핵심 SQL

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

※ 여기서 `#{value}`를 쓴 이유는, DAO에서 String 단일 파라미터를 넘길 때 MyBatis 기본 바인딩 이름이 `value`였기 때문입니다.

---

### 13) 아이템 사용

**Endpoint**
- `POST /point/main/store/inventory/use`

아이템은 타입에 따라 동작이 달라서, 인벤토리 사용 로직을 분기 처리했습니다.

- 닉네임 변경권: 닉네임 수정 후 수량 차감
- 랜덤 포인트 박스: 난수 기반 포인트 지급 후 차감
- 꾸미기 아이템: 같은 타입 장착 해제 후 현재 아이템 장착
- 상품권: 포인트 환급 후 차감

### 핵심 코드

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

### 핵심 SQL: 타입별 선 해제

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

### 14) 장착 해제

**Endpoint**
- `POST /point/main/store/inventory/unequip`

이미 장착한 아이템을 다시 해제할 수 있도록 별도 API를 두었습니다.

### 핵심 코드

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

### 15) 환불

**Endpoint**
- `POST /point/main/store/cancel`

환불은 포인트를 되돌려주고, 상점 재고를 복구하고, 인벤토리 수량을 줄이는 과정이 모두 함께 이루어져야 했습니다.

### 핵심 코드

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

### 16) 포인트 이력 조회

**Endpoint**
- `GET /point/history?page=1&type=all|earn|use|item`

이력은 사용자가 "내 포인트가 왜 늘었고 왜 줄었는지" 확인할 수 있는 근거이기 때문에 중요했습니다.  
전체 / 적립 / 사용 / 기타 타입 기준으로 필터링할 수 있게 했고, 페이지 단위로 조회하도록 구성했습니다.

### 핵심 코드

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

### 핵심 SQL

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

## 5. 관리자 기능 구현

포인트 생태계는 사용자 기능만 있어서는 완성되지 않는다고 생각했습니다.  
운영자가 직접 포인트와 아이템, 퀴즈, 자산을 관리할 수 있어야 실제 서비스 운영이 가능하기 때문에 관리자 기능도 함께 정리했습니다.

---

### 01) 회원 포인트 관리

**Endpoint**
- `GET  /admin/point/list?keyword=&page=1`
- `POST /admin/point/update`

관리자가 특정 회원의 포인트를 직접 지급하거나 회수할 수 있도록 만들었습니다.  
여기서도 별도 로직을 만들지 않고, 일반 포인트 처리와 동일하게 `addPoint()`를 타도록 연결해 이력이 남도록 했습니다.

### 핵심 코드

```java
public void adminUpdatePoint(String memberId, int amount) {
    String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
    String adminType = (amount > 0) ?  "GET" : "USE";
    boolean result = addPoint(memberId, amount, adminType, reason);
    if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
}
```

---

### 02) 데일리 퀴즈 관리

**Endpoint**
- `GET    /admin/quiz/list?type=all|question|answer&keyword=&page=1`
- `POST   /admin/quiz`
- `PUT    /admin/quiz`
- `DELETE /admin/quiz/{quizNo}`

운영자가 문제를 추가하고 수정·삭제할 수 있도록 CRUD를 구성했습니다.  
검색 조건과 페이징도 함께 넣어 실제 관리 화면에서 다루기 편하도록 했습니다.

### 핵심 SQL

```sql
INSERT INTO DAILY_QUIZ (QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER)
VALUES (SEQ_DAILY_QUIZ_NO.NEXTVAL, #{quizQuestion}, #{quizAnswer});

UPDATE DAILY_QUIZ
SET QUIZ_QUESTION = #{quizQuestion},
    QUIZ_ANSWER = #{quizAnswer}
WHERE QUIZ_NO = #{quizNo};

DELETE FROM DAILY_QUIZ WHERE QUIZ_NO = #{quizNo};

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
WHERE RN BETWEEN #{startRow} AND #{endRow}
```

---

### 03) 포인트 상점 아이템 관리

**Endpoint**
- `POST   /admin/store/add`
- `PUT    /admin/store/edit`
- `DELETE /admin/store/delete/{itemNo}`

운영자가 상품명, 가격, 재고, 제한 구매 여부 등을 직접 관리할 수 있도록 구성했습니다.

### 핵심 SQL

```sql
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

DELETE FROM POINT_ITEM_STORE WHERE point_item_no = #{pointItemNo};
```

---

### 04) 유저 자산 관리

**Endpoint**
- `GET    /admin/inventory/list?keyword=&page=1`
- `GET    /admin/inventory/{memberId}`
- `DELETE /admin/inventory/{inventoryNo}`
- `POST   /admin/inventory/{memberId}/{itemNo}`
- `GET    /admin/icon/{memberId}`
- `POST   /admin/icon/{memberId}/{iconId}`
- `DELETE /admin/icon/recall/{memberIconId}`

관리자는 특정 회원이 어떤 아이템과 아이콘을 갖고 있는지 확인할 수 있고, 필요하면 직접 지급하거나 회수할 수 있도록 했습니다.

### 핵심 SQL

```sql
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

---

## 6. 트러블슈팅

실제로 구현하면서 헷갈렸던 부분과 직접 해결한 내용을 정리했습니다.

---

### 01) 인벤토리 조회가 계속 0건으로 나왔던 문제

**현상**  
`GET /point/main/store/inventory/my` 응답이 계속 빈 배열로 내려왔습니다.

**원인**  
DAO에서 String 하나만 넘긴 경우 MyBatis 기본 파라미터명이 `value`로 잡히는데, mapper에서는 `#{loginId}`로 받고 있었습니다.  
결과적으로 WHERE 조건에 null이 들어가면서 조회가 되지 않았습니다.

**해결**  
mapper 바인딩을 `#{value}`로 수정했습니다.

```sql
-- 수정 전
WHERE I.INVENTORY_MEMBER_ID = #{loginId}

-- 수정 후
WHERE I.INVENTORY_MEMBER_ID = #{value}
```

---

### 02) 로그인은 되어 있는데 `loginId`가 null로 들어오던 문제

**현상**  
일부 API에서 `@RequestAttribute loginId`가 null로 들어왔습니다.

**원인**  
특정 요청에서 Authorization 헤더가 누락되거나, 프론트의 axios 인터셉터가 일관되게 적용되지 않았습니다.

**해결**  
프론트에서 axios 인스턴스를 하나로 통일하고, 인터셉터에서 토큰 헤더를 강제로 주입하도록 수정했습니다.  
서버에서는 인증 실패를 401로 명확히 반환해 원인을 빠르게 확인할 수 있게 했습니다.

---

### 03) 연속 출석 계산이 꼬이던 문제

**현상**  
연속 출석이 갑자기 1로 초기화되거나, 하루를 건너뛰었는데도 이어지는 것처럼 보이는 경우가 있었습니다.

**원인**  
DB에는 `timestamp`가 저장되어 있었고, 시간까지 포함해 비교하다 보니 자정 전후 케이스에서 오차가 생겼습니다.

**해결**  
`LocalDate`로 변환해 날짜만 비교하도록 바꿨습니다.

```java
LocalDate lastDate = status.getAttendanceStatusLastdate().toLocalDateTime().toLocalDate();
```

---

### 04) 퀘스트 로그가 같은 날 중복 생성되던 문제

**현상**  
같은 날 같은 퀘스트 타입인데 로그가 2개 이상 생겼습니다.

**원인**  
서버에서 넘기는 날짜 형식과 MERGE에서 비교하는 날짜 형식이 서로 달라 같은 날로 인식되지 않았습니다.

**해결**  
서버에서 날짜를 `YYYYMMDD`로 통일해서 넘기고, MERGE의 ON 조건도 그 형식에 맞춰 정리했습니다.

---

### 05) 같은 타입 아이템이 동시에 여러 개 장착되던 문제

**현상**  
배경 아이템이나 프레임 아이템이 여러 개 `Y`로 남아 화면 표시가 꼬였습니다.

**원인**  
새 아이템 장착 전에 같은 타입을 먼저 해제하는 로직이 빠져 있었습니다.

**해결**  
장착 전에 `unequipByType()`를 먼저 호출하고, 이후 선택 아이템만 `Y`로 바꾸도록 수정했습니다.

```java
unequipByType(loginId, type);
inven.setInventoryEquipped("Y");
inventoryDao.update(inven);
```

---

## 7. 마무리

이 프로젝트에서 제가 맡은 포인트 생태계 영역은 단순히 포인트를 적립하고 차감하는 기능을 만드는 데서 끝나지 않았습니다.

사용자가 활동을 통해 포인트를 얻고, 그 포인트를 다시 소비하고, 소비 결과가 인벤토리와 프로필 변화로 이어지도록 하나의 흐름으로 연결하는 것이 핵심이었습니다.  
동시에 포인트를 실제 서비스 재화처럼 다뤄야 했기 때문에, **잔액 검증**, **이력 기록**, **재고 처리**, **장착 상태 관리** 같은 정합성 문제를 함께 신경 써서 구현했습니다.

정리하면, 이 작업을 통해 저는 단일 기능 구현보다 더 중요한 **서비스 안의 흐름과 데이터 정합성을 함께 설계하는 경험**을 할 수 있었습니다.
