package com.example.tictactoeplus;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageButton[][] buttons = new ImageButton[3][3];
    private boolean xTurn = true;

    private Queue<int[]> xMoves = new LinkedList<>();
    private Queue<int[]> oMoves = new LinkedList<>();

    private boolean showFadedMarks = true;
    private boolean playVsAI = false;

    private Button settingsButton, aiToggleButton, aiModeButton, resetScoreButton;
    private TextView xScoreText, oScoreText;

    private int xScore = 0;
    private int oScore = 0;

    private final Random random = new Random();

    private enum AIMode { EASY, NORMAL, HARD, NIGHTMARE }
    private AIMode aiMode = AIMode.NORMAL;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridLayout gridLayout = findViewById(R.id.gridLayout);
        settingsButton = findViewById(R.id.settingsButton);
        aiToggleButton = findViewById(R.id.aiToggleButton);
        aiModeButton = findViewById(R.id.aiModeButton);
        resetScoreButton = findViewById(R.id.resetScoreButton);
        xScoreText = findViewById(R.id.xScoreText);
        oScoreText = findViewById(R.id.oScoreText);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                ImageButton button = (ImageButton) gridLayout.getChildAt(index);
                buttons[i][j] = button;

                final int row = i;
                final int col = j;

                button.setOnClickListener(v -> handleMove(row, col));
            }
        }

        settingsButton.setOnClickListener(v -> {
            showFadedMarks = !showFadedMarks;
            applyFading();
            settingsButton.setText(showFadedMarks ? "Disable Fading" : "Enable Fading");
        });

        aiToggleButton.setOnClickListener(v -> {
            playVsAI = !playVsAI;
            aiToggleButton.setText(playVsAI ? "Play vs Human" : "Play vs AI");
            resetBoard();
        });

        aiModeButton.setOnClickListener(v -> {
            switch (aiMode) {
                case EASY: aiMode = AIMode.NORMAL; break;
                case NORMAL: aiMode = AIMode.HARD; break;
                case HARD: aiMode = AIMode.NIGHTMARE; break;
                case NIGHTMARE: aiMode = AIMode.EASY; break;
            }
            aiModeButton.setText("Mode: " + aiMode.name());
            resetBoard();
        });

        resetScoreButton.setOnClickListener(v -> {
            xScore = 0;
            oScore = 0;
            updateScores();
        });

        aiModeButton.setText("Mode: " + aiMode.name());
        updateScores();
        showInstructions();
    }

    private void showInstructions() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_instructions, null);

        new AlertDialog.Builder(this)
                .setTitle("How to Play")
                .setView(dialogView)
                .setPositiveButton("Got it!", null)
                .show();
    }


    private void handleMove(int row, int col) {
        ImageButton button = buttons[row][col];
        if (button.getDrawable() != null) return;

        if (xTurn) {
            button.setImageResource(R.drawable.x_tack);
            button.setAlpha(1.0f);
            xMoves.add(new int[]{row, col});
            if (xMoves.size() > 3) {
                int[] oldest = xMoves.poll();
                buttons[oldest[0]][oldest[1]].setImageDrawable(null);
                buttons[oldest[0]][oldest[1]].setAlpha(1.0f);
            }
            highlightOldestX();

            if (checkWin(R.drawable.x_tack)) {
                xScore++;
                updateScores();
                showWinDialog("X");
                return;
            }

            xTurn = false;

            if (playVsAI) {
                handler.postDelayed(this::aiMove, 400);
            }

        } else {
            button.setImageResource(R.drawable.o_tack);
            button.setAlpha(1.0f);
            oMoves.add(new int[]{row, col});
            if (oMoves.size() > 3) {
                int[] oldest = oMoves.poll();
                buttons[oldest[0]][oldest[1]].setImageDrawable(null);
                buttons[oldest[0]][oldest[1]].setAlpha(1.0f);
            }
            highlightOldestO();

            if (checkWin(R.drawable.o_tack)) {
                oScore++;
                updateScores();
                showWinDialog("O");
                return;
            }

            xTurn = true;
        }
    }

    private void aiMove() {
        int[] move = null;

        switch (aiMode) {
            case EASY:
                move = getRandomMove();
                break;
            case NORMAL:
                move = getWinningOrBlockingMove(R.drawable.o_tack);
                if (move == null) move = getWinningOrBlockingMove(R.drawable.x_tack);
                if (move == null) move = getRandomMove();
                break;
            case HARD:
                move = getBestMove(false);
                break;
            case NIGHTMARE:
                move = oMoves.isEmpty() ? getRandomMove() : getBestMove(true);
                break;
        }

        if (move != null) {
            handleMove(move[0], move[1]);
        }
    }

    private int[] getWinningOrBlockingMove(int symbolResId) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getDrawable() == null) {
                    buttons[i][j].setImageResource(symbolResId);
                    boolean wins = checkWin(symbolResId);
                    buttons[i][j].setImageDrawable(null);
                    if (wins) return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private int[] getRandomMove() {
        List<int[]> available = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getDrawable() == null) {
                    available.add(new int[]{i, j});
                }
            }
        }
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private int[] getBestMove(boolean fullDepth) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getDrawable() == null) {
                    buttons[i][j].setImageResource(R.drawable.o_tack);
                    int score = minimax(0, false, fullDepth);
                    buttons[i][j].setImageDrawable(null);
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new int[]{i, j};
                    }
                }
            }
        }

        return bestMove;
    }

    private int minimax(int depth, boolean isMaximizing, boolean fullDepth) {
        if (checkWin(R.drawable.o_tack)) return 10 - depth;
        if (checkWin(R.drawable.x_tack)) return depth - 10;

        boolean full = true;
        for (ImageButton[] row : buttons) {
            for (ImageButton b : row) {
                if (b.getDrawable() == null) {
                    full = false;
                    break;
                }
            }
        }
        if (full || (!fullDepth && depth >= 3)) return 0;

        int best = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int symbol = isMaximizing ? R.drawable.o_tack : R.drawable.x_tack;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j].getDrawable() == null) {
                    buttons[i][j].setImageResource(symbol);
                    int score = minimax(depth + 1, !isMaximizing, fullDepth);
                    buttons[i][j].setImageDrawable(null);
                    best = isMaximizing ? Math.max(best, score) : Math.min(best, score);
                }
            }
        }

        return best;
    }

    private void highlightOldestX() {
        for (int[] move : xMoves) {
            buttons[move[0]][move[1]].setAlpha(1.0f);
        }
        if (showFadedMarks && xMoves.size() == 3) {
            int[] oldest = xMoves.peek();
            buttons[oldest[0]][oldest[1]].setAlpha(0.4f);
        }
    }

    private void highlightOldestO() {
        for (int[] move : oMoves) {
            buttons[move[0]][move[1]].setAlpha(1.0f);
        }
        if (showFadedMarks && oMoves.size() == 3) {
            int[] oldest = oMoves.peek();
            buttons[oldest[0]][oldest[1]].setAlpha(0.4f);
        }
    }

    private void applyFading() {
        if (aiMode == AIMode.NIGHTMARE) return;
        highlightOldestX();
        highlightOldestO();
    }

    private boolean checkWin(int drawableId) {
        for (int i = 0; i < 3; i++) {
            if (checkLine(drawableId, buttons[i][0], buttons[i][1], buttons[i][2])) return true;
            if (checkLine(drawableId, buttons[0][i], buttons[1][i], buttons[2][i])) return true;
        }
        return checkLine(drawableId, buttons[0][0], buttons[1][1], buttons[2][2]) ||
                checkLine(drawableId, buttons[0][2], buttons[1][1], buttons[2][0]);
    }

    private boolean checkLine(int drawableId, ImageButton b1, ImageButton b2, ImageButton b3) {
        return compareDrawable(b1, drawableId) &&
                compareDrawable(b2, drawableId) &&
                compareDrawable(b3, drawableId);
    }

    private boolean compareDrawable(ImageButton button, int resId) {
        return button.getDrawable() != null &&
                button.getDrawable().getConstantState() != null &&
                button.getDrawable().getConstantState().equals(getDrawable(resId).getConstantState());
    }

    private void updateScores() {
        xScoreText.setText("X: " + xScore);
        oScoreText.setText("O: " + oScore);
    }

    private void resetBoard() {
        for (ImageButton[] row : buttons) {
            for (ImageButton btn : row) {
                btn.setImageDrawable(null);
                btn.setAlpha(1.0f);
            }
        }

        xMoves.clear();
        oMoves.clear();

        if (aiMode == AIMode.NIGHTMARE) {
            showFadedMarks = false;
            settingsButton.setText("Fading Disabled");
        }

        xTurn = !(playVsAI && aiMode == AIMode.NIGHTMARE);
        if (!xTurn) {
            handler.postDelayed(this::aiMove, 200);
        }
    }

    private void showWinDialog(String winner) {
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(winner + " wins!\nPlay again?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> resetBoard())
                .setNegativeButton("No", (dialog, which) -> finish())
                .show();
    }
}
