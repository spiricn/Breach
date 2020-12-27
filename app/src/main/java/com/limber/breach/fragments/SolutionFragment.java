package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.limber.breach.Analyzer;
import com.limber.breach.DrawUtils;
import com.limber.breach.R;
import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.Path;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;

import java.util.ArrayList;
import java.util.List;

public class SolutionFragment extends Fragment {
    public SolutionFragment() {
        super(R.layout.fragment_solution);
    }

    SolutionFragmentArgs mArgs;
    NumberPicker mNumberPicker;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        mArgs = SolutionFragmentArgs.fromBundle(getArguments());

        ((Button) view.findViewById(R.id.btnRetry)).setOnClickListener((View.OnClickListener) view1 -> {
            NavDirections action = SolutionFragmentDirections.actionSolutionFragmentToCaptureFragment();
            Navigation.findNavController(getView()).navigate(action);
        });

        mNumberPicker = view.findViewById(R.id.bufferSize);
        mNumberPicker.setMaxValue(8);
        mNumberPicker.setMinValue(4);

        ((SurfaceView) view.findViewById(R.id.solutionSurface)).getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                draw(surfaceHolder);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });

        ((Button) view.findViewById(R.id.btnSolve)).setOnClickListener((View.OnClickListener) v -> {


            new Thread(() -> {
                Analyzer.Result result = mArgs.getResult();

                List<List<Integer>> matrix = convertRows(result.matrix.nodes);
                List<List<Integer>> sequences = convertRows(result.sequences.nodes);


                Snackbar.make(getView(),
                        "[ BREACHING .. ]", Snackbar.LENGTH_LONG)
                        .show();

                PathScore path = Solver.solve(matrix, sequences, mNumberPicker.getValue());
                showResult(path);
            }).start();
        });
    }

    SurfaceHolder mHolder;

    void draw(SurfaceHolder holder) {
        mHolder = holder;

        Canvas canvas = holder.lockCanvas();

        Analyzer.Result result = mArgs.getResult();

        DrawUtils.drawGrid(result.matrix, result.bitmap, canvas);

        holder.unlockCanvasAndPost(canvas);
    }

    void showResult(PathScore pathScore) {

        Path path = pathScore.path();

        Canvas canvas = mHolder.lockCanvas();

        DrawUtils.drawGrid(mArgs.getResult().matrix, mArgs.getResult().bitmap, canvas);

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(60);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setColor(Color.argb(200, 255, 0, 0));

        int stepCounter = 0;
        for (Coordinate coord : path.coordinates()) {

            Analyzer.GridNode resNode = mArgs.getResult().matrix.nodes.get(coord.row).get(coord.column);

            canvas.drawText("" + stepCounter, resNode.boundingBox.left, resNode.boundingBox.top, textPaint);

            stepCounter++;
        }

        mHolder.unlockCanvasAndPost(canvas);
    }


    static List<List<Integer>> convertRows(List<List<Analyzer.GridNode>> inRows) {
        List<List<Integer>> rows = new ArrayList<>();

        for (List<Analyzer.GridNode> row : inRows) {
            List<Integer> irow = new ArrayList<>();
            for (Analyzer.GridNode node : row) {
                irow.add(Integer.parseInt(node.text, 16));
            }

            rows.add(irow);
        }

        return rows;
    }

}
