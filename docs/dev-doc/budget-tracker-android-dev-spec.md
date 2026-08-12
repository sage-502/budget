# Budget Tracker Android 개발 명세서

> `budget-tracker-android-plan.md` 기획서를 코드 레벨로 구체화한 문서입니다.
> 기획서가 "무엇을 만들지"를 다룬다면, 이 문서는 "어떻게 구현할지"를 다룹니다.

---

## 01. 개발 환경 설정

| 항목 | 결정 | 비고 |
|---|---|---|
| minSdk | 26 (Android 8.0) | Compose·Room·WorkManager 모두 문제없이 지원되는 하한선. "Android 8~10까지 호환성 우선" 방침에 맞춤 |
| targetSdk | 최신 안정 버전 (앱 출시 시점 기준) | Play 정책 대응, 별도 특이사항 없으면 항상 최신 유지 |
| DI(의존성 주입) | Hilt | Repository·다수 ViewModel·WorkManager(반복 지출)가 서로 얽혀 있어 수동 배선 비용이 큼. 특히 WorkManager는 `@HiltWorker`로 Repository 주입이 깔끔해짐. 안드로이드 개발이 처음이라 초기 러닝커브가 부담되면 수동 DI로 시작 후 전환도 가능 |

## 02. 아키텍처 개요

```
UI (Jetpack Compose)
   ↓ observe
ViewModel
   ↓ call
Repository (interface)
   ↓ implement
Room (DAO)
```

- ViewModel은 Room을 직접 호출하지 않고 Repository를 통해서만 접근한다.
- Repository는 인터페이스로 선언하고, 구현체(`RoomBudgetRepository` 등)를 별도로 둔다.
  나중에 백업 방식이나 데이터 소스가 바뀌어도 구현체만 교체하면 되고 ViewModel/UI는 그대로 둔다.
- DB 변경 감지는 Room의 `Flow` 반환 쿼리를 사용해 UI에 자동 반영한다 (수동 새로고침 불필요).

## 03. 패키지 구조 (제안)

```
com.example.budgettracker
 ├ data
 │   ├ entity        // Category, PaymentMethod, Budget, Transaction, RecurringExpense
 │   ├ dao           // CategoryDao, PaymentMethodDao, BudgetDao, TransactionDao, RecurringExpenseDao
 │   ├ db            // AppDatabase, Migration 정의
 │   └ repository     // Repository 인터페이스 + Room 구현체
 ├ domain
 │   └ usecase        // 반복 지출 처리, 백업 export/import 로직 등 순수 비즈니스 로직
 ├ ui
 │   ├ dashboard        // 카테고리 카드 클릭 시 categorydetail 다이얼로그 호출
 │   ├ categorydetail   // 카테고리별 거래 상세 (바텀시트, 이전 버전 팝업 계승)
 │   ├ add
 │   ├ transactions     // 개별 삭제 시 확인 다이얼로그 포함
 │   └ settings
 │       ├ budget
 │       ├ category
 │       ├ recurring
 │       ├ backup       // 가져오기 확인 + 파싱 실패 에러 처리
 │       └ statistics
 └ worker             // RecurringExpenseWorker (WorkManager)
```

> 이 구조는 기능 단위(반복 지출 / 통계 / 백업)로 폴더가 분리돼 있어, 한 기능을 수정할 때 다른 기능 코드를 건드릴 일이 적다.

## 04. Room 엔티티

```kotlin
@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int
)

@Entity(tableName = "payment_method")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,          // 예: "카드", "현금", "계좌"
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int
)

@Entity(
    tableName = "budget",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"]
    )],
    indices = [Index("categoryId")]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val monthKey: String,      // "2026-08"
    val categoryId: Int,
    val amount: Long
)

@Entity(
    tableName = "transaction",
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"]),
        ForeignKey(entity = PaymentMethod::class, parentColumns = ["id"], childColumns = ["paymentMethodId"]),
        ForeignKey(entity = RecurringExpense::class, parentColumns = ["id"], childColumns = ["recurringId"])
    ],
    indices = [Index("categoryId"), Index("paymentMethodId"), Index("date")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Long,
    val categoryId: Int,
    val paymentMethodId: Int,
    val date: Long,            // timestamp
    val memo: String,
    val recurringId: Int? = null   // 반복 지출로 생성된 경우 출처
)

@Entity(
    tableName = "recurring_expense",
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"]),
        ForeignKey(entity = PaymentMethod::class, parentColumns = ["id"], childColumns = ["paymentMethodId"])
    ],
    indices = [Index("categoryId"), Index("paymentMethodId")]
)
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val paymentMethodId: Int,
    val amount: Long,
    val dayOfMonth: Int,       // 결제일 (사용자 지정)
    val memo: String,
    val isActive: Boolean
)
```

**설계 결정 사항**

| 항목 | 결정 | 이유 |
|---|---|---|
| `Transaction.date` 타입 | `Long` (timestamp) | 기간 필터·정렬·통계 쿼리가 문자열보다 단순해짐 |
| 결제수단 | `PaymentMethod` 테이블로 분리 | 카테고리처럼 사용자 커스텀 가능하게 |
| 월(monthKey) 저장 방식 | Transaction에는 저장하지 않고 조회 시 `date` 범위로 계산 | 컬럼 중복 제거로 날짜 수정 시 동기화 버그 방지. 1인용 앱 데이터량 기준 성능 문제 없음 |
| 카테고리/결제수단 삭제 | 물리 삭제 대신 `isActive = false` | 과거 거래의 FK 무결성 보존, 통계 왜곡 방지 |

## 05. DAO

Transaction 기준 예시 (나머지 DAO는 동일 패턴):

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transaction` WHERE date BETWEEN :monthStart AND :monthEnd ORDER BY date DESC")
    fun getByMonth(monthStart: Long, monthEnd: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM `transaction` WHERE categoryId = :categoryId AND date BETWEEN :monthStart AND :monthEnd")
    fun getCategorySum(categoryId: Int, monthStart: Long, monthEnd: Long): Flow<Long?>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
```

동일 패턴 적용 대상:
- `CategoryDao` — `getActiveCategories()`, `insert/update`, 삭제는 `isActive` 업데이트로 대체 (물리 delete 쿼리 두지 않음)
- `PaymentMethodDao` — Category와 동일 패턴
- `BudgetDao` — `getByMonth(monthKey)`, `upsert`
- `RecurringExpenseDao` — `getActiveByDayOfMonth(day)` — WorkManager가 매일 실행 시 오늘 날짜에 해당하는 반복 지출만 조회

## 06. 초기 데이터 시딩

앱 최초 실행 시 `Category`, `PaymentMethod`, `Budget` 테이블에 기본값을 미리 채워 넣는다. Room의 `RoomDatabase.Callback.onCreate()`에서 DB 최초 생성 시점에 1회 삽입한다 (앱 재실행 시 중복 삽입 방지).

**기본 카테고리 7종** (이전 버전 카테고리 유지, 표시 이름만 한글화)

| id 순서 | name | 이전 버전 대응 | 기본 예산 |
|---|---|---|---|
| 1 | 식비 | food | 300,000 |
| 2 | 주거 | house | 400,000 |
| 3 | 교통 | transport | 100,000 |
| 4 | 건강 | health | 30,000 |
| 5 | 쇼핑 | shopping | 150,000 |
| 6 | 여가 | entertainment | 80,000 |
| 7 | 기타 | etc | 50,000 |

모두 `isDefault = true`, `isActive = true`로 삽입한다.

**기본 결제수단 3종** (이전 버전과 동일, 표시 이름만 한글화)

| id 순서 | name | 이전 버전 대응 |
|---|---|---|
| 1 | 카드 | card |
| 2 | 현금 | cash |
| 3 | 계좌 | account |

**월별 Budget 초기화**: 이전 버전의 `ensureMonth()`와 동일하게, 새 월이 처음 조회될 때 해당 월에 `Budget` 레코드가 없는 카테고리에 한해 위 기본 예산으로 자동 생성한다. 이 로직은 `BudgetRepository`에 `ensureMonth(monthKey)` 형태로 옮겨 구현한다.

## 07. Database 및 마이그레이션 정책

```kotlin
@Database(
    entities = [Category::class, PaymentMethod::class, Budget::class, Transaction::class, RecurringExpense::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
}
```

**필수 규칙**

- 스키마 변경(컬럼 추가/테이블 추가 등) 시 반드시 `version`을 올리고 `Migration` 객체를 작성한다.
- `fallbackToDestructiveMigration()`은 사용 금지. 이걸 쓰면 마이그레이션 누락 시 앱이 기존 DB를 통째로 지우고 새로 만든다 — 예전 localStorage 사고와 동일한 유형의 데이터 손실이 재발할 수 있다.
- 마이그레이션 코드는 `data/db/migrations` 폴더에 버전별로 파일을 분리해 보관한다 (예: `Migration_1_2.kt`).

## 08. 반복 지출 처리 (WorkManager)

- `RecurringExpenseWorker`가 하루 1회(자정 직후) 실행된다.
- 오늘 날짜(`dayOfMonth`)와 일치하고 `isActive = true`인 `RecurringExpense`를 조회한다.
- 각 항목에 대해 `Transaction`을 생성하고 `recurringId`에 출처를 기록한다.
- 중복 생성 방지: 이번 달에 해당 `recurringId`로 이미 생성된 Transaction이 있는지 확인 후 없을 때만 생성한다.

## 09. 백업/복원 (SAF 기반, 가져오기 확인 절차)

Android 10(API 29)부터 적용된 Scoped Storage 정책으로 인해, 앱이 임의 경로에 직접 파일을 쓰거나 읽을 수 없다. 이전 웹앱은 브라우저 다운로드로 처리했지만, 네이티브 앱에서는 **Storage Access Framework(SAF)** 를 사용해 시스템 파일 선택창을 통해서만 파일에 접근한다. 별도의 런타임 저장소 권한 요청은 필요 없다.

- **내보내기(Export)**: `ActivityResultContracts.CreateDocument`로 시스템 파일 저장 다이얼로그를 띄운다. 사용자가 저장 위치(기기 저장소, 다운로드, 연결된 클라우드 앱 등)와 파일명을 직접 선택하며, 앱은 반환된 URI에만 쓴다. 현재 월의 Budget + Transaction을 JSON으로 직렬화해 저장한다.
- **가져오기(Import)**: `ActivityResultContracts.OpenDocument`로 시스템 파일 선택 다이얼로그를 띄운다. 사용자가 고른 파일의 URI를 받아 읽는다. 파일 선택 후 → **"현재 월 데이터를 덮어씁니다. 계속할까요?" 확인 다이얼로그** → 확인 시에만 기존 데이터 삭제 후 반영.
  (이전 버전의 최대 취약점이었던 "확인 없이 즉시 덮어쓰기"를 보완하는 부분이므로 구현 시 누락 주의)
- **파싱 실패 처리**: 가져오기 파일이 JSON 형식이 아니거나 스키마가 맞지 않으면 `try/catch`로 예외를 잡아 "파일을 읽을 수 없습니다" 등 사용자 알림을 띄우고 기존 데이터는 그대로 둔다. 이전 버전은 `JSON.parse()` 실패에 대한 처리가 전혀 없어 잘못된 파일을 선택하면 곧바로 오류로 이어졌다.

## 10. 앱 아이콘

이전 PWA 아이콘(`icon-192.png`, `icon-512.png`, 회색 배경(`#3F3F3F`) + 흰색 달러 사인)의 디자인은 그대로 유지하고, 안드로이드 adaptive icon 규격(배경/전경 분리, 런처별 마스크 대응)에 맞게 포맷만 재구성했다.

- **컨셉**: 기존과 동일 — 회색(`#3F3F3F`) 배경 + 흰색 `$` 심볼.
- **구성 파일**:
  - `ic_launcher_background` (432×432, xxxhdpi 기준) — 배경 레이어
  - `ic_launcher_foreground` (432×432, 투명 배경) — `$` 심볼, 안전 영역(반경 33dp) 안에 배치해 원형/사각형 등 어떤 런처 마스크로 잘려도 잘리지 않게 함
  - `play_store_icon` (512×512, 정사각형 full bleed) — Play 스토어 등록용, 마스크 없이 그대로 노출
  - `ic_launcher_legacy` (512×512, 모서리 둥글게 처리) — Android 8.0 미만 구형 런처 대응용 단일 아이콘
- **적용 방법**: Android Studio의 Image Asset Studio에 `ic_launcher_background` + `ic_launcher_foreground` 쌍을 입력하면 `mipmap-anydpi-v26`(adaptive) 및 각 밀도별(mdpi~xxxhdpi) 레거시 아이콘이 자동 생성된다.

## 11. 거래 삭제 확인 및 카테고리 상세 화면

- **개별 거래 삭제**: Transactions 화면에서 항목 삭제 시 확인 다이얼로그를 띄운다. 이전 버전은 월 전체 삭제에만 확인 절차가 있고 개별 거래 삭제는 확인 없이 즉시 지워졌다 — 이 비대칭을 없앤다.
- **Transactions 목록 항목 UI**: 각 항목에 카테고리·결제수단·날짜와 함께 메모를 표시한다. 메모가 비어 있으면 "(메모 없음)"으로 구분해 보여준다. 항목마다 수정/삭제 아이콘을 노출하며, 수정은 Add 화면으로 이동해 기존 값을 채운 채 편집하는 기존 흐름을 유지한다.
- **카테고리 상세 조회**: Dashboard의 카테고리 카드를 클릭하면 화면 하단에서 올라오는 바텀시트로 해당 카테고리의 이번 달 합계와 거래 목록(날짜·결제수단·금액·메모)을 보여준다. 이전 버전의 "카테고리 팝업" 기능을 그대로 계승하되, UI만 네이티브 바텀시트로 대체하고 합계 표시를 추가한다. 조회 전용으로 유지하며, 하단에 "조회 전용 · 수정/삭제는 내역 화면에서" 안내 문구를 두고 수정/삭제가 필요하면 Transactions 화면으로 이동하도록 안내한다.

## 12. Settings 하위 화면 UI 상세

와이어프레임 검토를 통해 확정된 각 하위 화면의 UI 디테일.

- **예산 설정**: 카테고리별 입력 필드 목록 + "예산 저장" 버튼. 하단에 "이번 달 데이터 삭제"는 위험 동작이므로 빨간 테두리(Material 3 `colorScheme.error`)로 다른 버튼과 시각적으로 구분한다.
- **카테고리 관리**: 각 항목에 드래그 핸들 아이콘을 두어 순서 변경(= `sortOrder` 갱신)이 가능하게 한다. 항목마다 편집/비활성화 아이콘을 노출한다. 비활성화된(`isActive = false`) 카테고리는 목록에서 흐리게(`Modifier.alpha(0.5f)`) 표시하고, 편집 아이콘 대신 "복구" 아이콘만 남긴다.
- **반복 지출**: 목록 각 항목에 카테고리·금액·결제일을 한 줄에서 바로 확인할 수 있게 구성한다. 상단에 + 버튼으로 등록 화면 진입.
- **백업/복원**: 내보내기/가져오기 버튼 아래에 "가져오기 시 현재 월 데이터를 덮어씁니다" 경고 배너(주황/노랑 계열 경고색 — Material 3 `colorScheme.errorContainer` 또는 커스텀 warning 색상)를 상시 노출해, 실제 가져오기 확인 다이얼로그가 뜨기 전부터 위험을 인지시킨다.

## 13. 입력 검증 규칙 (Add 화면)

| 항목 | 규칙 | 비고 |
|---|---|---|
| 금액 | 별도 하한 제한 없음 (0 이하도 허용) | 환불·정정 등 음수/0 입력이 필요한 경우를 막지 않기 위해 제한을 두지 않음 |
| 카테고리 | 필수, 활성 카테고리 중 선택 | 미선택 시 저장 버튼 비활성화 |
| 결제수단 | 필수, 활성 결제수단 중 선택 | 미선택 시 저장 버튼 비활성화 |
| 날짜 | 필수, 기본값은 오늘 날짜 | 이전 버전과 동일하게 화면 진입 시 자동 채움 |
| 메모 | 선택, 비워두면 빈 문자열로 저장 | 이전 버전과 동일하게 필수 아님 |

이 규칙은 반복 지출 등록 폼(`RecurringExpense`)에도 동일하게 적용한다 (단, 날짜 대신 `dayOfMonth` 필수 선택).

## 14. 통계 화면

- Settings > 통계에서 **월별 총지출 vs 총예산**을 막대 그래프로 표시한다 (카테고리별 분리 아님 — 카테고리별 비교는 Dashboard에서 이미 제공하므로, 이 화면은 월 단위 전체 추이 확인용).
- **기간 선택**: 3개월 / 6개월 / 12개월 세그먼트 컨트롤로 조회 범위를 사용자가 직접 전환할 수 있게 한다. 기본값은 6개월.
- **예산 초과 표시**: 각 월 막대는 해당 월 총예산까지는 기본 색상으로, 예산을 넘은 만큼은 막대 상단에 빨간색으로 이어붙여 표시한다 (스택형 막대). 초과분이 없는 달은 빨간 구간이 아예 생략된다. 범례에 "예산 초과분" 표기.
- 데이터 소스: `TransactionDao`에 월별 총합 range 쿼리를 추가하고, `BudgetDao`에서 해당 월 총예산(카테고리별 예산 합계)을 조회해 두 값을 ViewModel에서 결합한다. 기간(3/6/12개월) 만큼 반복 조회하거나, 월 범위를 파라미터로 받는 집계 쿼리로 한 번에 가져오는 방식 중 택 1.
  *(추천은 후자 — 쿼리 한 번으로 가져오는 게 코드가 단순함. 다만 이 부분은 구현 시 실제 성능 보고 결정해도 무방)*
- 그래프 라이브러리는 별도 명시하지 않음 — Compose 기반 차트 라이브러리(예: Vico) 사용을 검토 가능. 스택형 막대(예산 이하/초과분 2단 색상)를 지원하는지 확인 필요.
- **참고**: 이 화면 설계는 와이어프레임 기준으로 확정한 초안이며, 실제 구현 중 카테고리별 통계가 필요하다고 판단되면 재논의 대상이다.

## 15. 배포 방식

개인/가족용 앱이라 Play 스토어 정식 공개는 필요 없다. **Google Play 내부 테스트(Internal Testing) 트랙**으로 배포한다.

| 항목 | 결정 | 비고 |
|---|---|---|
| 배포 방식 | Google Play 내부 테스트 (Internal Testing) | 스토어에서 앱 받는 것과 동일한 사용자 경험(설치 경고 없음, 자동 업데이트) |
| 개발자 등록 | Google Play 개발자 계정 등록, $25 1회 결제 | 평생 1회, 연 갱신 없음. 신분증 인증 절차 별도 소요 (수 시간~영업일 2일) |
| 비공개 테스트 12명/14일 규칙 | 해당 없음 | 이 규칙은 프로덕션(정식 공개) 전환 시에만 적용됨. 내부 테스트 트랙은 이 요건 없이 바로 배포 가능 |
| 테스터 등록 | 본인 + 어머니 계정 이메일을 내부 테스트 목록에 추가 | 테스터는 초대 링크로 "테스터 참여" 후 스토어 앱처럼 설치 |

**대안(고려했으나 채택 안 함)**: APK 파일 직접 전달, Firebase App Distribution — 둘 다 "출처를 알 수 없는 앱" 설치 경고를 거쳐야 해 비개발자 사용자(어머니)에게는 진입장벽이 있음. 업데이트도 파일을 매번 재전달해야 하는 번거로움이 있어 배제.

## 16. 테스트 전략

1인 개인용 앱 규모에 맞춰 전체 커버리지보다 "데이터 손실 재발 방지"라는 프로젝트 목적에 직결되는 부분 위주로 작성한다.

**우선순위 높음**

- **Room DAO 단위 테스트**: in-memory Room DB로 CRUD 및 `getByMonth`(날짜 범위 조회), `getCategorySum`(집계) 등 쿼리의 정확성을 검증한다. 이 쿼리들이 틀리면 예산·통계 전체가 틀어진다.
- **Migration 테스트**: Room의 `MigrationTestHelper`로 스키마 버전을 올릴 때마다 기존 데이터가 유실 없이 이관되는지 검증한다. `fallbackToDestructiveMigration()` 금지 원칙(07번)이 실제로 지켜지는지 확인하는 테스트이므로, 이 프로젝트의 핵심 목적과 가장 직결된 항목이다.

**우선순위 중간**

- **반복 지출 생성 로직 테스트**: `RecurringExpenseWorker`의 중복 생성 방지 로직(이번 달에 이미 생성된 경우 스킵)이 의도대로 동작하는지 검증.
- **백업 가져오기 파싱 테스트**: 정상 JSON, 형식이 깨진 JSON, 스키마가 다른 JSON을 각각 입력했을 때 에러 처리가 의도대로 동작하는지 검증.

**우선순위 낮음 (생략 가능)**

- Compose UI 테스트: 개인용 앱이라 사용하면서 바로 육안 확인이 가능해 자동화 UI 테스트까지는 생략한다. 기능이 늘어나 회귀 위험이 커지면 그때 추가한다.

## 17. 마이그레이션 (기존 localStorage 데이터 이전)

기획서 08번과 동일 — 기존 JSON export 파일이 있다면 이를 파싱해 Category/Budget/Transaction으로 분해 저장하는 1회성 임포터를 별도 유스케이스(`domain/usecase`)로 구현한다. **단, 이번 사고로 기존 데이터가 이미 유실됐다면 이 항목 자체가 해당 없음 — 우선순위 최하위.**

혹시 남아있는 백업 JSON 파일이 있을 경우를 대비한 필드 매핑은 다음과 같다.

| 기존 웹앱 JSON 필드 | 신규 Room 필드 | 변환 방법 |
|---|---|---|
| `budgets.food` / `.house` / `.transport` / `.health` / `.shopping` / `.entertainment` / `.etc` (키) | `Budget.categoryId` | 문자열 키를 06번 기본 카테고리 시딩 테이블의 이름 매핑을 통해 해당 `Category.id`로 치환 |
| `budgets.{category}` (값) | `Budget.amount` | 숫자 그대로 이관 |
| `transactions[].id` | 사용 안 함 | 신규 `Transaction.id`는 Room이 자동 생성(autoGenerate)하므로 기존 `Date.now()` 기반 id는 버림 |
| `transactions[].amount` | `Transaction.amount` | 숫자 그대로 이관 |
| `transactions[].category` (문자열: `"food"` 등) | `Transaction.categoryId` | 위와 동일하게 이름 매핑으로 `Category.id` 치환 |
| `transactions[].payment` (문자열: `"card"`/`"cash"`/`"account"`) | `Transaction.paymentMethodId` | 06번 기본 결제수단 시딩 테이블 이름 매핑으로 `PaymentMethod.id` 치환 |
| `transactions[].date` (문자열 `"YYYY-MM-DD"`) | `Transaction.date` (`Long` timestamp) | 문자열을 파싱해 timestamp로 변환 (예: `LocalDate.parse(...).toEpochDay()` 기반 변환) |
| `transactions[].memo` | `Transaction.memo` | 문자열 그대로 이관, null이면 빈 문자열로 |

임포트 대상 JSON에 없는 필드(`recurringId`, `isActive` 등 신규 개념)는 전부 기본값(`null`, `true`)으로 채운다. 가져오기 UI는 09번(백업/복원)의 확인 다이얼로그·파싱 실패 처리 로직을 그대로 재사용한다.

---
*Budget Tracker Android 개발 명세서 · 2026-08-12*
