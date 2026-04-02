/*
 *
 *  * Copyright (C) 2025 AKS-Labs (original author)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.abast.homebot.circletosearch.utils

import android.content.Context
import android.content.SharedPreferences

class FriendlyMessageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("friendly_msg_prefs", Context.MODE_PRIVATE)
    private val messages = listOf(
        "Ready when you are, boss. What’s the mission today? 🕵️‍♂️😂",
        "Say the word, chief. What are we hunting for now? 🔍😄",
        "Alright boss, what mystery are we solving this time? 🧐😂",
        "I’m here, captain! What’s the next target? 🎯🤣",
        "Reporting for duty, boss. What do we search? 💼😆",
        "Okayyy boss, what are we looking up this time—something normal or chaos again? 😂🔥",
        "Search mode activated! What’s the order, boss? 🤖😄",
        "Ready, boss! Who are we stalking—uhh… searching today? 👀😂",
        "Here I am, boss. Drop the keyword. 😎🔍",
        "Alright boss, hit me. What are we digging up now? 🪖🤣",
        "Show me the pic, boss. Let’s go detective mode! 🔍😄",
        "Drop the image, chief. I’ll find its secrets! 🖼️✨😂",
        "Alright boss, what are we zooming into today? 📸👀🤣",
        "Got a picture? Hand it over. I’m in full CSI mode. 🕵️‍♂️📷😂",
        "What visual mystery are we cracking today, boss? 👁️🧩😆",
        "Show me the image. I promise I won’t judge… much. 😭📸😂",
        "Ready to search! Bring me your weirdest picture. 🤣🖼️🔍",
        "Picture, please! Let me work my magic. ✨📷😄",
        "Boss, got another random image for me to analyze? 😆🖼️🧐",
        "New photo? Awesome. Let me eye-spy everything in it. 👀😂",
        "Give me an image and I’ll dig up its whole life story. 📸📜🤣",
        "Image detective reporting! What’s today’s case? 🕵️‍♂️🔍😄",
        "Hand me the pic, boss. Time for some visual chaat-masala! 🌶️📷😂",
        "What are we zooming, scanning, stalking— I mean, searching today? 👀🤣",
        "Drop the image, boss. Let’s find where it came from… and where it’s been. 😂📸🌍"
    )

    fun getNextMessage(): String {
        val seenIndices = getSeenIndices()
        
        // Find available indices
        val allIndices = messages.indices.toSet()
        val availableIndices = allIndices.subtract(seenIndices).toList()

        if (availableIndices.isEmpty()) {
            // Reset if all seen
            clearSeenIndices()
            val newRandomIndex = messages.indices.random()
            markIndexSeen(newRandomIndex)
            return messages[newRandomIndex]
        }
        
        // Pick random from available
        val pickedIndex = availableIndices.random()
        markIndexSeen(pickedIndex)
        return messages[pickedIndex]
    }

    private fun getSeenIndices(): Set<Int> {
        val seenString = prefs.getString("seen_indices", "") ?: ""
        if (seenString.isEmpty()) return emptySet()
        return seenString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun markIndexSeen(index: Int) {
        val currentSeen = getSeenIndices().toMutableSet()
        currentSeen.add(index)
        prefs.edit().putString("seen_indices", currentSeen.joinToString(",")).apply()
    }

    private fun clearSeenIndices() {
        prefs.edit().remove("seen_indices").apply()
    }
}
