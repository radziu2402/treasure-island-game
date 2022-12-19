package Manager;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
public class TreasureIslandManager extends JFrame {

    public JLabel[][] board;
    public int[][] boardState;
    public ArrayList<Player> players;
    private final ArrayList<Thread> playerThreads;
    private ServerSocket serverSocket;
    private final JPanel playerListPanel;

    public TreasureIslandManager(int width, int height) {
        JPanel boardPanel = new JPanel();
        board = new JLabel[width][height];
        boardPanel.setLayout(new GridLayout(width, height));
        add(boardPanel);
        boardState = new int[width][height];
        players = new ArrayList<>();
        playerThreads = new ArrayList<>();
        // tworzenie planszy
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                board[i][j] = new JLabel("   ");
                board[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                boardPanel.add(board[i][j]);
            }
        }
        // losowanie przeszkód i skarbów na planszy
        Random rand = new Random();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int randNum = rand.nextInt(10);
                if (randNum == 0) {
                    board[i][j].setText("      X ");
                    boardState[i][j] = -1;
                } else if (randNum == 1 || randNum == 2) {
                    board[i][j].setText("      $ ");
                    boardState[i][j] = 1;
                }
            }
        }
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        add(playerListPanel, BorderLayout.SOUTH);
        setTitle("Wyspa skarbów - Zarządca");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);

        // inicjalizacja gniazda serwera
        try {
            serverSocket = new ServerSocket(8888);
            System.out.println("Zarządca: Oczekuję na graczy...");
            // utworzenie wątku dla każdego gracza
            while (true) {
                Socket playerSocket = serverSocket.accept();
                Player player = new Player(playerSocket, this);
                Thread playerThread = new Thread(player);
                playerThread.start();
                players.add(player);
                playerThreads.add(playerThread);
                addPlayer(player);
                System.out.println("Zarządca: Gracz dołączył!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public void addPlayer(Player player) {
        JLabel playerLabel = new JLabel(player.getName() + ": " + player.getTreasures() + " skarbów");
        playerListPanel.add(playerLabel);
        playerListPanel.revalidate();
    }
    public void updatePlayer(Player player) {
        // find the label for the player
        for (Component component : playerListPanel.getComponents()) {
            if (component instanceof JLabel playerLabel) {
                if (playerLabel.getText().startsWith(player.getName())) {
                    // update the label with the player's new score
                    playerLabel.setText(player.getName() + ": " + player.getTreasures() + " skarbów");
                    playerListPanel.revalidate();
                    break;
                }
            }
        }
    }

    public void movePlayer(Player player, int x, int y) {
        synchronized (TreasureIslandManager.class) {
            // sprawdzenie czy ruch jest możliwy
            if (x >= 0 && x < board.length && y >= 0 && y < board[0].length && boardState[x][y] != -1 && boardState[x][y] != 2 && boardState[x][y] != 3) {
                // aktualizacja pozycji gracza na planszy
                if (boardState[player.getX()][player.getY()] == 3) {
                    boardState[player.getX()][player.getY()] = 1;
                    board[player.getX()][player.getY()].setText("      $ ");
                } else {
                    boardState[player.getX()][player.getY()] = 0;
                    board[player.getX()][player.getY()].setText(" ");
                }
                player.setX(x);
                player.setY(y);
                if (boardState[x][y] != 1) {
                    board[x][y].setText("      P ");
                    boardState[x][y] = 2;
                }
            }
        }
    }
    public int[][] getBoardState(int x, int y) {
        // zwracanie stanu otoczenia gracza
        int[][] surroundings = new int[3][3];
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i >= 0 && i < board.length && j >= 0 && j < board[0].length) {
                    surroundings[i - x + 1][j - y + 1] = boardState[i][j];
                } else {
                    surroundings[i - x + 1][j - y + 1] = -1;
                }
            }
        }
        return surroundings;
    }
    public void notakeTreasure(Player player) {
        board[player.getX()][player.getY()].setText("    $/P ");
        boardState[player.getX()][player.getY()] = 3;
    }
    public void takeTreasure(Player player) {
        board[player.getX()][player.getY()].setText("      P ");
        boardState[player.getX()][player.getY()] = 2;
        player.addTreasure();
        updatePlayer(player);
        if (player.getTreasures() >= 5) {
            endGame();
            System.exit(0);
        }
        System.out.println("Zarządca: Gracz " + player.getName() + " podniósł skarb!");

    }



    public void endGame() {
        // zatrzymanie wątków graczy i zamknięcie gniazda serwera
        for (Thread playerThread : playerThreads) {
            playerThread.interrupt();
        }
        try {
            serverSocket.close();
            System.out.println("Zarządca: Koniec gry!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new TreasureIslandManager(10, 10);
    }
}

class Player implements Runnable {
    private final Socket socket;
    private Socket socketOut;
    private final TreasureIslandManager manager;
    private int x;
    private int y;
    private int treasures;
    private final String name;
    private int port;

    public Player(Socket socket, TreasureIslandManager manager) {
        this.socket = socket;
        this.manager = manager;
        // losowanie początkowej pozycji gracza
        Random rand = new Random();
        x = rand.nextInt(manager.board.length);
        y = rand.nextInt(manager.board[0].length);
        while (manager.boardState[x][y] == -1 || manager.boardState[x][y] == 2 || manager.boardState[x][y] == 1) {
            x = rand.nextInt(manager.board.length);
            y = rand.nextInt(manager.board[0].length);
        }
        // aktualizacja stanu planszy i ustawienie imienia gracza
        manager.boardState[x][y] = 2;
        manager.board[x][y].setText("      P ");
        System.out.println("x=" + x +"   y= " + y);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = in.readLine();
            port = Integer.parseInt(in.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getName() {
        return name;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void addTreasure() {
        treasures++;
    }

    public int getTreasures() {
        return treasures;
    }

    @Override
    public void run() {
        try {
            socketOut = new Socket("localhost",port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socketOut.getOutputStream(), true);
            while (true) {
                String input = in.readLine();
                // wykonanie polecenia ruchu gracza
                switch (input) {
                    case "move" -> {
                        int direction = Integer.parseInt(in.readLine());
                        if (direction == 0) {
                            manager.movePlayer(this, x - 1, y - 1);
                        } else if (direction == 1) {
                            manager.movePlayer(this, x - 1, y);
                        } else if (direction == 2) {
                            manager.movePlayer(this, x - 1, y + 1);
                        } else if (direction == 3) {
                            manager.movePlayer(this, x, y - 1);
                        } else if (direction == 4) {
                            manager.movePlayer(this, x, y + 1);
                        } else if (direction == 5) {
                            manager.movePlayer(this, x + 1, y - 1);
                        } else if (direction == 6) {
                            manager.movePlayer(this, x + 1, y);
                        } else if (direction == 7) {
                            manager.movePlayer(this, x + 1, y + 1);
                        }
                    }
                    // wykonanie polecenia zapytania o otoczenie gracza
                    case "see" -> {
                        int[][] surroundings = manager.getBoardState(x, y);
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                out.println(surroundings[i][j]);
                            }
                        }
                    }
                    // wykonanie polecenia podniesienia skarbu
                    case "take" -> manager.takeTreasure(this);
                    case "notake" -> manager.notakeTreasure(this);
                    case "getpos" -> {
                        out.println(x);
                        out.println(y);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}