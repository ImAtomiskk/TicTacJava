// Github: https://github.com/ImAtomiskk
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

static int     size        = 3;
        static boolean botplay     = false;
        static String  difficulty  = "normal";
        static String  startPlayer = "X";
        static int turnTimerSeconds = 0;
        static String username = "Player";
        static String COLOR_X    = "\u001B[31m";
        static String COLOR_O    = "\u001B[34m";
        static String COLOR_GRID = "\u001B[37m";
        static final String RESET  = "\u001B[0m";
        static final String GREEN  = "\u001B[32m";
        static final String YELLOW = "\u001B[33m";
        static final String PURPLE = "\u001B[35m";
        static final String CYAN   = "\u001B[36m";
        static final String BOLD   = "\u001B[1m";
        static final String RED    = "\u001B[31m";

        static final Map<String, String> COLOR_MAP = Map.ofEntries(
                Map.entry("RED",          "\u001B[31m"),
                Map.entry("GREEN",        "\u001B[32m"),
                Map.entry("YELLOW",       "\u001B[33m"),
                Map.entry("BLUE",         "\u001B[34m"),
                Map.entry("PURPLE",       "\u001B[35m"),
                Map.entry("CYAN",         "\u001B[36m"),
                Map.entry("WHITE",        "\u001B[37m"),
                Map.entry("BOLD_RED",     "\u001B[1;31m"),
                Map.entry("BOLD_GREEN",   "\u001B[1;32m"),
                Map.entry("BOLD_YELLOW",  "\u001B[1;33m"),
                Map.entry("BOLD_BLUE",    "\u001B[1;34m"),
                Map.entry("BOLD_CYAN",    "\u001B[1;36m")
        );

        static final String SCORES_FILE = "scores.ini";

        static boolean networkMode   = false;
        static boolean isServer      = false;
        static String  serverAddress = "localhost";
        static int     networkPort   = 55555;

        void main(String[] args) throws Exception {
            loadScores();
            parseARG(args);

            if (networkMode) runNetworkGame();
            else             runLocalGame();
        }

        void runLocalGame() throws Exception {
            Scanner scanner = new Scanner(System.in);

            final String HUMAN = colorOf("X") + "X" + RESET;
            final String BOT   = colorOf("O") + "O" + RESET;

            String[][] board = newBoard();
            String currentPlayer = startPlayer.equals("O") ? BOT : HUMAN;
            boolean gameOver = false;

            while (!gameOver) {
                clearConsole();
                displayBoard(board);
                IO.println("\n Player " + currentPlayer + "'s turn"
                        + (turnTimerSeconds > 0 ? "  [" + turnTimerSeconds + "s limit]" : ""));

                int row, col;
                if (botplay && currentPlayer.equals(BOT)) {
                    int[] move = botMove(board, difficulty, BOT, HUMAN);
                    row = move[0];
                    col = move[1];
                } else {
                    int[] input = readMoveWithTimer(scanner, turnTimerSeconds);
                    if (input == null) {
                        clearConsole();
                        displayBoard(board);
                        IO.println(YELLOW + BOLD + "\n Time's up! Turn forfeited." + RESET);
                        Thread.sleep(1500);
                        currentPlayer = currentPlayer.equals(HUMAN) ? BOT : HUMAN;
                        continue;
                    }
                    row = input[0];
                    col = input[1];
                }

                if (makeMove(board, row, col, currentPlayer)) {
                    if (checkWin(board, currentPlayer)) {
                        clearConsole();
                        displayBoard(board);
                        IO.println("\n Player " + currentPlayer + " Wins!");
                        String mark = currentPlayer.contains("X") ? "X" : "O";
                        recordWin(mark);
                        printScores();
                        gameOver = true;
                    } else if (isBoardFull(board)) {
                        clearConsole();
                        displayBoard(board);
                        IO.println("\n It's a draw!");
                        recordDraw();
                        printScores();
                        gameOver = true;
                    } else {
                        currentPlayer = currentPlayer.equals(HUMAN) ? BOT : HUMAN;
                    }
                } else {
                    IO.println("Invalid Move! Try again.");
                }
            }
            saveScores();
            scanner.close();
        }

        void runNetworkGame() throws Exception {
            Socket socket;
            if (isServer) {
                IO.println(CYAN + BOLD + " [Server] Waiting for opponent on port " + networkPort + "..." + RESET);
                ServerSocket ss = new ServerSocket(networkPort);
                socket = ss.accept();
                ss.close();
                IO.println(GREEN + " Opponent connected from " + socket.getInetAddress() + RESET);
            } else {
                IO.println(CYAN + BOLD + " [Client] Connecting to " + serverAddress + ":" + networkPort + "..." + RESET);
                socket = new Socket(serverAddress, networkPort);
                IO.println(GREEN + " Connected!" + RESET);
            }

            PrintWriter    out     = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader netIn   = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner        scanner = new Scanner(System.in);

            String opponentName;
            if (isServer) {
                out.println("SIZE="    + size);
                out.println("START="   + startPlayer);
                out.println("TIMER="   + turnTimerSeconds);
                out.println("NAME="    + username);
                String nameLine = netIn.readLine();
                opponentName = (nameLine != null && nameLine.startsWith("NAME="))
                        ? nameLine.substring(5).trim() : "Opponent";
            } else {
                String sizeLine  = netIn.readLine();
                String startLine = netIn.readLine();
                String timerLine = netIn.readLine();
                String nameLine  = netIn.readLine();
                if (sizeLine  != null && sizeLine.startsWith("SIZE="))   size              = Integer.parseInt(sizeLine.substring(5).trim());
                if (startLine != null && startLine.startsWith("START=")) startPlayer       = startLine.substring(6).trim();
                if (timerLine != null && timerLine.startsWith("TIMER=")) turnTimerSeconds  = Integer.parseInt(timerLine.substring(6).trim());
                opponentName = (nameLine != null && nameLine.startsWith("NAME=")) ? nameLine.substring(5).trim() : "Opponent";
                out.println("NAME=" + username);
                IO.println(CYAN + " Synced from server — board: " + size + "x" + size
                        + (turnTimerSeconds > 0 ? ", timer: " + turnTimerSeconds + "s" : "") + RESET);
            }

            final String MY_MARK    = isServer ? "X" : "O";
            final String THEIR_MARK = isServer ? "O" : "X";
            final String MY_SYMBOL    = colorOf(MY_MARK)    + MY_MARK    + RESET;
            final String THEIR_SYMBOL = colorOf(THEIR_MARK) + THEIR_MARK + RESET;

            String myLabel     = BOLD + username      + RESET + " (" + MY_SYMBOL    + ")";
            String theirLabel  = BOLD + opponentName  + RESET + " (" + THEIR_SYMBOL + ")";
            String matchHeader = isServer
                    ? " " + myLabel + "  vs  " + theirLabel
                    : " " + theirLabel + "  vs  " + myLabel;

            String[][] board = newBoard();
            boolean myTurn   = MY_MARK.equals(startPlayer);

            IO.println(matchHeader);
            IO.println(" You are " + MY_SYMBOL + (myTurn ? " — You go first!" : " — Opponent goes first."));
            Thread.sleep(1200);

            boolean gameOver = false;
            while (!gameOver) {
                clearConsole();
                IO.println(matchHeader);
                displayBoard(board);

                if (myTurn) {
                    IO.println("\n Your turn (" + MY_SYMBOL + ")"
                            + (turnTimerSeconds > 0 ? "  [" + turnTimerSeconds + "s]" : ""));

                    int[] input = readMoveWithTimer(scanner, turnTimerSeconds);
                    if (input == null) {
                        out.println("TIMEOUT");
                        clearConsole();
                        IO.println(matchHeader);
                        displayBoard(board);
                        IO.println(YELLOW + BOLD + "\n Time's up! Your turn was forfeited." + RESET);
                        Thread.sleep(1500);
                        myTurn = !myTurn;
                        continue;
                    }

                    int row = input[0], col = input[1];
                    if (!makeMove(board, row, col, MY_SYMBOL)) {
                        IO.println("Invalid move, try again.");
                        continue;
                    }
                    out.println(row + "," + col);

                } else {
                    IO.println("\n Waiting for " + BOLD + opponentName + RESET + "'s move..."
                            + (turnTimerSeconds > 0 ? "  [" + turnTimerSeconds + "s]" : ""));
                    String line = netIn.readLine();
                    if (line == null) { IO.println("Opponent disconnected."); break; }
                    if (line.equals("TIMEOUT")) {
                        clearConsole();
                        IO.println(matchHeader);
                        displayBoard(board);
                        IO.println(YELLOW + "\n " + opponentName + "'s turn timed out — your turn!" + RESET);
                        Thread.sleep(1500);
                        myTurn = !myTurn;
                        continue;
                    }
                    String[] parts = line.split(",");
                    int row = Integer.parseInt(parts[0].trim());
                    int col = Integer.parseInt(parts[1].trim());
                    makeMove(board, row, col, THEIR_SYMBOL);
                }

                String lastMover = myTurn ? MY_SYMBOL : THEIR_SYMBOL;
                if (checkWin(board, lastMover)) {
                    clearConsole();
                    IO.println(matchHeader);
                    displayBoard(board);
                    if (myTurn) {
                        IO.println(GREEN + BOLD + "\n You Win, " + username + "!" + RESET);
                        recordWin(MY_MARK);
                    } else {
                        IO.println(YELLOW + BOLD + "\n " + opponentName + " Wins!" + RESET);
                        recordWin(THEIR_MARK);
                    }
                    printScores();
                    gameOver = true;
                } else if (isBoardFull(board)) {
                    clearConsole();
                    IO.println(matchHeader);
                    displayBoard(board);
                    IO.println(CYAN + "\n It's a draw!" + RESET);
                    recordDraw();
                    printScores();
                    gameOver = true;
                }

                myTurn = !myTurn;
            }
            saveScores();
            socket.close();
            scanner.close();
        }

        int[] readMoveWithTimer(Scanner scanner, int seconds) throws Exception {
            if (seconds <= 0) {
                IO.print(" Enter Row # (0-" + (size - 1) + ") ");
                int row = scanner.nextInt();
                IO.print(" Enter column (0-" + (size - 1) + ") ");
                int col = scanner.nextInt();
                return new int[]{row, col};
            }

            AtomicBoolean inputDone    = new AtomicBoolean(false);
            AtomicBoolean timedOut     = new AtomicBoolean(false);
            int[]         result       = new int[2];

            Thread countdown = new Thread(() -> {
                for (int remaining = seconds; remaining >= 0 && !inputDone.get(); remaining--) {
                    String color = remaining <= 5 ? RED + BOLD : YELLOW;
                    System.out.print("\r " + color + "⏱  " + remaining + "s remaining " + RESET + "   ");
                    System.out.flush();
                    try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                }
                if (!inputDone.get()) timedOut.set(true);
            });
            countdown.setDaemon(true);
            countdown.start();

            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<int[]> future = exec.submit(() -> {
                System.out.print("\n Enter Row # (0-" + (size - 1) + ") ");
                int row = scanner.nextInt();
                System.out.print(" Enter column (0-" + (size - 1) + ") ");
                int col = scanner.nextInt();
                return new int[]{row, col};
            });

            try {
                int[] move = future.get(seconds, TimeUnit.SECONDS);
                inputDone.set(true);
                countdown.interrupt();
                exec.shutdownNow();
                System.out.println();                return move;
            } catch (TimeoutException e) {
                future.cancel(true);
                exec.shutdownNow();
                System.out.println();
                return null;            } catch (ExecutionException e) {
                exec.shutdownNow();
                return null;
            }
        }

        String[][] newBoard() {
            String[][] b = new String[size][size];
            for (String[] r : b) Arrays.fill(r, " ");
            return b;
        }

        void displayBoard(String[][] board) {
            int n = board.length;
            int w = String.valueOf(n - 1).length();
            String G = COLOR_GRID;

            IO.print(" ".repeat(w + 1));
            for (int c = 0; c < n; c++) {
                IO.print(String.format("%" + w + "d", c));
                if (c < n - 1) IO.print(G + "   " + RESET);
            }
            IO.println();

            for (int row = 0; row < n; row++) {
                IO.print(String.format("%" + w + "d ", row));
                for (int col = 0; col < n; col++) {
                    IO.print(" ".repeat(w - 1) + board[row][col]);
                    if (col < n - 1) IO.print(G + " | " + RESET);
                }
                IO.println();
                if (row < n - 1)
                    IO.println(G + " ".repeat(w) + "-".repeat(n * (w + 3) - 2) + RESET);
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
            boolean d1 = true, d2 = true;
            for (int i = 0; i < n; i++) {
                if (!board[i][i].equals(player))         d1 = false;
                if (!board[i][n - 1 - i].equals(player)) d2 = false;
            }
            return d1 || d2;
        }

        boolean isBoardFull(String[][] board) {
            for (String[] row : board)
                for (String cell : row)
                    if (cell.equals(" ")) return false;
            return true;
        }

        int[] botMove(String[][] board, String difficulty, String bot, String human) {
            if (difficulty.equals("hard") && board.length == 3) return bestMove(board, bot, human);
            if (!difficulty.equals("easy")) {
                int[] m = findWinningMove(board, bot);   if (m != null) return m;
                m = findWinningMove(board, human);       if (m != null) return m;
            }
            return randomMove(board);
        }

        int[] randomMove(String[][] board) {
            List<int[]> empty = new ArrayList<>();
            for (int r = 0; r < board.length; r++)
                for (int c = 0; c < board.length; c++)
                    if (board[r][c].equals(" ")) empty.add(new int[]{r, c});
            return empty.get(new Random().nextInt(empty.size()));
        }

        int[] findWinningMove(String[][] board, String player) {
            int n = board.length;
            for (int r = 0; r < n; r++)
                for (int c = 0; c < n; c++)
                    if (board[r][c].equals(" ")) {
                        board[r][c] = player;
                        boolean wins = checkWin(board, player);
                        board[r][c] = " ";
                        if (wins) return new int[]{r, c};
                    }
            return null;
        }

        int[] bestMove(String[][] board, String bot, String human) {
            int bestScore = Integer.MIN_VALUE;
            int[] best = null;
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    if (board[r][c].equals(" ")) {
                        board[r][c] = bot;
                        int score = minimax(board, false, bot, human);
                        board[r][c] = " ";
                        if (score > bestScore) { bestScore = score; best = new int[]{r, c}; }
                    }
            return best;
        }

        int minimax(String[][] board, boolean botTurn, String bot, String human) {
            if (checkWin(board, bot))   return 1;
            if (checkWin(board, human)) return -1;
            if (isBoardFull(board))     return 0;
            int best = botTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    if (board[r][c].equals(" ")) {
                        board[r][c] = botTurn ? bot : human;
                        int score = minimax(board, !botTurn, bot, human);
                        board[r][c] = " ";
                        best = botTurn ? Math.max(best, score) : Math.min(best, score);
                    }
            return best;
        }

        static int winsX = 0, winsO = 0, draws = 0;
        static int streakX = 0, streakO = 0;        static int bestStreakX = 0, bestStreakO = 0;
        void recordWin(String mark) {
            if (mark.equals("X")) {
                winsX++;
                streakX++;
                streakO = 0;
                if (streakX > bestStreakX) bestStreakX = streakX;
            } else {
                winsO++;
                streakO++;
                streakX = 0;
                if (streakO > bestStreakO) bestStreakO = streakO;
            }
        }

        void recordDraw() {
            draws++;
            streakX = 0;
            streakO = 0;
        }

        void printScores() {
            IO.println(BOLD + "\n ── Scores ──────────────────────────" + RESET);
            IO.println(colorOf("X") + String.format("  X  wins: %-4d  streak: %d  (best: %d)", winsX, streakX, bestStreakX) + RESET);
            IO.println(colorOf("O") + String.format("  O  wins: %-4d  streak: %d  (best: %d)", winsO, streakO, bestStreakO) + RESET);
            IO.println(CYAN         + String.format("  Draws  : %d", draws) + RESET);
            IO.println(BOLD         + " ────────────────────────────────────" + RESET);
        }

        static void loadScores() {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(SCORES_FILE)) {
                p.load(fis);
                winsX       = Integer.parseInt(p.getProperty("wins.X",        "0"));
                winsO       = Integer.parseInt(p.getProperty("wins.O",        "0"));
                draws       = Integer.parseInt(p.getProperty("draws",         "0"));
                streakX     = Integer.parseInt(p.getProperty("streak.X",      "0"));
                streakO     = Integer.parseInt(p.getProperty("streak.O",      "0"));
                bestStreakX  = Integer.parseInt(p.getProperty("bestStreak.X", "0"));
                bestStreakO  = Integer.parseInt(p.getProperty("bestStreak.O", "0"));
                COLOR_X     = COLOR_MAP.getOrDefault(p.getProperty("color.X",    "RED"),   COLOR_X);
                COLOR_O     = COLOR_MAP.getOrDefault(p.getProperty("color.O",    "BLUE"),  COLOR_O);
                COLOR_GRID  = COLOR_MAP.getOrDefault(p.getProperty("color.grid", "WHITE"), COLOR_GRID);
                startPlayer      = p.getProperty("startPlayer",      "X");
                username         = p.getProperty("username",         "Player");
                turnTimerSeconds = Integer.parseInt(p.getProperty("turnTimer", "0"));
            } catch (IOException e) {
            }
        }

        void saveScores() {
            Properties p = new Properties();
            p.setProperty("wins.X",       String.valueOf(winsX));
            p.setProperty("wins.O",       String.valueOf(winsO));
            p.setProperty("draws",        String.valueOf(draws));
            p.setProperty("streak.X",     String.valueOf(streakX));
            p.setProperty("streak.O",     String.valueOf(streakO));
            p.setProperty("bestStreak.X", String.valueOf(bestStreakX));
            p.setProperty("bestStreak.O", String.valueOf(bestStreakO));
            p.setProperty("color.X",      colorName(COLOR_X,    "RED"));
            p.setProperty("color.O",      colorName(COLOR_O,    "BLUE"));
            p.setProperty("color.grid",   colorName(COLOR_GRID, "WHITE"));
            p.setProperty("startPlayer",  startPlayer);
            p.setProperty("username",     username);
            p.setProperty("turnTimer",    String.valueOf(turnTimerSeconds));
            try (FileOutputStream fos = new FileOutputStream(SCORES_FILE)) {
                p.store(fos, "TicTacToe scores & preferences");
            } catch (IOException e) {
                System.err.println("Could not save scores: " + e.getMessage());
            }
        }

        String colorName(String ansi, String fallback) {
            return COLOR_MAP.entrySet().stream()
                    .filter(e -> e.getValue().equals(ansi))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(fallback);
        }

        String colorOf(String mark) { return mark.equals("X") ? COLOR_X : COLOR_O; }

        public static void parseARG(String[] args) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--botplay"     -> botplay = true;
                    case "--easy"        -> difficulty = "easy";
                    case "--normal"      -> difficulty = "normal";
                    case "--hard"        -> difficulty = "hard";
                    case "--size" -> {
                        if (i + 1 < args.length) {
                            try { size = Integer.parseInt(args[++i]); }
                            catch (NumberFormatException e) { System.out.println("Invalid size, using 3"); }
                        } else System.out.println("--size needs a number");
                    }
                    case "--startplayer" -> {
                        if (i + 1 < args.length) {
                            String sp = args[++i].toUpperCase();
                            if (sp.equals("X") || sp.equals("O")) startPlayer = sp;
                            else System.out.println("--startplayer must be X or O");
                        }
                    }
                    case "--timer" -> {
                        if (i + 1 < args.length) {
                            try {
                                int t = Integer.parseInt(args[++i]);
                                if (t > 0) turnTimerSeconds = t;
                                else System.out.println("Timer must be > 0");
                            } catch (NumberFormatException e) { System.out.println("Invalid timer value"); }
                        } else System.out.println("--timer needs a number of seconds");
                    }
                    case "--name" -> {
                        if (i + 1 < args.length) username = args[++i];
                        else System.out.println("--name needs a value");
                    }
                    case "--color-x" -> {
                        if (i + 1 < args.length) {
                            String code = COLOR_MAP.get(args[++i].toUpperCase());
                            if (code != null) COLOR_X = code;
                            else System.out.println("Unknown color. Options: " + COLOR_MAP.keySet());
                        }
                    }
                    case "--color-o" -> {
                        if (i + 1 < args.length) {
                            String code = COLOR_MAP.get(args[++i].toUpperCase());
                            if (code != null) COLOR_O = code;
                            else System.out.println("Unknown color. Options: " + COLOR_MAP.keySet());
                        }
                    }
                    case "--color-grid" -> {
                        if (i + 1 < args.length) {
                            String code = COLOR_MAP.get(args[++i].toUpperCase());
                            if (code != null) COLOR_GRID = code;
                            else System.out.println("Unknown color. Options: " + COLOR_MAP.keySet());
                        }
                    }
                    case "--server" -> { networkMode = true; isServer = true; }
                    case "--client" -> {
                        networkMode = true; isServer = false;
                        if (i + 1 < args.length && !args[i + 1].startsWith("--"))
                            serverAddress = args[++i];
                    }
                    case "--port" -> {
                        if (i + 1 < args.length) {
                            try { networkPort = Integer.parseInt(args[++i]); }
                            catch (NumberFormatException e) { System.out.println("Invalid port, using 55555"); }
                        }
                    }
                    case "--reset-scores" -> {
                        winsX = 0; winsO = 0; draws = 0;
                        streakX = 0; streakO = 0; bestStreakX = 0; bestStreakO = 0;
                        System.out.println("Scores reset.");
                    }
                    case "--scores" -> {
                        loadScores();
                        System.out.printf("X wins: %d (streak: %d, best: %d) | O wins: %d (streak: %d, best: %d) | Draws: %d%n",
                                winsX, streakX, bestStreakX, winsO, streakO, bestStreakO, draws);
                        System.exit(0);
                    }
                    case "--help" -> { Help(); System.exit(0); }
                    default -> System.out.println("Unknown argument: " + args[i]);
                }
            }
            if (size < 3) { System.out.println("Size must be at least 3, using 3"); size = 3; }
        }

        public static void clearConsole() {
            try {
                String os = System.getProperty("os.name");
                if (os.contains("Windows")) new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                else                        new ProcessBuilder("clear").inheritIO().start().waitFor();
            } catch (IOException | InterruptedException e) { e.printStackTrace(); }
        }

        static void Help() {
            String help = """
        \u001B[1;36m# TicTacToe — Help\u001B[0m

        \u001B[1;32mBasic usage:\u001B[0m
          java Main                             Local 2-player
          java Main --botplay --hard            Play against hard AI

        \u001B[1;32mUsername:\u001B[0m
          --name <name>                         Set your display name (saved to scores.ini)
                                                Shown as "Alice (X) vs Bob (O)" in network games

        \u001B[1;32mTurn timer:\u001B[0m
          --timer <seconds>                     Each player must move within N seconds
                                                (0 = disabled, saved to scores.ini)
                                                On timeout the turn is forfeited to the opponent

        \u001B[1;32mStarting player:\u001B[0m
          --startplayer X|O                     Choose who goes first (default X)

        \u001B[1;32mColors:\u001B[0m
          --color-x  <COLOR>                    Set X player color
          --color-o  <COLOR>                    Set O player color
          --color-grid <COLOR>                  Set grid/line color
          Colors: RED GREEN YELLOW BLUE PURPLE CYAN WHITE
                  BOLD_RED BOLD_GREEN BOLD_YELLOW BOLD_BLUE BOLD_CYAN

        \u001B[1;32mBoard size:\u001B[0m
          --size <N>                            NxN board (min 3, server controls in network mode)

        \u001B[1;32mNetwork play:\u001B[0m
          java Main --server                    Host a game
          java Main --server --port 12345       Host on a specific port
          java Main --client 192.168.1.5        Join a game at that IP
          java Main --client 192.168.1.5 --port 12345
          The server is X; use --startplayer O to let the client go first.
          Timer and board size are synced automatically from server to client.

        \u001B[1;32mScores:\u001B[0m
          --scores                              Print scores and exit
          --reset-scores                        Reset all scores and streaks to 0
          All preferences are saved in scores.ini automatically.

        \u001B[1;32mDifficulty (bot only):\u001B[0m
          --easy    Random moves
          --normal  Blocks wins and takes winning moves
          --hard    Unbeatable minimax (3x3 only)
        """;
            System.out.println(help);
            try {
                String content = Files.readString(Paths.get("README.md"));
                for (String line : content.split("\n")) {
                    if (line.startsWith("#"))
                        System.out.println(CYAN + BOLD + line + RESET);
                    else if (line.contains("**"))
                        System.out.println(line.replaceAll("\\*\\*(.*?)\\*\\*", BOLD + GREEN + "$1" + RESET));
                    else
                        System.out.println(line);
                }
            } catch (IOException ignored) {}
        }
