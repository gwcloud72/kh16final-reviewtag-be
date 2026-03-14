# Update Log

프로젝트 오류 수정 및 개선 기록

# 26/03/13 Ver1.0.1
### 1) 하드코딩 이메일 정보 제거
- 이유: 구현된 Email Properties를 통해  활용하여 application.properties에서 관리하려고 합니다. 
- 파일: `src/main/java/com/kh/finalproject/configuration/EmailConfiguration.java`
```java
sender.setUsername(emailProperties.getUsername());
sender.setPassword(emailProperties.getPassword());
props.setProperty("mail.smtp.debug", "false");
```