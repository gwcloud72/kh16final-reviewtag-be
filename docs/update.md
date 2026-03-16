# Update Log

프로젝트 오류 수정 및 개선 기록

# 26/03/13 Ver1.0.1
### 1) 하드코딩 이메일 정보 제거
- 이유: 구현된 Email Properties를 통해  활용하여 application.properties에서 관리하려고 합니다. 
- 파일: `src/main/java/com/kh/finalproject/configuration/EmailConfiguration.java`
```java
sender.setUsername(emailProperties.getUsername());
sender.setPassword(emailProperties.getPassword());
```
# 26/03/13 Ver1.0.2
### 2) 환경 변수 설정
- 이유: 민감정보를 시스템 환경변수에서 관리하려고 합니다.
- 파일: `src/main/resources/application.properties`
```java
spring.datasource.url=${APP_DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
custom.tmdb.key=${TMDB_API_KEY}
custom.tmdb.access-token=${TMDB_ACCESS_TOKEN}
custom.tmdb.base-url=${TMDB_BASE_URL}

custom.jwt.key-str=${APP_JWT_KEY}
custom.jwt.issuer=${APP_JWT_ISSUER}
custom.jwt.expiration=${APP_JWT_EXPIRATION_MINUTES}
custom.jwt.refresh-expiration=${APP_JWT_REFRESH_EXPIRATION_DAYS}
custom.jwt.renewal-limit=${APP_JWT_RENEWAL_LIMIT_MINUTES}

custom.email.username=${APP_EMAIL_USERNAME}
custom.email.password=${APP_EMAIL_PASSWORD}
```
# 26/03/14 Ver1.0.3
### 3) 비로그인 상태 포인트 상점 이용 불가능 해결
- 이유: 비로그인상태에서 포인트 상점을 이용이 불가능했습니다. 해결하기위해 인터셉터를 수정하였습니다.
- 파일: `src/main/java/com/kh/finalproject/aop/InterceptorConfiguration.java`
```java
 "/point/main/store",         // 포인트 상점 메인 조회는 제외
 "/point/main/store/",        // 포인트 상점 메인 조회는 제외
 "/point/main/store/detail/**", // 포인트 상품 상세 조회는 제외
```
# 26/03/14 Ver1.0.4
### 4) 멤버 