# 🧠 Backend - Point Reward Platform

Spring Boot 기반 포인트 보상 플랫폼 백엔드 서버입니다.

---

# 📦 기술 스택

- Spring Boot
- MyBatis
- Oracle 11g
- Redis (Session)
- Docker
- Nginx

---

# 📂 패키지 구조

```
com.project
 ├─ controller
 ├─ service
 ├─ mapper
 ├─ domain
 ├─ config
```

---

# 🎯 1. 출석 체크 시스템

## Controller

AttendanceRestController.java

```java
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceRestController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<?> attend(HttpSession session) {
        String memberId = (String) session.getAttribute("loginId");
        attendanceService.attend(memberId);
        return ResponseEntity.ok("출석 완료");
    }
}
```

---

## Service (중복 방지 + 트랜잭션)

```java
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final PointService pointService;

    @Transactional
    public void attend(String memberId){

        if(attendanceMapper.existsToday(memberId) > 0){
            throw new RuntimeException("이미 출석함");
        }

        attendanceMapper.insertAttendance(memberId);

        pointService.givePoint(memberId, "ATTENDANCE", 10);
    }
}
```

---

## Mapper

```java
int existsToday(String memberId);
void insertAttendance(String memberId);
```

---

## SQL

```sql
SELECT COUNT(*)
FROM attendance
WHERE member_id = #{memberId}
AND TRUNC(attendance_date) = TRUNC(SYSDATE);
```

---

# 🎯 2. 포인트 지급 / 차감 로직 (핵심)

PointService.java

```java
@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberMapper memberMapper;
    private final PointLogMapper pointLogMapper;

    @Transactional
    public void givePoint(String memberId, String type, int amount){

        memberMapper.addPoint(memberId, amount);

        pointLogMapper.insertLog(memberId, type, amount);
    }

    @Transactional
    public void usePoint(String memberId, int amount){

        int current = memberMapper.getPoint(memberId);

        if(current < amount){
            throw new RuntimeException("포인트 부족");
        }

        memberMapper.deductPoint(memberId, amount);

        pointLogMapper.insertLog(memberId, "USE", -amount);
    }
}
```

---

# 🎯 3. 상점 구매 로직 (정합성 보장)

```java
@Transactional
public void purchase(String memberId, int itemId){

    Item item = itemMapper.findById(itemId);

    if(item == null){
        throw new RuntimeException("존재하지 않는 아이템");
    }

    pointService.usePoint(memberId, item.getPrice());

    inventoryMapper.insert(memberId, itemId);
}
```

✔ 포인트 차감 실패 시 인벤토리 저장 안됨  
✔ 트랜잭션으로 정합성 보장  

---

# 🎯 4. 랭킹 시스템

RankingMapper.xml

```sql
SELECT m.member_id,
       SUM(pl.point_amount) total_point
FROM member m
JOIN point_log pl ON m.member_id = pl.member_id
GROUP BY m.member_id
ORDER BY total_point DESC
```

---

# 🎯 5. 아이콘 장착

```java
@Transactional
public void equipIcon(String memberId, int inventoryId){

    inventoryMapper.resetEquip(memberId);

    inventoryMapper.equip(memberId, inventoryId);
}
```

✔ 한 개만 장착 가능 구조

---

# 🎯 6. 관리자 기능

## 아이템 등록

```java
@PostMapping("/admin/item")
public void createItem(@RequestBody Item item){
    itemMapper.insert(item);
}
```

---

# 🧠 핵심 설계 포인트

- 모든 포인트 변화는 point_log 기록
- 트랜잭션 기반 정합성 유지
- 서버 중심 검증
- 중복 출석 방지
- 포인트 음수 방지

---

# 🐳 Docker 환경

docker-compose.yml

- oracle
- backend
- frontend
- nginx

---

# 📌 개선 예정

- Redis 캐싱 랭킹
- JWT 전환
- 이벤트 기반 포인트 정책
