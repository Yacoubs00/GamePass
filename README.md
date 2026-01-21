# 🎮 Game Pass Price Checker

An Android app that helps you find the best deals on Xbox Game Pass Ultimate subscriptions by comparing prices across 27+ reputable key sellers worldwide.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Version](https://img.shields.io/badge/Version-0.0.1-blue?style=for-the-badge)

## ✨ Features

- **🔍 Price Comparison** - Searches 27+ key seller websites simultaneously
- **🇦🇪 UAE + Global Priority** - Deals for UAE and Global regions shown first
- **🛡️ Trust Level Filter** - Filter by seller trustworthiness (High, Medium, Caution)
- **🚫 Exclude Trials** - Filter out trial offers to see full subscriptions only
- **🌍 Region Filters** - UAE, Global, US, UK, EU, Turkey, Brazil, Argentina, India
- **⏱️ Duration Filters** - 1, 3, 6, or 12 month subscriptions
- **🔑 Type Filters** - Key or Account type deals
- **🌓 Light/Dark Mode** - Toggle between themes
- **📋 Trust Info Page** - Detailed seller trust levels with sources
- **💰 Best Deal Highlight** - Cheapest option highlighted at the top
- **🔗 Direct Links** - One tap to visit the seller's page

## 🏪 Supported Sellers (27+)

### ✅ Highly Trusted
| Seller | Trustpilot | Notes |
|--------|------------|-------|
| CDKeys | 4.7/5 (100K+) | 12+ years, money-back guarantee |
| Eneba | 4.6/5 (200K+) | Eneba Protect program |
| G2A | 4.5/5 (200K+) | G2A Shield, 15M+ customers |
| Green Man Gaming | Official | Authorized Xbox partner |
| Humble Bundle | Official | Microsoft partner, charity |
| Instant Gaming | 4.5/5 (150K+) | EU-based, fast delivery |
| Kinguin | 4.4/5 (80K+) | Buyer protection, 10+ years |
| Gamivo | 4.6/5 (50K+) | Smart subscription benefits |
| Fanatical | Official | Authorized reseller |
| K4G | 4.6/5 (20K+) | Buyer protection |
| GAMESEAL | Verified | GG.deals verified |
| MMOGA | 4.5/5 (50K+) | German, since 2002 |
| Voidu | Official | Authorized, Netherlands |
| Nuuvem | Official | Brazil specialist |
| MTCGame | 4.5/5 | Turkey key specialist |
| Wyrel | Verified | Turkey region specialist |
| 2Game | Official | Authorized reseller |
| Driffle | Verified | GG.deals verified |
| G2Play | 4.4/5 (10K+) | Established marketplace |
| Play-Asia | 20+ years | Asia specialist |
| GameStop | Official | US retailer |
| Amazon | Official | A-to-z guarantee |
| Difmark | Verified | Regional key specialist |
| HRK Game | 4.3/5 | EU-based |
| Gamesplanet | Official | UK/EU authorized |

### 👍 Trusted (Use Buyer Protection)
| Seller | Notes |
|--------|-------|
| Gamers Outlet | Budget prices, check ratings |
| SCDKey | Very low prices, mixed reviews |

## 📱 Screenshots

*Screenshots coming soon*

## 🚀 Installation

### Download APK

1. Go to [Releases](https://github.com/Yacoubs00/GamePass/releases)
2. Download the latest `app-release.apk`
3. Transfer to your Android phone
4. Enable "Install from Unknown Sources" if prompted
5. Install and enjoy!

### Build from Source

#### Prerequisites
- Java JDK 17+
- Android SDK (API 34)

#### Using GitHub Actions (Recommended)
1. Fork this repository
2. Go to Actions → "Build Android APK"
3. Click "Run workflow"
4. Download APK from Artifacts

#### Local Build
```bash
git clone https://github.com/Yacoubs00/GamePass.git
cd GamePass
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 How It Works

1. **Smart Search** - Searches using multiple query variants:
   - "Xbox Game Pass Ultimate"
   - "Xbox GamePass Ultimate"
   - "Game Pass Ultimate"
   - "Xbox GPU"

2. **Parallel Scraping** - Fetches prices from all sellers simultaneously

3. **Trial Detection** - Automatically identifies trial offers

4. **Intelligent Sorting** - UAE and Global deals prioritized, then sorted by price

5. **Fallback Data** - Shows sample data if live scraping fails

## 🛡️ Trust Level Methodology

We determine seller trust levels based on:
- **Trustpilot ratings** and review count
- **Reddit community feedback** (r/GameDeals, r/xbox)
- **GG.deals** seller verification
- **AllKeyShop** merchant ratings
- **IsThereAnyDeal** authorized dealer list
- **Years in business** / company history
- **Buyer protection** policies
- **Customer support** responsiveness

View detailed trust information in the app by tapping the ℹ️ info button.

## ⚠️ Disclaimer

- This app is for **personal use only** to make price comparison easier
- Prices are scraped from third-party websites and may not always be accurate
- Always verify prices on the seller's website before purchasing
- Use regional keys at your own risk - ensure they work in your region
- We are not affiliated with any of the listed sellers
- Trust levels are based on publicly available information and may change

## 🔒 Privacy

- **No data collection** - The app doesn't collect or store any personal data
- **No accounts** - No sign-up or login required
- **No ads** - Completely ad-free
- **No tracking** - No analytics or telemetry
- **Open source** - Full transparency

## 📝 Changelog

### v0.0.1 (Initial Release)
- 27+ seller support
- UAE + Global prioritization
- Trust level filtering
- Exclude trials filter
- Light/Dark mode
- Trust info page with sources

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs or issues
- Suggest new sellers to add
- Improve scraping accuracy
- Add new features
- Submit pull requests

## 📧 Support

If you encounter any issues, please [open an issue](https://github.com/Yacoubs00/GamePass/issues).

## 📄 License

This project is open source and available under the MIT License.

---

**Made with ❤️ for Xbox gamers looking for the best Game Pass deals**
