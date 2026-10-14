plugins { id("com.android.application") }
android {
    namespace = "com.example.correoplus"
    compileSdk = 35
    defaultConfig { applicationId = "com.example.correoplus"; minSdk = 26; targetSdk = 28; versionCode = 5; versionName = "0.5" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources { excludes += "META-INF/LICENSE.md"; excludes += "META-INF/NOTICE.md" } }
}
dependencies { implementation("com.sun.mail:android-mail:1.6.7"); implementation("com.sun.mail:android-activation:1.6.7") }
