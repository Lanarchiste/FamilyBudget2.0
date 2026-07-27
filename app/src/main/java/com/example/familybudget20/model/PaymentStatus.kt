package com.example.familybudget20.model

data class PaymentStatus(
    val creatorPaid: Boolean = false,
    val partnerPaid: Boolean = false
)
