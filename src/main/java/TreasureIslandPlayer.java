import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TreasureIslandPlayer extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int x;
    private int y;
    private int treasures;
    private JPanel boardPanel;
    private JLabel[][] board;
    private JButton[] buttons;
    private JLabel treasureCounter;
    private String playerName;
    private Timer timer;

    public TreasureIslandPlayer() {
        playerName = JOptionPane.showInputDialog(null, "Wpisz swoje imię:","Gra Wyspa Skarbów",JOptionPane.QUESTION_MESSAGE);
        if(playerName == null){
            System.exit(0);
        }
        // nawiązanie połączenia z zarządcą gry
        try {
            socket = new Socket("localhost", 8888);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(playerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // utworzenie planszy wizualizującej stan gry
        setTitle("Wyspa skarbów - Gracz " + playerName);
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        treasureCounter = new JLabel("Ilość skarbów: 0");
        add(treasureCounter);
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(3, 3));
        board = new JLabel[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = new JLabel(" ");
                board[i][j].setHorizontalAlignment(JLabel.CENTER);
                board[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                boardPanel.add(board[i][j]);
            }
        }
        add(boardPanel);
        // utworzenie przycisków do poruszania się gracza
        buttons = new JButton[8];
        buttons[0] = new JButton("↖");
        buttons[1] = new JButton("↑");
        buttons[2] = new JButton("↗");
        buttons[3] = new JButton("←");
        buttons[4] = new JButton("→");
        buttons[5] = new JButton("↙");
        buttons[6] = new JButton("↓");
        buttons[7] = new JButton("↘");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(1, 8));
        for (int i = 0; i < 8; i++) {
            final int direction = i;
            buttons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    move(direction);
                    int[][] surroundings = see();
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
                            board[i][j].setText(text);
                        }
                    }
                }
            });
            buttonsPanel.add(buttons[i]);
        }
        add(buttonsPanel);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        timer = new Timer(200, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshBoard();
            }
        });
        timer.start();
    }

    private void refreshBoard() {
        int[][] surroundings = see();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String text;
                if (i == 1 && j == 1) {
                    text = " P ";
                }
                else if (surroundings[i][j] == 0) {
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
                board[i][j].setText(text);
            }
        }
    }

    public void move(int direction) {
        // wysłanie polecenia ruchu
        out.println("move");
        out.println(direction);
        // pobranie informacji o otoczeniu po ruchu
        int[][] surroundings = see();
        // sprawdzenie czy na polu na które gracz się przemieścił jest skarb
        if (surroundings[1][1] == 1) {
            // wysłanie polecenia zabrania skarbu
            decideTake();
        }
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
                board[i][j].setText(text);
            }
        }
        // sprawdzenie czy gracz zebrał już 5 skarbów
        if (treasures >= 5) {
            JOptionPane.showMessageDialog(this, "Wygrałeś brawo !");
            endGame();
            System.exit(0);
        }
    }
    public int[][] see() {
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
    public void decideTake() {
        // pytanie gracza o podjęcie decyzji
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Czy chcesz podnieść skarb?",
                "Podjęcie decyzji",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            // wysłanie polecenia podniesienia skarbu
            out.println("take");
            treasures++;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            treasureCounter.setText("Ilość skarbów: " + treasures);
        } else {
            // wysłanie polecenia niepodniesienia skarbu
            out.println("notake");
        }
    }

    public void endGame() {
        // zakończenie gry
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TreasureIslandPlayer player = new TreasureIslandPlayer();
        int[][] surroundings = player.see();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String text;
                if (i == 1 && j == 1) {
                    text = " P ";
                }
                else if (surroundings[i][j] == 0) {
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
                player.board[i][j].setText(text);
            }
        }
    }
}