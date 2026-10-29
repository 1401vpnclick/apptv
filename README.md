# چک اپ (Check App)

نسخهٔ اندرویدِ پنل ساخت کانفیگ دیالوگ عضویت. یک WebView که پنل را نشان می‌دهد؛ ورود با یوزرنیم/ریپو/توکن که فقط روی خود دستگاه ذخیره می‌شود.

## گرفتن APK (خودکار با GitHub Actions)
1. این پروژه را در ریپو قرار بده (push یا Upload files).
2. برو به تب **Actions** → ورک‌فلوی **Build APK** خودکار اجرا می‌شود (یا دستی: Run workflow).
3. بعد از پایان (سبز شدن)، پایین صفحهٔ همان اجرا، بخش **Artifacts** → فایل `check-app-debug-apk` را دانلود کن.
4. از حالت zip خارجش کن → `app-debug.apk` را روی گوشی نصب کن.

## ساخت محلی (اختیاری)
نیازی نیست؛ اما اگر Android SDK داشتی:
```
gradle assembleDebug
```
خروجی: `app/build/outputs/apk/debug/app-debug.apk`

## نکات
- خروجی فعلی **Debug APK** است (برای نصب شخصی و تست). برای انتشار نهایی، نسخهٔ Release امضاشده لازم است.
- لوگوی پیش‌فرض اندروید استفاده شده؛ لوگوی اختصاصی بعداً اضافه می‌شود.

## شمارندهٔ کلیک آفلاین (Cloudflare Workers + D1)
کد Worker در پوشهٔ `worker/` است و با ورک‌فلوی **Deploy offline counter** خودکار روی Cloudflare می‌رود (دیتابیس D1 هم خودکار ساخته می‌شود).

یک بار این ۳ Secret را در ریپو بگذار (Settings → Secrets and variables → Actions → New repository secret):

| نام | مقدار |
|---|---|
| `CLOUDFLARE_API_TOKEN` | توکن Cloudflare با قالب «Edit Cloudflare Workers» + دسترسی **D1: Edit** |
| `CLOUDFLARE_ACCOUNT_ID` | Account ID (داشبورد Cloudflare → Workers & Pages، ستون راست) |
| `COUNTER_ADMIN_KEY` | یک رمز دلخواه و طولانی (برای دیدن آمار در اپ) |

بعد: Actions → **Deploy offline counter** → Run workflow. آدرس Worker در خلاصهٔ همان اجرا نوشته می‌شود.

- این آدرس را در dex (کلاس `OfflineDialog`، متغیر `COUNTER_URL`) بگذار.
- در اپ «مدیریت گیت» → تب **آمار** → آدرس + همان `COUNTER_ADMIN_KEY` را وارد کن.

مسیرها: `GET /hit?id=<package>` (عمومی) · `GET /stats` و `POST /reset?id=` (با کلید ادمین).
