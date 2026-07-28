package com.example.familybudget20.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_REPO = "Lanarchiste/FamilyBudget2.0"

data class LatestRelease(val fileName: String, val downloadUrl: String)

// Lit la dernière Release GitHub publique et renvoie le nom + le lien de
// téléchargement de son premier fichier joint (l'APK). Pas de comparaison de
// version : on affiche juste ce qui est disponible, à l'utilisateur de
// constater lui-même s'il est à jour ou non.
suspend fun fetchLatestRelease(): LatestRelease = withContext(Dispatchers.IO) {
    val connection = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
        .openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    try {
        if (connection.responseCode != 200) {
            throw Exception("Aucune release trouvée (code ${connection.responseCode})")
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val assets = JSONObject(body).getJSONArray("assets")
        if (assets.length() == 0) {
            throw Exception("La dernière release ne contient aucun fichier")
        }
        val asset = assets.getJSONObject(0)
        LatestRelease(
            fileName = asset.getString("name"),
            downloadUrl = asset.getString("browser_download_url")
        )
    } finally {
        connection.disconnect()
    }
}
