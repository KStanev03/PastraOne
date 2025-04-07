package com.example.pastraone

class Team(val id: Int, val name: String) {
    val players = mutableListOf<Player>()

    fun addPlayer(player: Player) {
        players.add(player)
    }

    fun getScore(): Int {
        return players.sumOf { it.getScore() }
    }

    fun getCapturedCardCount(): Int {
        return players.sumOf { it.capturedCards.size }
    }
}