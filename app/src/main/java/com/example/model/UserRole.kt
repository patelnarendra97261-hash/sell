package com.example.model

enum class UserRole {
    ADMIN,
    SHOPKEEPER
}

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val role: UserRole = UserRole.SHOPKEEPER,
    val defaultPin: String = "1234", // Password or PIN
    val email: String = "",          // Username or Email
    val phone: String = ""           // Phone Number for Admin Phone Auth
) {
    companion object {
        val ADMIN_USER = UserProfile(
            id = "admin",
            name = "Admin Owner",
            role = UserRole.ADMIN,
            defaultPin = "1234",
            email = "admin@liquor.com",
            phone = "+919876543210"
        )
        val TINA = UserProfile(
            id = "sk_tina",
            name = "Tina",
            role = UserRole.SHOPKEEPER,
            defaultPin = "1111",
            email = "tina",
            phone = "+919800000001"
        )
        val KISHOR = UserProfile(
            id = "sk_kishor",
            name = "Kishor",
            role = UserRole.SHOPKEEPER,
            defaultPin = "2222",
            email = "kishor",
            phone = "+919800000002"
        )
        val NAVIN = UserProfile(
            id = "sk_navin",
            name = "Navin",
            role = UserRole.SHOPKEEPER,
            defaultPin = "3333",
            email = "navin",
            phone = "+919800000003"
        )

        val ALL_USERS = listOf(ADMIN_USER, TINA, KISHOR, NAVIN)
        val SHOPKEEPERS = listOf(TINA, KISHOR, NAVIN)
    }
}

