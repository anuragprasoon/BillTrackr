package com.example.billtrackr

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Addbill : Screen("add-bill")
    object Camera : Screen("camera")
}