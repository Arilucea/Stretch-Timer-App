package com.example.stretchtimer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.stretchtimer.databinding.FragmentFirstBinding
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startTimer()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.bindService(requireContext())
        setupObservers()

        binding.buttonStart.setOnClickListener {
            checkPermissionAndAction()
        }
    }

    private fun setupObservers() {
        viewModel.timerService.observe(viewLifecycleOwner) { service ->
            if (service != null) {
                observeService(service)
            }
        }
    }

    private fun observeService(service: TimerService) {


        service.isTimerRunning.observe(viewLifecycleOwner) { isRunning ->
            updateUiState(isRunning)
            if (!isRunning) {
                val roundsLeft = service.totalRoundsLeft.value
                if (roundsLeft == 0) {
                    binding.buttonStart.text = getString(R.string.buttonRestart)
                    requireActivity().title = "End"
                } else if (roundsLeft == null) {
                    requireActivity().title = getString(R.string.first_fragment_label)
                }
            }
        }

        service.isIntermediate.observe(viewLifecycleOwner) { isIntermediate ->
            if (service.isTimerRunning.value == true) {
                if (isIntermediate) {
                    requireActivity().title = "Between rounds"
                } else {
                    requireActivity().title = "${getString(R.string.round)} ${service.currentRound.value}"
                }
            }
        }
    }

    private fun checkPermissionAndAction() {
        if (viewModel.timerService.value?.isTimerRunning?.value == true) {
            viewModel.stopTimer(requireContext())
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startTimer()
            }
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        val totalRoundsText = binding.totalRounds.text.toString()
        val roundTimeText = binding.roundTime.text.toString()
        val intermediateTimeText = binding.intermediateTime.text.toString()

        val totalRounds = if (totalRoundsText.isEmpty()) 1 else totalRoundsText.toInt()
        val roundSeconds = if (roundTimeText.isEmpty()) 1 else roundTimeText.toInt() + 1
        val intermediateSeconds = if (intermediateTimeText.isEmpty()) 0 else intermediateTimeText.toInt() + 1

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)

        viewModel.startTimer(requireContext(), totalRounds, roundSeconds, intermediateSeconds)
        findNavController().navigate(R.id.RunningFragment)
    }

    private fun updateUiState(isRunning: Boolean) {
        if (isRunning) {
            binding.buttonStart.text = getString(R.string.buttonCancel)
            binding.totalRounds.visibility = View.INVISIBLE
            binding.roundTime.visibility = View.INVISIBLE
            binding.intermediateTime.visibility = View.INVISIBLE
        } else {
            binding.buttonStart.text = getString(R.string.buttonStart)
            binding.totalRounds.visibility = View.VISIBLE
            binding.roundTime.visibility = View.VISIBLE
            binding.intermediateTime.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.unbindService(requireContext())
        _binding = null
    }
}
