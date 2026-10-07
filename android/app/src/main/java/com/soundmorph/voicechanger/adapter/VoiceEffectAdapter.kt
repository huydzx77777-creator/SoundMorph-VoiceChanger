package com.soundmorph.voicechanger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soundmorph.voicechanger.databinding.ItemVoiceEffectBinding
import com.soundmorph.voicechanger.model.VoiceEffect

class VoiceEffectAdapter(
    private val effects: List<VoiceEffect>,
    private val onPlayClicked: (VoiceEffect) -> Unit,
    private val onSaveClicked: (VoiceEffect) -> Unit,
    private val onShareClicked: (VoiceEffect) -> Unit
) : RecyclerView.Adapter<VoiceEffectAdapter.EffectViewHolder>() {

    private var currentlyPlayingEffect: VoiceEffect? = null

    fun setPlayingEffect(effect: VoiceEffect?) {
        val oldIndex = effects.indexOf(currentlyPlayingEffect)
        currentlyPlayingEffect = effect
        val newIndex = effects.indexOf(currentlyPlayingEffect)

        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val binding = ItemVoiceEffectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EffectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        val effect = effects[position]
        holder.bind(effect, effect == currentlyPlayingEffect)
    }

    override fun getItemCount(): Int = effects.size

    inner class EffectViewHolder(private val binding: ItemVoiceEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(effect: VoiceEffect, isPlaying: Boolean) {
            binding.tvEffectEmoji.text = effect.emoji
            binding.tvEffectTitle.text = effect.title
            binding.tvEffectDesc.text = effect.description

            binding.btnPlayEffect.text = if (isPlaying) "⏹" else "▶"

            binding.btnPlayEffect.setOnClickListener {
                onPlayClicked(effect)
            }
            binding.btnSaveEffect.setOnClickListener {
                onSaveClicked(effect)
            }
            binding.btnShareEffect.setOnClickListener {
                onShareClicked(effect)
            }
        }
    }
}
