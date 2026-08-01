# Pix Gallery 📸

Ek clean Android **Gallery app** — Kotlin + Jetpack Compose me bana hua, tumhare diye hue design (Photos / Albums / Recommended tabs, date-wise grouping, album grid, full-screen photo/video viewer, multi-select, Settings, Trash bin) ke hisaab se.

100% original code hai (koi copyrighted asset, koi Redmi/MIUI branding nahi) — isliye **copyright-free** hai, jaisa tumne bola tha. App ka naam "Pix Gallery" rakha hai aur icon tumhare diye hue logo se generate kiya gaya hai.

---

## ✨ Features

- **Photos tab** — Saari photos/videos date-wise (Today / Yesterday / date) groups me, search bar ke saath
- **Albums tab** — Device ke saare folders (Camera, WhatsApp, Screenshots, etc.) automatically dikhte hain
- **Recommended tab** — Memories card, Creativity shortcuts, Cleaner/Trash bin links
- **Photo Viewer** — Swipe karke photos/videos dekho:
  - **Videos ab actually play hote hain** (tap to play, seek bar/pause built-in)
  - **Send** — Real Android share sheet khulta hai (WhatsApp, Gmail, etc. me bhej sakte ho)
  - **Edit** — Device ke kisi bhi photo editor app me khulta hai
  - **Favorite** — Toggle karke favorite mark kar sakte ho (poori app me yaad rehta hai)
  - **Delete** — Trash bin me chala jaata hai
  - **More → Details** — Naam, type, date, album dikhata hai
  - **More → Set as wallpaper** — Photo ko seedha wallpaper bana sakte ho
- **Multi-select mode** — Long-press karke multiple photos select karo; Photos tab, Album detail — dono jagah **real Send (share) aur Delete** kaam karta hai
- **Trash bin** — Delete ki hui photos yahan jaati hain, **tap karke restore** kar sakte ho, ya "Empty trash bin" se permanently clear
- **Settings screen** — Display, sorting, hidden albums, secure sharing jaise toggles
- **Light/Dark theme** — Settings me se **manually** Light / Dark / System choose kar sakte ho (turant apply hota hai)
- **Photo Viewer readability** — Back button aur bottom action bar (Send/Edit/Favorite/Delete/More) ab har photo ke peeche dark scrim ke saath dikhte hain, chahe photo white/light color ka hi kyun na ho
- **Android 13+ ready** — Naya `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` permission model use karta hai (purane Android par bhi chalega, minSdk 24)

Yeh app seedha tumhare phone ki **MediaStore** se photos/videos padhta hai — koi internet ya cloud ki zaroorat nahi, 100% offline kaam karta hai.

---

## 📂 Project Structure

```
PixGallery/
├── app/
│   ├── src/main/
│   │   ├── java/com/pixgallery/app/
│   │   │   ├── MainActivity.kt          → App ka entry point + navigation
│   │   │   ├── PixGalleryApp.kt         → Application class (image loader setup)
│   │   │   ├── data/                    → MediaStore repository + ViewModel
│   │   │   ├── model/                   → Data classes (MediaItem, AlbumItem, etc.)
│   │   │   ├── ui/screens/              → Har tab/screen ka Compose UI
│   │   │   ├── ui/components/           → Reusable UI pieces (grid, nav bar, etc.)
│   │   │   └── util/                    → Permission helper
│   │   ├── res/mipmap-*/                → Tumhare logo se bane app icons
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/build.yml          → GitHub Actions (auto APK build)
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

---

## 🛠️ Apne computer par kaise chalayein (Android Studio se — sabse aasan tareeka)

1. [Android Studio](https://developer.android.com/studio) install karo (agar pehle se nahi hai)
2. Android Studio kholo → **Open** → is `PixGallery` folder ko select karo
3. Studio pehli baar "Gradle Sync" karega (internet chahiye, sab dependencies download karega) — bas thoda wait karo
4. Upar toolbar me green **Run ▶️** button dabao, apna phone/emulator select karo
5. App install ho jayegi, permission maangega — "Allow" karo, aur saari photos/videos dikhne lagengi

> ⚠️ Note: is project me `gradle-wrapper.jar` (binary file) shamil nahi hai, kyunki wo internet se download hoti hai. Jab tum Android Studio me project khologe, wo khud-ba-khud isse download/regenerate kar dega. Agar koi error aaye to: **File → Sync Project with Gradle Files** dabao.

---

## 🚀 GitHub par build kaise karein (bina Android Studio ke bhi APK ban jayegi)

Maine ek **GitHub Actions workflow** already daal diya hai (`.github/workflows/build.yml`). Isse GitHub khud APK bana kar de dega — tumhe kuch install karne ki zaroorat nahi!

### Steps:

1. GitHub par ek naya repository banao (public ya private, jo chaho)
2. Is poore folder ko us repo me push karo:
   ```bash
   cd PixGallery
   git init
   git add .
   git commit -m "Initial commit - Pix Gallery app"
   git branch -M main
   git remote add origin https://github.com/<tumhara-username>/<repo-name>.git
   git push -u origin main
   ```
3. GitHub par apne repo me jaake **"Actions"** tab kholo
4. Tumhe "Build APK" workflow chalta hua dikhega (automatic, kyunki push kiya hai)
5. Jab wo green tick ✅ ho jaye, us workflow run ke andar **"Artifacts"** section me jaao
6. Wahan se `pix-gallery-debug-apk` download kar lo — yeh tumhari final APK file hai!

Is tarah se **har baar jab tum code push karoge, GitHub apne aap naya APK bana dega** — koi local setup ki zaroorat nahi.

---

## 🔑 Permissions

App khulte hi ye permissions maangega:
- **Android 13+**: Photos & Videos (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`)
- **Android 12 aur niche**: Storage (`READ_EXTERNAL_STORAGE`)

Bina permission diye app photos/videos nahi dikha payega (jaisa har real gallery app me hota hai).

---

## 📱 Tech Stack

- **Kotlin** + **Jetpack Compose** (modern Android UI toolkit)
- **Material 3** design components
- **Coil** — image/video thumbnail loading ke liye
- **MediaStore API** — device ki photos/videos read karne ke liye
- **AndroidX Lifecycle/ViewModel** — state management

---

## 🧩 Aage kya improve kar sakte ho

- Photo editing built-in (crop/filter) — abhi device ke external editor app khulta hai
- Cloud backup (Google Drive/Firebase se)
- Hidden albums ka actual implementation
- Grid me favorite items par heart badge dikhana
- Album cover ko custom select karna

Koi bhi feature chahiye ho to bata dena, add kar dunga bhai! 🙌
