import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
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

    public TreasureIslandPlayer() {
        // nawiązanie połączenia z zarządcą gry
        try {
            socket = new Socket("localhost", 8888);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // utworzenie planszy wizualizującej stan gry
        setTitle("Wyspa skarbów - Gracz");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
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
                    take();
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
                    System.out.println(Arrays.deepToString(surroundings));
                    if (treasures >= 5) {
                        endGame();
                        System.exit(0);
                    }
                }
            });
            buttonsPanel.add(buttons[i]);
        }
        add(buttonsPanel);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setVisible(true);
    }

    public void move(int direction) {
        // wysłanie polecenia ruchu
        out.println("move");
        out.println(direction);
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

    public void take() {
        // wysłanie polecenia podniesienia skarbu
        out.println("take");
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