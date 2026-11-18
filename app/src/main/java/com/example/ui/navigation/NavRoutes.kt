package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Dashboard : AppRoute

    @Serializable
    data object Tasks : AppRoute

    @Serializable
    data object Inventory : AppRoute

    @Serializable
    data object Scanner : AppRoute

    @Serializable
    data object ReportsAndTeam : AppRoute

    @Serializable
    data object StockGraphs : AppRoute
}
