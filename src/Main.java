// Github: https://github.com/ImAtomiskk
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

static int size = 3;
static boolean botplay = false;
static String difficulty = "normal";
static String startPlayer = "X";
static int turnTimerSeconds = 0;
static String username = "Player";
static final String BUILD_VERSION = "1.1";
static String COLOR_X = "\u001B[31m";
static String COLOR_O = "\u001B[34m";
static String COLOR_GRID = "\u001B[37m";
static final String RESET = "\u001B[0m";
static final String GREEN = "\u001B[32m";
static final String YELLOW = "\u001B[33m";
static final String PURPLE = "\u001B[35m";
static final String CYAN = "\u001B[36m";
static final String BOLD = "\u001B[1m";
static final String RED = "\u001B[31m";

static final Map<String, String> COLOR_MAP = Map.ofEntries(
        Map.entry("RED", "\u001B[31m"), Map.entry("GREEN", "\u001B[32m"),
        Map.entry("YELLOW", "\u001B[33m"), Map.entry("BLUE", "\u001B[34m"),
        Map.entry("PURPLE", "\u001B[35m"), Map.entry("CYAN", "\u001B[36m"),
        Map.entry("WHITE", "\u001B[37m"), Map.entry("BOLD_RED", "\u001B[1;31m"),
        Map.entry("BOLD_GREEN", "\u001B[1;32m"), Map.entry("BOLD_YELLOW", "\u001B[1;33m"),
        Map.entry("BOLD_BLUE", "\u001B[1;34m"), Map.entry("BOLD_CYAN", "\u001B[1;36m")
);

static final String SCORES_FILE = "scores.ini";
static final String SAVE_FILE = "savegame.properties";
static final String FRIENDS_FILE = "friends.ini";
static final String SERVERS_FILE = "servers.ini";
static final List<ServerEntry> configuredServers = new ArrayList<>();
static final int FRIENDS_PORT = 2002;
static final int FRIEND_TIMEOUT_MS = 700;
static final Map<String, Friend> friends = new LinkedHashMap<>();
static final String MODE_CLASSIC = "classic";
static final String MODE_MISERE = "misere";
static final String MODE_CONNECT = "connect";
static String gameMode = MODE_CLASSIC;
static boolean uiMode = false;
static boolean resumeRequested = false;
static boolean ansiEnabled = true;
static boolean networkMode = false;
static boolean isServer = false;
static String serverAddress = "eclipse.2bd.net";
static String ATOMICWFC_HOST = "eclipse.2bd.net";
static int networkPort = 2000;
static final int DISCOVERY_PORT = 2001;
static int serverRooms = 16;
static String serverName = "Local Server";
static String requestedRoom = "1";
static int discoveryTimeoutMs = 1500;
static boolean connected;
static AtomicServerInfo atomicServerInfo;
static final int USERNAME_MAX = 20;

static boolean validOnlineUsername(String name) {
    if (name == null) return false;
    String n = name.trim();
    return !n.isEmpty() && n.length() <= USERNAME_MAX && n.matches("[A-Za-z0-9 ]+");
}

static String sanitizeDisplayName(String name) {
    return name == null ? "" : name.replaceAll("[^A-Za-z0-9 ]", "").trim();
}

static class AtomicServerInfo {
    String name = "AtomicWFC Online";
    String motd = "";
    int season = 1;
    long seasonEnd = 0;
    int myPoints = 0;
    String myRank = "Bronze";
    String myBadge = "Rookie";
    List<String> leaderboard = new ArrayList<>();
}

        static int winsX = 0, winsO = 0, draws = 0;
        static int streakX = 0, streakO = 0, bestStreakX = 0, bestStreakO = 0;
        static int gamesPlayed = 0, movesPlayed = 0, timeouts = 0;
static int credits = 100;
static final Map<String, Integer> powerups = new TreeMap<>();
static final String POWER_UNDO = "undo";
static final String POWER_FREEZE = "freeze";
static final String POWER_EXTRA = "extra";
static final String POWER_SHIELD = "shield";
static final Set<String> achievements = new TreeSet<>();

void main(String[] args) throws Exception {
    loadScores();
    loadFriends();
    loadConfiguredServers();
    parseARG(args);
    if (!ansiEnabled) disableColors();
    showSplash();
    clearConsole();
    startFriendPresenceResponder();
    connected = pingWithAnimation("github.com");
    if (networkMode) {
        if (isServer && uiMode) networkMenu();
        else runNetworkGame();
    } else if (uiMode) {
        mainMenu();
    } else {
        runLocalGame();
    }
}

void mainMenu() throws Exception {
    Scanner scanner = new Scanner(System.in);
    if (resumeRequested) {
        resumeRequested = false;
        if (Files.exists(Paths.get(SAVE_FILE))) resumeGame(scanner);
        else pauseMessage(scanner, "No saved single-player game found.");
    }
    while (true) {
        clearConsole();
        refreshFriendStatuses();
        int onlineFriends = countOnlineFriends();
        IO.println(CYAN + BOLD + "╔══════════════════════════════════════╗" + RESET);
        IO.println(CYAN + "║       Tic-Tac-Java v" + BUILD_VERSION + "              ║" + RESET);
        IO.println(CYAN + "╠══════════════════════════════════════╣" + RESET);
        IO.println("║ Player: " + BOLD + String.format("%-28s", username) + RESET + " ║");
        IO.println("║ Mode:  " + String.format("%-10s", gameModeName(gameMode)) + " Board: " + String.format("%-12s", size + "x" + size) + "║");
        String status = connected ? "Connected" : "Disconnected";
        IO.println("║ AtomicWFC Status: " + String.format("%-17s", status) + "  ║");
        IO.println("║ Friends: " + String.format("%-27s", friends.size()) + " ║");
        IO.println("║ Friends Online: " + String.format("%-21s", onlineFriends) + "║");
        IO.println("║ Credits: " + String.format("%-27s", credits) + " ║");
        IO.println(CYAN + "╚══════════════════════════════════════╝" + RESET);
        IO.println("");
        IO.println("  1. Local 2-player game");
        IO.println("  2. Play against AI");
        IO.println("  3. Resume saved game");
        IO.println("  4. Change game settings");
        IO.println("  5. Scores & achievements");
        IO.println("  6. Network game");
        IO.println("  7. Help");
        IO.println("  8. Friends");
        IO.println("  9. Shop & powerups");
        IO.println("  0. Quit");
        IO.print("\n Select: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> { botplay = false; if (gameSetupMenu(scanner, false)) runLocalGame(scanner, false); }
            case "2" -> { botplay = true; if (gameSetupMenu(scanner, true)) runLocalGame(scanner, false); }
            case "3" -> { if (!resumeGame(scanner)) pauseMessage(scanner, "No saved game found."); }
            case "4" -> settingsMenu(scanner);
            case "5" -> { clearConsole(); printScores(); printAchievements(); pauseMessage(scanner, "Press Enter to return..."); }
            case "6" -> networkMenu(scanner);
            case "7" -> { clearConsole(); Help(); pauseMessage(scanner, "Press Enter to return..."); }
            case "8" -> friendsMenu(scanner);
            case "9" -> shopMenu(scanner);
            case "0", "q", "quit", "exit" -> { saveScores(); saveFriends(); return; }
            default -> pauseMessage(scanner, "Invalid selection.");
        }
    }
}

void shopMenu(Scanner scanner) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Atomic Shop ─────────────────────────" + RESET);
        IO.println("  Credits: " + YELLOW + credits + RESET);
        IO.println("");
        IO.println("  1. Undo Move       30 credits   (undo your previous move)");
        IO.println("  2. Freeze AI       40 credits   (skip the AI's next turn)");
        IO.println("  3. Extra Turn      50 credits   (take another move after yours)");
        IO.println("  4. Shield          60 credits   (block one AI winning move)");
        IO.println("");
        IO.println("  Inventory:");
        IO.println("    Undo: " + powerupCount(POWER_UNDO) + "   Freeze: " + powerupCount(POWER_FREEZE)
                + "   Extra: " + powerupCount(POWER_EXTRA) + "   Shield: " + powerupCount(POWER_SHIELD));
        IO.println("  0. Back");
        IO.print("\n Select: ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("0")) { saveScores(); return; }
        String type = switch (choice) { case "1" -> POWER_UNDO; case "2" -> POWER_FREEZE; case "3" -> POWER_EXTRA; case "4" -> POWER_SHIELD; default -> null; };
        if (type == null) { pauseMessage(scanner, "Invalid selection."); continue; }
        int cost = powerupCost(type);
        if (credits < cost) { pauseMessage(scanner, "Not enough credits."); continue; }
        credits -= cost;
        powerups.merge(type, 1, Integer::sum);
        saveScores();
        pauseMessage(scanner, "Purchased " + powerupName(type) + ".");
    }
}

static int powerupCount(String type) { return powerups.getOrDefault(type, 0); }
static int powerupCost(String type) { return switch (type) { case POWER_UNDO -> 30; case POWER_FREEZE -> 40; case POWER_EXTRA -> 50; case POWER_SHIELD -> 60; default -> 0; }; }
static String powerupName(String type) { return switch (type) { case POWER_UNDO -> "Undo Move"; case POWER_FREEZE -> "Freeze AI"; case POWER_EXTRA -> "Extra Turn"; case POWER_SHIELD -> "Shield"; default -> type; }; }
static boolean consumePowerup(String type) { int n = powerupCount(type); if (n <= 0) return false; if (n == 1) powerups.remove(type); else powerups.put(type, n - 1); return true; }

static class Friend {
    String name;
    String address;
    int port;
    boolean online;
    long pingMs;
    Friend(String name, String address, int port) { this.name = name; this.address = address; this.port = port; }
}

void friendsMenu(Scanner scanner) {
    while (true) {
        refreshFriendStatuses();
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Friends ─────────────────────────────" + RESET);
        IO.println("  You: " + BOLD + username + RESET);
        IO.println("  Online: " + countOnlineFriends() + "/" + friends.size());
        IO.println("");
        if (friends.isEmpty()) {
            IO.println(YELLOW + "  No friends added yet." + RESET);
            IO.println("  Add someone using their display name and network address.\n");
        } else {
            int i = 1;
            for (Friend f : friends.values()) {
                String status = f.online ? GREEN + "● Online" + RESET + " (" + f.pingMs + " ms)" : RED + "○ Offline" + RESET;
                IO.println("  " + i++ + ". " + BOLD + f.name + RESET + " — " + status + "  [" + f.address + ":" + f.port + "]");
            }
            IO.println("");
        }
        IO.println("  1. Add friend");
        IO.println("  2. Remove friend");
        IO.println("  3. Refresh statuses");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1" -> addFriend(scanner);
            case "2" -> removeFriend(scanner);
            case "3" -> { refreshFriendStatuses(); pauseMessage(scanner, "Friend statuses refreshed."); }
            case "0" -> { saveFriends(); return; }
            default -> pauseMessage(scanner, "Invalid selection.");
        }
    }
}

void addFriend(Scanner scanner) {
    clearConsole();
    IO.println(CYAN + BOLD + "\n── Add Friend ──────────────────────────" + RESET);
    IO.print(" Display name: ");
    String name = scanner.nextLine().trim();
    if (name.isBlank()) { pauseMessage(scanner, "A friend name is required."); return; }
    if (name.equalsIgnoreCase(username)) { pauseMessage(scanner, "You cannot add yourself."); return; }
    IO.print(" Address / hostname: ");
    String address = scanner.nextLine().trim();
    if (address.isBlank()) { pauseMessage(scanner, "An address is required."); return; }
    IO.print(" Presence port [" + FRIENDS_PORT + "]: ");
    String portText = scanner.nextLine().trim();
    int port = FRIENDS_PORT;
    if (!portText.isBlank()) {
        try { port = clamp(Integer.parseInt(portText), 1, 65535); }
        catch (NumberFormatException e) { pauseMessage(scanner, "Invalid port."); return; }
    }
    friends.put(name.toLowerCase(Locale.ROOT), new Friend(name, address, port));
    saveFriends();
    refreshFriendStatuses();
    pauseMessage(scanner, friends.get(name.toLowerCase(Locale.ROOT)).online ? "Friend added — currently online." : "Friend added — currently offline.");
}

void removeFriend(Scanner scanner) {
    if (friends.isEmpty()) { pauseMessage(scanner, "There are no friends to remove."); return; }
    IO.print(" Remove friend number: ");
    try {
        int pick = Integer.parseInt(scanner.nextLine().trim());
        if (pick < 1 || pick > friends.size()) { pauseMessage(scanner, "Invalid friend number."); return; }
        Friend f = new ArrayList<>(friends.values()).get(pick - 1);
        friends.remove(f.name.toLowerCase(Locale.ROOT));
        saveFriends();
        pauseMessage(scanner, f.name + " removed.");
    } catch (NumberFormatException e) { pauseMessage(scanner, "Invalid number."); }
}

void refreshFriendStatuses() {
    for (Friend f : friends.values()) {
        long start = System.currentTimeMillis();
        f.online = false;
        f.pingMs = 0;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(FRIEND_TIMEOUT_MS);
            byte[] request = ("TTT_FRIEND_PING|" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(request, request.length, InetAddress.getByName(f.address), f.port));
            byte[] buf = new byte[256];
            DatagramPacket response = new DatagramPacket(buf, buf.length);
            socket.receive(response);
            String text = new String(response.getData(), 0, response.getLength(), java.nio.charset.StandardCharsets.UTF_8);
            f.online = text.startsWith("TTT_FRIEND_PONG|");
            if (f.online) f.pingMs = System.currentTimeMillis() - start;
        } catch (Exception ignored) { }
    }
}

int countOnlineFriends() {
    int n = 0;
    for (Friend f : friends.values()) if (f.online) n++;
    return n;
}

void startFriendPresenceResponder() {
    Thread t = new Thread(() -> {
        try (DatagramSocket socket = new DatagramSocket(FRIENDS_PORT)) {
            byte[] buf = new byte[512];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                if (!request.startsWith("TTT_FRIEND_PING|")) continue;
                byte[] response = ("TTT_FRIEND_PONG|" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
            }
        } catch (Exception ignored) { }
    }, "ttt-friend-presence");
    t.setDaemon(true);
    t.start();
}

static void loadFriends() {
    friends.clear();
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(Paths.get(FRIENDS_FILE))) {
        p.load(in);
        int count = getInt(p, "count", 0);
        for (int i = 0; i < count; i++) {
            String prefix = "friend." + i + ".";
            String name = p.getProperty(prefix + "name", "").trim();
            String address = p.getProperty(prefix + "address", "").trim();
            int port = getInt(p, prefix + "port", FRIENDS_PORT);
            if (!name.isBlank() && !address.isBlank()) friends.put(name.toLowerCase(Locale.ROOT), new Friend(name, address, clamp(port, 1, 65535)));
        }
    } catch (IOException ignored) { }
}

static void saveFriends() {
    Properties p = new Properties();
    p.setProperty("count", String.valueOf(friends.size()));
    int i = 0;
    for (Friend f : friends.values()) {
        String prefix = "friend." + i++ + ".";
        p.setProperty(prefix + "name", f.name);
        p.setProperty(prefix + "address", f.address);
        p.setProperty(prefix + "port", String.valueOf(f.port));
    }
    try (OutputStream out = Files.newOutputStream(Paths.get(FRIENDS_FILE))) { p.store(out, "Tic-Tac-Java friends"); }
    catch (IOException e) { System.err.println("Could not save friends: " + e.getMessage()); }
}

void settingsMenu(Scanner scanner) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Game Settings ─────────────────────" + RESET);
        IO.println("  1. Board size       : " + size);
        IO.println("  2. Game mode        : " + gameModeName(gameMode));
        IO.println("  3. Starting player  : " + startPlayer);
        IO.println("  4. AI difficulty    : " + difficulty);
        IO.println("  5. Turn timer       : " + (turnTimerSeconds == 0 ? "off" : turnTimerSeconds + "s"));
        IO.println("  6. Player name      : " + username);
        IO.println("  7. Toggle ANSI      : " + ansiEnabled);
        IO.println("  0. Back");
        IO.print("\n Select: ");
        String choice = scanner.nextLine().trim();
        try {
            switch (choice) {
                case "1" -> {
                    IO.print("Board size (3-20): ");
                    size = clamp(Integer.parseInt(scanner.nextLine().trim()), 3, 20);
                }
                case "2" -> gameModeMenu(scanner);
                case "3" -> startPlayerMenu(scanner);
                case "4" -> difficultyMenu(scanner);
                case "5" -> {
                    IO.print("Timer seconds (0 to disable): ");
                    turnTimerSeconds = Math.max(0, Integer.parseInt(scanner.nextLine().trim()));
                }
                case "6" -> {
                    IO.print("Player name (letters, numbers, spaces only; max 20): ");
                    String n = scanner.nextLine().trim();
                    if (validOnlineUsername(n)) username = n;
                    else pauseMessage(scanner, "Invalid name. Online names may contain only letters, numbers, and spaces.");
                }
                case "7" -> { ansiEnabled = !ansiEnabled; if (!ansiEnabled) disableColors(); }
                case "0" -> { saveScores(); return; }
                default -> { }
            }
        } catch (NumberFormatException ignored) { pauseMessage(scanner, "Please enter a valid number."); }
        saveScores();
    }
}

void gameModeMenu(Scanner scanner) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Game Mode ──────────────────────────" + RESET);
        IO.println("  1. Classic      — complete a line to win");
        IO.println("  2. Misère       — complete a line and you lose");
        IO.println("  3. Connect-3    — get 3 in a row anywhere");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> { gameMode = MODE_CLASSIC; return; }
            case "2" -> { gameMode = MODE_MISERE; return; }
            case "3" -> { gameMode = MODE_CONNECT; return; }
            case "0" -> { return; }
            default -> pauseMessage(scanner, "Invalid selection.");
        }
    }
}

void difficultyMenu(Scanner scanner) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── AI Difficulty ──────────────────────" + RESET);
        IO.println("  1. Easy        — random moves");
        IO.println("  2. Normal      — attacks and blocks");
        IO.println("  3. Hard        — strongest available AI");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> { difficulty = "easy"; return; }
            case "2" -> { difficulty = "normal"; return; }
            case "3" -> { difficulty = "hard"; return; }
            case "0" -> { return; }
            default -> pauseMessage(scanner, "Invalid selection.");
        }
    }
}

void startPlayerMenu(Scanner scanner) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Starting Player ────────────────────" + RESET);
        IO.println("  1. X goes first");
        IO.println("  2. O goes first");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        switch (scanner.nextLine().trim()) {
            case "1" -> { startPlayer = "X"; return; }
            case "2" -> { startPlayer = "O"; return; }
            case "0" -> { return; }
            default -> pauseMessage(scanner, "Invalid selection.");
        }
    }
}

boolean gameSetupMenu(Scanner scanner, boolean againstAI) {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n╔══════════════════════════════════════╗");
        IO.println("║            GAME SETUP                ║");
        IO.println("╚══════════════════════════════════════╝" + RESET);
        IO.println("  Opponent : " + (againstAI ? "AI" : "Local player"));
        IO.println("  Mode     : " + gameModeName(gameMode));
        IO.println("  Difficulty: " + (againstAI ? difficulty : "N/A"));
        IO.println("  Board    : " + size + "x" + size);
        IO.println("  First    : " + startPlayer);
        IO.println("  Timer    : " + (turnTimerSeconds == 0 ? "Off" : turnTimerSeconds + "s"));
        IO.println("\n  1. Game mode");
        if (againstAI) IO.println("  2. AI difficulty");
        IO.println("  3. Board size");
        IO.println("  4. Starting player");
        IO.println("  5. Turn timer");
        IO.println("  6. Start game");
        IO.println("  0. Cancel");
        IO.print("\n Select: ");
        String c = scanner.nextLine().trim();
        try {
            switch (c) {
                case "1" -> gameModeMenu(scanner);
                case "2" -> { if (againstAI) difficultyMenu(scanner); else pauseMessage(scanner, "Difficulty only applies to AI games."); }
                case "3" -> {
                    IO.print("Board size (3-20) [" + size + "]: ");
                    String v = scanner.nextLine().trim();
                    if (!v.isEmpty()) size = clamp(Integer.parseInt(v), 3, 20);
                }
                case "4" -> startPlayerMenu(scanner);
                case "5" -> {
                    IO.print("Timer seconds (0 = off) [" + turnTimerSeconds + "]: ");
                    String v = scanner.nextLine().trim();
                    if (!v.isEmpty()) turnTimerSeconds = Math.max(0, Integer.parseInt(v));
                }
                case "6" -> { saveScores(); return true; }
                case "0" -> { return false; }
                default -> pauseMessage(scanner, "Invalid selection.");
            }
        } catch (NumberFormatException e) { pauseMessage(scanner, "Please enter a valid number."); }
    }
}

void networkMenu() throws Exception {
    networkMenu(new Scanner(System.in));
}

void networkMenu(Scanner scanner) throws Exception {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Network ────────────────────────────" + RESET);
        IO.println("  1. Host server (" + serverRooms + " rooms, port " + networkPort + ")");
        IO.println("  2. Join server <- (AtomicWFC Live!)");
        IO.println("  3. Scan network for game servers");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        String c = scanner.nextLine().trim();

        if (c.equals("1")) {
            networkMode = true;
            IO.print("Hosting port [" + networkPort + "]: ");
            String port = scanner.nextLine().trim();
            if (!port.isEmpty()) {
                try { networkPort = clamp(Integer.parseInt(port), 1, 65535); }
                catch (Exception ignored) { pauseMessage(scanner, "Invalid port."); continue; }
            }
            IO.print("Rooms to host (1-32): ");
            try { serverRooms = clamp(Integer.parseInt(scanner.nextLine().trim()), 1, 32); }
            catch (Exception ignored) { }

            IO.print("Room for you to join [1]: ");
            String hostRoom = scanner.nextLine().trim();
            requestedRoom = hostRoom.isEmpty() ? "1" : hostRoom;
            try {
                int roomNumber = Integer.parseInt(requestedRoom);
                if (roomNumber < 1 || roomNumber > serverRooms) {
                    pauseMessage(scanner, "Invalid room number.");
                    continue;
                }
            } catch (NumberFormatException e) {
                pauseMessage(scanner, "Invalid room number.");
                continue;
            }

            // The host now runs the server in the background and joins it as a normal player.
            // This keeps the server available for all other rooms while the host plays too.
            try {
                startServerInBackground();
            } catch (IOException e) {
                pauseMessage(scanner, "Could not start the server on port " + networkPort + ": " + e.getMessage());
                continue;
            }
            isServer = false;
            serverAddress = "127.0.0.1";
            saveScores();
            IO.println(GREEN + "Server started. Joining room " + requestedRoom + " as a player..." + RESET);
            Thread.sleep(150);
            runNetworkGame(scanner);
            return;
        }

        if (c.equals("2")) {
            joinServerMenu(scanner);
            continue;
        }

        if (c.equals("3")) {
            List<DiscoveredServer> servers = discoverServers();
            clearConsole();
            IO.println(CYAN + BOLD + "\n── Network Scan ──────────────────────" + RESET);
            if (servers.isEmpty()) {
                IO.println(YELLOW + " No Tic-Tac-Java servers responded." + RESET);
            } else {
                for (int i = 0; i < servers.size(); i++) {
                    DiscoveredServer d = servers.get(i);
                    IO.println("  [" + (i + 1) + "] " + d.name + "  (" + d.rooms + " rooms)  " + d.address + ":" + d.port);
                }
            }
            pauseMessage(scanner, "Press Enter to return...");
            continue;
        }

        if (c.equals("0")) return;
    }
}

void joinServerMenu(Scanner scanner) throws Exception {
    while (true) {
        List<ServerEntry> entries = buildJoinServerList();
        clearConsole();
        IO.println(CYAN + BOLD + "\n── Join Server ───────────────────────" + RESET);
        if (entries.isEmpty()) {
            IO.println(YELLOW + " No compatible servers are currently online." + RESET);
            IO.println("  [A] AtomicWFC Online appears here when it is online.");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                ServerEntry s = entries.get(i);
                String rooms = s.rooms > 0 ? "  (" + s.rooms + " rooms)" : "";
                IO.println("[" + (i + 1) + "] " + s.name + rooms);
            }
        }
        int manualIndex = entries.size() + 1;
        IO.println("[" + manualIndex + "] Enter Manual Address");
        IO.println("[R] Refresh");
        IO.println("[0] Back");
        IO.print("\n Select: ");

        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("0")) return;
        if (input.equalsIgnoreCase("r")) continue;

        int pick;
        try { pick = Integer.parseInt(input); }
        catch (NumberFormatException e) { pauseMessage(scanner, "Invalid selection."); continue; }

        if (pick == manualIndex) {
            IO.print("Server address [" + serverAddress + "]: ");
            String address = scanner.nextLine().trim();
            if (!address.isEmpty()) serverAddress = address;
            IO.print("Port [" + networkPort + "]: ");
            String port = scanner.nextLine().trim();
            if (!port.isEmpty()) {
                try { networkPort = clamp(Integer.parseInt(port), 1, 65535); }
                catch (Exception e) { pauseMessage(scanner, "Invalid port."); continue; }
            }
            showAtomicServerPage(scanner);
            return;
        }

        if (pick < 1 || pick > entries.size()) {
            pauseMessage(scanner, "Invalid selection.");
            continue;
        }

        ServerEntry selected = entries.get(pick - 1);
        serverAddress = selected.address;
        networkPort = selected.port;
        showAtomicServerPage(scanner);
        return;
    }
}

void showAtomicServerPage(Scanner scanner) throws Exception {
    atomicServerInfo = fetchAtomicServerInfo(username);
    clearConsole();
    IO.println(CYAN + BOLD + "\n╔══════════════════════════════════════════════╗" + RESET);
    IO.println(CYAN + "║              AtomicWFC Online                ║" + RESET);
    IO.println(CYAN + "╚══════════════════════════════════════════════╝" + RESET);
    if (atomicServerInfo == null) {
        IO.println(RED + "\n Unable to retrieve AtomicWFC information." + RESET);
        pauseMessage(scanner, "Press Enter to continue...");
        return;
    }
    IO.println("\n " + BOLD + "MESSAGE OF THE DAY" + RESET);
    IO.println(" ──────────────────────────────────────────────");
    if (atomicServerInfo.motd.isBlank()) IO.println(" No message from the server.");
    else for (String line : atomicServerInfo.motd.split("\\n", -1)) IO.println(" " + line);
    IO.println("\n " + BOLD + "SEASON " + atomicServerInfo.season + RESET);
    long remaining = Math.max(0, atomicServerInfo.seasonEnd - System.currentTimeMillis()/1000L);
    IO.println(" Ends in: " + formatDuration(remaining));
    IO.println(" Your league: " + atomicServerInfo.myRank + "  |  Points: " + atomicServerInfo.myPoints + "  |  Badge: " + atomicServerInfo.myBadge);
    if (!atomicServerInfo.leaderboard.isEmpty()) {
        IO.println("\n " + BOLD + "TOP PLAYERS" + RESET);
        for (String row : atomicServerInfo.leaderboard) IO.println(" " + row);
    }
    IO.println("\n  1. Join an existing room");
    IO.println("  2. Create a new room");
    IO.println("  0. Back");
    IO.print("\n Select: ");
    String choice = scanner.nextLine().trim();
    if (choice.equals("0")) return;
    if (choice.equals("1") || choice.equals("2")) {
        if (choice.equals("1")) {
            IO.print("Room number [1]: ");
            String r = scanner.nextLine().trim();
            requestedRoom = r.isBlank() ? "1" : r;
        }
        if (!validOnlineUsername(username)) {
            pauseMessage(scanner, "Online play requires a name containing only letters, numbers, and spaces.");
            return;
        }
        requestedRoom = choice.equals("2") ? "NEW" : requestedRoom;
        isServer = false; networkMode = true; runNetworkGame(scanner);
    }
}

String formatDuration(long seconds) {
    long days=seconds/86400; seconds%=86400; long hours=seconds/3600; seconds%=3600; long mins=seconds/60;
    if (days>0) return days+"d "+hours+"h";
    if (hours>0) return hours+"h "+mins+"m";
    return mins+"m";
}

AtomicServerInfo fetchAtomicServerInfo(String player) {
    try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(serverAddress, networkPort), 2000);
        socket.setSoTimeout(2000);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("INFO=" + (validOnlineUsername(player) ? player : "Player"));
        AtomicServerInfo info = new AtomicServerInfo();
        StringBuilder motd = new StringBuilder();
        String line;
        while ((line=in.readLine()) != null) {
            if (line.equals("INFO_END")) break;
            if (line.startsWith("SERVER=")) info.name=line.substring(7);
            else if (line.startsWith("MOTD=")) { if (motd.length()>0) motd.append('\n'); motd.append(line.substring(5)); }
            else if (line.startsWith("SEASON=")) info.season=Integer.parseInt(line.substring(7));
            else if (line.startsWith("SEASON_END=")) info.seasonEnd=Long.parseLong(line.substring(11));
            else if (line.startsWith("POINTS=")) info.myPoints=Integer.parseInt(line.substring(7));
            else if (line.startsWith("RANK=")) info.myRank=line.substring(5);
            else if (line.startsWith("BADGE=")) info.myBadge=line.substring(6);
            else if (line.startsWith("TOP=")) info.leaderboard.add(line.substring(4).replace('|',' '));
        }
        info.motd=motd.toString();
        return info;
    } catch (Exception e) { return null; }
}

void chooseRoomAndConnect(Scanner scanner) throws Exception {
    while (true) {
        clearConsole();
        IO.println(CYAN + BOLD + "\n── AtomicWFC Online ─────────────────" + RESET);
        IO.println("  Server: " + serverAddress + ":" + networkPort);
        IO.println("\n  1. Join an existing room");
        IO.println("  2. Create a new room");
        IO.println("  0. Back");
        IO.print("\n Select: ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("0")) return;
        if (choice.equals("1")) {
            IO.print("Room number [1]: ");
            String r = scanner.nextLine().trim();
            requestedRoom = r.isEmpty() ? "1" : r;
            break;
        }
        if (choice.equals("2")) {
            requestedRoom = "NEW";
            break;
        }
        pauseMessage(scanner, "Invalid selection.");
    }
    isServer = false;
    networkMode = true;
    runNetworkGame(scanner);
}

List<ServerEntry> buildJoinServerList() {
    List<ServerEntry> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    // Official AtomicWFC Online server is always checked directly; servers.ini is optional.
    ServerEntry official = new ServerEntry("AtomicWFC Online", ATOMICWFC_HOST, 2000, 16, true, false);
    ServerEntry officialOnline = probeServer(official);
    if (officialOnline != null && seen.add(officialOnline.address + ":" + officialOnline.port)) {
        result.add(officialOnline);
    }

    // Only show configured servers that are actually online AND report this exact build.
    for (ServerEntry configured : configuredServers) {
        ServerEntry online = probeServer(configured);
        if (online != null && seen.add(online.address + ":" + online.port)) result.add(online);
    }

    // LAN discovery already filters out incompatible builds.
    List<DiscoveredServer> discovered = discoverServers();
    int localNumber = 1;
    for (DiscoveredServer d : discovered) {
        String key = d.address + ":" + d.port;
        if (!seen.add(key)) continue;
        String displayName = d.name == null || d.name.isBlank() ? "Local Server " + localNumber : d.name;
        result.add(new ServerEntry(displayName, d.address, d.port, d.rooms, false, true));
        localNumber++;
    }

    // AtomicWFC Online is shown only when its configured endpoint is online and compatible.
    // If it is not configured/online, it is intentionally absent from the dynamic list.
    result.sort((a, b) -> {
        if (a.name.equalsIgnoreCase("AtomicWFC Online")) return -1;
        if (b.name.equalsIgnoreCase("AtomicWFC Online")) return 1;
        return a.name.compareToIgnoreCase(b.name);
    });
    return result;
}

ServerEntry probeServer(ServerEntry configured) {
    try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(configured.address, configured.port), 700);
        socket.setSoTimeout(700);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("STATUS=" + BUILD_VERSION);
        String response = in.readLine();
        if (response == null || !response.startsWith("STATUS_OK|")) return null;
        String[] p = response.split("\\|", -1);
        if (p.length < 4 || !BUILD_VERSION.equals(p[1])) return null;
        String name = configured.name;
        int rooms = Integer.parseInt(p[3]);
        return new ServerEntry(name, configured.address, configured.port, rooms, true, false);
    } catch (Exception ignored) {
        return null;
    }
}

static class ServerEntry {
    final String name;
    final String address;
    final int port;
    final int rooms;
    final boolean configured;
    final boolean discovered;

    ServerEntry(String name, String address, int port, int rooms, boolean configured, boolean discovered) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.rooms = rooms;
        this.configured = configured;
        this.discovered = discovered;
    }
}

void loadConfiguredServers() {
    configuredServers.clear();
    Path path = Paths.get(SERVERS_FILE);
    if (!Files.exists(path)) return;
    try {
        for (String line : Files.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            if (p.length < 3) continue;
            String name = p[0].trim();
            String address = p[1].trim();
            int port = Integer.parseInt(p[2].trim());
            int rooms = p.length >= 4 ? Integer.parseInt(p[3].trim()) : 0;
            if (!name.isEmpty() && !address.isEmpty() && port >= 1 && port <= 65535) {
                configuredServers.add(new ServerEntry(name, address, port, Math.max(0, rooms), true, false));
            }
        }
    } catch (Exception ignored) { }
}


void runLocalGame() throws Exception { runLocalGame(new Scanner(System.in), false); }

void runLocalGame(Scanner scanner, boolean resumed) throws Exception {
    LocalState state;
    if (resumed) {
        state = loadGame();
        if (state == null) { IO.println("No saved game."); return; }
    } else {
        state = new LocalState(newBoard(), startPlayer, botplay, gameMode, difficulty);
    }

    boolean gameOver = false;
    while (!gameOver) {
        clearConsole();
        displayBoard(state.board);
        String humanMark = "X";
        String botMark = "O";
        String playerName = state.currentPlayer;
        IO.println("\n " + gameModeName(state.mode) + " | " + (state.bot ? "AI" : "2 players")
                + " | Turn: " + playerName
                + (turnTimerSeconds > 0 ? " [" + turnTimerSeconds + "s]" : ""));
        if (state.bot) IO.println(" Credits: " + YELLOW + credits + RESET + " | Powerups: undo=" + powerupCount(POWER_UNDO)
                + " freeze=" + powerupCount(POWER_FREEZE) + " extra=" + powerupCount(POWER_EXTRA) + " shield=" + powerupCount(POWER_SHIELD));
        IO.println(" Commands: " + CYAN + "pause" + RESET + ", " + CYAN + "save" + RESET + ", " + CYAN + "power" + RESET + ", " + CYAN + "shop" + RESET + ", " + CYAN + "quit" + RESET);

        int[] move;
        if (state.bot && state.currentPlayer.equals(botMark)) {
            if (state.freezeBotNext) {
                state.freezeBotNext = false;
                IO.println(YELLOW + " Freeze activated — the AI loses this turn!" + RESET);
                state.currentPlayer = other(state.currentPlayer);
                continue;
            }
            move = botMove(state.board, state.difficulty, botMark, humanMark);
            if (move == null) { gameOver = true; continue; }
        } else {
            InputResult input = readMoveWithTimer(scanner, turnTimerSeconds);
            if (input.command != null) {
                switch (input.command) {
                    case "pause", "save" -> {
                        if (!state.bot) {
                            IO.println(YELLOW + " Save/resume is available for single-player (AI) games only." + RESET);
                            continue;
                        }
                        saveGame(state);
                        clearConsole();
                        IO.println(GREEN + " Game saved. You can resume it from the main menu." + RESET);
                        if (input.command.equals("pause")) {
                            pauseMessage(scanner, "Game paused. Press Enter to return to the menu...");
                            return;
                        }
                        continue;
                    }
                    case "quit", "exit" -> {
                        if (confirm(scanner, "Quit this game without saving? (y/n): ")) return;
                        continue;
                    }
                    case "shop" -> { shopMenu(scanner); continue; }
                    case "power" -> { usePowerupMenu(scanner, state); continue; }
                    default -> { }
                }
            }
            if (input.move == null) {
                state.currentPlayer = other(state.currentPlayer);
                continue;
            }
            move = input.move;
        }

        String[][] beforeMove = copyBoard(state.board);
        if (!makeMove(state.board, move[0], move[1], state.currentPlayer)) {
            IO.println(RED + " Invalid move. Try again." + RESET);
            continue;
        }
        movesPlayed++;
        state.previousBoard = beforeMove;

        if (state.bot && state.currentPlayer.equals(botMark) && state.shieldActive && checkWin(state.board, botMark, state.mode)) {
            state.board = beforeMove;
            state.shieldActive = false;
            IO.println(GREEN + " Shield blocked the AI's winning move!" + RESET);
            state.currentPlayer = humanMark;
            continue;
        }

        if (checkWin(state.board, state.currentPlayer, state.mode)) {
            clearConsole(); displayBoard(state.board);
            if (state.mode.equals(MODE_MISERE)) {
                IO.println(YELLOW + BOLD + "\n " + state.currentPlayer + " completed a line and loses!" + RESET);
                recordWin(other(state.currentPlayer));
            } else {
                IO.println(GREEN + BOLD + "\n " + state.currentPlayer + " wins!" + RESET);
                recordWin(state.currentPlayer);
            }
            gamesPlayed++;
            awardCredits(state.bot ? (state.currentPlayer.equals(humanMark) ? 50 : 10) : 25);
            updateAchievements(state);
            printScores(); printAchievements();
            deleteSave();
            gameOver = true;
        } else if (isBoardFull(state.board)) {
            clearConsole(); displayBoard(state.board);
            IO.println(CYAN + "\n It's a draw!" + RESET);
            recordDraw(); gamesPlayed++;
            awardCredits(state.bot ? 20 : 15);
            updateAchievements(state);
            printScores(); printAchievements();
            deleteSave();
            gameOver = true;
        } else {
            if (state.extraTurn && state.currentPlayer.equals(humanMark)) {
                state.extraTurn = false;
                IO.println(GREEN + " Extra Turn activated!" + RESET);
            } else {
                state.currentPlayer = other(state.currentPlayer);
            }
        }
    }
    saveScores();
    pauseMessage(scanner, "Press Enter to continue...");
}

void usePowerupMenu(Scanner scanner, LocalState state) {
    if (!state.bot || !state.currentPlayer.equals("X")) { pauseMessage(scanner, "Powerups are available only on your AI turn."); return; }
    clearConsole();
    IO.println(CYAN + BOLD + "\n── Use Powerup ────────────────────────" + RESET);
    IO.println("  1. Undo Move  (" + powerupCount(POWER_UNDO) + ")");
    IO.println("  2. Freeze AI  (" + powerupCount(POWER_FREEZE) + ")");
    IO.println("  3. Extra Turn (" + powerupCount(POWER_EXTRA) + ")");
    IO.println("  4. Shield     (" + powerupCount(POWER_SHIELD) + ")");
    IO.println("  0. Cancel");
    IO.print("\n Select: ");
    String c = scanner.nextLine().trim();
    String type = switch (c) { case "1" -> POWER_UNDO; case "2" -> POWER_FREEZE; case "3" -> POWER_EXTRA; case "4" -> POWER_SHIELD; default -> null; };
    if (type == null) return;
    if (!consumePowerup(type)) { pauseMessage(scanner, "You don't own that powerup."); return; }
    switch (type) {
        case POWER_UNDO -> {
            if (state.previousBoard == null) { powerups.merge(type,1,Integer::sum); pauseMessage(scanner,"There is no move to undo yet."); return; }
            state.board = copyBoard(state.previousBoard); state.previousBoard = null; state.currentPlayer = "X";
            pauseMessage(scanner, "Your previous move was undone.");
        }
        case POWER_FREEZE -> { state.freezeBotNext = true; pauseMessage(scanner, "The AI will skip its next turn."); }
        case POWER_EXTRA -> { state.extraTurn = true; pauseMessage(scanner, "Your next move will grant another turn."); }
        case POWER_SHIELD -> { state.shieldActive = true; pauseMessage(scanner, "Shield active. It will block the next AI winning move."); }
    }
    saveScores();
}

static String[][] copyBoard(String[][] board) { String[][] copy = new String[board.length][board.length]; for (int r=0;r<board.length;r++) copy[r]=Arrays.copyOf(board[r],board[r].length); return copy; }
static void awardCredits(int amount) { credits += Math.max(0, amount); saveScores(); }

boolean resumeGame(Scanner scanner) throws Exception {
    LocalState state = loadGame();
    if (state == null) return false;
    botplay = state.bot;
    gameMode = state.mode;
    difficulty = state.difficulty;
    runLocalGame(scanner, true);
    return true;
}

void runNetworkGame() throws Exception {
    try (Scanner scanner = new Scanner(System.in)) {
        runNetworkGame(scanner);
    }
}

void runNetworkGame(Scanner scanner) throws Exception {
    if (!validOnlineUsername(username)) {
        pauseMessage(scanner, "Online play blocked: your username may contain only letters, numbers, and spaces (max 20 characters).");
        return;
    }
    if (isServer) {
        runServer();
        return;
    }
    try (Socket socket = new Socket()) {
        try {
            socket.connect(new InetSocketAddress(serverAddress, networkPort), 5000);
        } catch (IOException e) {
            pauseMessage(scanner, "Could not connect to " + serverAddress + ":" + networkPort + "\n" + e.getMessage());
            return;
        }
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader netIn = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("VERSION=" + BUILD_VERSION);
            out.println("HELLO=" + username);
            out.println("ROOM=" + requestedRoom);
            String first = netIn.readLine();
            if (first == null) throw new IOException("Server closed the connection.");
            if (first.startsWith("ERROR=")) { IO.println(RED + first.substring(6) + RESET); return; }
            if (!first.startsWith("ROOM=")) throw new IOException("Invalid server response.");
            String room = first.substring(5);
            String roleLine = netIn.readLine();
            if (roleLine != null && roleLine.equals("WAITING")) {
                String waitingMessage = netIn.readLine();
                if (waitingMessage != null) IO.println(YELLOW + waitingMessage + RESET);
                roleLine = netIn.readLine();
            }
            if (roleLine == null || !roleLine.startsWith("ROLE=")) throw new IOException("Invalid room setup response.");
            String role = roleLine.substring(5);
            int syncedSize = Integer.parseInt(netIn.readLine().substring(5));
            String syncedStart = netIn.readLine().substring(6);
            int syncedTimer = Integer.parseInt(netIn.readLine().substring(6));
            String opponentName = netIn.readLine().substring(5);
            size = syncedSize; startPlayer = syncedStart; turnTimerSeconds = syncedTimer;
            playNetworkSession(socket, out, netIn, scanner, role, room, opponentName);
        }
    }
}

static class Room {
    final Socket socket;
    final PrintWriter out;
    final String name;
    final CountDownLatch paired = new CountDownLatch(1);
    Room(Socket socket, PrintWriter out, String name) { this.socket = socket; this.out = out; this.name = name; }
}

static final Map<String, Room> ROOMS = new HashMap<>();

static class DiscoveredServer {
    final String name;
    final String address;
    final int port;
    final int rooms;
    final String version;
    DiscoveredServer(String name, String address, int port, int rooms, String version) {
        this.name = name; this.address = address; this.port = port; this.rooms = rooms; this.version = version;
    }
}

List<DiscoveredServer> discoverServers() {
    Map<String, DiscoveredServer> found = new LinkedHashMap<>();
    try (DatagramSocket socket = new DatagramSocket()) {
        socket.setBroadcast(true);
        byte[] data = "TTT_DISCOVER".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) continue;
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress broadcast = ia.getBroadcast();
                if (broadcast != null) socket.send(new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT));
            }
        }
        // Also probe localhost so the feature works when firewall/broadcast rules block LAN broadcast.
        try { socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), DISCOVERY_PORT)); } catch (Exception ignored) {}
        socket.setSoTimeout(250);
        long deadline = System.currentTimeMillis() + discoveryTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String response = new String(packet.getData(), 0, packet.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                String[] p = response.split("\\|");
                if (p.length >= 5 && p[0].equals("TTT_SERVER") && BUILD_VERSION.equals(p[4])) {
                    int port = Integer.parseInt(p[2]);
                    int rooms = Integer.parseInt(p[3]);
                    String address = packet.getAddress().getHostAddress();
                    String name = p.length >= 2 && !p[1].isBlank() ? p[1] : "Local Server";
                    found.put(address + ":" + port, new DiscoveredServer(name, address, port, rooms, BUILD_VERSION));
                }
            } catch (SocketTimeoutException ignored) {}
            catch (Exception ignored) {}
        }
    } catch (Exception e) {
        System.err.println("Network discovery failed: " + e.getMessage());
    }
    return new ArrayList<>(found.values());
}

void startDiscoveryResponder() {
    Thread t = new Thread(() -> {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setBroadcast(true);
            byte[] buf = new byte[256];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength(), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!request.equals("TTT_DISCOVER")) continue;
                String response = "TTT_SERVER|" + InetAddress.getLocalHost().getHostName() + "|" + networkPort + "|" + serverRooms + "|" + BUILD_VERSION;
                byte[] data = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort()));
            }
        } catch (Exception ignored) { }
    }, "ttt-discovery");
    t.setDaemon(true);
    t.start();
}

void runServer() throws Exception {
    startDiscoveryResponder();
    IO.println(CYAN + BOLD + "Hosting " + serverRooms + " rooms on port " + networkPort + RESET);
    IO.println("The server can host players in every room; it does not need to be a player itself.");
    IO.println("Clients join with: java Main --client <address> --room <room>");
    ExecutorService pool = Executors.newCachedThreadPool();
    try (ServerSocket server = new ServerSocket(networkPort)) {
        while (true) {
            Socket socket = server.accept();
            pool.submit(() -> {
                try { handleRoomConnection(socket); }
                catch (Exception e) { System.err.println("Room connection ended: " + e.getMessage()); }
            });
        }
    } finally { pool.shutdownNow(); }
}

/** Starts the same room server used by --server, but leaves it running in the background
 *  so the computer hosting it can also connect as a player. */
void startServerInBackground() throws IOException {
    startDiscoveryResponder();
    final ServerSocket server = new ServerSocket(networkPort);
    Thread listener = new Thread(() -> {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            IO.println(CYAN + BOLD + "Hosting " + serverRooms + " rooms on port " + networkPort + RESET);
            IO.println("Host player is enabled — you can play in any room while the server stays online.");
            while (!server.isClosed()) {
                Socket socket = server.accept();
                pool.submit(() -> {
                    try { handleRoomConnection(socket); }
                    catch (Exception e) { System.err.println("Room connection ended: " + e.getMessage()); }
                });
            }
        } catch (IOException ignored) {
            // Server socket closed or accept failed.
        } finally {
            pool.shutdownNow();
            try { server.close(); } catch (IOException ignored) { }
        }
    }, "ttt-host-server");
    listener.setDaemon(true);
    listener.start();
}

void handleRoomConnection(Socket socket) throws Exception {
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    String firstLine = in.readLine();
    if (firstLine == null) {
        out.println("ERROR=Invalid handshake.");
        socket.close();
        return;
    }
    if (firstLine.startsWith("STATUS=")) {
        String requestedVersion = firstLine.substring(7).trim();
        if (!BUILD_VERSION.equals(requestedVersion)) {
            out.println("STATUS_INCOMPATIBLE|" + BUILD_VERSION);
        } else {
            out.println("STATUS_OK|" + BUILD_VERSION + "|" + serverName + "|" + serverRooms);
        }
        socket.close();
        return;
    }
    if (!firstLine.startsWith("VERSION=")) {
        out.println("ERROR=Version handshake required.");
        socket.close();
        return;
    }
    String clientVersion = firstLine.substring(8).trim();
    if (!BUILD_VERSION.equals(clientVersion)) {
        out.println("ERROR=Version mismatch. Server: " + BUILD_VERSION + " | Client: " + clientVersion);
        socket.close();
        return;
    }
    String hello = in.readLine();
    String requested = in.readLine();
    if (hello == null || !hello.startsWith("HELLO=") || requested == null || !requested.startsWith("ROOM=")) {
        out.println("ERROR=Invalid room handshake.");
        return;
    }
    String name = hello.substring(6).trim();
    String roomId = requested.substring(5).trim();
    int roomNumber;
    try { roomNumber = Integer.parseInt(roomId); } catch (Exception e) { roomNumber = -1; }
    if (roomNumber < 1 || roomNumber > serverRooms) {
        out.println("ERROR=Invalid room. Available rooms: 1-" + serverRooms);
        return;
    }

    Room first;
    Room waiting = null;
    synchronized (ROOMS) {
        first = ROOMS.remove(roomId);
        if (first == null) {
            waiting = new Room(socket, out, name);
            ROOMS.put(roomId, waiting);
        }
    }
    if (waiting != null) {
        out.println("ROOM=" + roomId);
        out.println("WAITING");
        out.println("Waiting for another player to join room " + roomId + "...");
        try { waiting.paired.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return;
    }

    // Second player joined: pair the two clients and start a relay session.
    out.println("ROOM=" + roomId);
    first.paired.countDown();
    out.println("ROLE=O");
    first.out.println("ROLE=X");
    out.println("SIZE=" + size);
    first.out.println("SIZE=" + size);
    out.println("START=" + startPlayer);
    first.out.println("START=" + startPlayer);
    out.println("TIMER=" + turnTimerSeconds);
    first.out.println("TIMER=" + turnTimerSeconds);
    out.println("NAME=" + first.name);
    first.out.println("NAME=" + name);

    // Relay commands between the two clients. The clients own game state and UI.
    ExecutorService relay = Executors.newFixedThreadPool(2);
    relay.submit(() -> relayPlayer(first.socket, out, socket));
    relay.submit(() -> relayPlayer(socket, first.out, first.socket));
    relay.shutdown();
}

void relayPlayer(Socket from, PrintWriter to, Socket other) {
    try {
        BufferedReader in = new BufferedReader(new InputStreamReader(from.getInputStream()));
        String line;
        while ((line = in.readLine()) != null) {
            to.println(line);
            if (line.equals("QUIT")) break;
        }
    } catch (IOException ignored) {
    } finally {
        try { other.close(); } catch (IOException ignored) { }
    }
}

void playNetworkSession(Socket socket, PrintWriter out, BufferedReader netIn, Scanner scanner,
                        String myMark, String room, String opponentName) throws Exception {
    final String theirMark = other(myMark);
    String opponentBadge = "Rookie";
    String line;
    while ((line = netIn.readLine()) != null) {
        if (line.startsWith("BADGE=")) { opponentBadge = line.substring(6); break; }
        if (line.startsWith("LEAGUE=")) continue;
        if (line.startsWith("MOTD=")) continue;
        if (line.startsWith("READY")) break;
    }
    String mySymbol = colorOf(myMark) + myMark + RESET;
    String theirSymbol = colorOf(theirMark) + theirMark + RESET;
    String matchHeader = " Room " + room + " | " + BOLD + username + RESET + " (" + mySymbol + ") vs "
            + BOLD + opponentName + RESET + " (" + theirSymbol + ") [" + opponentBadge + "]";
    String[][] board = newBoard();
    boolean myTurn = false;
    IO.println(matchHeader);

    while (true) {
        clearConsole(); IO.println(matchHeader); displayBoard(board);
        if (myTurn) {
            IO.println("\n Your turn (server-authoritative) — type 'quit' to leave.");
            InputResult input = readMoveWithTimer(scanner, turnTimerSeconds);
            if (input.command != null && input.command.equals("quit")) { out.println("QUIT"); return; }
            if (input.move == null) { IO.println(YELLOW + "Waiting for server timeout result..." + RESET); }
            else {
                out.println("MOVE=" + input.move[0] + "," + input.move[1]);
            }
            myTurn = false;
        } else {
            String msg = netIn.readLine();
            if (msg == null) { IO.println(YELLOW + "Connection to AtomicWFC ended." + RESET); return; }
            if (msg.equals("QUIT")) { IO.println(YELLOW + "Opponent left the room." + RESET); return; }
            if (msg.startsWith("ERROR=")) { IO.println(RED + msg.substring(6) + RESET); continue; }
            if (msg.startsWith("MOVE=")) {
                String[] p = msg.substring(5).split("\\|");
                if (p.length == 3) {
                    String mark=p[0]; int r=Integer.parseInt(p[1]), c=Integer.parseInt(p[2]);
                    if (!makeMove(board,r,c,mark)) { IO.println(RED+"Server sent an invalid move."+RESET); return; }
                }
                continue;
            }
            if (msg.startsWith("TURN=")) { myTurn = msg.substring(5).equals(myMark); continue; }
            if (msg.startsWith("RESULT=")) {
                String[] p=msg.split("\\|",-1);
                String outcome=p.length>1?p[1]:"DRAW";
                int delta=p.length>2?Integer.parseInt(p[2]):0;
                int points=p.length>3?Integer.parseInt(p[3]):0;
                String rank=p.length>4?p[4]:"Bronze";
                String badge=p.length>5?p[5]:"Rookie";
                clearConsole(); IO.println(matchHeader); displayBoard(board);
                if (outcome.equals("WIN")) IO.println(GREEN+BOLD+"\n You win!  +"+delta+" league points"+RESET);
                else if (outcome.equals("LOSS")) IO.println(YELLOW+"\n You lose.  "+delta+" league points"+RESET);
                else IO.println(CYAN+"\n Draw.  "+delta+" league points"+RESET);
                IO.println(" League: "+rank+"  |  Points: "+points+"  |  Badge: "+badge);
                gamesPlayed++; achievements.add("network"); updateAchievements(null); saveScores();
                pauseMessage(scanner,"Press Enter to return..."); return;
            }
        }
    }
}

static class LocalState {
    String[][] board;
    String currentPlayer, mode, difficulty;
    boolean bot;
    String[][] previousBoard;
    boolean freezeBotNext, extraTurn, shieldActive;
    LocalState(String[][] board, String currentPlayer, boolean bot, String mode, String difficulty) {
        this.board = board; this.currentPlayer = currentPlayer; this.bot = bot; this.mode = mode; this.difficulty = difficulty;
    }
}

static class InputResult {
    int[] move; String command;
    InputResult(int[] move, String command) { this.move = move; this.command = command; }
}

InputResult readMoveWithTimer(Scanner scanner, int seconds) throws Exception {
    if (seconds <= 0) return parseInput(scanner.nextLine());
    ExecutorService exec = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    Future<String> future = exec.submit(scanner::nextLine);
    try {
        String line = future.get(seconds, TimeUnit.SECONDS);
        return parseInput(line);
    } catch (TimeoutException e) {
        future.cancel(true);
        timeouts++;
        return new InputResult(null, null);
    } finally { exec.shutdownNow(); }
}

InputResult parseInput(String line) {
    if (line == null) return new InputResult(null, "quit");
    String s = line.trim();
    switch (s.toLowerCase()) {
        case "pause", "p" -> { return new InputResult(null, "pause"); }
        case "save", "s" -> { return new InputResult(null, "save"); }
        case "quit", "q", "exit" -> { return new InputResult(null, "quit"); }
        default -> { }
    }
    String[] parts = s.replace(',', ' ').split("\\s+");
    if (parts.length != 2) return new InputResult(null, "invalid");
    try { return new InputResult(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])}, null); }
    catch (NumberFormatException e) { return new InputResult(null, "invalid"); }
}

String[][] newBoard() {
    String[][] b = new String[size][size];
    for (String[] row : b) Arrays.fill(row, " ");
    return b;
}

void displayBoard(String[][] board) {
    int n = board.length;
    int w = Math.max(1, String.valueOf(n - 1).length());
    IO.print(" ".repeat(w + 1));
    for (int c = 0; c < n; c++) { IO.print(String.format("%" + w + "d", c)); if (c < n - 1) IO.print(COLOR_GRID + "   " + RESET); }
    IO.println("");
    for (int r = 0; r < n; r++) {
        IO.print(String.format("%" + w + "d ", r));
        for (int c = 0; c < n; c++) { IO.print(" ".repeat(Math.max(0, w - 1)) + board[r][c]); if (c < n - 1) IO.print(COLOR_GRID + " | " + RESET); }
        IO.println("");
        if (r < n - 1) IO.println(COLOR_GRID + " ".repeat(w) + "-".repeat(n * (w + 3) - 2) + RESET);
    }
}

boolean makeMove(String[][] board, int row, int col, String player) {
    if (row >= 0 && row < board.length && col >= 0 && col < board.length && board[row][col].equals(" ")) {
        board[row][col] = player; return true;
    }
    return false;
}

boolean checkWin(String[][] board, String player) { return checkWin(board, player, MODE_CLASSIC); }

boolean checkWin(String[][] board, String player, String mode) {
    if (mode.equals(MODE_CONNECT)) return checkConnect(board, player, 3);
    int n = board.length;
    for (int i = 0; i < n; i++) {
        boolean row = true, col = true;
        for (int j = 0; j < n; j++) { if (!board[i][j].equals(player)) row = false; if (!board[j][i].equals(player)) col = false; }
        if (row || col) return true;
    }
    boolean d1 = true, d2 = true;
    for (int i = 0; i < n; i++) { if (!board[i][i].equals(player)) d1 = false; if (!board[i][n - 1 - i].equals(player)) d2 = false; }
    return d1 || d2;
}

boolean checkConnect(String[][] board, String player, int target) {
    int n = board.length;
    int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
    for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (board[r][c].equals(player)) {
        for (int[] d : dirs) {
            int count = 0, rr = r, cc = c;
            while (rr >= 0 && rr < n && cc >= 0 && cc < n && board[rr][cc].equals(player)) { count++; if (count >= target) return true; rr += d[0]; cc += d[1]; }
        }
    }
    return false;
}

boolean isBoardFull(String[][] board) { for (String[] row : board) for (String cell : row) if (cell.equals(" ")) return false; return true; }

int[] botMove(String[][] board, String difficulty, String bot, String human) {
    if (difficulty.equals("hard") && board.length == 3 && gameMode.equals(MODE_CLASSIC)) return bestMove(board, bot, human);
    if (!difficulty.equals("easy")) {
        int[] m = findWinningMove(board, bot); if (m != null) return m;
        m = findWinningMove(board, human); if (m != null) return m;
    }
    if (difficulty.equals("hard")) {
        int[] center = findCenter(board); if (center != null) return center;
    }
    return randomMove(board);
}

int[] findCenter(String[][] board) { int c = board.length / 2; return board[c][c].equals(" ") ? new int[]{c,c} : null; }

int[] randomMove(String[][] board) {
    List<int[]> empty = new ArrayList<>();
    for (int r = 0; r < board.length; r++) for (int c = 0; c < board.length; c++) if (board[r][c].equals(" ")) empty.add(new int[]{r,c});
    return empty.isEmpty() ? null : empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
}

int[] findWinningMove(String[][] board, String player) {
    for (int r = 0; r < board.length; r++) for (int c = 0; c < board.length; c++) if (board[r][c].equals(" ")) {
        board[r][c] = player; boolean wins = checkWin(board, player, gameMode); board[r][c] = " "; if (wins) return new int[]{r,c};
    }
    return null;
}

int[] bestMove(String[][] board, String bot, String human) {
    int bestScore = Integer.MIN_VALUE; int[] best = null;
    for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (board[r][c].equals(" ")) {
        board[r][c] = bot; int score = minimax(board, false, bot, human); board[r][c] = " "; if (score > bestScore) { bestScore = score; best = new int[]{r,c}; }
    }
    return best;
}

int minimax(String[][] board, boolean botTurn, String bot, String human) {
    if (checkWin(board, bot, MODE_CLASSIC)) return 1;
    if (checkWin(board, human, MODE_CLASSIC)) return -1;
    if (isBoardFull(board)) return 0;
    int best = botTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (board[r][c].equals(" ")) {
        board[r][c] = botTurn ? bot : human; int score = minimax(board, !botTurn, bot, human); board[r][c] = " "; best = botTurn ? Math.max(best, score) : Math.min(best, score);
    }
    return best;
}

void recordWin(String mark) {
    if (mark.equals("X")) { winsX++; streakX++; streakO = 0; bestStreakX = Math.max(bestStreakX, streakX); }
    else { winsO++; streakO++; streakX = 0; bestStreakO = Math.max(bestStreakO, streakO); }
}

void recordDraw() { draws++; streakX = 0; streakO = 0; }

static void printScores() {
    IO.println(BOLD + "\n ── Scores ─────────────────────────────" + RESET);
    IO.println(colorOf("X") + String.format("  X wins: %-4d streak: %d (best: %d)", winsX, streakX, bestStreakX) + RESET);
    IO.println(colorOf("O") + String.format("  O wins: %-4d streak: %d (best: %d)", winsO, streakO, bestStreakO) + RESET);
    IO.println(CYAN + String.format("  Draws: %-4d games: %-4d moves: %-4d timeouts: %d", draws, gamesPlayed, movesPlayed, timeouts) + RESET);
    IO.println(BOLD + " ───────────────────────────────────────" + RESET);
}

static void printAchievements() {
    String[][] list = {
            {"first_win", "First Win", "Win your first game"},
            {"win_3", "Hat Trick", "Reach a 3-game win streak"},
            {"win_5", "On Fire", "Reach a 5-game win streak"},
            {"win_10", "Unstoppable", "Reach a 10-game win streak"},
            {"games_25", "Veteran", "Play 25 games"},
            {"games_100", "Centurion", "Play 100 games"},
            {"moves_100", "Board Regular", "Make 100 moves"},
            {"moves_1000", "Board Master", "Make 1,000 moves"},
            {"giant", "Giant Board", "Play on a board of at least 10x10"},
            {"large", "Big Board", "Play on a board of at least 5x5"},
            {"hard_ai", "AI Slayer", "Beat the hard AI"},
            {"misere", "Reverse Psychology", "Win a Misère game"},
            {"connect", "Connected", "Win a Connect game"},
            {"all_modes", "Game Explorer", "Complete all three game modes"},
            {"network", "Online", "Finish a network game"},
            {"speed", "Speed Demon", "Win a timed game"},
            {"timeouts", "Timekeeper", "Reach 5 turn timeouts"},
            {"saver", "Prepared", "Save a game for later"},
            {"size_15", "Titan", "Play on a 15x15 board"}
    };
    IO.println(PURPLE + BOLD + "\n ── Achievements ───────────────────────" + RESET);
    for (String[] a : list) IO.println((achievements.contains(a[0]) ? GREEN + " [✓] " : " [ ] ") + a[1] + " — " + a[2] + RESET);
}

void updateAchievements(LocalState state) {
    if (winsX + winsO >= 1) achievements.add("first_win");
    int best = Math.max(bestStreakX, bestStreakO);
    if (best >= 3) achievements.add("win_3");
    if (best >= 5) achievements.add("win_5");
    if (best >= 10) achievements.add("win_10");
    if (gamesPlayed >= 25) achievements.add("games_25");
    if (gamesPlayed >= 100) achievements.add("games_100");
    if (movesPlayed >= 100) achievements.add("moves_100");
    if (movesPlayed >= 1000) achievements.add("moves_1000");
    if (size >= 5) achievements.add("large");
    if (size >= 10) achievements.add("giant");
    if (size >= 15) achievements.add("size_15");
    if (timeouts >= 5) achievements.add("timeouts");
    if (state != null) {
        if (state.difficulty.equals("hard") && state.bot) achievements.add("hard_ai");
        if (state.mode.equals(MODE_MISERE)) achievements.add("misere");
        if (state.mode.equals(MODE_CONNECT)) achievements.add("connect");
        achievements.add("mode_" + state.mode);
        if (achievements.contains("mode_" + MODE_CLASSIC) && achievements.contains("mode_" + MODE_MISERE) && achievements.contains("mode_" + MODE_CONNECT)) achievements.add("all_modes");
    }
    if (turnTimerSeconds > 0) achievements.add("speed");
    saveScores();
}

void saveGame(LocalState state) {
    achievements.add("saver");
    Properties p = new Properties();
    p.setProperty("size", String.valueOf(state.board.length)); p.setProperty("current", state.currentPlayer);
    p.setProperty("bot", String.valueOf(state.bot)); p.setProperty("mode", state.mode); p.setProperty("difficulty", state.difficulty);
    for (int r = 0; r < state.board.length; r++) for (int c = 0; c < state.board.length; c++) p.setProperty("cell." + r + "." + c, state.board[r][c]);
    try (OutputStream out = Files.newOutputStream(Paths.get(SAVE_FILE))) { p.store(out, "TicTacToe saved game"); }
    catch (IOException e) { System.err.println("Could not save game: " + e.getMessage()); }
}

LocalState loadGame() {
    if (!Files.exists(Paths.get(SAVE_FILE))) return null;
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(Paths.get(SAVE_FILE))) {
        p.load(in); int n = clamp(Integer.parseInt(p.getProperty("size", "3")), 3, 20); String[][] b = new String[n][n];
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) b[r][c] = p.getProperty("cell." + r + "." + c, " ");
        return new LocalState(b, p.getProperty("current", "X"), Boolean.parseBoolean(p.getProperty("bot", "false")), p.getProperty("mode", MODE_CLASSIC), p.getProperty("difficulty", "normal"));
    } catch (Exception e) { System.err.println("Could not load saved game: " + e.getMessage()); return null; }
}

void deleteSave() { try { Files.deleteIfExists(Paths.get(SAVE_FILE)); } catch (IOException ignored) {} }

static void loadScores() {
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(Paths.get(SCORES_FILE))) {
        p.load(in); winsX = getInt(p,"wins.X",0); winsO = getInt(p,"wins.O",0); draws = getInt(p,"draws",0);
        credits = getInt(p,"credits",100);
        powerups.clear();
        for (String type : List.of(POWER_UNDO, POWER_FREEZE, POWER_EXTRA, POWER_SHIELD)) { int count = getInt(p,"powerup." + type,0); if (count > 0) powerups.put(type,count); }
        streakX = getInt(p,"streak.X",0); streakO = getInt(p,"streak.O",0); bestStreakX = getInt(p,"bestStreak.X",0); bestStreakO = getInt(p,"bestStreak.O",0);
        gamesPlayed = getInt(p,"gamesPlayed",0); movesPlayed = getInt(p,"movesPlayed",0); timeouts = getInt(p,"timeouts",0);
        COLOR_X = COLOR_MAP.getOrDefault(p.getProperty("color.X","RED"), COLOR_X); COLOR_O = COLOR_MAP.getOrDefault(p.getProperty("color.O","BLUE"), COLOR_O); COLOR_GRID = COLOR_MAP.getOrDefault(p.getProperty("color.grid","WHITE"), COLOR_GRID);
        startPlayer = p.getProperty("startPlayer","X"); username = p.getProperty("username","Player"); turnTimerSeconds = getInt(p,"turnTimer",0); gameMode = p.getProperty("gameMode",MODE_CLASSIC); difficulty = p.getProperty("difficulty","normal");
        String savedAchievements = p.getProperty("achievements",""); if (!savedAchievements.isBlank()) achievements.addAll(Arrays.asList(savedAchievements.split(",")));
    } catch (IOException ignored) { }
}

static void saveScores() {
    Properties p = new Properties();
    p.setProperty("credits",String.valueOf(credits));
    for (String type : List.of(POWER_UNDO, POWER_FREEZE, POWER_EXTRA, POWER_SHIELD)) p.setProperty("powerup." + type, String.valueOf(powerupCount(type)));
    p.setProperty("wins.X",String.valueOf(winsX)); p.setProperty("wins.O",String.valueOf(winsO)); p.setProperty("draws",String.valueOf(draws));
    p.setProperty("streak.X",String.valueOf(streakX)); p.setProperty("streak.O",String.valueOf(streakO)); p.setProperty("bestStreak.X",String.valueOf(bestStreakX)); p.setProperty("bestStreak.O",String.valueOf(bestStreakO));
    p.setProperty("gamesPlayed",String.valueOf(gamesPlayed)); p.setProperty("movesPlayed",String.valueOf(movesPlayed)); p.setProperty("timeouts",String.valueOf(timeouts));
    p.setProperty("color.X",colorName(COLOR_X,"RED")); p.setProperty("color.O",colorName(COLOR_O,"BLUE")); p.setProperty("color.grid",colorName(COLOR_GRID,"WHITE"));
    p.setProperty("startPlayer",startPlayer); p.setProperty("username",username); p.setProperty("turnTimer",String.valueOf(turnTimerSeconds)); p.setProperty("gameMode",gameMode); p.setProperty("difficulty",difficulty);
    p.setProperty("achievements",String.join(",",achievements));
    try (OutputStream out = Files.newOutputStream(Paths.get(SCORES_FILE))) { p.store(out,"TicTacToe scores & preferences"); } catch (IOException e) { System.err.println("Could not save scores: " + e.getMessage()); }
    saveFriends();
}

static int getInt(Properties p, String key, int fallback) { try { return Integer.parseInt(p.getProperty(key,String.valueOf(fallback))); } catch (Exception e) { return fallback; } }
static String colorName(String ansi, String fallback) { return COLOR_MAP.entrySet().stream().filter(e -> e.getValue().equals(ansi)).map(Map.Entry::getKey).findFirst().orElse(fallback); }
static String colorOf(String mark) { return mark.equals("X") ? COLOR_X : COLOR_O; }
String other(String p) { return p.equals("X") ? "O" : "X"; }
String gameModeName(String mode) { return switch (mode) { case MODE_MISERE -> "Misère"; case MODE_CONNECT -> "Connect-3"; default -> "Classic"; }; }
static void setGameMode(String mode) { String m = mode.trim().toLowerCase(); if (Set.of(MODE_CLASSIC,MODE_MISERE,MODE_CONNECT).contains(m)) gameMode = m; }
static int clamp(int v,int min,int max) { return Math.max(min,Math.min(max,v)); }

boolean confirm(Scanner scanner, String prompt) { IO.print(prompt); return scanner.hasNextLine() && scanner.nextLine().trim().toLowerCase().startsWith("y"); }
void pauseMessage(Scanner scanner, String message) { IO.println("\n" + message); if (scanner.hasNextLine()) scanner.nextLine(); }
void disableColors() { COLOR_X = COLOR_O = COLOR_GRID = ""; }

static class IO { static void print(String s) { System.out.print(s); } static void println(String s) { System.out.println(s); } }

public static void parseARG(String[] args) {
    for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        try {
            switch (arg) {
                case "--botplay" -> botplay = true;
                case "--ui" -> uiMode = true;
                case "--no-color" -> ansiEnabled = false;
                case "--easy" -> difficulty = "easy";
                case "--normal" -> difficulty = "normal";
                case "--hard" -> difficulty = "hard";
                case "--mode" -> { if (i + 1 < args.length) setGameMode(args[++i]); }
                case "--size" -> { if (i + 1 < args.length) size = clamp(Integer.parseInt(args[++i]),3,20); }
                case "--startplayer" -> { if (i + 1 < args.length) { String sp=args[++i].toUpperCase(); if (sp.equals("X")||sp.equals("O")) startPlayer=sp; } }
                case "--timer" -> { if (i + 1 < args.length) turnTimerSeconds=Math.max(0,Integer.parseInt(args[++i])); }
                case "--name" -> { if (i + 1 < args.length) username=args[++i]; }
                case "--resume" -> { uiMode = true; resumeRequested = true; }
                case "--save" -> { /* save occurs after a game; kept as a compatibility flag */ }
                case "--color-x" -> { if (i+1<args.length) { String c=COLOR_MAP.get(args[++i].toUpperCase()); if(c!=null) COLOR_X=c; } }
                case "--color-o" -> { if (i+1<args.length) { String c=COLOR_MAP.get(args[++i].toUpperCase()); if(c!=null) COLOR_O=c; } }
                case "--color-grid" -> { if (i+1<args.length) { String c=COLOR_MAP.get(args[++i].toUpperCase()); if(c!=null) COLOR_GRID=c; } }
                case "--server" -> { networkMode=true; isServer=true; }
                case "--client" -> { networkMode=true; isServer=false; if(i+1<args.length&&!args[i+1].startsWith("--")) serverAddress=args[++i]; }
                case "--port" -> { if(i+1<args.length) networkPort=Integer.parseInt(args[++i]); }
                case "--rooms" -> { if(i+1<args.length) serverRooms=clamp(Integer.parseInt(args[++i]),1,32); }
                case "--room" -> { if(i+1<args.length) requestedRoom=args[++i]; }
                case "--reset-scores" -> { winsX=winsO=draws=streakX=streakO=bestStreakX=bestStreakO=gamesPlayed=movesPlayed=timeouts=0; credits=100; powerups.clear(); achievements.clear(); saveScores(); }
                case "--scores" -> { loadScores(); printScores(); printAchievements(); System.exit(0); }
                case "--help" -> { Help(); return; }
                default -> System.out.println("Unknown argument: " + arg);
            }
        } catch (NumberFormatException e) { System.out.println("Invalid value for " + arg); }
    }
}

static void showSplash() throws InterruptedException {
    clearConsole();
    String title = "TicTacJava";
    StringBuilder built = new StringBuilder();
    IO.println(CYAN + BOLD + "\n");
    for (char ch : title.toCharArray()) {
        built.append(ch);
        IO.print("\r        " + built);
        Thread.sleep(100);
    }
    IO.println(RESET + "\n\n        Online-ready Tic-Tac-Toe");
    Thread.sleep(500);
}

public static void clearConsole() {
    try {
        String os=System.getProperty("os.name");
        if(os.toLowerCase().contains("windows")) new ProcessBuilder("cmd","/c","cls").inheritIO().start().waitFor();
        else System.out.print("\033[H\033[2J");
        System.out.flush();
    } catch(Exception ignored) { }
}

public static boolean pingWithAnimation(String host) throws InterruptedException {
    AtomicBoolean done = new AtomicBoolean(false);
    AtomicBoolean success = new AtomicBoolean(false);
    long startTime = System.currentTimeMillis();
    Thread pinger = new Thread(() -> {
        Process p = null;
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            ProcessBuilder pb = windows
                    ? new ProcessBuilder("ping", "-n", "2", "-w", "1500", host)
                    : new ProcessBuilder("ping", "-c", "2", "-W", "2", host);
            pb.redirectErrorStream(true);
            p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (reader.readLine() != null) { }
            }
            success.set(p.waitFor() == 0);
        } catch (Exception e) {
            success.set(false);
        } finally {
            if (p != null) p.destroyForcibly();
            done.set(true);
        }
    }, "ttt-internet-check");
    pinger.setDaemon(true);
    pinger.start();
    final int WIDTH = 16;
    final int DELAY_MS = 80;
    int pos = 0, direction = 1;
    final int MIN_DISPLAY_MS = 1200;
    final int MAX_WAIT_MS = 5000;
    while ((!done.get() || System.currentTimeMillis() - startTime < MIN_DISPLAY_MS)
            && System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < WIDTH; i++) bar.append(i == pos ? '•' : '-');
        bar.append("]  Connecting to AtomicWFC for Online Play...");
        System.out.print("\r" + bar);
        pos += direction;
        if (pos == WIDTH - 1) direction = -1;
        if (pos == 0) direction = 1;
        Thread.sleep(DELAY_MS);
    }
    System.out.println("\r" + " ".repeat(75));
    return success.get();
}

static void Help() {
    System.out.println("""
\u001B[1;36m# TicTacJava — Help\u001B[0m

Basic usage:
  java Main                              Local 2-player game
  java Main --botplay --hard             Hard AI
  java Main --ui                         Full terminal menu UI
  java Main --resume --ui                Open the UI and resume a save

Game modes:
  --mode classic                         Normal Tic-Tac-Toe
  --mode misere                          Completing a winning line loses
  --mode connect                         Connect-3 on any board size
  --size <N>                             Board size, 3-20

Local game controls:
  row col                                Make a move, e.g. 1 2
  pause / p                              Save and return to menu
  save / s                               Save without leaving the game
  quit / q                               Quit the current game
  --resume                               Resume from savegame.properties

AI / turn options:
  --botplay                              Play against O
  --easy | --normal | --hard             AI difficulty
  --startplayer X|O                      Choose first player
  --timer <seconds>                      Turn time limit; 0 disables

Network:
  Build version: " + BUILD_VERSION + "
  java Main --server --rooms 8 --port 2000
  java Main --client 192.168.1.5 --room 3
  --rooms <N>                             Host multiple simultaneous rooms
  --port <PORT>                           TCP hosting/join port (default 2000)
  --room <ID>                             Join a specific room
  servers.ini                              Named servers: Name|Address|Port|Rooms

Friends:
  Friends are saved locally in friends.ini. Add a friend with their display name,
  hostname/IP address, and presence port (default 2002). Online status is checked
  directly between players; Internet friends may need port forwarding/firewall rules.

Appearance:
  --name <name>                           Player name
  --no-color                              Disable ANSI colors
  --color-x <COLOR>                       X color
  --color-o <COLOR>                       O color
  --color-grid <COLOR>                    Grid color

Stats:
  --scores                                Print scores/achievements and exit
  --reset-scores                          Reset all stats

Achievements include streaks, game/move milestones, board-size milestones,
mode mastery, hard-AI wins, timed play, online play, timeouts, and save/resume.
""");
}
