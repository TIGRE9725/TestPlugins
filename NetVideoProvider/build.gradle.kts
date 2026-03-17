dependencies {
    val implementation by configurations
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Películas y Series de NetVideo"
    authors = listOf("NetVideo")
    status = 1 
    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = false 
    language = "es"
}

android {
    namespace = "com.netvideo" 
    
    buildFeatures {
        buildConfig = true
        viewBinding = false
    }
}
