plugins { id("com.android.application") }
android { namespace = "com.example.correoplus"; compileSdk = 35
 defaultConfig { applicationId = "com.example.correoplus"; minSdk = 26; targetSdk = 28; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("com.sun.mail:android-mail:1.6.7"); implementation("com.sun.mail:android-activation:1.6.7") }
