package com.example.pastraone.domain.model

// Player class with added Bastra points tracking
class Player(val name: String, val teamId: Int, val isHuman: Boolean = false) {
    val hand = mutableListOf<Card>()
    val capturedCards = mutableListOf<Card>()
    var bastraPoints = 0 // Track Bastra points separately

    fun playCard(index: Int): Card {
        return hand.removeAt(index)
    }

    fun addToHand(card: Card) {
        hand.add(card)
    }

    fun captureCards(cards: List<Card>) {
        capturedCards.addAll(cards)
    }

    fun addBastraPoints(points: Int) {
        bastraPoints += points
    }

    fun getScore(): Int {
        var score = 0
        for (card in capturedCards) {
            score += card.getPoints()
        }
        // Add the Bastra bonus points to the score
        score += bastraPoints
        return score
    }
}