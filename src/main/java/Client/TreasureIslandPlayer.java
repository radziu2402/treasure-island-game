package Client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreasureIslandPlayer extends JFrame {
    private final Socket socketOut;
    private final ServerSocket socket;
    private final Socket socketIn;
    private final BufferedReader in;
    private final PrintWriter out;
    private static int x;
    private static int y;
    private int treasures;
    private final JLabel[][] board;
    private final JLabel treasureCounter;
    private final int port;

    public TreasureIslandPlayer() {
        String playerName = JOptionPane.showInputDialog(null, "Wpisz swoje imię:", "Gra Wyspa Skarbów", JOptionPane.QUESTION_MESSAGE);
        if(playerName == null){
            System.exit(0);
        }
        String portString = JOptionPane.showInputDialog(null, "Wpisz port:", "Gra Wyspa Skarbów", JOptionPane.QUESTION_MESSAGE);
        if(portString == null){
            System.exit(0);
        }
        port = Integer.parseInt(portString);
        // nawiązanie połączenia z zarządcą gry
        try {
            socketOut = new Socket("localhost", 8888);
            out = new PrintWriter(socketOut.getOutputStream(), true);
            out.println(playerName);
            out.println(port);
            socket = new ServerSocket(port);
            socketIn = socket.accept();
            in = new BufferedReader(new InputStreamReader(socketIn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // utworzenie planszy wizualizującej stan gry
        setTitle("Wyspa skarbów - Gracz " + playerName);
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        treasureCounter = new JLabel("Ilość skarbów: 0");
        add(treasureCounter);
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(10, 10));
        board = new JLabel[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                board[i][j] = new JLabel(" ");
                board[i][j].setHorizontalAlignment(JLabel.CENTER);
                board[i][j].setOpaque(true);
                board[i][j].setBackground(Color.GREEN);
                board[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                boardPanel.add(board[i][j]);
            }
        }
        add(boardPanel);
        getPosition();
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        int[][] surroundin = see();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String text;
                if (i == 1 && j == 1) {
                    text = " P ";
                } else if (surroundin[i][j] == 0) {
                    text = " ";
                } else if (surroundin[i][j] == 1) {
                    text = " $ ";
                } else if (surroundin[i][j] == -1) {
                    text = " X ";
                } else if (surroundin[i][j] == 2) {
                    text = " P ";
                } else {
                    text = "    $/P ";
                }
                if(i+x-1 > -1 && i+x-1 < 10 && j+y-1 >-1 && j+y-1 < 10) {
                    board[i + x - 1][j + y - 1].setText(text);
                    board[i + x - 1][j + y - 1].setBackground(Color.yellow);
                }
            }
        }
        while(true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
                    int direction = chooseDirection();
                    move(direction);
                    getPosition();
                    // sprawdzenie czy gracz zebrał już 5 skarbów
                    if (treasures >= 5) {
                        JOptionPane.showMessageDialog(this, "Wygrałeś brawo !");
                        endGame();
                        System.exit(0);
                    }
                    int[][] surroundings = see();
                    for (int i1 = 0; i1 < 3; i1++) {
                        for (int j = 0; j < 3; j++) {
                            String text;
                            if (surroundings[i1][j] == 0) {
                                text = " ";
                            } else if (surroundings[i1][j] == 1) {
                                text = " $ ";
                            } else if (surroundings[i1][j] == -1) {
                                text = " X ";
                            } else if (surroundings[i1][j] == 2) {
                                text = " P ";
                            } else {
                                text = "    $/P ";
                            }
                            if (i1 + x - 1 > -1 && i1 + x - 1 < 10 && j + y - 1 > -1 && j + y - 1 < 10) {
                                board[i1 + x - 1][j + y - 1].setText(text);
                                board[i1 + x - 1][j + y - 1].setBackground(Color.yellow);
                            }
                        }
                    }

        }
    }
    private int direct(int i, int j){
        if (i == 0 && j == 0) {
            return 0;
        } else if (i == 0 && j == 1) {
            return 1;
        } else if (i == 0 && j == 2) {
            return 2;
        } else if (i == 1 && j == 0) {
            return 3;
        } else if (i == 1 && j == 2) {
            return 4;
        } else if (i == 2 && j == 0) {
            return 5;
        } else if (i == 2 && j == 1) {
            return 6;
        } else if (i == 2 && j == 2) {
            return 7;
        }
        return 0;
    }
    private int chooseDirection() {
        int[][] surroundings = see();
        //szuka skarbu
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (surroundings[i][j] == 1) {
                    return direct(i,j);
                }
            }
        }
        //szuka pustego pola
        List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (surroundings[i][j] == 0) {
                    integers.add(direct(i,j));
                }
            }
        }
        Random rand = new Random();
        return integers.get(rand.nextInt(integers.size()));
    }

    private void move(int direction) {
        // wysłanie polecenia ruchu
        out.println("move");
        out.println(direction);
        // pobranie informacji o otoczeniu po ruchu
        getPosition();
        int[][] surroundings = see();
        // sprawdzenie czy na polu na które gracz się przemieścił jest skarb
        if (surroundings[1][1] == 1) {
            // wysłanie polecenia zabrania skarbu
            decideTake();
        }
        getPosition();
        // aktualizacja planszy wizualizującej stan gry
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String text;
                if (surroundings[i][j] == 0) {
                    text = " ";
                } else if (surroundings[i][j] == 1) {
                    text = " $ ";
                } else if (surroundings[i][j] == -1) {
                    text = " X ";
                } else if (surroundings[i][j] == 2){
                    text = " P ";
                }
                else {
                    text = "    $/P ";
                }
                if(i+x-1 > -1 && i+x-1 < 10 && j+y-1 >-1 && j+y-1 < 10) {
                    board[i + x - 1][j + y - 1].setText(text);
                    board[i + x - 1][j + y - 1].setBackground(Color.yellow);
                }
            }
        }
    }
    private int[][] see() {
        // wysłanie polecenia zapytania o otoczenie
        out.println("see");
        int[][] surroundings = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                try {
                    surroundings[i][j] = Integer.parseInt(in.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return surroundings;
    }
    private void decideTake() {
        // pytanie gracza o podjęcie decyzji
        Random rand = new Random();
        int delay = rand.nextInt(6) + 1;

        if (delay < 7 ) {
            // wysłanie polecenia podniesienia skarbu
            out.println("take");
            treasures++;
            try {
                Thread.sleep(delay*1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            treasureCounter.setText("Ilość skarbów: " + treasures);
        } else {
            // wysłanie polecenia niepodniesienia skarbu
            out.println("notake");
        }
    }

    private void endGame() {
        // zakończenie gry
        try {
            socketOut.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPosition(){
        out.println("getpos");
        try {
            x = Integer.parseInt(in.readLine());
            System.out.println("x= " + x);
            y = Integer.parseInt(in.readLine());
            System.out.println("y= " + y);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        TreasureIslandPlayer player = new TreasureIslandPlayer();
  }
}