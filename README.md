# 🎮 Game Pass Price Checker

An Android app that helps you find the best deals on Xbox Game Pass Ultimate subscriptions by comparing prices across multiple reputable key sellers.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)

## ✨ Features

- **🔍 Price Comparison** - Searches multiple reputable key seller websites
- **🌍 Region Filters** - Filter by UAE, Global, US, UK, EU, Turkey, Brazil, Argentina, India
- **⏱️ Duration Filters** - 1 Month, 3 Months, 6 Months, 12 Months
- **🔑 Type Filters** - Key or Account
- **✅ Trust Indicators** - Shows seller trustworthiness (Highly Trusted, Trusted, Use Caution)
- **💰 Best Deal Highlight** - Highlights the cheapest option
- **🔗 Direct Links** - One tap to visit the seller's page

## 🏪 Supported Sellers

| Seller | Trust Level | Notes |
|--------|-------------|-------|
| CDKeys | ✅ Highly Trusted | Well-established, reliable |
| Eneba | ✅ Highly Trusted | Marketplace with buyer protection |
| Instant Gaming | ✅ Highly Trusted | European-based, trustworthy |
| Green Man Gaming | ✅ Highly Trusted | Authorized reseller |
| Humble Bundle | ✅ Highly Trusted | Official partner |
| Kinguin | 👍 Trusted | Marketplace with protection available |
| Gamivo | 👍 Trusted | Smart subscription benefits |
| G2A | ⚠️ Use Caution | Large marketplace - use G2A Shield |

## 📱 Screenshots

*Coming soon after first build*

## 🚀 Building the App

### Option 1: GitHub Actions (Recommended - No Setup Required)

1. **Fork this repository** to your GitHub account

2. **Enable GitHub Actions:**
   - Go to your forked repo → Actions tab
   - Click "I understand my workflows, go ahead and enable them"

3. **Trigger a build:**
   - Go to Actions → "Build Android APK"
   - Click "Run workflow" → "Run workflow"

4. **Download the APK:**
   - Once the build completes (green checkmark), click on the workflow run
   - Scroll down to "Artifacts"
   - Download `app-debug` for testing or `app-release-unsigned` for release

### Option 2: Build Locally

#### Prerequisites
- Java JDK 17+
- Android SDK (API 34)
- Android Build Tools

#### Steps

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/GamePassPriceChecker.git
cd GamePassPriceChecker

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

## 📲 Installation

1. **Enable "Install from Unknown Sources"** on your Android device:
   - Settings → Security → Unknown Sources (or Settings → Apps → Special Access → Install Unknown Apps)

2. **Transfer the APK** to your phone (email, cloud storage, USB)

3. **Open the APK** and tap "Install"

4. **Launch the app** and start finding deals!

## 🔧 How It Works

The app works by:

1. **Web Scraping** - Fetches prices from key seller websites using Jsoup
2. **Parallel Requests** - Searches multiple sites simultaneously for speed
3. **Data Parsing** - Extracts prices, regions, and product details
4. **Filtering** - Applies your selected filters (region, duration, type)
5. **Sorting** - Shows results sorted by price, lowest first
6. **Fallback Data** - Shows sample data if live scraping fails

## ⚠️ Disclaimer

- This app is for **personal use only** to make price comparison easier
- Prices shown are scraped from third-party websites and may not always be accurate
- Always verify prices on the seller's website before purchasing
- The developers are not affiliated with any of the listed sellers
- Use regional keys at your own risk - ensure they work in your region

## 🛡️ Privacy

- **No data collection** - The app doesn't collect or store any personal data
- **No accounts** - No sign-up or login required
- **No ads** - Completely ad-free
- **Open source** - Full transparency

## 📝 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest new features
- Add more sellers
- Improve scraping accuracy
- Submit pull requests

## 📧 Support

If you encounter any issues or have questions, please open an issue on GitHub.

---

**Made with ❤️ for Xbox gamers looking for the best deals**
