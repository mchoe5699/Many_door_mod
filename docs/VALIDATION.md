# 0.3.0 검증 결과 — 2026-10-12

Minecraft 1.20.1 / Forge 47.4.0, Temurin Java 17.0.16, Gradle 8.8 환경에서 검증했습니다.

| 실행 | 결과 |
| --- | --- |
| HomeNet 1.0.0을 함께 로드한 실제 GameTest 서버 | **27개 필수 테스트 통과**, 정상 종료 |
| HomeNet을 제외한 GameTest 서버 | **26개 필수 테스트 통과**, 정상 종료 |
| 실제 그래픽 클라이언트 | 설정·기기·문 렌더링, PNG/JPEG 읽기, 브러시 입력, 네트워크 왕복, 적용된 문, 로비폰/자동문 확인 후 정상 종료 |
| 최종 배포 빌드 | 성공. 일반 JUnit 단계는 NO-SOURCE이며 위 테스트는 별도 GameTest 실행 결과 |

[홈넷 서버 로그](validation-homenet.log), [단독 서버 로그](validation-standalone.log),
[클라이언트 검증 로그](validation-client.log), [실제 게임 캡처](SCREENSHOTS.md).

## 0.3.0에서 검증한 수정

- 주변 블록을 예약하지 않고 문을 확대하며 이동 경로의 벽을 보존한 채 끝까지 열기
- 충돌 엔티티의 실제 플레이어 충돌, 블록 설치를 막지 않는 속성, 비저장 속성 및 철거 후 제거
- 0.2 숨은 부품 정리 후 동일 문 UUID와 문 자체 유지
- 실제 고정 로비폰 기둥의 벽면 지원과 로비폰 문 열림
- 기기 위치·크기·범위 저장/복원, 방향별 선택 영역, NaN 좌표 거절
- 커스텀 표면 저장/복원·초기화, 배열 소유권 보존, 초과 길이 거절
- 최악의 픽셀 데이터도 서버 사용자 페이로드 32 KB 한도 안에 인코딩하고 정확히 디코딩
- 비정상 저장 진행값 NaN을 정상값으로 복원
- 홈넷 원본 문 연결 도구로 실제 문짝 엔티티를 클릭해 기준 블록에 전달
- 기존 모드·수동/자동/겸용·다중 문·버튼/센서·레드스톤·권한·차원 제한·닫힘 안전 동작 유지
- 33종의 이동 기하와 설치/철거, 제작법 41개 확인

## 그래픽 클라이언트

Xvfb와 Mesa 소프트웨어 OpenGL에서 HomeNet 1.0.0을 함께 로드했습니다.
PNG와 JPEG를 각각 실제로 읽고, 그 위에 브러시로 픽셀을 칠한 다음 실제 네트워크 패킷을 보냈습니다.
수신한 표면의 사진 중앙 색상과 칠한 픽셀을 검사해 왕복 저장이 정확함을 확인했습니다.
로비 자동문의 OPEN 상태와 완전 개방 진행값도 확인했습니다.
작은 기본 창에서 설정과 편집의 적용/취소 버튼이 화면 안에 보이도록 레이아웃을 수정했습니다.

재현 명령:

```bash
/workspace/.tools/cloud-gradle runGameTestServer
/workspace/.tools/cloud-gradle -PwithoutHomeNet runGameTestServer
python3 scripts/prepare_visual_test.py
# 준비된 클라우드의 GUI 검사 도구. 별도 터미널/세션에서 유지합니다.
/workspace/.tools/xvfb/usr/bin/Xvfb :97 -screen 0 1280x960x24 -nolisten tcp -ac
DISPLAY=:97 LIBGL_ALWAYS_SOFTWARE=1 /workspace/.tools/cloud-gradle -PvisualTest runClient
```

`prepare_visual_test.py`의 개발용 이미지 생성에는 Pillow가 필요합니다. 클라우드에는 설치되어 있습니다.
검사 클래스는 `src/visualTest`에 있으며 배포 JAR에는 들어가지 않습니다. 검사 세계 폴더가
`manydoors_visual_test`일 때만 실행하며, 다른 월드에서 장면을 만들지 않습니다.
스크린샷은 `run/visual-test/screenshots`에 저장됩니다.

## 튕김 조사 범위

비정상 진행값·클래스 교체 중 렌더링·이미지 크기·패킷 크기·텍스처 캐시·보조 엔티티 수명 관련 방어를 추가했습니다.
이 검증에서는 게임 크래시가 발생하지 않았습니다. 사용자의 실제 crash report가 없어 이전의 간헐적 튕김을
재현하거나 원인을 확정한 것은 아닙니다. 다른 모드·리소스팩·그래픽 드라이버의 조합과 장시간 부하는 미검증입니다.
클라우드에 오디오 장치가 없어 사운드 재생은 검증하지 않았습니다.

배포 JAR SHA-256: `334992b1020fb0db609d768b65ba942104a3ec6334efed2aabc360ff8d9bb2bf`
