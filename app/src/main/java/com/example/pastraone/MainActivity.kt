package com.example.pastraone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.example.pastraone.domain.model.BastraGame


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
//        val tableCards = game.getTableCards()
//        tableCardsView.adapter = CardAdapter(tableCards, { _ ->
//            // Nothing happens when clicking table cards
//        }, resources.getDimensionPixelSize(R.dimen.card_width))
        val tableCards = game.getTableCards()
// Show only the last card if there are cards on the table
        val visibleTableCards = if (tableCards.isNotEmpty()) {
            listOf(tableCards.last())
        } else {
            emptyList()
        }

        tableCardsView.adapter = CardAdapter(visibleTableCards, { _ ->
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
        val pastraText = findViewById<TextView>(R.id.bastraText)
        pastraText.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(this, R.anim.bastra_flash)
        pastraText.startAnimation(anim)

        // Ensure animation cleanup with a firm handler timeout
        handler.removeCallbacksAndMessages(null) // Remove any pending callbacks
        handler.postDelayed({
            pastraText.clearAnimation() // Clear any ongoing animations
            pastraText.visibility = View.INVISIBLE
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