package com.sakethh.linkora.ui.domain

import com.sakethh.linkora.shared.generated.resources.LOLCATpl_logo
import com.sakethh.linkora.shared.generated.resources.Res
import com.sakethh.linkora.shared.generated.resources.legacy_logo
import com.sakethh.linkora.shared.generated.resources.mondstern_logo
import com.sakethh.linkora.shared.generated.resources.new_logo
import com.sakethh.linkora.shared.generated.resources.oh_arthur
import com.sakethh.linkora.shared.generated.resources.weather_logo
import org.jetbrains.compose.resources.DrawableResource

enum class AppIconCode(val icon: DrawableResource) {
    mondstern_logo(Res.drawable.mondstern_logo),
    LOLCATpl_logo(Res.drawable.LOLCATpl_logo),
    legacy_logo(Res.drawable.legacy_logo),
    new_logo(Res.drawable.new_logo),
    oh_arthur(Res.drawable.oh_arthur),
    must_be_weather(Res.drawable.weather_logo),
}
