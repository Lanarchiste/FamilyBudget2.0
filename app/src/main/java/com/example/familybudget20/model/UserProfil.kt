package com.example.familybudget20.model

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val color: String = "",
    val familyId: String = "",
    @get:PropertyName("isCreator")
    val isCreator: Boolean = false,
    val salary: Double = 0.0,
    val mode: String = "family",
    val savingPaidThisMonth: Boolean = false
)

