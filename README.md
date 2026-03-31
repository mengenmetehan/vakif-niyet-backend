# Vakit Niyet Backend

Spring Boot 3.3 + Kotlin + PostgreSQL + Redis

## Lokal Kurulum

### 1. Gereksinimler
- JDK 21
- Docker Desktop
- IntelliJ IDEA

### 2. Veritabanı Başlat
```bash
docker-compose up -d
```

### 3. Environment Variables
```bash
cp .env.example .env
```

IntelliJ'de: Run Configuration → Environment Variables → .env dosyasını ekle

### 4. Çalıştır
```bash
./gradlew bootRun
```

Uygulama `http://localhost:8080` adresinde çalışır.

## API Endpoints

### Auth
| Method | URL | Açıklama |
|--------|-----|----------|
| POST | /api/auth/apple | Apple Sign In |
| POST | /api/auth/refresh | Token yenile |
| PUT | /api/auth/device-token | APNs token güncelle |

### Prayer
| Method | URL | Açıklama |
|--------|-----|----------|
| GET | /api/prayer/today | Bugünkü namazlar |
| GET | /api/prayer/day/{date} | Belirli gün |
| GET | /api/prayer/month?year=&month= | Aylık takvim |
| POST | /api/prayer/toggle | Namaz tikle |
| GET | /api/prayer/streak | Streak bilgisi |
| GET | /api/prayer/stats | İstatistikler |

### Subscription
| Method | URL | Açıklama |
|--------|-----|----------|
| GET | /api/subscription/status | Abonelik durumu |
| POST | /api/subscription/verify | Apple receipt doğrula |

## Railway Deploy

1. Railway'de yeni proje oluştur
2. GitHub repo'yu bağla
3. PostgreSQL ve Redis plugin ekle
4. Environment variables'ları `.env.example`'dan kopyala
5. Deploy

## Test (Local Mock)

Apple Sign In olmadan test için mock token kullanabilirsin:
```
POST /api/auth/apple
{
  "identityToken": "mock_test-user-123_test",
  "authorizationCode": "mock",
  "name": "Test Kullanıcı"
}
```
