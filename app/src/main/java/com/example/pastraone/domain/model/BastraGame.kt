package com.example.pastraone.domain.model


import com.example.pastraone.Card
import com.example.pastraone.PlayResult
import com.example.pastraone.Player
import com.example.pastraone.Rank
import com.example.pastraone.Suit
import com.example.pastraone.Team
import kotlin.random.Random

class BastraGame {
    private val deck = mutableListOf<Card>()
    private val tableCards = mutableListOf<Card>()
    private val players = mutableListOf<Player>()
    private val teams = mutableListOf<Team>()
    private var currentPlayerIndex = 0
    private var lastCaptor: Player? = null
    private var gameLog = mutableListOf<String>()

    // Last played card for animation
    var lastPlayedCard: Card? = null

    init {
        // Create the deck (no jokers)
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                deck.add(Card(rank, suit))
            }
        }

        // Create teams
        teams.add(Team(0, "Team 1"))
        teams.add(Team(1, "Team 2"))
    }

    // Player and card dealing methods unchanged
    fun addPlayer(name: String, teamId: Int, isHuman: Boolean = false): Player {
        val player = Player(name, teamId, isHuman)
        players.add(player)
        teams[teamId].addPlayer(player)
        return player
    }

    fun dealCards() {
        deck.shuffle()

        // Deal 4 cards to each player
        for (player in players) {
            for (i in 0 until 4) {
                if (deck.isNotEmpty()) {
                    player.addToHand(deck.removeAt(0))
                }
            }
        }

        // Place 4 cards on the table
        for (i in 0 until 4) {
            if (deck.isNotEmpty()) {
                tableCards.add(deck.removeAt(0))
            }
        }

        addToLog("Game started. 4 cards dealt to each player and 4 cards placed on the table.")
    }

    fun dealNextRound() {
        if (deck.isEmpty()) return

        // Deal 4 more cards to each player
        for (player in players) {
            for (i in 0 until 4) {
                if (deck.isNotEmpty()) {
                    player.addToHand(deck.removeAt(0))
                }
            }
        }

        addToLog("Next round: 4 new cards dealt to each player.")
    }

    // Updated play turn function with correct capturing logic and proper Bastra points
    fun playTurn(cardIndex: Int): PlayResult {
        val currentPlayer = players[currentPlayerIndex]
        if (cardIndex >= currentPlayer.hand.size) {
            return PlayResult(false, "Invalid card index")
        }

        val playedCard = currentPlayer.playCard(cardIndex)
        lastPlayedCard = playedCard // Store for animation

        addToLog("${currentPlayer.name} plays $playedCard")

        var captured = false
        var isBastra = false
        var bastraPoints = 0
        var capturedCards = mutableListOf<Card>()

        // Check if played card is a Jack (captures all cards)
        if (playedCard.rank == Rank.JACK) {
            if (tableCards.isNotEmpty()) {
                capturedCards.addAll(tableCards)
                capturedCards.add(playedCard)

                // Jack captures all cards but it's NOT a Bastra unless it matches the only card on the table
                if (tableCards.size == 1 && tableCards[0].rank == Rank.JACK) {
                    isBastra = true
                    bastraPoints = 20
                    // Add the Bastra points to the player's score
                    currentPlayer.addBastraPoints(bastraPoints)
                    addToLog("BASTRA with Jack! ${currentPlayer.name} gets 20 extra points!")
                }

                currentPlayer.captureCards(capturedCards)
                tableCards.clear() // Clear all table cards
                lastCaptor = currentPlayer
                captured = true
                addToLog("${currentPlayer.name} captured all cards with a Jack!")
            } else {
                tableCards.add(playedCard)
            }
        }
        // Check if played card matches the LAST card on the table
        else if (tableCards.isNotEmpty() && playedCard.matches(tableCards.last())) {
            // If matches the last card, capture ALL cards from the table and the played card
            capturedCards.addAll(tableCards)
            capturedCards.add(playedCard)

            // Check if this is a Bastra (exactly one card on the table that matches)
            if (tableCards.size == 1) {
                isBastra = true
                bastraPoints = 10
                // Add the Bastra points to the player's score
                currentPlayer.addBastraPoints(bastraPoints)
                addToLog("BASTRA! ${currentPlayer.name} gets 10 extra points!")
            }

            currentPlayer.captureCards(capturedCards)
            tableCards.clear() // Clear all table cards
            lastCaptor = currentPlayer
            captured = true
            addToLog("${currentPlayer.name} captured ${tableCards.size} cards")
        } else {
            // No match with last card, just add to table
            tableCards.add(playedCard)
        }

        if (!captured) {
            addToLog("${currentPlayer.name} placed $playedCard on the table")
        }

        // Move to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size

        return PlayResult(
            true,
            if (captured) {
                if (isBastra) "Bastra! Captured cards with $playedCard"
                else "Captured cards with $playedCard"
            } else {
                "Placed $playedCard on the table"
            },
            captured,
            isBastra,
            bastraPoints,
            capturedCards
        )
    }

    // AI play (unchanged)
    fun playAITurn(): PlayResult {
        val aiPlayer = players[currentPlayerIndex]
        if (aiPlayer.isHuman) {
            return PlayResult(false, "Current player is not AI")
        }

        // Simple AI strategy:
        // 1. If has a card that matches table card, play it
        // 2. If has a Jack and there are cards on table, play it
        // 3. Otherwise play random card

        var cardToPlay = -1

        // Check for matching cards
        for (i in aiPlayer.hand.indices) {
            val card = aiPlayer.hand[i]

            // Check if this card matches any on the table
            for (tableCard in tableCards) {
                if (card.matches(tableCard)) {
                    cardToPlay = i
                    break
                }
            }

            if (cardToPlay != -1) break

            // Check for Jack if there are cards on the table
            if (card.rank == Rank.JACK && tableCards.isNotEmpty()) {
                cardToPlay = i
                break
            }
        }

        // If no strategic card found, choose random
        if (cardToPlay == -1) {
            cardToPlay = Random.nextInt(aiPlayer.hand.size)
        }

        return playTurn(cardToPlay)
    }

    // Game state checking methods (unchanged)
    fun isRoundComplete(): Boolean {
        return players.all { it.hand.isEmpty() }
    }

    fun isGameComplete(): Boolean {
        return deck.isEmpty() && isRoundComplete()
    }

    fun finalizeGame() {
        // Give remaining table cards to last player who captured
        if (tableCards.isNotEmpty() && lastCaptor != null) {
            lastCaptor!!.captureCards(tableCards)
            addToLog("Remaining ${tableCards.size} table cards given to ${lastCaptor!!.name}")
            tableCards.clear()
        }

        // Find team with most cards and give 3 extra points
        val team1CardCount = teams[0].getCapturedCardCount()
        val team2CardCount = teams[1].getCapturedCardCount()

        if (team1CardCount > team2CardCount) {
            addToLog("${teams[0].name} has the most cards and gets 3 extra points")
            // Add 3 extra cards worth 1 point each to simulate 3 bonus points
            for (i in 0 until 3) {
                teams[0].players.first().capturedCards.add(Card(Rank.ACE, Suit.HEARTS))
            }
        } else if (team2CardCount > team1CardCount) {
            addToLog("${teams[1].name} has the most cards and gets 3 extra points")
            for (i in 0 until 3) {
                teams[1].players.first().capturedCards.add(Card(Rank.ACE, Suit.HEARTS))
            }
        } else {
            addToLog("Both teams have the same number of cards, no extra points awarded")
        }
    }

    private fun addToLog(message: String) {
        gameLog.add(message)
    }

    fun getLogMessages(): List<String> = gameLog

    fun getTeamScores(): Map<Int, Int> {
        return teams.associateBy({ it.id }, { it.getScore() })
    }

    fun getCurrentPlayer(): Player {
        return players[currentPlayerIndex]
    }

    fun getTableCards(): List<Card> {
        return tableCards.toList()
    }

    fun getRemainingCards(): Int {
        return deck.size
    }

    fun getWinningTeam(): Team? {
        val scores = getTeamScores()
        return if (scores[0]!! > scores[1]!!) teams[0]
        else if (scores[0]!! < scores[1]!!) teams[1]
        else null // tie
    }
}