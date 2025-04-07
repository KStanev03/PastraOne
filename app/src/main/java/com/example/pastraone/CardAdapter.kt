package com.example.pastraone

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

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