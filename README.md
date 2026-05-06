# 📸 PhotoBooth App

אפליקציית פוטו בוט לאירועים | Android (Jetpack Compose + Kotlin)

---

## 🏗️ ארכיטקטורה

```
PhotoBoothApp/
├── data/
│   ├── model/          AppSettings.kt, PhotoSession.kt
│   ├── local/          SettingsDataStore.kt
│   └── repository/     SettingsRepository.kt, PhotoRepository.kt
├── domain/
│   ├── service/        CameraService, PrinterService, SharingService (interfaces)
│   └── usecase/        BuildPhotoStripUseCase, PrintStripUseCase, SharePhotosUseCase
├── impl/               CameraXService, AndroidPrinterService, AndroidSharingService
├── di/                 AppModule.kt (Hilt bindings)
└── ui/
    ├── screen/         WelcomeScreen, CaptureScreen, ReviewScreen,
    │                   SettingsScreen, PinEntryScreen
    ├── viewmodel/      PhotoBoothViewModel, SettingsViewModel
    ├── navigation/     NavGraph.kt
    └── theme/          Theme.kt (4 themes)
```

### עקרונות SOLID שיושמו:
- **S** – כל מחלקה אחראית לדבר אחד (Repository רק שומר, UseCase רק לוגיקה)
- **O** – PhotoFrame / AppTheme ניתנים להרחבה ללא שינוי קוד קיים
- **L** – CameraXService מחליף CameraService ללא שינוי ב-ViewModel
- **I** – 3 interfaces נפרדים: CameraService / PrinterService / SharingService
- **D** – ViewModel תלוי בinterfaces, לא ב-CameraX ישירות

---

## 🚀 הוראות הרצה ב-Android Studio

### דרישות מערכת
- Android Studio Hedgehog (2023.1.1) ומעלה
- JDK 17
- Android SDK API 26+
- מכשיר פיזי או אמולטור עם API 26+

### שלבי הגדרה

1. **פתח את הפרויקט** ב-Android Studio:
   `File → Open → בחר את תיקיית PhotoBoothApp`

2. **Sync Gradle:**
   `File → Sync Project with Gradle Files`
   (ייקח 2-5 דקות בפעם הראשונה)

3. **הגדר אמולטור:**
   - `Tools → Device Manager → Create Device`
   - בחר: Pixel 6 Pro, API 34
   - ✅ ודא שה-Camera מופעל ב-Hardware Profile

4. **הרץ:**
   - בחר את המכשיר ולחץ ▶️ Run

### הרצה על מכשיר פיזי
1. `Settings → Developer Options → USB Debugging` = ON
2. חבר בUSB
3. אשר את החיבור במכשיר
4. בחר את המכשיר ב-Android Studio

---

## 📱 פיצ'רים

### מסך פתיחה
- כפתור SHOOT מונפש (pulse)
- שם האירוע בולט
- כפתור הגדרות (⚙️) בפינה – מוגן PIN

### צילום
- מצלמת CameraX (קדמית/אחורית)
- ספירה לאחור 1-10 שניות (ניתן להגדרה)
- אנימציית `AnimatedContent` על המספרים
- אחרי כל צילום – הפסקה קצרה ואז הבא
- 3 תמונות → בניית strip אוטומטית

### Strip של תמונות
- 3 תמונות מסודרות אנכית
- מסגרת לבחירה (8 אפשרויות)
- שמירה ל-MediaStore (גלריה)

### מסך סיום
- תצוגת ה-strip
- כפתור הדפסה (דרך Android Print Framework)
- שיתוף: System Sheet / WhatsApp / Bluetooth
- חזרה לסשן חדש

### הגדרות (מוגן PIN)
| הגדרה | אפשרויות |
|-------|----------|
| ערכת נושא | Dark Gold / Neon Party / Minimal White / Vintage Retro |
| שפה | עברית / English / דו-לשוני |
| מסגרת | 8 מסגרות לבחירה |
| ספירה לאחור | 1-10 שניות |
| מצלמה | קדמית/אחורית |
| מדפסת | IP/Bluetooth MAC + auto-print + מקסימום הדפסות |
| PIN | שינוי קוד גישה |

---

## 🎨 ערכות נושא

| שם | צבעים |
|----|-------|
| Dark Gold (ברירת מחדל) | שחור + זהב – אלגנטי |
| Neon Party | כחול כהה + ציאן + מגנטה |
| Minimal White | לבן + אפור כהה |
| Vintage Retro | חום + צהוב-חם |

---

## 🖨️ הדפסה

האפליקציה משתמשת ב-**Android Print Framework** המובנה:
- עובד עם כל מדפסת Wi-Fi תואמת Android (Canon SELPHY, DNP, Mitsubishi, Hiti)
- פותח את dialog ההדפסה הסטנדרטי של Android
- לחיבור Bluetooth – הזן את כתובת ה-MAC בהגדרות

---

## 🔧 הרחבות עתידיות

הארכיטקטורה תומכת בהוספה קלה של:
- **מצלמה חיצונית USB** → מימוש חדש של `CameraService`
- **מדפסת Bluetooth תרמית** → מימוש חדש של `PrinterService`
- **פילטרים** → הוסף ל-`PhotoRepository.applyFilter()`
- **QR Code** → הוסף ל-`ReviewScreen`
- **גלריה מקוונת** → הוסף UseCase + Repository חדש

---

## 📦 תלויות עיקריות

| ספרייה | מטרה |
|--------|------|
| Jetpack Compose | UI |
| CameraX | מצלמה |
| Hilt | Dependency Injection |
| DataStore | שמירת הגדרות |
| Navigation Compose | ניווט |
| Coil | טעינת תמונות |
| Accompanist Permissions | הרשאות |
