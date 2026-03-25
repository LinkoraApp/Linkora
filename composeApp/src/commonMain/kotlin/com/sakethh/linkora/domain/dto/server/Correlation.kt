package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Correlation(
    val id: String, val clientName: String
) {
    companion object {
        private val adjectives = listOf(
            "Agile",
            "Bold",
            "Brave",
            "Bright",
            "Calm",
            "Cheerful",
            "Clever",
            "Cool",
            "Creative",
            "Curious",
            "Daring",
            "Dynamic",
            "Eager",
            "Elegant",
            "Energetic",
            "Fierce",
            "Friendly",
            "Gentle",
            "Glorious",
            "Happy",
            "Helpful",
            "Heroic",
            "Honest",
            "Inventive",
            "Kind",
            "Lively",
            "Loyal",
            "Majestic",
            "Mighty",
            "Noble",
            "Peaceful",
            "Quick",
            "Quiet",
            "Radiant",
            "Resourceful",
            "Sharp",
            "Smart",
            "Strong",
            "Swift",
            "Thoughtful",
            "Unique",
            "Vivid",
            "Warm",
            "Wise",
            "Witty",
            "Zany",
            "Zesty",
            "Fearless",
            "Generous",
            "Vibrant"
        )

        private val nouns = listOf(
            "Bear",
            "Wolf",
            "Fox",
            "Lion",
            "Tiger",
            "Eagle",
            "Hawk",
            "Falcon",
            "Panther",
            "Leopard",
            "Dragon",
            "Phoenix",
            "Unicorn",
            "Griffin",
            "Jaguar",
            "Cheetah",
            "Otter",
            "Dolphin",
            "Shark",
            "Whale",
            "Panda",
            "Koala",
            "Owl",
            "Hedgehog",
            "Rabbit",
            "Sparrow",
            "Robin",
            "Stag",
            "Bison",
            "Buffalo",
            "Horse",
            "Zebra",
            "Cobra",
            "Viper",
            "Python",
            "Lynx",
            "Wolverine",
            "Raven",
            "Pelican",
            "Seagull",
            "Moose",
            "Elk",
            "PolarBear",
            "Seal",
            "Penguin",
            "Crane",
            "Flamingo",
            "Bee",
            "Butterfly",
            "Swan"
        )

        @OptIn(ExperimentalUuidApi::class)
        fun generateRandomCorrelation(): Correlation {
            return Correlation(
                id = Uuid.random().toString(),
                clientName = "${adjectives.random()} ${nouns.random()}"
            )
        }
    }
}
