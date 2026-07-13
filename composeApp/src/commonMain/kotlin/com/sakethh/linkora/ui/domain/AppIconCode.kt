package com.sakethh.linkora.ui.domain

import linkora.composeapp.generated.resources.LOLCATpl_logo
import linkora.composeapp.generated.resources.Res
import linkora.composeapp.generated.resources.legacy_logo
import linkora.composeapp.generated.resources.mondstern_logo
import linkora.composeapp.generated.resources.new_logo
import linkora.composeapp.generated.resources.oh_arthur
import linkora.composeapp.generated.resources.weather_logo
import org.jetbrains.compose.resources.DrawableResource

enum class AppIconCode(val icon: DrawableResource) {
    mondstern_logo(Res.drawable.mondstern_logo),
    LOLCATpl_logo(Res.drawable.LOLCATpl_logo),
    legacy_logo(Res.drawable.legacy_logo),
    new_logo(Res.drawable.new_logo),
    oh_arthur(Res.drawable.oh_arthur),
    must_be_weather(Res.drawable.weather_logo),
}
