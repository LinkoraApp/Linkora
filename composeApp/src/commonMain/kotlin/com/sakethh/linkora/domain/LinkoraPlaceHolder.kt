package com.sakethh.linkora.domain

enum class LinkoraPlaceHolder(
    val value: String,
) {
    First("{#LINKORA_PLACE_HOLDER_1#}"),
    Second("{#LINKORA_PLACE_HOLDER_2#}"),
    ;

    override fun toString(): String = value
}

fun linkoraPlaceHolders(): List<String> = LinkoraPlaceHolder.entries.map { it.value }
