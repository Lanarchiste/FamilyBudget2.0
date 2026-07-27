package com.example.familybudget20.model

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val lineId: String = "",
    val lineTitle: String = "",
    val amount: Double = 0.0,
    val type: String = "ajout",      // "ajout" | "depense"
    val authorName: String = "",
    val annotation: String = "",
    val createdAt: Timestamp? = null
)