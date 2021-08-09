package io.legado.app.data.entities.rule

data class LogInRule(
    val ui: HashMap<String, String>,
    val logInUrl: String,
    val checkJs: String
)