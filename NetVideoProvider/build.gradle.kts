dependencies {
    val implementation by configurations
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Películas y Series de NetVideo con Failover" [cite: 5]
    authors = listOf("NetVideo") // Pon tu nombre aquí [cite: 5]
    status = 1 
    tvTypes = listOf("Movie", "TvSeries") // Añadimos TvSeries
    requiresResources = true
    language = "es" // Cambiado a español
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
