package com.example.stretchtimer

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.stretchtimer.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var totalRounds = 0
    private var roundCounter = 1
    private var roundSeconds = 0
    private var intermediateSeconds = 0
    private var roundTimer: CountDownTimer? = null
    private var intermediateTimer: CountDownTimer? = null
    var timerRunning = false
    var thereIsIntermedateRound = false

    var endRoundSound: MediaPlayer? = null
    var intermediateSound: MediaPlayer? = null
    var finishSessionSound: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity!!.title = getString(R.string.first_fragment_label)
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        endRoundSound = MediaPlayer.create(context, R.raw.alert_sound_mg)
        intermediateSound = MediaPlayer.create(context, R.raw.alert_sound_intermed)
        finishSessionSound = MediaPlayer.create(context, R.raw.alert_sound_end)

        binding.buttonFirst.setOnClickListener {
            parseValues()
            activity!!.title = "Round number $roundCounter"

            if (!timerRunning) {
                roundTimer = startCycle()
                intermediateTimer = createIntermediate()
                timerRunning = true
                binding.buttonFirst.text = "Cancel"
                binding.totalRounds.visibility = View.INVISIBLE
                binding.roundTime.visibility = View.INVISIBLE
                binding.intermediateTime.visibility = View.INVISIBLE
                roundTimer!!.start()
                binding.currentRound.text = "Rounds left " + (totalRounds).toString()
                val imm =
                    context!!.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            } else {
                roundTimer!!.cancel()
                intermediateTimer!!.cancel()
                timerRunning = false
                binding.buttonFirst.text = "Start rounds"
                binding.seconds.text = ""
                binding.totalRounds.visibility = View.VISIBLE
                binding.roundTime.visibility = View.VISIBLE
                binding.intermediateTime.visibility = View.VISIBLE
                binding.currentRound.visibility = View.INVISIBLE
                activity!!.title = getString(R.string.first_fragment_label)
                roundCounter = 1
            }
        }
    }

    private fun parseValues() {
        totalRounds = if (binding.totalRounds.text.toString().isEmpty()) {
            1
        } else {
            binding.totalRounds.text.toString().toInt()
        }
        roundSeconds = if (binding.roundTime.text.toString().isEmpty()) {
            1
        } else {
            binding.roundTime.text.toString().toInt()+1
        }
        if (binding.intermediateTime.text.toString().isEmpty()) {
            intermediateSeconds = 0
        } else {
            intermediateSeconds = binding.intermediateTime.text.toString().toInt()+1
            thereIsIntermedateRound = true
        }
    }

    private fun createTimer(roundTimeMillis: Int): CountDownTimer {
        return object : CountDownTimer(roundTimeMillis.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.seconds.text = "" + (millisUntilFinished / 1000)
            }

            override fun onFinish() {
                totalRounds--
                roundCounter++
                if (totalRounds > 0) {
                    endRoundSound!!.start()
                    if (thereIsIntermedateRound) {
                        intermediateTimer!!.start()
                        activity!!.title = "Between rounds"
                        binding.currentRound.visibility = View.VISIBLE
                        binding.currentRound.text = "Rounds left " + (totalRounds).toString()
                    } else {
                        activity!!.title = "Round number $roundCounter"
                        start()
                    }
                } else {
                    finishSessionSound!!.start()
                    binding.seconds.text = "End!"
                    binding.buttonFirst.text = "Restart"
                    binding.currentRound.visibility = View.INVISIBLE
                    activity!!.title = "End"
                    roundCounter = 1
                }
            }
        }
    }

    private fun createIntermediateTimer(roundTimeMillis: Int): CountDownTimer {
        return object : CountDownTimer(roundTimeMillis.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.seconds.text = "" + (millisUntilFinished / 1000)
            }

            override fun onFinish() {
                activity!!.title = "Round number $roundCounter"
                binding.currentRound.visibility = View.INVISIBLE
                intermediateSound!!.start()
                roundTimer!!.start()
            }
        }
    }

    private fun startCycle(): CountDownTimer? {
        return createTimer(roundSeconds * 1000)
    }

    private fun createIntermediate(): CountDownTimer? {
        return createIntermediateTimer(intermediateSeconds * 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}