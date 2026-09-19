package com.xothiques.vin.ui.navigation

object AuthRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER_HOUSEHOLD = "register_household"
    const val JOIN_HOUSEHOLD = "join_household"
}

object MainRoutes {
    const val CELLAR = "cellar"
    /** Flat, scrollable list of every bottle in the cellar (vs. the grid view).
     *  Optionally pre-filtered to one wine color -- reached either from the
     *  cave header (no filter) or by tapping one of the "Ma collection" rows
     *  (e.g. "Vins Rouges") on the cave screen. */
    const val BOTTLES = "bottles?color={color}"
    fun bottles(color: String? = null) = if (color != null) "bottles?color=$color" else "bottles"
    const val PAIRING = "pairing"
    const val SETTINGS = "settings"
    const val WISHLIST = "wishlist"
    const val DASHBOARD = "dashboard"

    const val BOTTLE_DETAIL = "bottle_detail/{bottleId}"
    fun bottleDetail(bottleId: String) = "bottle_detail/$bottleId"

    const val BOTTLE_FORM = "bottle_form?locationId={locationId}"
    fun bottleForm(locationId: String? = null) =
        if (locationId != null) "bottle_form?locationId=$locationId" else "bottle_form"

    const val BOTTLE_FORM_EDIT = "bottle_form_edit/{bottleId}"
    fun bottleFormEdit(bottleId: String) = "bottle_form_edit/$bottleId"

    const val AI_PROVIDER_SETTINGS = "ai_provider_settings"
    const val SCAN = "scan?locationId={locationId}"
    fun scan(locationId: String? = null) =
        if (locationId != null) "scan?locationId=$locationId" else "scan"
}
