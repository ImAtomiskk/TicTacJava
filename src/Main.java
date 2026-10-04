//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


// Github: https://github.com/ImAtomiskk
import java.util.*;
import java.io.*;

void main(String[] args) {
    parseARG(args);

    Scanner scanner = new Scanner(System.in);
    final String HUMAN = RED + "X" + RESET;
    final String BOT = BLUE + "O" + RESET;

    String[][] board = new String[size][size];
    for (String[] r : board) Arrays.fill(r, " ");

    String currentPlayer = HUMAN;
    boolean gameOver = false;

    while (!gameOver) {
        clearConsole();
        displayBoard(board);
        IO.println("\n Player " + currentPlayer + "'s turn");

        int row, col;
        if (botplay && currentPlayer.equals(BOT)) {
            int[] move = botMove(board, difficulty, BOT, HUMAN);
            row = move[0];
            col = move[1];
        } else {
            IO.print(" Enter Row # (0-" + (size - 1) + ") ");
            row = scanner.nextInt();
            IO.print(" Enter column (0-" + (size - 1) + ") ");
            col = scanner.nextInt();
        }

        if (makeMove(board, row, col, currentPlayer)) {
            if (checkWin(board, currentPlayer)) {
                clearConsole();
                displayBoard(board);
                IO.println("\n Player " + currentPlayer + " Wins! ");
                gameOver = true;
            } else if (isBoardFull(board)) {
                clearConsole();
                displayBoard(board);
                IO.println("\n It's a draw");
                gameOver = true;
            } else {
                currentPlayer = currentPlayer.equals(HUMAN) ? BOT : HUMAN;
            }
        } else {
            clearConsole();
            IO.println("Invalid Move! Try again.");
        }
    }
    scanner.close();
}


void displayBoard(String[][] board) {
    int n = board.length;
    int w = String.valueOf(n - 1).length();

    IO.print(" ".repeat(w + 1));
    for (int c = 0; c < n; c++) {
        IO.print(String.format("%" + w + "d", c));
        if (c < n - 1) IO.print("   ");
    }
    IO.println();

    for (int row = 0; row < n; row++) {
        IO.print(String.format("%" + w + "d ", row));
        for (int col = 0; col < n; col++) {
            IO.print(" ".repeat(w - 1) + board[row][col]);
            if (col < n - 1) IO.print(" | ");
        }
        IO.println();
        if (row < n - 1) {
            IO.println(" ".repeat(w) + "-".repeat(n * (w + 3) - 2));
        }
    }
}

boolean makeMove(String[][] board, int row, int col, String player) {
    int n = board.length;
    if (row >= 0 && row < n && col >= 0 && col < n && board[row][col].equals(" ")) {
        board[row][col] = player;
        return true;
    }
    return false;
}

boolean checkWin(String[][] board, String player) {
    int n = board.length;

    for (int i = 0; i < n; i++) {
        boolean rowWin = true, colWin = true;
        for (int j = 0; j < n; j++) {
            if (!board[i][j].equals(player)) rowWin = false;
            if (!board[j][i].equals(player)) colWin = false;
        }
        if (rowWin || colWin) return true;
    }

    boolean diag1 = true, diag2 = true;
    for (int i = 0; i < n; i++) {
        if (!board[i][i].equals(player)) diag1 = false;
        if (!board[i][n - 1 - i].equals(player)) diag2 = false;
    }
    return diag1 || diag2;
}

boolean isBoardFull(String[][] board) {
    for (String[] row : board)
        for (String cell : row)
            if (cell.equals(" ")) return false;
    return true;
}

public static void clearConsole() {
    try {
        String os = System.getProperty("os.name");

        if (os.contains("Windows")) {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } else {
            new ProcessBuilder("clear").inheritIO().start().waitFor();
        }
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
    }
}

public static void parseARG(String[] args) {
    for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
            case "--botplay" -> botplay = true;
            case "--easy"    -> difficulty = "easy";
            case "--normal"  -> difficulty = "normal";
            case "--hard"    -> difficulty = "hard";
            case "--size" -> {
                if (i + 1 < args.length) {
                    try {
                        size = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid size, using 3");
                    }
                } else {
                    System.out.println("--size needs a number");
                }
            }
            default -> System.out.println("Unknown argument: " + args[i]);
        }
    }
    if (size < 3) {
        System.out.println("Size must be at least 3, using 3");
        size = 3;
    }
}

int[] botMove(String[][] board, String difficulty, String bot, String human) {
    if (difficulty.equals("hard") && board.length == 3) {
        return bestMove(board, bot, human);
    }
    if (!difficulty.equals("easy")) {
        int[] move = findWinningMove(board, bot);
        if (move != null) return move;
        move = findWinningMove(board, human);
        if (move != null) return move;
    }
    return randomMove(board);
}

int[] randomMove(String[][] board) {
    List<int[]> empty = new ArrayList<>();
    for (int r = 0; r < 3; r++)
        for (int c = 0; c < 3; c++)
            if (board[r][c].equals(" ")) empty.add(new int[]{r, c});
    return empty.get(new Random().nextInt(empty.size()));
}

int[] findWinningMove(String[][] board, String player) {
    for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
            if (board[r][c].equals(" ")) {
                board[r][c] = player;
                boolean wins = checkWin(board, player);
                board[r][c] = " ";
                if (wins) return new int[]{r, c};
            }
        }
    }
    return null;
}

int[] bestMove(String[][] board, String bot, String human) {
    int bestScore = Integer.MIN_VALUE;
    int[] best = null;
    for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
            if (board[r][c].equals(" ")) {
                board[r][c] = bot;
                int score = minimax(board, false, bot, human);
                board[r][c] = " ";
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{r, c};
                }
            }
        }
    }
    return best;
}

int minimax(String[][] board, boolean botTurn, String bot, String human) {
    if (checkWin(board, bot)) return 1;
    if (checkWin(board, human)) return -1;
    if (isBoardFull(board)) return 0;

    int best = botTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (int r = 0; r < 3; r++) {
        for (int c = 0; c < 3; c++) {
            if (board[r][c].equals(" ")) {
                board[r][c] = botTurn ? bot : human;
                int score = minimax(board, !botTurn, bot, human);
                board[r][c] = " ";
                best = botTurn ? Math.max(best, score) : Math.min(best, score);
            }
        }
    }
    return best;
}
static int size = 3;
static boolean botplay = false;
static String difficulty = "normal";
public static final String RESET = "\u001B[0m";
public static final String RED = "\u001B[31m";
public static final String GREEN = "\u001B[32m";
public static final String YELLOW = "\u001B[33m";
public static final String BLUE = "\u001B[34m";
public static final String PURPLE = "\u001B[35m";
public static final String CYAN = "\u001B[36m";