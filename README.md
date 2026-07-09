# [기술명세서] On-Device AI 기반 오프라인 작업자 안전 및 위치 역추적 솔루션

## 1. 프로젝트 개요 (Overview)
* **프로젝트명:** 통신 음영 밀폐 공간을 위한 On-Device AI 기반 오프라인 작업자 신체 안전 특화 시스템
* **타겟 환경:** 조선 및 제조업 선박 블록 내부, 탱크 등 두꺼운 철판으로 인해 외부 통신(LTE/5G, Wi-Fi)이 차단되는 밀폐·음영 지역
* **플랫폼:** Android Native (타겟 OS: Android 13 / API 33 이상 권장)
* **주요 목적:** 통신이 단절된 극한 환경에서도 스마트폰 내장 센서와 On-Device AI를 활용하여 작업자의 LOTO 수칙 준수를 강제하고, 낙상 및 비상 상황을 실시간 감지하여 피어 투 피어(P2P) 형태로 구조 신호를 전송 및 경로를 역추적함.

---

## 2. 주요 기능 명세 (Functional Specifications)

### ① 디지털 LOTO(Lockout-Tagout) 게이트키핑
* **기능 설명:** 작업자가 밀폐 공간 진입 전, 지정된 위험 구역의 안전 조치(밸브 차단, 전원 잠금 등)를 확인하도록 강제하는 기능.
* **상세 로직:**
  * 앱 내 진입 대상 구역 선택 시 해당 위치의 LOTO 체크리스트 로드.
  * 구글 ML Kit 기반 카메라 QR 코드 스캔을 통해 현장 일치 여부 인증 및 사진 촬영 기능 연동.
  * 모든 LOTO 리스트가 체크 완료(`State = READY`)되기 전에는 메인 안전 모드 및 작업 화면 진입을 원천 차단(화면 잠금 및 경고 표시).

### ② GPS ➔ BLE/UWB 하이브리드 위치 추적 및 PDR 역추적
* **기능 설명:** 통신 두절 환경에서 인프라 도움 없이 작업자의 이동 경로를 추적 및 보존하는 기능.
* **상세 로직:**
  * **온라인 상태:** GPS 수신이 가능할 경우 `FusedLocationProviderClient`를 최우선으로 활용.
  * **오프라인(두절) 상태:** 즉시 BLE 스캔 모드로 전환하여 사전에 설치된 고정식 비콘(Beacon) 또는 주변 작업자의 스마트폰 BLE 신호(RSSI)를 수집.
  * **순수 오프라인 내비게이션:** 자이로 및 가속도 센서를 활용한 **추측 항법(PDR)** 알고리즘을 가동하여 작업자의 이동 경로(걸음 수, 회전각)를 폰 내부에 지속적으로 기록. 주요 거점의 BLE/UWB 신호를 만날 때마다 누적 오차 보정(Calibration).

### ③ On-Device AI 기반 낙상 감지 및 피어 투 피어(P2P) 보고
* **기능 설명:** 스마트폰 단독 드롭(False Positive)과 실제 인간 낙상을 구별하여 오진을 최소화하고 비상 상황을 전파하는 기능.
* **상세 로직:**
  * 상시 가동되는 포그라운드 서비스에서 3축 가속도 및 자이로 센서 데이터를 3초 단위 슬라이딩 윈도우 큐(Queue)로 수집.
  * 배터리 절약을 위해 가속도 벡터 합성 값이 특정 임계값(예: 3g) 이상을 치는 순간에만 1차 트리거가 발동하여 내장된 경량 AI 모델(TFLite)로 데이터 윈도우를 전달.
  * AI 추론 결과 진짜 낙상으로 판정(확률 85% 이상)될 경우, 즉시 BLE 백그라운드 브로드캐스팅(`BluetoothLeAdvertiser`)을 가동하여 주변 기기에 비상 플래그와 최후 위치 데이터를 무차별 살포.

### ④ 데드맨 스위치 (Dead Man's Switch)
* **기능 설명:** 작업자가 의식을 상실했으나 낙상 파형이 잡히지 않은 사각지대를 보완하는 타임아웃 시스템.
* **상세 로직:**
  * 외부 관리 기기(노트북 등)와 연동하여 진입 전 가동 예정 시간 설정 후 카운트다운 시작.
  * 제한 시간 만료 시점까지 위치 변화가 없거나, 반응이 없을 경우 스마트폰 화면에 `Full Screen Intent`를 활용한 강제 알림 팝업 현시.
  * 1분 이내에 작업자의 터치 입력이나 물리적 움직임이 감지되지 않으면 내부 상태를 '비상(Emergency)'으로 전환하고 경고 시스템 발동.

### ⑤ 응급 구조 버튼 (SOS Overriding)
* **기능 설명:** 위급 상황 시 작업자가 직관적으로 구조 요청을 보낼 수 있는 기능.
* **상세 로직:**
  * 화면 내 대형 SOS 버튼 터치 외에도 현장 장갑 착용 상태를 고려해 **'볼륨 하(Down) 버튼 3초 이상 길게 누르기'** 물리 버튼 이벤트 가로채기 구현.
  * LTE/Wi-Fi 연결 시 즉시 중앙 서버로 다이렉트 전송, 통신 두절 시 즉시 BLE 브로드캐스팅 모드로 전환하여 주변 사용자에게 메시지 전레이(Mesh 릴레이) 유도.

### BLE 목격자 로그 기반 사후 삼각측량 (Distributed Witness Triangulation)
문제의식: 지금 설계의 SOS/낙상 브로드캐스트는 "사고 발생 순간에 근처에 누가 있어야" 작동. 근처에 아무도 없으면 무용지물.

아이디어: 코로나 시절 접촉 추적 앱처럼, 모든 작업자 폰이 평상시에도 누구를 몇 미터 거리에서 언제 스쳤는지 익명 RSSI 로그를 로컬에 계속 쌓아둠. 사고자가 실종되면, 사고자의 마지막 목격 로그를 여러 사람 폰에서 나중에 회수해 "A가 B를 14:03에 만났고, B가 C를 14:07에 만났다"는 목격 그래프를 재구성해서, 중앙 서버 없이도 사후적으로 실종자의 마지막 이동 벡터를 여러 증인의 교차 데이터로 역산할 수 있음.

기존 산업안전 앱은 "실시간 위치추적"에만 집중하지, "통신이 완전히 끊긴 상태에서 사후 포렌식으로 위치를 복원"하는 개념은 거의 없음. 이건 조선소뿐 아니라 재난구조(건물 붕괴, 실종자 수색)에도 그대로 팔 수 있는 독자적 IP가 됨.
프로토타입 난이도: 낮음. BLE 스캔 로그를 SQLite에 (익명ID, RSSI, timestamp)로 쌓기만 하면 되고, 삼각측량 알고리즘은 오프라인 분석 스크립트로 시연 가능

---

## 3. 시스템 아키텍처 및 AI 이원화 설계 (System Architecture)

리소스가 제한된 스마트폰 환경에서의 상시 모니터링을 위해 **이벤트 구동형 AI 이원화 구조(Dual-Model Edge AI)**를 채택한다.

| AI 구분            | 적용 모델                   | 탑재 엔진                   | 가동 방식                                    | 주요 역할                                                                                   |
| :----------------- | :-------------------------- | :-------------------------- | :------------------------------------------- | :------------------------------------------------------------------------------------------ |
| **초경량 센서 AI** | 1D-CNN 또는 LSTM (1MB 이하) | TensorFlow Lite             | **상시 가동 (24/7)**<br>(Foreground Service) | 가속도/자이로 시계열 패턴 분석을 통한 인간 낙상 여부 이진 분류 (오탐 방지)                  |
| **콘텍스트 LLM**   | Gemma 2B 클래스             | MediaPipe LLM Inference API | **이벤트 가동**<br>(낙상 및 SOS 발동 시)     | LOTO 구역, PDR 최후 경로, 타임아웃 정보를 취합하여 대원용 압축 구조 마니페스트(요약문) 생성 |

---

## 4. 기술 스택 (Technical Stack)

### 📱 Android Client Component
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (디자인 시스템 완벽 대응)
* **Asynchronous Process:** Kotlin Coroutines & Flow (실시간 센서 데이터 스트림 처리)
* **Local Database:** Room DB (LOTO 리스트 및 오프라인 PDR 이동 경로 저장)
* **Background Process:** Foreground Service (Doze 모드 방지 및 상시 센서/BLE 가동)

### 🤖 On-Device AI / 라이브러리
* **Fall Detection:** `org.tensorflow:tensorflow-lite`
* **Context LLM:** `com.google.mediapipe:tasks-genai` (Gemma 2B 인프라 활용)
* **QR Reader:** `com.google.mlkit:barcode-scanning` (LOTO 인증용)

### 📡 하드웨어 제어 API
* **Location:** `FusedLocationProviderClient`, `BluetoothLeScanner`, `BluetoothLeAdvertiser`, `UwbManager`
* **Sensors:** `Sensor.TYPE_ACCELEROMETER`, `Sensor.TYPE_GYROSCOPE`

---

## 5. 개발 및 디자인 워크플로우 명세 (Workflow Specification)

1. **디자인 단계 (Figma):**
   * UI 컴포넌트 간 레이아웃 일관성을 위해 **Auto Layout** 규격을 철저히 준수하여 설계.
   * 각 컴포넌트 네이밍은 안드로이드 리소스 규칙에 부합하도록 명확히 명명 (예: `btn_sos`, `txt_loto_status`).
2. **컨텍스트 주입 단계 (MCP):**
   * 완성된 Figma 가이드라인 및 컴포넌트 구조를 MCP(Model Context Protocol)를 통해 IDE 환경으로 전송.
3. **구현 단계 (Antigravity):**
   * MCP로 주입된 디자인 자원을 기반으로 Jetpack Compose UI 스캐폴딩 우선 생성.
   * 생성된 UI 단에 MVVM(ViewModel) 패턴을 적용하고, 백그라운드에서 동작하는 `SafetyTrackingService`(Foreground Service)와 `StateFlow`로 데이터 레이어를 바인딩하여 최종 비즈니스 로직 완성.