.

## ادغام دیالوگ در APK مود (کلاس‌ها در `androidx.core.app`)
کلاس‌ها عمداً زیر پکیج `androidx.core.app` هستند تا بین کلاس‌های کتابخانه‌ها گم شوند و نقششان معلوم نباشد. فایل `server.dex` را با MT Manager ادغام کن و در `MainActivity` (داخل `onCreate`) یکی از این دو خط را صدا بزن:

```smali
# الف) دیالوگ آنلاین (کانفیگ از گیت‌هاب)
invoke-static {p0}, Landroidx/core/app/AppCompatInit;->a(Landroid/app/Activity;)V

# ب) دیالوگ آفلاین پیش‌فرض (یک‌بار، همراه شمارنده)
invoke-static {p0}, Landroidx/core/app/AppCompatInit;->b(Landroid/app/Activity;)V
```
معادل جاوا: `androidx.core.app.AppCompatInit.a(this);` و `androidx.core.app.AppCompatInit.b(this);`

- لینک کانفیگ آنلاین هر برنامه: کلاس `AppCompatSync`، متغیر `SRC`.
- لینک/آیدی تلگرام دیالوگ آفلاین: کلاس `AppCompatNotice`، متغیر `TG_LINK`.
- آدرس شمارنده پلن‌تکست نیست؛ به‌صورت XOR+Base64 در `AppCompatNotice.EP` بسته شده و در متد `ep()` بازسازی می‌شود. برای مبهم‌سازی بیشتر، متدهای `ep()` و `a()/b()` را به dex2c/dex2cPro بسپار.
