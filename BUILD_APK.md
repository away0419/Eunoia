# APK 빌드 가이드

## 방법 1: Android Studio를 사용한 빌드 (권장)

### 1. 서명 키 생성 (최초 1회만)
1. Android Studio에서 `Build` > `Generate Signed Bundle / APK` 선택
2. `APK` 선택 후 `Next`
3. `Create new...` 클릭하여 새 키스토어 생성
   - Key store path: 키스토어 파일 저장 위치 선택
   - Password: 키스토어 비밀번호 입력
   - Key alias: 키 별칭 입력 (예: `eunoia-key`)
   - Key password: 키 비밀번호 입력
   - Validity: 25년 이상 권장
   - Certificate 정보 입력 (이름, 조직 등)
4. `OK` 클릭하여 키스토어 생성

### 2. APK 빌드
1. `Build` > `Generate Signed Bundle / APK` 선택
2. `APK` 선택 후 `Next`
3. 생성한 키스토어 선택
4. 키스토어 비밀번호와 키 비밀번호 입력
5. `release` 빌드 타입 선택
6. `Finish` 클릭
7. 빌드 완료 후 `locate` 클릭하여 APK 파일 위치 확인
   - 경로: `app/release/app-release.apk`

## 방법 2: 명령줄을 사용한 빌드

### 1. 서명 키 생성 (최초 1회만)
```bash
keytool -genkey -v -keystore eunoia-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias eunoia-key
```

### 2. 키스토어 정보 설정
프로젝트 루트에 `keystore.properties` 파일 생성:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=eunoia-key
storeFile=../eunoia-release-key.jks
```

### 3. build.gradle.kts 수정
`app/build.gradle.kts`의 `android` 블록에 추가:
```kotlin
signingConfigs {
    create("release") {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = java.util.Properties()
        if (keystorePropertiesFile.exists()) {
            keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 4. APK 빌드 실행
```bash
# Windows
gradlew.bat assembleRelease

# Mac/Linux
./gradlew assembleRelease
```

빌드된 APK 위치: `app/build/outputs/apk/release/app-release.apk`

## 방법 3: 간단한 디버그 APK (테스트용)

디버그 APK는 서명 없이 빌드 가능합니다 (테스트용으로만 사용):

```bash
# Windows
gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

빌드된 APK 위치: `app/build/outputs/apk/debug/app-debug.apk`

## APK 설치 방법

1. APK 파일을 안드로이드 기기로 전송 (USB, 이메일, 클라우드 등)
2. 기기에서 "알 수 없는 출처" 설치 허용
   - 설정 > 보안 > 알 수 없는 출처 허용
3. APK 파일을 탭하여 설치

## 주의사항

- **릴리스 APK는 반드시 서명해야 합니다** (Google Play 스토어 업로드 시 필요)
- 키스토어 파일과 비밀번호는 안전하게 보관하세요 (분실 시 업데이트 불가)
- 디버그 APK는 테스트용으로만 사용하세요

