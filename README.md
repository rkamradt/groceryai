# GroceryAI – Weekly Nutrition Tracker

Track your weekly grocery purchases and automatically look up nutrition data
from the USDA FoodData Central database.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Browser                                                                │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  nutrition-ui  (React 18 + Vite, port 5173)                     │   │
│  │                                                                  │   │
│  │  /                  WeekSelector   ──── list / create weeks      │   │
│  │  /entry/:weekId     ItemEntry      ──── add grocery items        │   │
│  │  /report/:weekId    WeeklyReport   ──── charts + sortable table  │   │
│  └──────────────────────────┬───────────────────────────────────────┘  │
│                             │ fetch /api/*                             │
└─────────────────────────────┼───────────────────────────────────────────┘
                              │ (dev: Vite proxy; prod: configure reverse proxy)
┌─────────────────────────────▼───────────────────────────────────────────┐
│  groceryai  (Spring Boot 3, port 8080)                                  │
│                                                                         │
│  WeekController    → WeekService    → WeekRepository (JPA)             │
│  ItemController    → ItemService    → GroceryItemRepository (JPA)      │
│                         │                                               │
│                         └── UsdaFoodService  ──► USDA FoodData Central │
│  NutritionController → NutritionService                                │
│                                                                         │
│  Database: H2 in-memory (dev)  │  API envelope: ApiResponse<T>         │
└─────────────────────────────────────────────────────────────────────────┘
                                         │
                           ┌─────────────▼───────────────┐
                           │  USDA FoodData Central API   │
                           │  api.nal.usda.gov/fdc/v1     │
                           └─────────────────────────────┘
```

---

## Getting a USDA FoodData Central API Key

1. Visit **https://api.nal.usda.gov** and click **Sign Up / Get an API Key**.
2. Fill in your name and email. You will receive your key by email immediately.
3. **DEMO_KEY** is available for testing without registration (30 req/hr,
   50 req/day per IP). The app defaults to `DEMO_KEY` when no key is set.

Set your key before starting the backend:

```bash
export USDA_API_KEY=your_key_here
```

Or add it to `src/main/resources/application-local.properties`:

```properties
usda.api.key=your_key_here
```

---

## Running the Backend

**Prerequisites:** Java 21, Maven 3.9+

```bash
# From the repo root
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

H2 console (for dev inspection): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:groceryai`
- Username: `sa` / Password: *(empty)*

---

## Running the Frontend

**Prerequisites:** Node.js 20+

```bash
cd nutrition-ui
npm install
npm run dev
```

The UI will be available at `http://localhost:5173`.  
The Vite dev-server proxies all `/api` calls to `http://localhost:8080`.

---

## API Reference

All responses use the envelope:

```json
{ "data": <T>, "status": "ok" | "error", "message": "..." }
```

| Method | Path                        | Description                        |
|--------|-----------------------------|------------------------------------|
| GET    | `/api/weeks`                | List all weeks (newest first)      |
| POST   | `/api/weeks`                | Create a new week                  |
| GET    | `/api/weeks/{id}`           | Get a single week                  |
| POST   | `/api/items`                | Save items for a week (replaces)   |
| GET    | `/api/nutrition/{weekId}`   | Full nutrition report              |
| GET    | `/api/nutrition/{weekId}/pdf` | PDF export (stub)                |

### POST /api/weeks — request body

```json
{ "startDate": "2026-05-01", "people": 2 }
```

### POST /api/items — request body

```json
{
  "weekId": "uuid",
  "items": [
    { "name": "Chicken breast", "category": "Proteins (Meat / Fish / Poultry)",
      "servingSizeG": 150, "servingsPerWeek": 7 }
  ]
}
```

---

## Project Structure

```
groceryai/
├── pom.xml                          Spring Boot Maven project
├── src/
│   └── main/
│       ├── java/com/rkamradt/groceryai/
│       │   ├── GroceryAiApplication.java
│       │   ├── config/
│       │   │   └── WebConfig.java   CORS configuration
│       │   ├── controller/
│       │   │   ├── WeekController.java
│       │   │   ├── ItemController.java
│       │   │   └── NutritionController.java
│       │   ├── dto/
│       │   │   ├── ApiResponse.java
│       │   │   ├── WeekDto.java
│       │   │   ├── WeekCreateRequest.java
│       │   │   ├── GroceryItemRequest.java
│       │   │   ├── ItemsRequest.java
│       │   │   ├── NutritionReportDto.java
│       │   │   ├── NutritionSummaryDto.java
│       │   │   ├── CategoryCaloriesDto.java
│       │   │   └── ItemNutritionDto.java
│       │   ├── model/
│       │   │   ├── Week.java
│       │   │   └── GroceryItem.java
│       │   ├── repository/
│       │   │   ├── WeekRepository.java
│       │   │   └── GroceryItemRepository.java
│       │   └── service/
│       │       ├── UsdaFoodService.java
│       │       ├── WeekService.java
│       │       ├── ItemService.java
│       │       └── NutritionService.java
│       └── resources/
│           └── application.properties
└── nutrition-ui/                    React 18 + Vite frontend
    ├── package.json
    ├── vite.config.js
    ├── index.html
    ├── .env.example
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── App.module.css
        ├── index.css
        ├── api/
        │   └── nutritionApi.js
        └── pages/
            ├── WeekSelector.jsx / .module.css
            ├── ItemEntry.jsx    / .module.css
            └── WeeklyReport.jsx / .module.css
```

---

## Design Conventions

- **No Lombok `@Data`** on JPA entities — explicit getters/setters only
- **`RestClient`** (not `RestTemplate`) for HTTP calls
- **Constructor injection** throughout — no `@Autowired` on fields
- Service layer between controller and repository
- All responses wrapped in `ApiResponse<T>`
- Plain CSS Modules — no component library; design tokens in `:root`
