package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Data model representing a supported application language in Zyphuel.
 */
data class AppLanguage(
    val code: String,
    val nameEn: String,
    val nameNative: String,
    val flag: String,
    val isRtl: Boolean = false
)

/**
 * AppLanguageManager manages application localization, user language preference persistence,
 * and translation dictionaries across 14+ languages.
 */
object AppLanguageManager {

    private const val PREFS_NAME = "zyphuel_ui_prefs"
    private const val KEY_LANGUAGE_CODE = "app_language_code"

    val DEFAULT_LANGUAGE = AppLanguage(
        code = "en",
        nameEn = "English",
        nameNative = "English",
        flag = "🇬🇧",
        isRtl = false
    )

    private val supportedLanguages = listOf(
        DEFAULT_LANGUAGE,
        AppLanguage("ur", "Urdu", "اردو", "🇵🇰", isRtl = true),
        AppLanguage("pa", "Punjabi", "پنجابی", "🇵🇰", isRtl = true),
        AppLanguage("sd", "Sindhi", "سنڌي", "🇵🇰", isRtl = true),
        AppLanguage("ps", "Pashto", "پښتو", "🇵🇰", isRtl = true),
        AppLanguage("ar", "Arabic", "العربية", "🇸🇦", isRtl = true),
        AppLanguage("fa", "Persian", "فارسی", "🇮🇷", isRtl = true),
        AppLanguage("tr", "Turkish", "Türkçe", "🇹🇷", isRtl = false),
        AppLanguage("es", "Spanish", "Español", "🇪🇸", isRtl = false),
        AppLanguage("fr", "French", "Français", "🇫🇷", isRtl = false),
        AppLanguage("de", "German", "Deutsch", "🇩🇪", isRtl = false),
        AppLanguage("zh", "Chinese", "中文", "🇨🇳", isRtl = false),
        AppLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", isRtl = false),
        AppLanguage("ru", "Russian", "Русский", "🇷🇺", isRtl = false)
    )

    fun getSupportedLanguages(): List<AppLanguage> = supportedLanguages

    fun getPopularLanguages(): List<AppLanguage> = supportedLanguages.take(6)

    fun getLanguageByCode(code: String): AppLanguage {
        return supportedLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT_LANGUAGE
    }

    fun loadSavedLanguage(context: Context): AppLanguage {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE.code) ?: DEFAULT_LANGUAGE.code
        return getLanguageByCode(savedCode)
    }

    fun saveLanguage(context: Context, language: AppLanguage) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()
    }

    // Translation dictionary mapping keys to language codes
    private val translations = mapOf(
        "app_name" to mapOf(
            "en" to "Zyphuel",
            "ur" to "زیفیول",
            "pa" to "زیفیول",
            "sd" to "زيفيوئل",
            "ps" to "زیفیول",
            "ar" to "زيفيول"
        ),
        "nav_home" to mapOf(
            "en" to "Home",
            "ur" to "ہوم",
            "pa" to "گھر / ہوم",
            "sd" to "مکاني / گهر",
            "ps" to "کور / کورپانه",
            "ar" to "الرئيسية",
            "es" to "Inicio",
            "fr" to "Accueil",
            "de" to "Startseite",
            "tr" to "Ana Sayfa",
            "fa" to "خانه",
            "hi" to "होम",
            "zh" to "首页",
            "ru" to "Главная"
        ),
        "nav_orders" to mapOf(
            "en" to "Orders",
            "ur" to "آرڈرز",
            "pa" to "آرڈر",
            "sd" to "آرڊر",
            "ps" to "فرمایشونه",
            "ar" to "الطلبات",
            "es" to "Pedidos",
            "fr" to "Commandes",
            "de" to "Bestellungen",
            "tr" to "Siparişler",
            "fa" to "سفارش‌ها",
            "hi" to "ऑर्डर",
            "zh" to "订单",
            "ru" to "Заказы"
        ),
        "nav_profile" to mapOf(
            "en" to "Profile Settings",
            "ur" to "پروفائل کی ترتیبات",
            "pa" to "پروفائل سیٹنگز",
            "sd" to "پروفائل سيٽنگون",
            "ps" to "پروفایل امستنې",
            "ar" to "إعدادات الملف الشخصي",
            "es" to "Ajustes de Perfil",
            "fr" to "Paramètres du Profil",
            "de" to "Profileinstellungen",
            "tr" to "Profil Ayarları",
            "fa" to "تنظیمات نمایه",
            "hi" to "प्रोफ़ाइल सेटिंग्स",
            "zh" to "个人资料设置",
            "ru" to "Настройки профиля"
        ),
        "nav_support" to mapOf(
            "en" to "Help & Support",
            "ur" to "مدد اور رہنمائی",
            "pa" to "مدد تے سپورٹ",
            "sd" to "مدد ۽ رهنمائي",
            "ps" to "مرسته او ملاتړ",
            "ar" to "المساعدة والدعم",
            "es" to "Ayuda y Soporte",
            "fr" to "Aide et Support",
            "de" to "Hilfe & Support",
            "tr" to "Yardım ve Destek",
            "fa" to "راهنما و پشتیبانی",
            "hi" to "सहायता और समर्थन",
            "zh" to "帮助与支持",
            "ru" to "Помощь и поддержка"
        ),
        "nav_security" to mapOf(
            "en" to "Security & Biometrics",
            "ur" to "سیکیورٹی اور بائیو میٹرکس",
            "pa" to "سیکیورٹی تے بائیو میٹرکس",
            "sd" to "سيڪيورٽي ۽ بايوميٽرڪس",
            "ps" to "امنیت او بایومیټریک",
            "ar" to "الأمان والقياسات الحيوية",
            "es" to "Seguridad y Biometría",
            "fr" to "Sécurité et Biométrie",
            "de" to "Sicherheit & Biometrie",
            "tr" to "Güvenlik ve Biyometri",
            "fa" to "امنیت و بیومتریک",
            "hi" to "सुरक्षा और बायोमेट्रिक्स",
            "zh" to "安全与生物识别",
            "ru" to "Безопасность и биометрия"
        ),
        "service_petrol" to mapOf(
            "en" to "Super Petrol (Euro-V)",
            "ur" to "سپر پیٹرول (Euro-V)",
            "pa" to "سپر پیٹرول",
            "sd" to "سپر پيٽرول",
            "ps" to "سوپر پټرول",
            "ar" to "بنزين ممتاز (Euro-V)",
            "es" to "Súper Gasolina (Euro-V)",
            "fr" to "Super Essence (Euro-V)",
            "de" to "Superbenzin (Euro-V)",
            "tr" to "Süper Benzin (Euro-V)",
            "fa" to "بنزین سوپر",
            "hi" to "सुपर पेट्रोल",
            "zh" to "超级汽油 (Euro-V)",
            "ru" to "Супер Бензин (Euro-V)"
        ),
        "service_diesel" to mapOf(
            "en" to "High-Speed Diesel",
            "ur" to "ہائی اسپیڈ ڈیزل",
            "pa" to "ہائی سپیڈ ڈیزل",
            "sd" to "هاءِ اسپيڊ ڊيزل",
            "ps" to "های سپیډ ډیزل",
            "ar" to "ديزل عالي السرعة",
            "es" to "Diésel de Alta Velocidad",
            "fr" to "Diesel Haute Vitesse",
            "de" to "Hochgeschwindigkeitsdiesel",
            "tr" to "Yüksek Hızlı Dizel",
            "fa" to "دیزل با سرعت بالا",
            "hi" to "हाई-स्पीड डीजल",
            "zh" to "高速柴油",
            "ru" to "Высокоскоростной дизель"
        ),
        "service_octane" to mapOf(
            "en" to "High-Octane (RON 97)",
            "ur" to "ہائی اوکٹین (RON 97)",
            "pa" to "ہائی اوکٹین (RON 97)",
            "sd" to "هاءِ آڪٽين (RON 97)",
            "ps" to "های اوکټین (RON 97)",
            "ar" to "أوكتان عالي (RON 97)",
            "es" to "Alto Octanaje (RON 97)",
            "fr" to "Haut Octane (RON 97)",
            "de" to "Hochoktan (RON 97)",
            "tr" to "Yüksek Oktan (RON 97)",
            "fa" to "اوکتان بالا (RON 97)",
            "hi" to "हाई-ऑक्टेन (RON 97)",
            "zh" to "高辛烷值汽油 (RON 97)",
            "ru" to "Высокооктановый (RON 97)"
        ),
        "service_water" to mapOf(
            "en" to "Pure Drinking Water",
            "ur" to "پینے کا صاف پانی",
            "pa" to "پین دا صاف پانی",
            "sd" to "پيئڻ جو صاف پاڻي",
            "ps" to "د څښاک پاکې اوبه",
            "ar" to "مياه نقية للشرب",
            "es" to "Agua Potable Pura",
            "fr" to "Eau Potable Pure",
            "de" to "Reines Trinkwasser",
            "tr" to "Saf İçme Suyu",
            "fa" to "آب آشامیدنی خالص",
            "hi" to "शुद्ध पीने का पानी",
            "zh" to "纯净饮用水",
            "ru" to "Чистая питьевая вода"
        ),
        "service_lpg" to mapOf(
            "en" to "LPG Gas Cylinder",
            "ur" to "ایل پی جی گیس سلنڈر",
            "pa" to "ایل پی جی سلنڈر",
            "sd" to "ايل پي جي گئس سلنڊر",
            "ps" to "ایل پی جی ګاز سلنډر",
            "ar" to "أسطوانة غاز نفطي مسال",
            "es" to "Cilindro de Gas GLP",
            "fr" to "Bouteille de Gaz GPL",
            "de" to "LPG-Gasflasche",
            "tr" to "LPG Gaz Tüpü",
            "fa" to "سیلندر گاز ال‌پی‌جی",
            "hi" to "एलपीजी गैस सिलेंडर",
            "zh" to "液化石油气钢瓶",
            "ru" to "Газовый баллон LPG"
        ),
        "btn_place_order" to mapOf(
            "en" to "Place Order (Cash on Delivery)",
            "ur" to "آرڈر بک کریں (کیش آن ڈیلیوری)",
            "pa" to "آرڈر کرو (کیش آن ڈیلیوری)",
            "sd" to "آرڊر ڪريو (ڪيش آن ڊليوري)",
            "ps" to "امر ثبت کړئ (نغدې په سپارلو)",
            "ar" to "تأكيد الطلب (الدفع عند الاستلام)",
            "es" to "Realizar Pedido (Pago contra entrega)",
            "fr" to "Commander (Paiement à la livraison)",
            "de" to "Bestellen (Nachnahme)",
            "tr" to "Sipariş Ver (Kapıda Ödeme)",
            "fa" to "ثبت سفارش (پرداخت در محل)",
            "hi" to "ऑर्डर दें (कैश ऑन डिलीवरी)",
            "zh" to "下订单 (货到付款)",
            "ru" to "Оформить заказ (Оплата при получении)"
        ),
        "btn_cancel" to mapOf(
            "en" to "Cancel",
            "ur" to "منسوخ کریں",
            "pa" to "منسوخ کرو",
            "sd" to "منسوخ ڪريو",
            "ps" to "لغوه کول",
            "ar" to "إلغاء",
            "es" to "Cancelar",
            "fr" to "Annuler",
            "de" to "Abbrechen",
            "tr" to "İptal",
            "fa" to "لغو",
            "hi" to "रद्द करें",
            "zh" to "取消",
            "ru" to "Отмена"
        ),
        "btn_save" to mapOf(
            "en" to "Save",
            "ur" to "محفوظ کریں",
            "pa" to "محفوظ کرو",
            "sd" to "محفوظ ڪريو",
            "ps" to "ساتل",
            "ar" to "حفظ",
            "es" to "Guardar",
            "fr" to "Enregistrer",
            "de" to "Speichern",
            "tr" to "Kaydet",
            "fa" to "ذخیره",
            "hi" to "सहेजें",
            "zh" to "保存",
            "ru" to "Сохранить"
        ),
        "lbl_language" to mapOf(
            "en" to "App Language",
            "ur" to "ایپ کی زبان",
            "pa" to "ایپ دی بولی",
            "sd" to "ايپ جي ٻولي",
            "ps" to "د اپلیکیشن ژبه",
            "ar" to "لغة التطبيق",
            "es" to "Idioma de la Aplicación",
            "fr" to "Langue de l'Application",
            "de" to "App-Sprache",
            "tr" to "Uygulama Dili",
            "fa" to "زبان برنامه",
            "hi" to "ऐप की भाषा",
            "zh" to "应用语言",
            "ru" to "Язык приложения"
        ),
        "lbl_language_desc" to mapOf(
            "en" to "Select your preferred language",
            "ur" to "اپنی پسندیدہ زبان منتخب کریں",
            "pa" to "اپنی پسند دی بولی چنو",
            "sd" to "پنهنجي پسند جي ٻولي چونڊيو",
            "ps" to "خپله خوښه ژبه وټاکئ",
            "ar" to "اختر لغتك المفضلة",
            "es" to "Selecciona tu idioma preferido",
            "fr" to "Choisissez votre langue préférée",
            "de" to "Wählen Sie Ihre bevorzugte Sprache",
            "tr" to "Tercih ettiğiniz dili seçin",
            "fa" to "زبان مورد نظر خود را انتخاب کنید",
            "hi" to "अपनी पसंदीदा भाषा चुनें",
            "zh" to "选择您的首选语言",
            "ru" to "Выберите предпочитаемый язык"
        ),
        "msg_lang_changed" to mapOf(
            "en" to "Language updated to",
            "ur" to "زبان تبدیل ہو گئی:",
            "pa" to "بولی تبدیل ہو گئی:",
            "sd" to "ٻولي تبديل ٿي وئي:",
            "ps" to "ژبه بدله شوه:",
            "ar" to "تم تغيير اللغة إلى:",
            "es" to "Idioma cambiado a:",
            "fr" to "Langue modifiée en:",
            "de" to "Sprache geändert zu:",
            "tr" to "Dil değiştirildi:",
            "fa" to "زبان تغییر یافت به:",
            "hi" to "भाषा बदली गई:",
            "zh" to "语言已更改为:",
            "ru" to "Язык изменен на:"
        ),
        "lbl_role" to mapOf(
            "en" to "ACCOUNT ROLE",
            "ur" to "اکاؤنٹ کا کردار",
            "pa" to "اکاؤنٹ رول",
            "sd" to "اڪائونٽ جو رول",
            "ps" to "د حساب رول",
            "ar" to "نوع الحساب"
        ),
        "lbl_order_alerts" to mapOf(
            "en" to "Order Alerts",
            "ur" to "آرڈر الرٹس",
            "pa" to "آرڈر الرٹس",
            "sd" to "آرڊر الرٽ",
            "ps" to "د امر خبرتیاوې",
            "ar" to "تنبيهات الطلب"
        ),
        "lbl_delete_account" to mapOf(
            "en" to "Delete Account / Erase Data",
            "ur" to "اکاؤنٹ ڈیلیٹ کریں / ڈیٹا مٹائیں",
            "pa" to "اکاؤنٹ مٹاؤ",
            "sd" to "اڪائونٽ ختم ڪريو",
            "ps" to "حساب ړنګ کړئ",
            "ar" to "حذف الحساب / محو البيانات"
        ),
        "lbl_logout" to mapOf(
            "en" to "Logout",
            "ur" to "لاگ آؤٹ",
            "pa" to "باہر نکلو",
            "sd" to "لاگ آئوٽ",
            "ps" to "وتل",
            "ar" to "تسجيل الخروج"
        ),

        // ─────────────────────────────────────────────────────────────────────────────
        // Priority customer flow (Urdu-first). Languages without an entry below fall
        // back to English automatically via translate() — add them incrementally later.
        // ─────────────────────────────────────────────────────────────────────────────

        // Navigation drawer — customer items & section headers
        "nav_active_orders" to mapOf(
            "en" to "My Active Orders",
            "ur" to "میرے فعال آرڈرز",
            "pa" to "میرے چالو آرڈر",
            "ar" to "طلباتي النشطة"
        ),
        "nav_order_history" to mapOf(
            "en" to "Customer Order History",
            "ur" to "آرڈرز کی تاریخ",
            "pa" to "آرڈر دی تاریخ",
            "ar" to "سجل الطلبات"
        ),
        "nav_received_orders" to mapOf(
            "en" to "Received Orders",
            "ur" to "موصولہ آرڈرز",
            "pa" to "ملے آرڈر",
            "ar" to "الطلبات المستلمة"
        ),
        "nav_faq" to mapOf(
            "en" to "FAQ / Guidelines",
            "ur" to "عمومی سوالات / ہدایات",
            "pa" to "عام سوال / ہدایات",
            "ar" to "الأسئلة الشائعة / الإرشادات"
        ),
        "nav_live_support" to mapOf(
            "en" to "Live Support & Help Center",
            "ur" to "لائیو سپورٹ اور مدد سینٹر",
            "pa" to "لائیو سپورٹ تے مدد سینٹر",
            "ar" to "الدعم المباشر ومركز المساعدة"
        ),
        "nav_terms_privacy" to mapOf(
            "en" to "Terms & Privacy Policy",
            "ur" to "شرائط اور رازداری کی پالیسی",
            "pa" to "شرطاں تے پرائیویسی پالیسی",
            "ar" to "الشروط وسياسة الخصوصية"
        ),
        "nav_switch_rider" to mapOf(
            "en" to "Switch as Rider",
            "ur" to "رائیڈر کے طور پر جائیں",
            "pa" to "رائیڈر بن کے جاؤ",
            "ar" to "التبديل كسائق"
        ),
        "nav_switch_customer" to mapOf(
            "en" to "Switch as Customer",
            "ur" to "کسٹمر کے طور پر جائیں",
            "pa" to "گاہک بن کے جاؤ",
            "ar" to "التبديل كعميل"
        ),
        "sec_account_settings" to mapOf(
            "en" to "ACCOUNT & SETTINGS",
            "ur" to "اکاؤنٹ اور ترتیبات",
            "pa" to "اکاؤنٹ تے سیٹنگز",
            "ar" to "الحساب والإعدادات"
        ),
        "sec_orders_deliveries" to mapOf(
            "en" to "ORDERS & DELIVERIES",
            "ur" to "آرڈرز اور ترسیل",
            "pa" to "آرڈر تے ڈلیوری",
            "ar" to "الطلبات والتوصيل"
        ),
        "sec_help_support" to mapOf(
            "en" to "HELP & SUPPORT",
            "ur" to "مدد اور رہنمائی",
            "pa" to "مدد تے سپورٹ",
            "ar" to "المساعدة والدعم"
        ),

        // Order dialog
        "lbl_order_fuel_gas" to mapOf(
            "en" to "Order Fuel & Gas",
            "ur" to "ایندھن اور گیس آرڈر کریں",
            "pa" to "تیل تے گیس آرڈر کرو",
            "ar" to "اطلب الوقود والغاز"
        ),
        "dlg_order_title" to mapOf(
            "en" to "Place Your Order",
            "ur" to "اپنا آرڈر دیں",
            "pa" to "اپنا آرڈر دیو",
            "ar" to "قدّم طلبك"
        ),
        "dlg_order_subtitle" to mapOf(
            "en" to "Order items together for delivery discounts",
            "ur" to "ڈیلیوری رعایت کے لیے اشیاء ایک ساتھ آرڈر کریں",
            "ar" to "اطلب المنتجات معًا للحصول على خصومات التوصيل"
        ),
        "btn_place_delivery_order" to mapOf(
            "en" to "Place Delivery Order 🚀",
            "ur" to "ڈیلیوری آرڈر بک کریں 🚀",
            "pa" to "ڈلیوری آرڈر کرو 🚀",
            "ar" to "🚀 تأكيد طلب التوصيل"
        ),
        "btn_placing_order" to mapOf(
            "en" to "Placing Order...",
            "ur" to "آرڈر بک ہو رہا ہے...",
            "pa" to "آرڈر ہو ریا اے...",
            "ar" to "جارٍ تقديم الطلب..."
        ),

        // Short service names (catalog chips / order-dialog selectors).
        // NOTE: kept separate from the long service_* descriptions on purpose.
        "svc_petrol" to mapOf(
            "en" to "Petrol",
            "ur" to "پیٹرول",
            "pa" to "پیٹرول",
            "sd" to "پيٽرول",
            "ps" to "پټرول",
            "ar" to "بنزين"
        ),
        "svc_super_petrol" to mapOf(
            "en" to "Super Petrol",
            "ur" to "سپر پیٹرول",
            "pa" to "سپر پیٹرول",
            "sd" to "سپر پيٽرول",
            "ps" to "سوپر پټرول",
            "ar" to "بنزين ممتاز"
        ),
        "svc_diesel" to mapOf(
            "en" to "Diesel",
            "ur" to "ڈیزل",
            "pa" to "ڈیزل",
            "sd" to "ڊيزل",
            "ps" to "ډیزل",
            "ar" to "ديزل"
        ),
        "svc_octane" to mapOf(
            "en" to "High-Octane",
            "ur" to "ہائی اوکٹین",
            "pa" to "ہائی اوکٹین",
            "sd" to "هاءِ آڪٽين",
            "ps" to "های اوکټین",
            "ar" to "أوكتان عالي"
        ),
        "svc_water" to mapOf(
            "en" to "Water",
            "ur" to "پانی",
            "pa" to "پانی",
            "sd" to "پاڻي",
            "ps" to "اوبه",
            "ar" to "ماء"
        ),
        "svc_lpg" to mapOf(
            "en" to "LPG Gas",
            "ur" to "ایل پی جی گیس",
            "pa" to "ایل پی جی گیس",
            "sd" to "ايل پي جي گئس",
            "ps" to "ایل پی جی ګاز",
            "ar" to "غاز LPG"
        ),

        // Order statuses (display-only; logic keeps English via order.status).
        "status_pending" to mapOf(
            "en" to "Pending",
            "ur" to "زیر التوا",
            "ar" to "قيد الانتظار"
        ),
        "status_assigned" to mapOf(
            "en" to "Assigned",
            "ur" to "تفویض شدہ",
            "ar" to "تم التعيين"
        ),
        "status_delivering" to mapOf(
            "en" to "Delivering",
            "ur" to "ترسیل جاری",
            "ar" to "جارٍ التوصيل"
        ),
        "status_in_transit" to mapOf(
            "en" to "In Transit",
            "ur" to "راستے میں",
            "pa" to "راہ وچ",
            "ar" to "في الطريق"
        ),
        "status_dispatched" to mapOf(
            "en" to "Dispatched",
            "ur" to "روانہ کر دیا گیا",
            "ar" to "تم الإرسال"
        ),
        "status_arriving" to mapOf(
            "en" to "Arriving",
            "ur" to "پہنچ رہا ہے",
            "ar" to "قادم"
        ),
        "status_arriving_soon" to mapOf(
            "en" to "Arriving Soon",
            "ur" to "جلد پہنچ رہا ہے",
            "ar" to "يصل قريبًا"
        ),
        "status_arrived" to mapOf(
            "en" to "Arrived",
            "ur" to "پہنچ گیا",
            "ar" to "وصل"
        ),
        "status_out_for_delivery" to mapOf(
            "en" to "Out for Delivery",
            "ur" to "ترسیل کے لیے روانہ",
            "ar" to "خرج للتوصيل"
        ),
        "status_completed" to mapOf(
            "en" to "Completed",
            "ur" to "مکمل",
            "pa" to "مکمل",
            "ar" to "مكتمل"
        ),
        "status_delivered" to mapOf(
            "en" to "Delivered",
            "ur" to "پہنچا دیا گیا",
            "ar" to "تم التسليم"
        ),
        "status_cancelled" to mapOf(
            "en" to "Cancelled",
            "ur" to "منسوخ",
            "pa" to "منسوخ",
            "ar" to "ملغى"
        ),
        "status_canceled" to mapOf(
            "en" to "Canceled",
            "ur" to "منسوخ",
            "ar" to "ملغى"
        ),
        "status_declined" to mapOf(
            "en" to "Declined",
            "ur" to "مسترد",
            "ar" to "مرفوض"
        )
    )

    fun translate(key: String, langCode: String, fallback: String): String {
        val entry = translations[key] ?: return fallback
        return entry[langCode] ?: entry["en"] ?: fallback
    }
}
