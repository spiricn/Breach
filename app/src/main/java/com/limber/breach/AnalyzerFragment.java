package com.limber.breach;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

public class AnalyzerFragment extends Fragment {
    public AnalyzerFragment() {
        super(R.layout.fragment_analyze);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        AnalyzerFragmentArgs args = AnalyzerFragmentArgs.fromBundle(getArguments());

        Analyzer.analyze(args.getBitmap(), result -> {
            NavDirections action = AnalyzerFragmentDirections.actionAnalyzerFragmentToFragmentVerify(result);

            Navigation.findNavController(getView()).navigate(action);

        }, error -> {
            Snackbar.make(getView(),
                    "Analyze failed " + error.getMessage(), Snackbar.LENGTH_LONG)
                    .show();

            NavDirections action = AnalyzerFragmentDirections.actionAnalyzerFragmentToCaptureFragment();
            Navigation.findNavController(getView()).navigate(action);
        });
    }

}
