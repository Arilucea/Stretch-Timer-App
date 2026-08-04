package com.example.stretchtimer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.stretchtimer.databinding.FragmentRunningBinding

class RunningFragment : Fragment() {
    private var _binding: FragmentRunningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.bindService(requireContext())
        setupObservers()
        binding.buttonStart.setOnClickListener {
            // Stop timer and navigate back to setup screen
            viewModel.stopTimer(requireContext())
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.timerService.observe(viewLifecycleOwner) { service ->
            service?.let { observeService(it) }
        }
    }

    private fun observeService(service: TimerService) {
        service.timeLeft.observe(viewLifecycleOwner) { seconds ->
            binding.seconds.text = seconds?.toString() ?: ""
        }
        service.currentRound.observe(viewLifecycleOwner) { round ->
            activity?.title = "${getString(R.string.round)} $round"
        }
        service.totalRoundsLeft.observe(viewLifecycleOwner) { roundsLeft ->
            binding.roundsLeft.text = "${getString(R.string.roundsLefs)} $roundsLeft"
        }
        service.isTimerRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.buttonStart.text = if (isRunning == true) getString(R.string.buttonCancel) else getString(R.string.buttonStart)
        }
        service.isIntermediate.observe(viewLifecycleOwner) { isInter ->
            if (isInter) {
                // Break between rounds: show pause message and rounds left
                activity?.title = getString(R.string.pause)
                binding.roundsLeft.visibility = View.VISIBLE
            } else {
                // Active round: hide rounds left
                binding.roundsLeft.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.unbindService(requireContext())
        _binding = null
    }
}
