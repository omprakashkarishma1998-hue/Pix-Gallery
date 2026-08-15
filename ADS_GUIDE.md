# 📺 Ads Configuration Guide

**Haan bhai! Ads already the app mein hain!** 🎯

---

## 🎬 Ads Kaun Se Hain?

App mein **3 types ke ads** hain:

### 1️⃣ **Banner Ads** (Bottom of screen)
- Every screen ke neeche dikhta hai
- Static banner, non-intrusive
- File: `ui/components/BannerAdView.kt`

### 2️⃣ **Rewarded Ads** (Full-screen video)
- Photo open karte waqt dikhta hai
- Har 3 photos mein ek baar (configurable)
- File: `ads/RewardedAdManager.kt`

### 3️⃣ **App Open Ads** (App launch par)
- App open hote waqt ek ad
- File: `ads/AppOpenAdManager.kt`

---

## 💰 Current Ad Unit IDs

### Google Play Test IDs (Safe for Development)
```
Banner:   ca-app-pub-3940256099942544/6300978111
Rewarded: ca-app-pub-3940256099942544/5224354917
```

### Live Ad Unit IDs (Main App)
```
Banner:   ca-app-pub-2350728358948132/4244085034
Rewarded: ca-app-pub-2350728358948132/2993938347
```

> ⚠️ **Important:** These ad units belong to the original developer. 
> Tujhe apne AdMob account se naye IDs milne chahiye!

---

## 🔧 Ads Ko Configure Kaise Karo

### Step 1: AdMob Account Banao
1. Go to [admob.google.com](https://admob.google.com)
2. Google account se login karo
3. "Get Started" click karo
4. App register karo
5. Ad units create karo (Banner + Rewarded)

### Step 2: Naye Ad Unit IDs Copy Karo
AdMob dashboard se ye copy karo:
- Banner Ad Unit ID
- Rewarded Ad Unit ID

### Step 3: Code Mein Replace Karo

#### **File 1: `ui/components/BannerAdView.kt`**
```kotlin
// Line 9-10: Ye wale replace karo
private const val BANNER_AD_UNIT_ID = "ca-app-pub-2350728358948132/4244085034"
                    ↓
// Naya ID
private const val BANNER_AD_UNIT_ID = "YOUR_NEW_BANNER_AD_UNIT_ID"
```

#### **File 2: `ads/RewardedAdManager.kt`**
```kotlin
// Line 11-12: Ye wale replace karo
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-2350728358948132/2993938347"
                    ↓
// Naya ID
private const val REWARDED_AD_UNIT_ID = "YOUR_NEW_REWARDED_AD_UNIT_ID"
```

---

## 🧪 Testing Ke Saath

### Development Mein (Testing)
```kotlin
// BannerAdView.kt mein
BannerAdView(useTestAd = true)  // ✅ Google ke test ads dikhenge

// MainActivity.kt mein
RewardedAdManager(context, useTestAd = true)  // ✅ Test ads
```

**Benefits of Test Ads:**
- Google ke fake ads dikhenge
- Real impressions nahi count hote
- Apne account ko suspend hone se bachega
- Bilkul safe development

### Release Mein (Production)
```kotlin
// Change to false
BannerAdView(useTestAd = false)  // ❌ Real ads

RewardedAdManager(context, useTestAd = false)  // ❌ Real ads
```

---

## 📍 Ads Kahan Dikhte Hain?

### 1. Banner Ad
```
┌─────────────────────┐
│  Photos Grid        │
│  ┌─┐ ┌─┐ ┌─┐       │
│  │ │ │ │ │ │       │
│  └─┘ └─┘ └─┘       │
├─────────────────────┤
│   BANNER AD HERE    │  ← Har screen ke neeche
├─────────────────────┤
│ Photos Albums etc   │  (BottomNavBar ke uper)
└─────────────────────┘
```

### 2. Rewarded Ad
```
Photo click karte ho → Ad dikh jayega → Photo open hota hai

Settings:
- Har 3rd photo pe ad dikhta hai
- File: RewardedAdManager.kt, line 46
```

### 3. App Open Ad
```
App launch → Ad dikhega → Phir home screen
```

---

## ⚙️ Ads Ko Customize Karo

### Banner Ad Frequency
**Abhi:** Every screen ke neeche

Change karne ke liye:
- `BannerAdView.kt` ko modify karo
- Ya hide karo: `MainActivity.kt` line remove karo

```kotlin
// Nahi dikhana banner? Ye line remove karo:
BannerAdView(useTestAd = false)
```

### Rewarded Ad Frequency
**Abhi:** Har 3 photos mein ek ad

Change karne ke liye:
```kotlin
// RewardedAdManager.kt mein
private val everyNActions: Int = 3  // Change to: 2, 4, 5, etc.
                           ↓
private val everyNActions: Int = 2  // Har 2 photos mein ad
```

### App Open Ad
**File:** `ads/AppOpenAdManager.kt`

---

## 🚀 Launch Se Pehle Checklist

- [ ] AdMob account banaya
- [ ] Ad units create kiye (Banner + Rewarded)
- [ ] Naye AD Unit IDs copy kiye
- [ ] Code mein replace kiye
- [ ] Test ads enable kiye
- [ ] Build kiya aur test kiya
- [ ] Ready release APK build karne ke liye

### Release ke liye:
- [ ] `useTestAd = false` set kiya (dono files mein)
- [ ] Apne live Ad Unit IDs verify kiye
- [ ] Release APK build kiya
- [ ] Upload kiya Uptodown/Google Play

---

## 💡 Pro Tips

### 1. Test Ads Hamesha Use Karo Development Mein
```kotlin
useTestAd = true  // ✅ Safe for development
useTestAd = false // ❌ Never use for testing (account ban risk)
```

### 2. Ad Unit IDs Public Hote Hain
- It's safe to commit to GitHub
- It's NOT a secret key
- Alag secret hota hai (AdMob API key), wo share mat karna

### 3. Ad Revenue Tips
- Too many ads = users hate it
- Too few ads = no revenue
- Balance: Har 3-5 actions mein 1 ad
- Banner ads = constant, low-annoyance revenue
- Rewarded ads = high engagement

### 4. Monitor Analytics
AdMob dashboard par:
- Impressions dekho
- Click-through rates (CTR)
- Revenue track karo
- Optimize karo

---

## 🔴 Common Issues

### Issue 1: "Ad Unit ID Invalid"
**Solution:**
1. AdMob dashboard se double-check karo ID
2. Exact copy-paste karo (space mistakes mat karo)
3. App ko register kiya hai AdMob mein?

### Issue 2: Ads Nahi Dikh Rahe
**Possible Reasons:**
- `useTestAd = true` set ho
- Ad Unit ID wrong ho
- App ko internet nahi mil raha
- AdMob account mein app verify nahi kiya

**Solution:**
1. Logs dekho: `adb logcat | grep -i admob`
2. Test Ad Unit IDs use karo (development mein)
3. Internet check karo

### Issue 3: "Test Device" Alert
**Matlab:** Phone test device nahi register ho

**Solution (optional):**
AdMob mein phone ko test device banao:
1. AdMob dashboard → Settings
2. Add test device → Copy device ID from logs
3. AdMob mein add karo

---

## 📊 Ads Revenue Strategy

### Current Setup
```
Banner (Bottom) = Constant, non-intrusive income
     ↓
Rewarded (Every 3 photos) = Higher revenue, good UX
     ↓
App Open (Launch) = Bonus revenue
```

### Optimization Ideas
1. **Increase Rewarded Frequency:**
   - `everyNActions = 2` → Har 2 photos
   - ⚠️ Balancing act: Revenue vs User happiness

2. **Add Interstitial Ads:**
   - Album transitions par
   - Settings tab exit par
   - ⚠️ Add carefully, users hate interruptions

3. **Monitor & Adjust:**
   - Track AdMob analytics
   - User feedback dekho
   - Optimize based on data

---

## 🎯 For Uptodown Submission

**Important:** Disclose ads in description!

```
Description mein likho:

"✨ Features:
- PIN-protected Vault
- Duplicate Finder
- Memories

📺 Ad-supported free app
- Banner ads at bottom
- Rewarded video ads (optional viewing)

Upgrade to Premium (optional) to remove ads"
```

Uptodown waalon ko transparent hona pasand aata hai. 🎉

---

## 🔐 Security Notes

### Safe to Commit:
```kotlin
private const val BANNER_AD_UNIT_ID = "ca-app-pub-2350728358948132/4244085034"
✅ Yes, this is safe. It's a public ID.
```

### DO NOT Commit:
```kotlin
private const val ADMOB_API_KEY = "AIza..."  // Secret key
❌ Never share this!
```

---

## ✅ Step-by-Step Setup (Quick)

```
1. AdMob account banao (free)
2. App register karo
3. Banner ad unit create karo
4. Rewarded ad unit create karo
5. IDs copy karo
6. Code mein replace karo (2 files):
   - BannerAdView.kt
   - RewardedAdManager.kt
7. Test ads use karo (development)
8. Build → Test → Live ads enable
9. Submit to Uptodown
10. Revenue milna start! 💰
```

---

## 📱 Where Ads Show Up in App

```
PHOTOS TAB
├── Banner ad neeche (always)
├── Photo click → Rewarded ad (every 3rd)
└── App navigate → App open ad (optional)

ALBUMS TAB
├── Banner ad neeche (always)
└── Album click → Rewarded ad (every 3rd)

FAVORITES TAB
├── Banner ad neeche
└── Same rewarded ad logic

VAULT / DUPLICATES / MEMORIES
├── Banner ad neeche
└── Photo click → Rewarded ad
```

---

## 🤔 Agar Ads Nahi Dikhne Hain?

Agar tu ads bilkul remove karna chahta hai:

### Option 1: Banner Remove
Remove ye line from `MainActivity.kt`:
```kotlin
BannerAdView(useTestAd = false)  // Delete ye line
```

### Option 2: Rewarded Remove
Change `RewardedAdManager.kt`:
```kotlin
private val everyNActions: Int = 999999  // Ads kabhi show nahi honge
```

### Option 3: Disable Both
Comment out banner + set rewarded to very high number.

---

## 💬 Final Notes

✅ **Ads already integrated**
✅ **Just need AdMob IDs from you**
✅ **Test ads work out of box**
✅ **Can be tuned for revenue**
✅ **User-friendly setup**

**Ab tu bas apne AdMob account se IDs copy kar aur code update kar!**

**That's it!** 🚀

---

## 📞 Quick Reference

| Component | File | Current Setup |
|-----------|------|---------------|
| Banner | `BannerAdView.kt` | Every screen bottom |
| Rewarded | `RewardedAdManager.kt` | Every 3 photos |
| App Open | `AppOpenAdManager.kt` | App launch |

| Action | Code | Value |
|--------|------|-------|
| Use Test Ads | `useTestAd = true` | Development |
| Use Live Ads | `useTestAd = false` | Production |
| Change Frequency | `everyNActions = X` | 2-5 recommended |

---

**Good luck with monetization!** 💰

Ads properly set up ho to app se revenue aayega aur users ko bhi naraz nahi hoga! 🎉
