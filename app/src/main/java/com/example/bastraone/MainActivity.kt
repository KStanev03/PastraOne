package com.example.pastraone

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import kotlin.random.Random

// Card representation
data class Card(val rank: Rank, val suit: Suit) {
    override fun toString(): String = "$rank of $suit"

    fun getPoints(): Int {
        return when (rank) {
            Rank.ACE, Rank.JACK -> 1
            Rank.TEN -> if (suit == Suit.DIAMONDS) 3 else 0
            Rank.TWO -> if (suit == Suit.CLUBS) 2 else 0
            else -> 0
        }
    }

    fun matches(other: Card): Boolean {
        return rank == other.rank
    }

    fun getImageResourceId(): Int {
        val rankStr = when(rank) {
            Rank.ACE -> "ace"
            Rank.TWO -> "2"
            Rank.THREE -> "3"
            Rank.FOUR -> "4"
            Rank.FIVE -> "5"
            Rank.SIX -> "6"
            Rank.SEVEN -> "7"
            Rank.EIGHT -> "8"
            Rank.NINE -> "9"
            Rank.TEN -> "10"
            Rank.JACK -> "jack"
            Rank.QUEEN -> "queen"
            Rank.KING -> "king"
        }

        val suitStr = when(suit) {
            Suit.CLUBS -> "clubs"
            Suit.DIAMONDS -> "diamonds"
            Suit.HEARTS -> "hearts"
            Suit.SPADES -> "spades"
        }

        // This assumes you have card images named like "ace_of_hearts", "2_of_clubs", etc.
        return R.drawable::class.java.getField("card_${rankStr}_of_${suitStr}").getInt(null)
    }
}

// Card ranks and suits (unchanged)
enum class Rank {
    ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING;

    override fun toString(): String {
        return when(this) {
            ACE -> "A"
            TWO -> "2"
            THREE -> "3"
            FOUR -> "4"
            FIVE -> "5"
            SIX -> "6"
            SEVEN -> "7"
            EIGHT -> "8"
            NINE -> "9"
            TEN -> "10"
            JACK -> "J"
            QUEEN -> "Q"
            KING -> "K"
        }
    }
}

enum class Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;

    override fun toString(): String {
        return when(this) {
            CLUBS -> "♣"
            DIAMONDS -> "♦"
            HEARTS -> "♥"
            SPADES -> "♠"
        }
    }
}

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

// Team class (unchanged)
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

// Card adapter with improved animations
class CardAdapter(
    private val cards: List<Card>,
    private val onCardClick: (Int) -> Unit,
    private val cardWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(val cardView: FrameLayout) : RecyclerView.ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item, parent, false) as FrameLayout

        cardView.layoutParams = ViewGroup.LayoutParams(
            cardWidth,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return CardViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        val imageView = holder.cardView.findViewById<ImageView>(R.id.cardImageView)

        imageView.setImageResource(card.getImageResourceId())

        holder.cardView.setOnClickListener {
            // Apply a click animation
            val clickAnim = AnimationUtils.loadAnimation(holder.cardView.context, R.anim.card_click)
            holder.cardView.startAnimation(clickAnim)

            // Delay the actual click action to let the animation play
            Handler(Looper.getMainLooper()).postDelayed({
                onCardClick(position)
            }, 200)
        }
    }

    override fun getItemCount() = cards.size
}

// Game class with fixed Bastra points
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

// Updated PlayResult with captured cards list
data class PlayResult(
    val success: Boolean,
    val message: String,
    val captured: Boolean = false,
    val isBastra: Boolean = false,
    val bastraPoints: Int = 0,
    val capturedCards: List<Card> = emptyList()
)

// MainActivity with improved animations and timing
class MainActivity : AppCompatActivity() {
    private lateinit var game: BastraGame
    private lateinit var handRecyclerView: RecyclerView
    private lateinit var tableCardsView: RecyclerView
    private lateinit var statusTextView: TextView
    private lateinit var team1ScoreTextView: TextView
    private lateinit var team2ScoreTextView: TextView
    private lateinit var gameLogTextView: TextView
    private lateinit var playCardView: FrameLayout
    private lateinit var nextButton: Button

    private var humanPlayerIndex = 0
    private val handler = Handler(Looper.getMainLooper())

    // Animation control flags
    private var isAnimating = false
    private var pendingCardPlay: Int? = null

    // Animation delays
    private val cardPlayDelay = 800L // ms
    private val aiTurnDelay = 1500L // ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        handRecyclerView = findViewById(R.id.playerHandRecyclerView)
        tableCardsView = findViewById(R.id.tableCardsRecyclerView)
        statusTextView = findViewById(R.id.statusTextView)
        team1ScoreTextView = findViewById(R.id.team1ScoreTextView)
        team2ScoreTextView = findViewById(R.id.team2ScoreTextView)
        gameLogTextView = findViewById(R.id.gameLogTextView)
        playCardView = findViewById(R.id.playCardView)
        nextButton = findViewById(R.id.nextButton)

        // Set up RecyclerViews
        handRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        tableCardsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Set up play area for animations
        playCardView.visibility = View.INVISIBLE

        // Set up game
        setupGame()

        // Set up next button
        nextButton.setOnClickListener {
            if (!isAnimating) {
                handleAITurn()
            }
        }
    }

    private fun setupGame() {
        game = BastraGame()

        // Add players (0 is human, others are AI)
        game.addPlayer("You", 0, true)
        game.addPlayer("AI Player 2", 1)
        game.addPlayer("AI Partner", 0)
        game.addPlayer("AI Player 4", 1)

        // Deal initial cards
        game.dealCards()

        // Update UI
        updateUI()
    }

    private fun updateUI() {
        val currentPlayer = game.getCurrentPlayer()
        val isHumanTurn = currentPlayer.isHuman

        // Update status
        statusTextView.text = "Current player: ${currentPlayer.name}"

        // Update scores
        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Team 1: ${scores[0]} points"
        team2ScoreTextView.text = "Team 2: ${scores[1]} points"

        // Update table cards
        val tableCards = game.getTableCards()
        tableCardsView.adapter = CardAdapter(tableCards, { _ ->
            // Nothing happens when clicking table cards
        }, resources.getDimensionPixelSize(R.dimen.card_width))

        // Update game log
        val logMessages = game.getLogMessages().takeLast(5)
        gameLogTextView.text = logMessages.joinToString("\n")

        if (isHumanTurn) {
            // Update player hand only when it's human's turn
            handRecyclerView.adapter = CardAdapter(currentPlayer.hand, { cardIndex ->
                if (!isAnimating) {
                    handleCardPlay(cardIndex)
                }
            }, resources.getDimensionPixelSize(R.dimen.card_width))
            nextButton.visibility = View.GONE
            handRecyclerView.visibility = View.VISIBLE
        } else {
            // Show empty hand for AI's turn
            handRecyclerView.adapter = CardAdapter(emptyList(), { _ -> })
            nextButton.visibility = View.VISIBLE
            handRecyclerView.visibility = View.INVISIBLE
        }

        // Check if game is complete
        if (game.isGameComplete()) {
            handleGameEnd()
        }

        // Check if round is complete
        if (game.isRoundComplete() && !game.isGameComplete()) {
            game.dealNextRound()
            updateUI()
        }
    }

    // Improved card play with animations
    private fun handleCardPlay(cardIndex: Int) {
        isAnimating = true

        val currentPlayer = game.getCurrentPlayer()
        // Create a copy of the card for animation before removing it from hand
        val card = currentPlayer.hand[cardIndex]

        // Show the played card in center
        val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
        playedCardImage.setImageResource(card.getImageResourceId())
        playCardView.visibility = View.VISIBLE

        // Apply card play animation
        val animIn = AnimationUtils.loadAnimation(this, R.anim.card_play_in)
        playCardView.startAnimation(animIn)

        // Play sound effect
        // playCardSound()

        // After animation, process the move
        handler.postDelayed({
            val result = game.playTurn(cardIndex)

            if (result.success) {
                // Animate card movement to table or captured pile
                val animOut = AnimationUtils.loadAnimation(this,
                    if (result.captured) R.anim.card_capture_out else R.anim.card_play_out)

                playCardView.startAnimation(animOut)

                handler.postDelayed({
                    playCardView.visibility = View.INVISIBLE

                    if (result.isBastra) {
                        // Show Bastra animation and play sound
                        showBastraAnimation()
                        Toast.makeText(this, "BASTRA! ${result.bastraPoints} points!", Toast.LENGTH_SHORT).show()
                        handler.postDelayed({
                            updateUI()

                            // Process next AI turn after a delay
                            handler.postDelayed({
                                isAnimating = false
                                if (!game.getCurrentPlayer().isHuman && !game.isGameComplete()) {
                                    handleAITurn()
                                }
                            }, 500)
                        }, 1500) // Extra delay for Bastra celebration
                    } else {
                        updateUI()

                        // Process next AI turn after a delay
                        handler.postDelayed({
                            isAnimating = false
                            if (!game.getCurrentPlayer().isHuman && !game.isGameComplete()) {
                                handleAITurn()
                            }
                        }, 500)
                    }
                }, animOut.duration)
            } else {
                // If there was an error, just hide the card
                playCardView.visibility = View.INVISIBLE
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                isAnimating = false
            }
        }, cardPlayDelay)
    }

    // Helper method to show Bastra celebration animation
    private fun showBastraAnimation() {
        // You would implement a special animation here
        // For example, flashing text or special effects
        val bastraText = findViewById<TextView>(R.id.bastraText)
        bastraText.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(this, R.anim.bastra_flash)
        bastraText.startAnimation(anim)

        handler.postDelayed({
            bastraText.visibility = View.INVISIBLE
        }, 1500)
    }

    // Handle a single AI turn with animation
    private fun handleAITurn() {
        if (isAnimating || game.isGameComplete()) return

        isAnimating = true
        val currentPlayer = game.getCurrentPlayer()

        if (!currentPlayer.isHuman) {
            // Show thinking animation
            statusTextView.text = "${currentPlayer.name} is thinking..."

            // Delay to make it feel more natural
            handler.postDelayed({
                // Apply the AI move
                val result = game.playAITurn()

                if (result.success) {
                    // Show the played card in center
                    val playedCardImage = playCardView.findViewById<ImageView>(R.id.playedCardImage)
                    playedCardImage.setImageResource(game.lastPlayedCard!!.getImageResourceId())
                    playCardView.visibility = View.VISIBLE

                    // Apply card play animation
                    val animIn = AnimationUtils.loadAnimation(this, R.anim.card_play_in)
                    playCardView.startAnimation(animIn)

                    // After animation, update the table
                    handler.postDelayed({
                        // Animate card movement to table or captured pile
                        val animOut = AnimationUtils.loadAnimation(this,
                            if (result.captured) R.anim.card_capture_out else R.anim.card_play_out)

                        playCardView.startAnimation(animOut)

                        handler.postDelayed({
                            playCardView.visibility = View.INVISIBLE

                            if (result.isBastra) {
                                // Show Bastra animation
                                showBastraAnimation()
                                Toast.makeText(this, "${currentPlayer.name} got a BASTRA!", Toast.LENGTH_SHORT).show()
                                handler.postDelayed({
                                    updateUI()
                                    isAnimating = false

                                    // If still not human's turn, enable the next button
                                    if (!game.getCurrentPlayer().isHuman && !game.isGameComplete()) {
                                        nextButton.isEnabled = true
                                    }
                                }, 1500) // Extra delay for Bastra
                            } else {
                                updateUI()
                                isAnimating = false

                                // If still not human's turn, enable the next button
                                if (!game.getCurrentPlayer().isHuman && !game.isGameComplete()) {
                                    nextButton.isEnabled = true
                                }
                            }
                        }, animOut.duration)
                    }, cardPlayDelay)
                } else {
                    // If AI play failed, just update UI
                    updateUI()
                    isAnimating = false
                }
            }, aiTurnDelay)
        } else {
            // If it's human turn, just update UI
            updateUI()
            isAnimating = false
        }
    }

    private fun handleGameEnd() {
        game.finalizeGame()

        // Update scores one last time
        val scores = game.getTeamScores()
        team1ScoreTextView.text = "Team 1: ${scores[0]} points"
        team2ScoreTextView.text = "Team 2: ${scores[1]} points"

        // Determine winner
        val winningTeam = game.getWinningTeam()
        val message = if (winningTeam != null) {
            "${winningTeam.name} wins with ${scores[winningTeam.id]} points!"
        } else {
            "It's a tie!"
        }

        // Show game over dialog
        Toast.makeText(this, "Game Over! $message", Toast.LENGTH_LONG).show()

        // Disable interactions
        nextButton.text = "New Game"
        nextButton.setOnClickListener {
            setupGame()
        }
    }
}