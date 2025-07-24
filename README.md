# Aracanix Analyseplattform



Velkommen til **Aracanix**, en fullstack analyse- og beslutningsstøtteplattform utviklet som et omfattende læringsprosjekt. Målet er å utforske hele livssyklusen til data – fra innsamling og prosessering til avansert analyse med maskinlæring – for å identifisere potensielle "edges" i datadrevne markeder.

Dette prosjektet er delt inn i tre separate repositories som utgjør en komplett, fungerende applikasjon:

*   **Frontend:** [martingit2/AiPortal-Frontend](https://github.com/martingit2/AiPortal-Frontend) 
*   **Backend:** [martingit2/AiPortal-Backend](https://github.com/martingit2/AiPortal-Backend) (Denne repoen)
*   **ML Service:** [martingit2/AiPortal-ML-Service](https://github.com/martingit2/AiPortal-ML-Service)

---

## Innholdsfortegnelse

- [Om Prosjektet](#om-prosjektet)
    - [Prosjektstatus og Formål](#prosjektstatus-og-formål)
    - [Hovedfunksjoner](#hovedfunksjoner)
    - [Teknologistack](#teknologistack)
- [Visuell Oversikt](#visuell-oversikt)
- [Prosjektstruktur](#prosjektstruktur)
    - [Frontend (`aracanix-frontend`)](#frontend-aracanix-frontend-1)
    - [Backend (`Aracanix-Backend`)](#backend-aracanix-backend-1)
    - [ML Service (`aracanix-ml-service`)](#ml-service-aracanix-ml-service-1)
- [Komme i Gang](#komme-i-gang)
    - [Forutsetninger](#forutsetninger)
    - [Installasjon og Kjøring](#installasjon-og-kjøring)
- [Lisens](#lisens)

---

## Om Prosjektet

Per i dag er plattformen primært fokusert på **sportsanalyse og betting**, men arkitekturen er designet for å være modulær og utvidbar til andre domener som aksjer og krypto i fremtiden.

### Prosjektstatus og Formål

**Dette er et aktivt læringsprosjekt under utvikling.** Hensikten er å bygge og forstå en komplett, moderne systemarkitektur.

*   **Ikke alle funksjoner er implementert:** Funksjoner som "Modeller"-siden, "Innstillinger", og analyse for aksjer/krypto er foreløpig kun plassholdere som representerer fremtidsvisjonen for prosjektet.
*   **Modellene er for demonstrasjon:** Prediksjonsmodellene er ment som en "proof-of-concept" for den tekniske pipelinen og er **ikke** presise eller pålitelige nok for reelle finansielle beslutninger.

### Hovedfunksjoner (Implementert)

-   **Avansert Datainnhenting:** En robust, kø-basert backend i Java (Spring Boot) orkestrerer datainnhenting fra flere eksterne API-er, inkludert kampdata, spillerstatistikk, odds og sosiale medier (Twitter).
-   **Maskinlærings-pipeline:** En dedikert mikrotjeneste i Python (Flask/XGBoost) trener og serverer prediksjonsmodeller for ulike markeder, som kampvinner og Over/Under, beriket med Head-to-Head (H2H) data.
-   **Interaktivt Dashboard:** Et moderne React-dashboard lar brukeren administrere datainnsamlere ("boter"), utforske innsamlet data og visualisere resultatene av analysene.
-   **Dynamisk Datavisning:** Viser komplekse data som ligatabeller, kampresultater, spillerstatistikk og H2H-data på en intuitiv og oversiktlig måte.
-   **Sikker Autentisering:** Full bruker- og sesjonshåndtering er implementert med Clerk.

### Teknologistack

-   **Frontend:** React, TypeScript, Vite, React Router, Recharts, CSS Modules.
-   **Backend:** Java 21, Spring Boot, Spring Data JPA, Spring Security (OAuth2), WebClient, Project Reactor.
-   **ML Service:** Python, Flask, Pandas, Scikit-learn, XGBoost.
-   **Database:** PostgreSQL (hostet på Supabase).
-   **Autentisering:** Clerk.

---

## Visuell Oversikt

Her er noen glimt fra plattformens ulike funksjoner, som viser administrasjon av datakilder, analyse av resultater og visualisering av data.

Her er noen glimt fra plattformens ulike funksjoner, som viser administrasjon av datakilder, analyse av resultater og visualisering av data.

| Bot-administrasjon (CRUD) | Ligatabeller med Drill-Down |
| :----------------------------------------------------------: | :----------------------------------------------------------: |
| ![Bilde av bot-administrasjonssiden](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/boter.png) | ![Bilde av fotball-stats siden](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/fotball-stats.png) |
| **Oddsanalyse & Verdispill**                             | **Detaljert Spillerstatistikk**                     |
| ![Bilde av oddsanalyse-siden](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/odds-analyse.png)        | ![Bilde av spillerstatistikk-modalen](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/spiller-stats.png) |
| **Interaktiv Form-analyse**                                         | **Oversikt over Tilgjengelig Odds**                               |
| ![Bilde av lagdetaljer-siden med graf](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/lagdetaljer.png) | ![Bilde av odds-oversikt modalen](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/odds.png) |
| **Porteføljeoversikt** | **Administrasjon av Porteføljespill** |
| ![Bilde av porteføljeoversikt](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/portfolio.png) | ![Bilde av spesifikke spill i porteføljen](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/portfoliobets.png) |
| **Prediksjonsmodeller** | **Utvidet Odds-visning** |
| ![Bilde av oversikt over modeller](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/modeller.png) | ![Bilde av utvidet odds-visning](https://raw.githubusercontent.com/martingit2/AiPortal-Frontend/main/src/bilder/merodds.png) |

---

## Prosjektstruktur

### Frontend (`aracanix-frontend`)

```
src
├── App.css
├── App.tsx
├── index.css
├── main.tsx
├── bilder/
│   ├── DATA1.png
│   └── DATA2.png
├── components/
│   ├── CreateBotModal.css
│   ├── CreateBotModal.tsx
│   ├── MatchStatsModal.css
│   ├── MatchStatsModal.tsx
│   ├── OddsDetailModal.css
│   ├── OddsDetailModal.tsx
│   ├── Searchbar.css
│   ├── Searchbar.tsx
│   ├── Sidebar.css
│   ├── Sidebar.tsx
│   ├── TeamFormChart.css
│   └── TeamFormChart.tsx
├── layouts/
│   ├── DashboardLayout.css
│   └── DashboardLayout.tsx
├── pages/
│   ├── LandingPage.css
│   ├── LandingPage.tsx
│   └── dashboard/
│       ├── AnalysesPage.css
│       ├── AnalysesPage.tsx
│       ├── BotsPage.css
│       ├── BotsPage.tsx
│       ├── DashboardPage.css
│       ├── DashboardPage.tsx
│       ├── DataFeedPage.css
│       ├── DataFeedPage.tsx
│       ├── FixturesPage.css
│       ├── FixturesPage.tsx
│       ├── FootballStatsPage.css
│       ├── FootballStatsPage.tsx
│       ├── OddsAnalysisPage.css
│       ├── OddsAnalysisPage.tsx
│       ├── TeamDetailsPage.css
│       ├── TeamDetailsPage.tsx
│       ├── UpcomingOddsPage.css
│       └── UpcomingOddsPage.tsx
└── types/
    └── index.ts
```

### Backend (`Aracanix-Backend`)

```
src/main/java/com/AiPortal
├── Demo1Application.java
├── config/
│   ├── DataSourceConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AdminController.java
│   ├── AnalysisController.java
│   ├── BotController.java
│   ├── FixtureController.java
│   ├── HelloController.java
│   ├── StatisticsController.java
│   ├── TrainingDataController.java
│   ├── TweetController.java
│   └── ValueBetsController.java
├── dto/
│   ├── HeadToHeadStatsDto.java
│   ├── LeagueStatsGroupDto.java
│   ├── MatchOddsDto.java
│   ├── MatchStatisticsDto.java
│   ├── ParsedTweetResponse.java
│   ├── PlayerMatchStatisticsDto.java
│   ├── TeamDetailsDto.java
│   ├── TeamStatisticsDto.java
│   ├── TrainingDataDto.java
│   ├── TweetDto.java
│   ├── UpcomingFixtureDto.java
│   └── ValueBetDto.java
├── entity/
│   ├── Analysis.java
│   ├── BetType.java
│   ├── Bookmaker.java
│   ├── BotConfiguration.java
│   ├── Fixture.java
│   ├── HeadToHeadStats.java
│   ├── Injury.java
│   ├── League.java
│   ├── MatchOdds.java
│   ├── MatchStatistics.java
│   ├── PendingFixtureChunk.java
│   ├── Player.java
│   ├── PlayerMatchStatistics.java
│   ├── RawTweetData.java
│   ├── TeamStatistics.java
│   ├── TestEntity.java
│   └── TwitterQueryState.java
├── repository/
│   ├── AnalysisRepository.java
│   ├── BetTypeRepository.java
│   ├── BookmakerRepository.java
│   ├── BotConfigurationRepository.java
│   ├── FixtureRepository.java
│   ├── HeadToHeadStatsRepository.java
│   ├── InjuryRepository.java
│   ├── LeagueRepository.java
│   ├── MatchOddsRepository.java
│   ├── MatchStatisticsRepository.java
│   ├── PendingFixtureChunkRepository.java
│   ├── PlayerMatchStatisticsRepository.java
│   ├── PlayerRepository.java
│   ├── RawTweetDataRepository.java
│   ├── TeamStatisticsRepository.java
│   ├── TestRepository.java
│   └── TwitterQueryStateRepository.java
└── service/
    ├── AnalysisService.java
    ├── BotConfigurationService.java
    ├── FixtureService.java
    ├── FootballApiService.java
    ├── HistoricalDataWorker.java
    ├── OddsCalculationService.java
    ├── PinnacleApiService.java
    ├── PredictionService.java
    ├── ScheduledBotRunner.java
    ├── StatisticsService.java
    ├── TestService.java
    ├── TrainingDataService.java
    ├── TweetService.java
    └── twitter/
        ├── OfficialTwitterService.java
        ├── TwitterApi45Service.java
        ├── TwitterServiceManager.java
        ├── TwitterServiceProvider.java
        └── TwttrApi241Service.java
```

### ML Service (`aracanix-ml-service`)

```
aracanix-ml-service/
├── app.py
├── train_model.py
├── train_over_under_model.py
├── football_predictor_v5_h2h.joblib
├── over_under_v2_h2h.joblib
├── result_encoder_v5.joblib
└── requirements.txt
```

---

## Komme i Gang

### Forutsetninger

-   Node.js (v18+)
-   Java JDK 21+ & Maven 3.8+
-   Python 3.9+ & `pip`
-   Aktive API-nøkler for api-sports.io, Pinnacle, og Twitter.
-   En Clerk-konto og tilhørende nøkler.
-   En Supabase-konto for PostgreSQL-databasen.

### Installasjon og Kjøring

1.  **Backend (`AiPortal-Backend`):**
    *   Oppdater `src/main/resources/application.properties` med dine database-credentials og API-nøkler.
    *   Kjør `mvn spring-boot:run` fra rotmappen.

2.  **ML Service (`AiPortal-ML-Service`):**
    *   Opprett et virtuelt miljø: `python -m venv .venv` og aktiver det.
    *   Installer avhengigheter: `pip install -r requirements.txt`.
    *   Kjør `python app.py` for å starte API-serveren.
    *   *Merk: Du må kjøre `train_model.py` og `train_over_under_model.py` for å generere modellfilene første gang.*

3.  **Frontend (`AiPortal-Frontend`):**
    *   Opprett en `.env.local`-fil i rotmappen med `VITE_CLERK_PUBLISHABLE_KEY=din_key_her`.
    *   Kjør `npm install` og deretter `npm run dev`.

Applikasjonen vil være tilgjengelig på `http://localhost:5173`.

---

## Lisens

Distribuert under MIT-lisensen.