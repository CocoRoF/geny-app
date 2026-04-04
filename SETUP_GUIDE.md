# geny-app 개발 환경 설정 및 테스트 가이드

Android 개발이 처음인 사람을 위한 단계별 가이드입니다.

---

## 1. 설치해야 하는 것

### 1-1. Android Studio 설치

Android 앱 개발에 필요한 모든 도구가 포함된 IDE입니다.

1. https://developer.android.com/studio 에서 다운로드
2. 설치 실행 (기본 옵션 그대로 Next)
3. 설치 완료 후 처음 실행하면 **Setup Wizard**가 나옴:
   - Install Type: **Standard** 선택
   - SDK Components: 체크된 그대로 두고 Next
   - License Agreement: 모두 Accept
   - Finish (SDK 다운로드 시작, 약 3~5GB, 10분+ 소요)

> 이 과정에서 **Android SDK**, **Android Emulator**, **Platform Tools**가 자동 설치됩니다.
> 별도로 설치할 것은 없습니다.

### 1-2. JDK 확인

Android Studio에 JDK가 내장되어 있어서 따로 설치할 필요 없습니다.
만약 빌드 시 Java 관련 에러가 나면:

1. Android Studio 메뉴: `File` → `Settings`
2. `Build, Execution, Deployment` → `Build Tools` → `Gradle`
3. Gradle JDK: **jbr-17** (Embedded JDK) 선택

---

## 2. 프로젝트 열기 및 첫 빌드

### 2-1. 프로젝트 열기

1. Android Studio 실행
2. `Open` 클릭 (또는 `File` → `Open`)
3. 경로 선택: `D:\local_workspace\2026\geny-vtuber\geny-app`
4. `OK` 클릭

### 2-2. Gradle Sync 대기

프로젝트를 열면 자동으로 Gradle Sync가 시작됩니다.

- 화면 하단 진행바에 "Gradle sync" 또는 "Download" 표시
- **처음에는 의존성 다운로드 때문에 5~15분 걸릴 수 있음**
- 완료되면 하단에 "BUILD SUCCESSFUL" 또는 "Gradle sync finished" 표시

> **Sync 실패 시 흔한 해결법:**
> - `File` → `Invalidate Caches` → `Invalidate and Restart`
> - `File` → `Sync Project with Gradle Files` (툴바의 코끼리+화살표 아이콘)

### 2-3. 빌드 확인

1. 상단 메뉴: `Build` → `Make Project` (또는 단축키 `Ctrl+F9`)
2. 하단 `Build` 탭에서 진행 상황 확인
3. "BUILD SUCCESSFUL" 나오면 성공

---

## 3. 에뮬레이터로 테스트하기

핸드폰 없이 PC에서 먼저 테스트할 수 있습니다.

### 3-1. 에뮬레이터 생성

1. 상단 툴바에서 **Device Manager** 클릭 (핸드폰 아이콘)
   - 또는 메뉴: `Tools` → `Device Manager`
2. `Create Virtual Device` 클릭
3. 기기 선택:
   - Category: **Phone**
   - 기기: **Pixel 7** (또는 아무거나) 선택 → `Next`
4. 시스템 이미지 선택:
   - **API 34** (Android 14) 옆의 `Download` 클릭 → 다운로드 완료 후 선택 → `Next`
5. 기본 설정 그대로 → `Finish`

### 3-2. 에뮬레이터에서 실행

1. 상단 툴바 중앙에 기기 선택 드롭다운 → 방금 만든 에뮬레이터 선택
2. 그 옆의 **초록색 재생 버튼 ▶** 클릭 (또는 `Shift+F10`)
3. 에뮬레이터가 부팅되고 앱이 자동 설치/실행됨

### 3-3. 앱에서 서버 연결

앱이 실행되면 연결 화면이 표시됩니다:

| 항목 | 입력값 |
|------|--------|
| **Server URL** | `https://geny-x.hrletsgo.me` |
| **Username** | (본인 geny 계정 ID) |
| **Password** | (본인 geny 계정 PW) |
| **Auto Login** | ON으로 켜기 |

→ **Connect** 버튼 클릭 → Dashboard 화면 진입

---

## 4. 실제 핸드폰에서 테스트하기

### 4-1. 핸드폰에서 개발자 옵션 활성화

1. `설정` → `휴대전화 정보` (맨 아래)
2. **소프트웨어 정보** 클릭
3. **빌드번호**를 **7번 연속 터치**
4. "개발자 모드가 활성화되었습니다" 메시지 확인

### 4-2. USB 디버깅 켜기

1. `설정` → `개발자 옵션` (방금 활성화됨)
2. **USB 디버깅** 토글 ON

### 4-3. PC와 핸드폰 연결

1. USB 케이블로 핸드폰을 PC에 연결
2. 핸드폰에 "USB 디버깅을 허용하시겠습니까?" 팝업 → **허용** (항상 허용 체크 권장)
3. 핸드폰에 "이 기기를 어떤 용도로 사용?" 팝업 → **파일 전송(MTP)** 선택

### 4-4. Android Studio에서 핸드폰으로 실행

1. 상단 툴바 기기 선택 드롭다운 → 본인 핸드폰 이름이 표시됨 (예: `Samsung SM-S911N`)
2. 핸드폰 선택
3. **초록색 재생 버튼 ▶** 클릭 (또는 `Shift+F10`)
4. 핸드폰에 앱이 설치되고 자동 실행됨

> **핸드폰이 목록에 안 보일 때:**
> - USB 케이블이 데이터 전송 지원 케이블인지 확인 (충전 전용이면 안 됨)
> - `설정` → `개발자 옵션` → USB 디버깅이 켜져 있는지 확인
> - 케이블 뺐다 다시 연결
> - PC에서 삼성 드라이버 설치 필요할 수 있음: https://developer.samsung.com/android-usb-driver

### 4-5. 무선 디버깅 (USB 없이)

Android 11 이상이면 Wi-Fi로도 가능합니다:

1. PC와 핸드폰이 **같은 Wi-Fi**에 연결되어 있어야 함
2. 핸드폰: `설정` → `개발자 옵션` → **무선 디버깅** ON
3. "무선 디버깅" 항목을 터치하여 들어감
4. **페어링 코드로 기기 페어링** 터치
5. Android Studio: `Tools` → `Device Manager` → `Pair Devices Using Wi-Fi`
6. 핸드폰에 표시된 페어링 코드 입력
7. 페어링 완료 후 기기 목록에 표시됨

---

## 5. APK 파일로 만들어서 배포하기

핸드폰에 APK 파일을 직접 보내서 설치할 수도 있습니다.

### 5-1. Debug APK 생성

1. Android Studio 메뉴: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 빌드 완료 후 우측 하단에 `locate` 링크 클릭
3. APK 파일 위치: `geny-app\app\build\outputs\apk\debug\app-debug.apk`

### 5-2. 핸드폰에 설치

1. APK 파일을 카카오톡, 이메일, USB 등으로 핸드폰에 전송
2. 핸드폰에서 APK 파일 실행
3. "출처를 알 수 없는 앱" 설치 허용 팝업 → 허용
4. 설치 → 열기

---

## 6. 문제 해결

### Gradle Sync 실패: "Could not resolve..."
```
File → Settings → Build, Execution, Deployment → Gradle
→ "Use Gradle from" → "gradle-wrapper.properties file" 선택
```

### 빌드 에러: "SDK location not found"
```
File → Project Structure → SDK Location
→ Android SDK Location이 비어있으면 기본 경로 입력:
   C:\Users\{사용자명}\AppData\Local\Android\Sdk
```

### 에뮬레이터가 느림
- BIOS에서 **Intel VT-x** 또는 **AMD SVM** 활성화 필요
- `File` → `Settings` → `Languages & Frameworks` → `Android SDK` → `SDK Tools`
  → **Intel x86 Emulator Accelerator (HAXM)** 설치 확인
- 또는 실제 핸드폰으로 테스트 (훨씬 빠름)

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
이전에 같은 앱을 다른 방식으로 설치한 적이 있을 때:
- 핸드폰에서 geny 앱 삭제 후 다시 실행

---

## 7. 요약 (Quick Reference)

| 단계 | 할 일 |
|------|-------|
| **설치** | Android Studio 다운로드 및 설치 (SDK 자동 포함) |
| **열기** | Android Studio → Open → `geny-app` 폴더 |
| **대기** | Gradle Sync 완료 대기 (첫 회 5~15분) |
| **빌드** | `Build` → `Make Project` (Ctrl+F9) |
| **에뮬** | Device Manager → 에뮬레이터 생성 → ▶ 실행 |
| **실기기** | 개발자옵션 ON → USB디버깅 ON → USB연결 → ▶ 실행 |
| **APK** | `Build` → `Build APK` → 핸드폰에 전송/설치 |
| **접속** | Server URL: `https://geny-x.hrletsgo.me` + 로그인 |
