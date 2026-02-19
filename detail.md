Backend (Spring Boot + MyBatis + Oracle)
0. 개요

본 프로젝트 백엔드는 포인트 기반 보상 시스템을 중심으로 동작합니다.
사용자의 활동(출석/퀘스트/룰렛 등)을 포인트로 환산하고, 포인트로 상점 아이템을 구매/선물/환불하며, 인벤토리(소모/장착) 및 랭킹/이력/관리자 운영 기능까지 포함합니다.

인증: JWT 기반 (요청 시 @RequestAttribute loginId로 인증 사용자 식별)

DB: Oracle

ORM: MyBatis (SQL 명시형)

무결성 핵심: 포인트 증감은 반드시 이력(POINT_HISTORY)과 함께 처리

1. 핵심 설계 원칙 (무결성 의도)
1) 포인트 무결성

포인트 증감은 PointService.addPoint() 단일 경로로 통일

잔액 부족 시 차감 불가(음수 방지)

포인트 변경 성공 시 POINT_HISTORY 무조건 INSERT

핵심 코드 (PointService.addPoint)

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


핵심 쿼리 (POINT_HISTORY insert / MyBatis: point-history-mapper.xml)

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

2. 핵심 기능 16개 (코드 + 쿼리 + 처리설명)

기능은 “사용자 기능 12개 + 관리자 기능 4개”로 구성했습니다.
각 기능마다 Endpoint / 처리 흐름 / 핵심 코드 / 핵심 SQL을 같이 적었습니다.

[사용자] 01) 출석체크 (중복 방지 + 연속 출석 + 포인트 지급)
Endpoint

GET /point/main/attendance/status

POST /point/main/attendance/check

GET /point/main/attendance/calendar

처리 흐름

출석 현황(AttendanceStatus) 조회

오늘 이미 출석했으면 예외(중복 방지)

어제 출석이면 연속출석 +1, 아니면 1로 초기화

현황 업데이트 + 출석 히스토리 INSERT

포인트 지급(기본 100, 7일마다 +50)

핵심 코드 (AttendanceService.checkAttendance)
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

핵심 쿼리 (attendance-status-mapper.xml)
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

[사용자] 02) 일일 퀘스트 목록 조회 (진행도/보상여부 포함)
Endpoint

GET /point/quest/list

처리 흐름

오늘 날짜 기준으로 POINT_GET_QUEST_LOG를 조회하여
각 퀘스트의 (현재 count / rewardYn) 값을 반환에 포함합니다.

핵심 쿼리 (daily-quest-mapper.xml)
SELECT
    POINT_GET_QUEST_TYPE      AS "type",
    POINT_GET_QUEST_COUNT     AS "count",
    POINT_GET_QUEST_REWARD_YN AS "rewardYn"
FROM POINT_GET_QUEST_LOG
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}

[사용자] 03) 퀘스트 진행도 증가 (UPSERT: 없으면 생성, 있으면 +1)
Endpoint

퀘스트는 여러 기능에서 진행도가 올라가며(퀴즈/룰렛 등), 공통으로 questProgress()를 사용합니다.

핵심 코드 (DailyQuestService.questProgress)
public void questProgress(String memberId, String type) {
    boolean isValid = questProps.getList().stream().anyMatch(q -> q.getType().equals(type));
    if(isValid) {
        questDao.upsertQuestLog(memberId, type, getTodayStr());
    }
}

핵심 쿼리 (daily-quest-mapper.xml: MERGE)
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

[사용자] 04) 퀘스트 보상 수령 (중복 수령 방지 + 포인트 지급)
Endpoint

POST /point/quest/claim

처리 흐름

오늘 로그 조회 → 내 type 로그 찾기

목표치 미달이면 실패

rewardYn이 Y면 실패(중복수령 방지)

rewardYn을 Y로 업데이트 성공 시 포인트 지급

핵심 코드 (DailyQuestService.claimReward)
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

핵심 쿼리 (daily-quest-mapper.xml)
UPDATE POINT_GET_QUEST_LOG
SET POINT_GET_QUEST_REWARD_YN = 'Y'
WHERE POINT_GET_QUEST_MEMBER_ID = #{memberId}
  AND POINT_GET_QUEST_TYPE = #{type}
  AND TO_CHAR(POINT_GET_QUEST_DATE, 'YYYYMMDD') = #{date}
  AND POINT_GET_QUEST_REWARD_YN = 'N'

[사용자] 05) 데일리 퀴즈 랜덤 출제 (1일 1회 제한)
Endpoint

GET /point/quest/quiz/random

핵심 코드 (DailyQuestService.getRandomQuiz)
public DailyQuizVO getRandomQuiz(String memberId) {
    List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
    boolean alreadySolved = logs.stream().anyMatch(m -> "QUIZ".equals(m.get("type")));

    if (alreadySolved) return null;
    return quizDao.getRandomQuiz();
}

핵심 쿼리 (daily-quiz-mapper.xml)
SELECT * FROM (
    SELECT QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER
    FROM DAILY_QUIZ
    ORDER BY DBMS_RANDOM.VALUE
)
WHERE ROWNUM = 1

[사용자] 06) 데일리 퀴즈 정답 검증 + 퀘스트 진행도 반영
Endpoint

POST /point/quest/quiz/check

핵심 코드 (DailyQuestService.checkQuizAndProgress)
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

핵심 쿼리 (daily-quiz-mapper.xml)
SELECT QUIZ_ANSWER
FROM DAILY_QUIZ
WHERE QUIZ_NO = #{quizNo}

[사용자] 07) 룰렛 게임 (티켓 소모 + 랜덤 보상 + 퀘스트 진행)
Endpoint

POST /point/main/store/roulette

처리 흐름

내 인벤토리 조회

아이템 상세 조회하면서 RANDOM_ROULETTE 타입 티켓 찾기

랜덤 인덱스 생성 → 일부 인덱스는 포인트 보상

티켓 수량 감소(1개 남았으면 delete)

당첨이면 포인트 지급

퀘스트 진행도(ROULETTE) 반영

핵심 코드 (PointService.playRoulette + decreaseInventoryOrDelete)
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

핵심 쿼리 (inventory-mapper.xml)
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

[사용자] 08) 포인트 상점 목록 조회 (검색/필터/페이징)
Endpoint

GET /point/main/store

핵심 쿼리 (point-item-store-mapper.xml)
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

[사용자] 09) 상품 구매 (포인트 차감 + 재고 감소 + 인벤 지급 + 찜 정리)
Endpoint

POST /point/main/store/buy

처리 흐름

상품 조회

꾸미기(DECO_) 아이템은 “중복 보유” 구매 차단

재고 확인

포인트 차감 (이력 저장 포함)

재고 -1 업데이트

인벤토리 지급(이미 있으면 수량 +1)

찜 되어 있으면 삭제

핵심 코드 (PointService.purchaseItem + giveItemToInventory)
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

핵심 쿼리

(1) 인벤토리 보유 여부 체크 / inventory-mapper.xml

SELECT
    inventory_no as inventoryNo,
    inventory_member_id as inventoryMemberId,
    inventory_item_no as inventoryItemNo,
    inventory_quantity as inventoryQuantity,
    inventory_equipped as inventoryEquipped
FROM INVENTORY
WHERE inventory_member_id = #{inventoryMemberId}
  AND inventory_item_no = #{inventoryItemNo}


(2) 인벤토리 insert / inventory-mapper.xml

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


(3) 상점 아이템 update (재고 포함) / point-item-store-mapper.xml

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

[사용자] 10) 선물하기 (보낸사람 차감 + 재고 감소 + 상대 인벤 지급 + 찜 정리)
Endpoint

POST /point/main/store/gift

핵심 코드 (PointService.giftItem)
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

핵심 쿼리

재고 update: 위 09와 동일

인벤 지급/수량 증가: 위 09와 동일

포인트 차감/이력 insert: 위 0의 addPoint/point_history insert 동일

[사용자] 11) 찜(위시리스트) 토글 + 내 찜 목록 조회
Endpoint

POST /point/main/store/wish/toggle

GET /point/main/store/wish/my

GET /point/main/store/wish/check

핵심 코드 (PointService.toggleWish)
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

핵심 쿼리 (point-wishlist-mapper.xml)
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


내 찜 목록 조회 JOIN (point-wishlist-mapper.xml)

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

[사용자] 12) 인벤토리 조회 (상점 정보 JOIN)
Endpoint

GET /point/main/store/inventory/my

핵심 코드 (PointStoreRestController.myInventory)
@GetMapping("/inventory/my")
public List<InventoryDto> myInventory(@RequestAttribute(required = false) String loginId) {
    if(loginId == null) return List.of();
    return inventoryDao.selectListByMemberId(loginId);
}

핵심 쿼리 (inventory-mapper.xml) ✅ (※ 트러블슈팅에서 수정 포인트도 설명)
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
WHERE I.INVENTORY_MEMBER_ID = #{loginId}   -- ⚠️ 여기 트러블슈팅 포인트
ORDER BY I.INVENTORY_CREATED_AT DESC

[사용자] 13) 아이템 사용 (소모/랜덤포인트/장착형 분기)
Endpoint

POST /point/main/store/inventory/use

처리 흐름

인벤 번호로 내 아이템인지 검증

아이템 타입별 분기

닉네임 변경(CHANGE_NICK)

하트 충전(HEART_RECHARGE)

랜덤 포인트 지급(RANDOM_POINT)

꾸미기 장착(DECO_*)

상품권(VOUCHER)

핵심 코드 (PointService.useItem)
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

핵심 쿼리 (inventory-mapper.xml: unequipByType)
UPDATE inventory
SET inventory_equipped = 'N'
WHERE inventory_member_id = #{memberId}
AND inventory_item_no IN (
    SELECT point_item_no
    FROM point_item_store
    WHERE point_item_type = #{type}
)

[사용자] 14) 장착 해제 (단일 아이템 unequip)
Endpoint

POST /point/main/store/inventory/unequip

핵심 코드 (PointService.unequipItem)
public void unequipItem(String loginId, long inventoryNo) {
    InventoryDto inv = inventoryDao.selectOne(inventoryNo);
    if(inv != null && loginId.equals(inv.getInventoryMemberId())) {
        inv.setInventoryEquipped("N");
        inventoryDao.update(inv);
    }
}

핵심 쿼리 (inventory-mapper.xml: update)
UPDATE INVENTORY
SET
    inventory_quantity = #{inventoryQuantity},
    inventory_equipped = #{inventoryEquipped}
WHERE
    inventory_no = #{inventoryNo}

[사용자] 15) 환불 (포인트 반환 + 재고 복구 + 인벤 차감)
Endpoint

POST /point/main/store/cancel

핵심 코드 (PointService.cancelItem)
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

핵심 쿼리

포인트 반환/이력: addPoint + point_history insert (상단 동일)

재고 복구: point-item-store update (상단 동일)

인벤 차감/삭제: inventory update/delete (상단 동일)

[사용자] 16) 포인트 이력 조회 (페이징 + 타입 필터)
Endpoint

GET /point/history?page=1&type=all|earn|use|item

핵심 코드 (PointService.getHistoryList)
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

핵심 쿼리 (point-history-mapper.xml)
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

3. 랭킹/프로필 커스터마이징 (추가 핵심 섹션)
(참고) 포인트 랭킹 (WINDOW RANK + 페이징)
Endpoint

GET /point/ranking/total?keyword=&page=1&size=10

핵심 쿼리 (member-mapper.xml)
SELECT * FROM (
    SELECT TMP.*, ROWNUM AS rn FROM (
        SELECT
            member_id AS "memberId",
            member_nickname AS "nickname",
            member_point AS "point",
            member_level AS "level",
            RANK() OVER (ORDER BY member_point DESC) AS "ranking"
        FROM member
        WHERE member_level != '관리자'
        <if test="keyword != null and keyword != ''">
            AND member_nickname LIKE '%' || #{keyword} || '%'
        </if>
        ORDER BY "ranking" ASC
    ) TMP
)
WHERE rn BETWEEN #{start} AND #{end}

(참고) 내 프로필 정보 (장착된 꾸미기/아이콘 반영)
Endpoint

GET /point/main/store/my-info

핵심 코드 (PointService.getMyPointInfo)
public MemberPointVO getMyPointInfo(String id) {
    MemberDto m = memberDao.selectOne(id);
    if (m == null) return null;

    String iconSrc = memberIconDao.selectEquippedIconSrc(id);
    String frameStyle = memberIconDao.selectEquippedFrameStyle(id);
    String bgStyle = memberIconDao.selectEquippedBgStyle(id);
    String nickStyle = memberIconDao.selectEquippedNickStyle(id);

    if (iconSrc == null) iconSrc = "https://i.postimg.cc/tJdMNh4T/-Image-2025nyeon-12wol-21il-ohu-09-13-24.png";

    return MemberPointVO.builder()
            .memberId(m.getMemberId())
            .nickname(m.getMemberNickname())
            .point(m.getMemberPoint())
            .level(m.getMemberLevel())
            .iconSrc(iconSrc)
            .frameSrc(frameStyle)
            .bgSrc(bgStyle)
            .nickStyle(nickStyle)
            .build();
}

4. 관리자 기능 (운영 패널) 4종
[관리자] A) 상점 상품 관리 (등록/수정/삭제/목록)
Endpoint

GET /admin/store/list

POST /admin/store/add

DELETE /admin/store/delete/{itemNo}

핵심 쿼리 (point-item-store-mapper.xml)
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

DELETE FROM POINT_ITEM_STORE
WHERE point_item_no = #{pointItemNo};

[관리자] B) 인벤토리 관리 (유저 지급/회수)
Endpoint

POST /admin/inventory/{memberId}/{itemNo} (지급)

DELETE /admin/inventory/{inventoryNo} (회수)

핵심 코드 (AdminAssetService.grantAsset / recallAsset)
public String grantAsset(String type, String memberId, int targetNo) {
    if ("item".equals(type)) {
        InventoryDto dto = new InventoryDto();
        dto.setInventoryMemberId(memberId);
        dto.setInventoryItemNo((long)targetNo);
        inventoryDao.insert(dto);
        return "success";
    } else {
        if (memberIconDao.checkUserHasIcon(memberId, targetNo) > 0) return "duplicate";
        memberIconDao.insertMemberIcon(memberId, targetNo);
        return "success";
    }
}

public boolean recallAsset(String type, long id) {
    return "item".equals(type) ? inventoryDao.delete(id) : memberIconDao.deleteMemberIcon(id) > 0;
}

[관리자] C) 포인트 이력 회원별 조회
Endpoint

GET /admin/point/history/{memberId}

(쿼리는 사용자 이력 조회와 동일 구조 + memberId만 파라미터로 받습니다.)

[관리자] D) 데일리 퀴즈 관리 (CRUD + 검색/페이징)
Endpoint

GET /admin/dailyquiz/list?page=1&type=all&keyword=

POST /admin/dailyquiz/ (등록)

DELETE /admin/dailyquiz/{quizNo} (삭제)

핵심 쿼리 (daily-quiz-mapper.xml)
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
WHERE RN BETWEEN #{startRow} AND #{endRow};

INSERT INTO DAILY_QUIZ(QUIZ_NO, QUIZ_QUESTION, QUIZ_ANSWER)
VALUES(seq_daily_quiz.nextval, #{quizQuestion}, #{quizAnswer});

DELETE FROM DAILY_QUIZ
WHERE QUIZ_NO = #{quizNo};

5. 트러블슈팅 (실제 코드 기반)
1) 인벤토리 조회했는데 항상 빈 배열이 나오는 문제
증상

/point/main/store/inventory/my 호출 시 결과가 0건

원인 (MyBatis 단일 파라미터 이름 불일치)

DAO는 selectListByMemberId(String memberId)로 문자열 하나만 넘기는데,
mapper에서는 #{loginId}로 받고 있어 바인딩이 null이 되어 조회가 0건이 됩니다.

DAO

public List<InventoryDto> selectListByMemberId(String memberId) {
    return sqlSession.selectList("inventory.selectListByMemberId", memberId);
}


문제 mapper

WHERE I.INVENTORY_MEMBER_ID = #{loginId}

해결

mapper를 단일 파라미터 방식에 맞게 수정합니다.

수정 후

WHERE I.INVENTORY_MEMBER_ID = #{value}

2) 장착 아이템이 여러 개 동시에 Y가 되는 문제
원인

장착 시 기존 장착 해제를 안 하면 누적됨

해결

장착 전에 unequipByType(memberId, type)로 해당 타입 전체 N 처리 후 장착

핵심 쿼리

UPDATE inventory
SET inventory_equipped = 'N'
WHERE inventory_member_id = #{memberId}
AND inventory_item_no IN (
    SELECT point_item_no
    FROM point_item_store
    WHERE point_item_type = #{type}
)

3) 포인트가 음수가 되는 문제
해결

addPoint()에서 잔액 검증으로 차단(이미 적용됨)

6. 마무리

이 백엔드는 “포인트”를 중심으로 출석/퀘스트/상점/인벤/랭킹/이력/운영까지 흐름이 한 번에 연결되는 구조입니다.
특히 포인트 증감과 이력을 강제로 묶어서 관리해서, 운영/정산/분석 관점에서도 확장 가능한 형태로 구성했습니다.
